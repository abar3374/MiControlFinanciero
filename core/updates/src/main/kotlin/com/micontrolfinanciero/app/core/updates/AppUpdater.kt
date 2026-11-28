package com.micontrolfinanciero.app.core.updates

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.micontrolfinanciero.app.core.domain.update.AvailableUpdate
import com.micontrolfinanciero.app.core.domain.update.ReleaseAsset
import com.micontrolfinanciero.app.core.domain.update.ReleaseInfo
import com.micontrolfinanciero.app.core.domain.update.UpdateSelector
import com.micontrolfinanciero.app.core.model.UpdateChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.coroutineContext

/** Error de actualización con un mensaje para mostrar al usuario. */
class UpdateException(message: String) : Exception(message)

/**
 * Actualizador (Fase 9).
 *
 * Qué envía a internet: solo una consulta GET a la API pública de GitHub y la descarga del APK.
 * Nunca envía datos financieros ni personales.
 *
 * Antes de ofrecer la instalación verifica:
 *  1. que el enlace es del repositorio oficial y por HTTPS,
 *  2. que el SHA-256 del APK coincide con el .sha256 publicado,
 *  3. que el APK es de esta misma app (mismo paquete),
 *  4. que su versionCode es mayor al instalado,
 *  5. que está firmado con el MISMO certificado que la app instalada (y, en la versión oficial,
 *     con el certificado fijo de Finance). Si alguien publicara un APK con otra firma, se rechaza.
 * La instalación la confirma siempre el usuario en la ventana de Android: no hay instalación silenciosa.
 */
class AppUpdater(private val context: Context) {

    private val folder: File get() = File(context.cacheDir, "updates").apply { mkdirs() }

    fun currentVersionCode(): Long = packageInfo(context.packageName, 0)?.let(::versionCodeOf) ?: 0L

    suspend fun check(channel: UpdateChannel): AvailableUpdate? = withContext(Dispatchers.IO) {
        val json = httpGetText(UpdateSelector.RELEASES_API, maxBytes = 2 * 1024 * 1024)
        UpdateSelector.select(parseReleases(json), channel, currentVersionCode())
    }

    /** Descarga y verifica el APK. Devuelve el archivo listo para instalar. */
    suspend fun download(update: AvailableUpdate, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        folder.listFiles()?.forEach { it.delete() }
        val expected = UpdateSelector.parseChecksum(httpGetText(update.checksum.downloadUrl, maxBytes = 4_096), update.apk.name)
            ?: throw UpdateException("La publicación no trae una huella SHA-256 válida.")

        val target = File(folder, "finance-update.apk")
        val digest = MessageDigest.getInstance("SHA-256")
        val connection = open(update.apk.downloadUrl)
        try {
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: update.apk.size
            if (total > UpdateSelector.MAX_APK_BYTES) throw UpdateException("El archivo es demasiado grande.")
            var read = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        read += n
                        if (read > UpdateSelector.MAX_APK_BYTES) throw UpdateException("El archivo es demasiado grande.")
                        digest.update(buffer, 0, n)
                        out.write(buffer, 0, n)
                        onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        } catch (e: Exception) {
            target.delete()
            throw e
        } finally {
            connection.disconnect()
        }

        if (UpdateSelector.toHex(digest.digest()) != expected) {
            target.delete()
            throw UpdateException("El archivo descargado no coincide con su huella SHA-256. Se borró por seguridad.")
        }
        verifyApk(target, update)
        target
    }

    /** ¿Android permite que esta app abra el instalador? (Ajuste "Instalar apps desconocidas"). */
    fun canRequestInstall(): Boolean =
        Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /** Abre el instalador de Android: el usuario ve los datos y confirma. */
    fun launchInstaller(apk: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // ------------------------------------------------------------------ verificación

    private fun verifyApk(file: File, update: AvailableUpdate) {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        @Suppress("DEPRECATION")
        val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
        fun reject(message: String): Nothing { file.delete(); throw UpdateException(message) }

        if (archive == null) reject("El archivo descargado no es un APK válido.")
        if (archive.packageName != context.packageName) reject("El APK es de otra aplicación. Se rechazó.")
        val code = versionCodeOf(archive)
        if (code != update.versionCode) reject("La versión del APK no coincide con la publicada.")
        if (code <= currentVersionCode()) reject("No se permite instalar una versión igual o anterior.")

        val installed = certificates(packageInfo(context.packageName, flags))
        val downloaded = certificates(archive)
        if (downloaded.isEmpty() || downloaded != installed) reject("El APK no está firmado por Finance. Se rechazó.")
        // La versión oficial (no la de desarrollo) además exige el certificado fijo.
        if (!context.packageName.endsWith(".debug") && OFFICIAL_CERT_SHA256 !in downloaded) {
            reject("El certificado de firma no es el oficial de Finance. Se rechazó.")
        }
    }

    @Suppress("DEPRECATION")
    private fun certificates(info: PackageInfo?): Set<String> {
        if (info == null) return emptySet()
        val sigs = if (Build.VERSION.SDK_INT >= 28) {
            val signing = info.signingInfo ?: return emptySet()
            if (signing.hasMultipleSigners()) signing.apkContentsSigners else signing.signingCertificateHistory
        } else {
            info.signatures
        } ?: return emptySet()
        return sigs.map { UpdateSelector.toHex(MessageDigest.getInstance("SHA-256").digest(it.toByteArray())) }.toSet()
    }

    @Suppress("DEPRECATION")
    private fun versionCodeOf(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    private fun packageInfo(name: String, flags: Int): PackageInfo? = runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(name, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(name, flags)
        }
    }.getOrNull()

    // ------------------------------------------------------------------ red

    private fun open(url: String): HttpURLConnection {
        if (!url.startsWith("https://")) throw UpdateException("Solo se permiten conexiones seguras (HTTPS).")
        val connection = URL(url).openConnection() as? HttpsURLConnection ?: throw UpdateException("Conexión no segura.")
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "Finance-Android")
        val status = try {
            connection.responseCode
        } catch (e: IOException) {
            connection.disconnect()
            throw UpdateException("Sin conexión a internet o GitHub no responde.")
        }
        // Las redirecciones de GitHub van a sus servidores de archivos; nunca se acepta bajar a HTTP.
        if (connection.url.protocol != "https") {
            connection.disconnect()
            throw UpdateException("La descarga fue redirigida a una conexión no segura.")
        }
        if (status == 403 || status == 429) {
            connection.disconnect()
            throw UpdateException("GitHub limitó las consultas por un rato. Intenta de nuevo más tarde.")
        }
        if (status !in 200..299) {
            connection.disconnect()
            throw UpdateException("GitHub respondió con un error ($status).")
        }
        return connection
    }

    private fun httpGetText(url: String, maxBytes: Int): String {
        val connection = open(url)
        try {
            val bytes = connection.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                    if (out.size() > maxBytes) throw UpdateException("Respuesta demasiado grande.")
                }
                out.toByteArray()
            }
            return String(bytes, Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseReleases(json: String): List<ReleaseInfo> = runCatching {
        val array = JSONArray(json)
        (0 until array.length()).map { i ->
            val r = array.getJSONObject(i)
            val assets = r.optJSONArray("assets") ?: JSONArray()
            ReleaseInfo(
                tag = r.optString("tag_name"),
                title = r.optString("name"),
                notes = r.optString("body"),
                prerelease = r.optBoolean("prerelease"),
                draft = r.optBoolean("draft"),
                publishedAt = r.optString("published_at"),
                assets = (0 until assets.length()).map { j ->
                    val a = assets.getJSONObject(j)
                    ReleaseAsset(a.optString("name"), a.optString("browser_download_url"), a.optLong("size"))
                },
            )
        }
    }.getOrElse { throw UpdateException("No se pudo leer la respuesta de GitHub.") }

    companion object {
        /** SHA-256 del certificado fijo con que se firman las versiones oficiales de Finance. */
        const val OFFICIAL_CERT_SHA256 = "cd56e0e1d9367ce8b3f88a829a2d0f839311d5b0c2bcdc07616fbc33e90d7338"
    }
}
