package com.micontrolfinanciero.app.core.domain.update

import com.micontrolfinanciero.app.core.model.UpdateChannel

/** Un archivo adjunto a una publicación de GitHub. */
data class ReleaseAsset(val name: String, val downloadUrl: String, val size: Long)

/** Una publicación (release) de GitHub, ya leída del JSON. */
data class ReleaseInfo(
    val tag: String,
    val title: String,
    val notes: String,
    val prerelease: Boolean,
    val draft: Boolean,
    val publishedAt: String,
    val assets: List<ReleaseAsset>,
)

/** Actualización disponible: el APK y su archivo de huella SHA-256. */
data class AvailableUpdate(
    val versionName: String,
    val versionCode: Long,
    val title: String,
    val notes: String,
    val prerelease: Boolean,
    val apk: ReleaseAsset,
    val checksum: ReleaseAsset,
)

/**
 * Decide si hay una actualización y cuál (sección 15 del proyecto). Reglas:
 *  - Solo el repositorio oficial y solo enlaces de descarga de ese repositorio.
 *  - Canal Estable: solo publicaciones normales. Canal Pruebas: también pre-releases.
 *  - El APK debe llamarse finance-<versión>-<versionCode>.apk y traer su .sha256.
 *  - Solo versiones con versionCode MAYOR al instalado (nunca bajar de versión).
 */
object UpdateSelector {

    const val OWNER = "abar3374"
    const val REPO = "MiControlFinanciero"
    const val RELEASES_API = "https://api.github.com/repos/$OWNER/$REPO/releases?per_page=30"
    const val DOWNLOAD_PREFIX = "https://github.com/$OWNER/$REPO/releases/download/"

    /** Tamaño máximo aceptado para un APK (protege la memoria y el almacenamiento). */
    const val MAX_APK_BYTES = 150L * 1024 * 1024

    private val APK_NAME = Regex("""^finance-(\d+\.\d+\.\d+)-(\d+)\.apk$""")
    private val SHA256 = Regex("""^[0-9a-fA-F]{64}$""")

    fun select(releases: List<ReleaseInfo>, channel: UpdateChannel, currentVersionCode: Long): AvailableUpdate? =
        releases.asSequence()
            .filter { !it.draft }
            .filter { channel == UpdateChannel.BETA || !it.prerelease }
            .mapNotNull { release -> candidate(release) }
            .filter { it.versionCode > currentVersionCode }
            .maxByOrNull { it.versionCode }

    private fun candidate(release: ReleaseInfo): AvailableUpdate? {
        val apk = release.assets.firstOrNull { APK_NAME.matches(it.name) } ?: return null
        if (!isOfficialUrl(apk.downloadUrl) || apk.size <= 0 || apk.size > MAX_APK_BYTES) return null
        val checksum = release.assets.firstOrNull { it.name == apk.name + ".sha256" } ?: return null
        if (!isOfficialUrl(checksum.downloadUrl)) return null
        val match = APK_NAME.matchEntire(apk.name) ?: return null
        return AvailableUpdate(
            versionName = match.groupValues[1],
            versionCode = match.groupValues[2].toLong(),
            title = release.title.ifBlank { release.tag },
            notes = release.notes.take(4_000),
            prerelease = release.prerelease,
            apk = apk,
            checksum = checksum,
        )
    }

    /** Solo descargas del repositorio oficial, por HTTPS, sin trucos de ruta. */
    fun isOfficialUrl(url: String): Boolean =
        url.startsWith(DOWNLOAD_PREFIX) && !url.contains("..") && !url.contains('@') && !url.contains('\\')

    /** Lee "hash  nombre" (formato de sha256sum) y devuelve el hash en minúsculas, o null. */
    fun parseChecksum(content: String, apkName: String): String? {
        val line = content.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: return null
        val parts = line.split(Regex("\\s+"))
        val hash = parts.firstOrNull() ?: return null
        if (!SHA256.matches(hash)) return null
        val name = parts.getOrNull(1)?.removePrefix("*")
        if (name != null && name != apkName) return null
        return hash.lowercase()
    }

    fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
}
