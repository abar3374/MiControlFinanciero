package com.micontrolfinanciero.app.core.domain.update

import com.micontrolfinanciero.app.core.model.UpdateChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSelectorTest {

    private val base = UpdateSelector.DOWNLOAD_PREFIX

    private fun release(tag: String, name: String, code: Int, pre: Boolean, url: String = base) = ReleaseInfo(
        tag = tag, title = tag, notes = "", prerelease = pre, draft = false, publishedAt = "",
        assets = listOf(
            ReleaseAsset("finance-$name-$code.apk", "$url$tag/finance-$name-$code.apk", 9_000_000),
            ReleaseAsset("finance-$name-$code.apk.sha256", "$url$tag/finance-$name-$code.apk.sha256", 90),
        ),
    )

    private val releases = listOf(
        release("v0.9.0-9-beta", "0.9.0", 9, pre = true),
        release("v0.8.0", "0.8.0", 8, pre = false),
        release("v0.7.0-7-beta", "0.7.0", 7, pre = true),
    )

    @Test
    fun `canal estable ignora pre-releases y canal pruebas toma la mas nueva`() {
        assertEquals(8L, UpdateSelector.select(releases, UpdateChannel.STABLE, 7)?.versionCode)
        assertEquals(9L, UpdateSelector.select(releases, UpdateChannel.BETA, 7)?.versionCode)
    }

    @Test
    fun `nunca baja de version ni repite la instalada`() {
        assertNull(UpdateSelector.select(releases, UpdateChannel.BETA, 9))
        assertNull(UpdateSelector.select(releases, UpdateChannel.STABLE, 8))
    }

    @Test
    fun `rechaza enlaces de otro repositorio y archivos sin sha256`() {
        val ajeno = release("v2.0.0", "2.0.0", 20, pre = false, url = "https://github.com/otro/repo/releases/download/")
        assertNull(UpdateSelector.select(listOf(ajeno), UpdateChannel.STABLE, 1))
        val sinHash = release("v2.0.0", "2.0.0", 20, pre = false).let { it.copy(assets = it.assets.take(1)) }
        assertNull(UpdateSelector.select(listOf(sinHash), UpdateChannel.STABLE, 1))
        assertTrue(!UpdateSelector.isOfficialUrl(base + "../../otro/x.apk"))
    }

    @Test
    fun `lee el archivo sha256`() {
        val h = "CD56E0E1D9367CE8B3F88A829A2D0F839311D5B0C2BCDC07616FBC33E90D7338"
        assertEquals(h.lowercase(), UpdateSelector.parseChecksum("$h  finance-0.8.0-8.apk\n", "finance-0.8.0-8.apk"))
        assertNull(UpdateSelector.parseChecksum("$h  otro.apk", "finance-0.8.0-8.apk"))
        assertNull(UpdateSelector.parseChecksum("hola", "finance-0.8.0-8.apk"))
    }
}
