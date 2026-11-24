package com.micontrolfinanciero.app.core.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.micontrolfinanciero.app.core.domain.profile.ProfilePhotoStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Guarda la foto de perfil en la carpeta privada de la app (ninguna otra app la ve).
 * La reduce a máximo 512 px y la guarda como JPEG para que pese poco.
 * No necesita permisos: la imagen llega desde el selector de fotos de Android.
 */
@Singleton
class FileProfilePhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) : ProfilePhotoStorage {

    private val folder: File get() = File(context.filesDir, "profile").apply { mkdirs() }

    override suspend fun save(sourceUri: String): String = withContext(Dispatchers.IO) {
        val uri = Uri.parse(sourceUri)
        val resolver = context.contentResolver

        // 1) Leer solo el tamaño para decidir cuánto reducir sin cargar la imagen completa.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw IllegalArgumentException("No se pudo abrir la imagen")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IllegalArgumentException("El archivo no es una imagen")

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2

        // 2) Cargar la imagen ya reducida.
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IllegalArgumentException("No se pudo leer la imagen")

        // 3) Girarla si la cámara la guardó de lado, y ajustarla a 512 px.
        val rotated = rotateIfNeeded(decoded, readRotation(uri))
        val scaled = scaleToMax(rotated, MAX_SIDE)

        // 4) Guardar como archivo nuevo (el nombre cambia para que la pantalla se refresque).
        val file = File(folder, "photo_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        file.absolutePath
    }

    override suspend fun delete(path: String) {
        withContext(Dispatchers.IO) {
            val file = File(path)
            // Solo se borran archivos dentro de la carpeta propia del perfil.
            if (file.parentFile?.canonicalPath == folder.canonicalPath) file.delete()
        }
    }

    private fun readRotation(uri: Uri): Int = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    } catch (e: Exception) {
        0
    }

    private fun rotateIfNeeded(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleToMax(bitmap: Bitmap, maxSide: Int): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= maxSide) return bitmap
        val factor = maxSide.toFloat() / largest
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * factor).toInt(), (bitmap.height * factor).toInt(), true)
    }

    private companion object {
        const val MAX_SIDE = 512
        const val JPEG_QUALITY = 85
    }
}
