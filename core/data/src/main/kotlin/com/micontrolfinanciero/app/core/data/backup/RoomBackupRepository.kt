package com.micontrolfinanciero.app.core.data.backup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.sqlite.db.SupportSQLiteDatabase
import com.micontrolfinanciero.app.core.database.FinanceDatabase
import com.micontrolfinanciero.app.core.domain.backup.BackupCrypto
import com.micontrolfinanciero.app.core.domain.backup.BackupException
import com.micontrolfinanciero.app.core.domain.backup.BackupInfo
import com.micontrolfinanciero.app.core.domain.backup.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Respaldo completo: todas las tablas de la base de datos y los archivos privados
 * (foto de perfil y comprobantes), en JSON cifrado con [BackupCrypto].
 *
 * La restauración se hace en UNA transacción: si algo falla, los datos actuales quedan intactos.
 * Un respaldo de una versión más nueva de la base de datos se rechaza; uno más viejo se acepta
 * (las columnas nuevas toman su valor por defecto).
 */
@Singleton
class RoomBackupRepository @Inject constructor(
    private val db: FinanceDatabase,
    @ApplicationContext private val context: Context,
) : BackupRepository {

    private val fileFolders = listOf("profile", "attachments")

    override suspend fun createBackup(password: CharArray, appVersion: String): ByteArray = withContext(Dispatchers.IO) {
        val sdb = db.openHelper.writableDatabase
        val root = JSONObject()
        root.put("format", 1)
        root.put("createdAt", System.currentTimeMillis())
        root.put("appVersion", appVersion)
        root.put("schemaVersion", sdb.version)
        root.put("filesDir", context.filesDir.absolutePath)

        val tables = JSONObject()
        userTables(sdb).forEach { table ->
            val rows = JSONArray()
            sdb.query("SELECT * FROM `$table`").use { c ->
                while (c.moveToNext()) {
                    val row = JSONObject()
                    for (i in 0 until c.columnCount) row.put(c.getColumnName(i), cellValue(c, i))
                    rows.put(row)
                }
            }
            tables.put(table, rows)
        }
        root.put("tables", tables)

        val files = JSONArray()
        fileFolders.forEach { folder ->
            File(context.filesDir, folder).listFiles()?.filter { it.isFile }?.forEach { f ->
                files.put(JSONObject().put("path", "$folder/${f.name}").put("data", Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)))
            }
        }
        root.put("files", files)

        val plain = root.toString().toByteArray(Charsets.UTF_8)
        val result = BackupCrypto.encrypt(plain, password)
        audit(sdb, "CREATE", "Respaldo cifrado creado (${countRows(tables)} registros, ${files.length()} archivos)")
        result
    }

    override suspend fun inspect(data: ByteArray, password: CharArray): BackupInfo = withContext(Dispatchers.IO) {
        info(parse(data, password))
    }

    override suspend fun restore(data: ByteArray, password: CharArray): BackupInfo = withContext(Dispatchers.IO) {
        val root = parse(data, password)
        val info = info(root)
        val sdb = db.openHelper.writableDatabase
        if (info.schemaVersion > sdb.version) {
            throw BackupException("El respaldo es de una versión más nueva de Finance. Actualiza la app y vuelve a intentarlo.")
        }
        val oldFilesDir = root.optString("filesDir")
        val newFilesDir = context.filesDir.absolutePath
        val tables = root.getJSONObject("tables")

        // 1) Base de datos en una sola transacción.
        db.runInTransaction {
            sdb.execSQL("PRAGMA defer_foreign_keys = ON")
            val current = userTables(sdb)
            current.forEach { sdb.execSQL("DELETE FROM `$it`") }
            current.filter { tables.has(it) }.forEach { table ->
                val columns = columnsOf(sdb, table)
                val rows = tables.getJSONArray(table)
                for (r in 0 until rows.length()) {
                    val row = rows.getJSONObject(r)
                    val values = ContentValues()
                    row.keys().forEach { key ->
                        if (key in columns) putValue(values, key, row.get(key), oldFilesDir, newFilesDir)
                    }
                    sdb.insert(table, SQLiteDatabase.CONFLICT_REPLACE, values)
                }
            }
            audit(sdb, "UPDATE", "Datos restaurados desde un respaldo del ${java.time.Instant.ofEpochMilli(info.createdAt)} (versión ${info.appVersion})")
        }

        // 2) Archivos: se reemplazan las carpetas privadas por las del respaldo.
        fileFolders.forEach { folder -> File(context.filesDir, folder).listFiles()?.forEach { it.delete() } }
        val files = root.optJSONArray("files") ?: JSONArray()
        for (i in 0 until files.length()) {
            val f = files.getJSONObject(i)
            val path = f.getString("path")
            val folder = path.substringBefore('/')
            val name = path.substringAfter('/')
            // Solo carpetas conocidas y nombres simples: un respaldo no puede escribir fuera de ellas.
            if (folder !in fileFolders || name.isBlank() || name.contains('/') || name.contains("..")) continue
            val dir = File(context.filesDir, folder).apply { mkdirs() }
            File(dir, name).writeBytes(Base64.decode(f.getString("data"), Base64.NO_WRAP))
        }
        info
    }

    // ------------------------------------------------------------------ apoyo

    private fun parse(data: ByteArray, password: CharArray): JSONObject {
        val plain = BackupCrypto.decrypt(data, password)
        return try {
            JSONObject(String(plain, Charsets.UTF_8)).also {
                if (it.optInt("format") != 1 || !it.has("tables")) throw BackupException("El respaldo está dañado.")
            }
        } catch (e: JSONException) {
            throw BackupException("El respaldo está dañado.")
        }
    }

    private fun info(root: JSONObject) = BackupInfo(
        createdAt = root.optLong("createdAt"),
        appVersion = root.optString("appVersion"),
        schemaVersion = root.optInt("schemaVersion"),
        rows = countRows(root.getJSONObject("tables")),
        files = root.optJSONArray("files")?.length() ?: 0,
    )

    private fun countRows(tables: JSONObject): Int = tables.keys().asSequence().sumOf { tables.getJSONArray(it).length() }

    /** Tablas de datos del usuario (sin las internas de SQLite y Room). */
    private fun userTables(sdb: SupportSQLiteDatabase): List<String> =
        sdb.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }.filterNot { it.startsWith("sqlite_") || it.startsWith("android_") || it == "room_master_table" }

    private fun columnsOf(sdb: SupportSQLiteDatabase, table: String): Set<String> =
        sdb.query("PRAGMA table_info(`$table`)").use { c ->
            val idx = c.getColumnIndexOrThrow("name")
            buildSet { while (c.moveToNext()) add(c.getString(idx)) }
        }

    private fun cellValue(c: Cursor, i: Int): Any = when (c.getType(i)) {
        Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
        Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
        Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
        Cursor.FIELD_TYPE_BLOB -> JSONObject().put("\$b", Base64.encodeToString(c.getBlob(i), Base64.NO_WRAP))
        else -> c.getString(i)
    }

    private fun putValue(values: ContentValues, key: String, value: Any?, oldDir: String, newDir: String) {
        when (value) {
            null, JSONObject.NULL -> values.putNull(key)
            is Int -> values.put(key, value.toLong())
            is Long -> values.put(key, value)
            is Double -> values.put(key, value)
            is Boolean -> values.put(key, if (value) 1L else 0L)
            is JSONObject -> values.put(key, Base64.decode(value.optString("\$b"), Base64.NO_WRAP))
            is String -> values.put(
                key,
                // Rutas de archivos privados: se ajustan a la carpeta de este teléfono.
                if ((key == "file_path" || key == "photo_path") && oldDir.isNotEmpty() && value.startsWith(oldDir)) {
                    newDir + value.removePrefix(oldDir)
                } else {
                    value
                },
            )
            else -> values.put(key, value.toString())
        }
    }

    private fun audit(sdb: SupportSQLiteDatabase, action: String, details: String) {
        sdb.insert(
            "audit_log",
            SQLiteDatabase.CONFLICT_NONE,
            ContentValues().apply {
                put("entity", "backup")
                put("entity_id", 0L)
                put("action", action)
                put("details", details)
                put("at", System.currentTimeMillis())
            },
        )
    }
}
