package com.micontrolfinanciero.app.core.data.finance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.micontrolfinanciero.app.core.database.dao.AttachmentDao
import com.micontrolfinanciero.app.core.database.dao.AuditDao
import com.micontrolfinanciero.app.core.database.entity.AttachmentEntity
import com.micontrolfinanciero.app.core.database.entity.AuditLogEntity
import com.micontrolfinanciero.app.core.domain.finance.AttachmentRepository
import com.micontrolfinanciero.app.core.model.Attachment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Comprobantes: se copian a filesDir/attachments (carpeta privada, sin permisos de almacenamiento).
 * Las fotos se reducen a 1600 px en JPEG; los PDF se copian tal cual (máximo 10 MB).
 */
@Singleton
class RoomAttachmentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AttachmentDao,
    private val auditDao: AuditDao,
) : AttachmentRepository {

    private val folder: File get() = File(context.filesDir, "attachments").apply { mkdirs() }

    override fun observeForExpense(expenseId: Long): Flow<List<Attachment>> =
        dao.observeForExpense(expenseId).map { list -> list.map { it.toModel() } }

    override suspend fun add(expenseId: Long, sourceUri: String): Attachment = withContext(Dispatchers.IO) {
        val uri = Uri.parse(sourceUri)
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val name = displayName(uri) ?: "comprobante"
        val stamp = System.currentTimeMillis()
        val file = when {
            mime.startsWith("image/") -> {
                val target = File(folder, "receipt_$stamp.jpg")
                saveImage(uri, target)
                target
            }
            mime == "application/pdf" -> {
                val target = File(folder, "receipt_$stamp.pdf")
                resolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output ->
                        val copied = input.copyTo(output)
                        if (copied > MAX_PDF_BYTES) {
                            target.delete()
                            throw IllegalArgumentException("El PDF supera 10 MB.")
                        }
                    }
                } ?: throw IllegalArgumentException("No se pudo abrir el archivo")
                target
            }
            else -> throw IllegalArgumentException("Solo se admiten fotos o PDF.")
        }
        val storedMime = if (mime.startsWith("image/")) "image/jpeg" else mime
        val entity = AttachmentEntity(expenseId = expenseId, filePath = file.absolutePath, mimeType = storedMime, displayName = name, createdAt = stamp)
        val id = dao.insert(entity)
        auditDao.insert(AuditLogEntity(entity = "attachment", entityId = id, action = "CREATE", details = "gasto=$expenseId; tipo=$storedMime", at = stamp))
        entity.copy(id = id).toModel()
    }

    override suspend fun delete(attachmentId: Long) {
        withContext(Dispatchers.IO) {
            val entity = dao.get(attachmentId) ?: return@withContext
            dao.delete(attachmentId)
            val file = File(entity.filePath)
            if (file.parentFile?.canonicalPath == folder.canonicalPath) file.delete()
            auditDao.insert(
                AuditLogEntity(entity = "attachment", entityId = attachmentId, action = "DELETE", details = "gasto=${entity.expenseId}", at = System.currentTimeMillis()),
            )
        }
    }

    private fun saveImage(uri: Uri, target: File) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) throw IllegalArgumentException("La imagen no se pudo leer")
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IllegalArgumentException("La imagen no se pudo leer")
        val largest = max(bitmap.width, bitmap.height)
        val scaled = if (largest > MAX_SIDE) {
            val f = MAX_SIDE.toFloat() / largest
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * f).toInt(), (bitmap.height * f).toInt(), true)
        } else {
            bitmap
        }
        target.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 82, it) }
    }

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

    private fun AttachmentEntity.toModel() = Attachment(id, expenseId, filePath, mimeType, displayName)

    private companion object {
        const val MAX_SIDE = 1600
        const val MAX_PDF_BYTES = 10L * 1024 * 1024
    }
}
