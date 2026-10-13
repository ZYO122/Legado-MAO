package io.legado.app.help.update

import org.junit.Assert.assertEquals
import org.junit.Test

class AppReleaseInfoTest {

    @Test
    fun `main variant picks the asset without coexist suffix`() {
        val selected = selectCompatibleAssets(
            assets = listOf(
                asset("legado-1.0.0.apk"),
                asset("legado-1.0.0-A.apk"),
                asset("legado-1.0.0-B.apk")
            ),
            variantSuffix = ""
        )

        assertEquals(listOf("legado-1.0.0.apk"), selected.map { it.name })
    }

    @Test
    fun `coexist a picks the -A asset`() {
        val selected = selectCompatibleAssets(
            assets = listOf(
                asset("legado-1.0.0.apk"),
                asset("legado-1.0.0-A.apk"),
                asset("legado-1.0.0-B.apk")
            ),
            variantSuffix = "-A"
        )

        assertEquals(listOf("legado-1.0.0-A.apk"), selected.map { it.name })
    }

    @Test
    fun `coexist b picks the -B asset`() {
        val selected = selectCompatibleAssets(
            assets = listOf(
                asset("legado-1.0.0.apk"),
                asset("legado-1.0.0-A.apk"),
                asset("legado-1.0.0-B.apk")
            ),
            variantSuffix = "-B"
        )

        assertEquals(listOf("legado-1.0.0-B.apk"), selected.map { it.name })
    }

    @Test
    fun `beta version suffix is not mistaken for coexist marker`() {
        val selected = selectCompatibleAssets(
            assets = listOf(
                asset("legado-1.0.0-beta.1.apk"),
                asset("legado-1.0.0-beta.1-A.apk")
            ),
            variantSuffix = ""
        )

        assertEquals(listOf("legado-1.0.0-beta.1.apk"), selected.map { it.name })
    }

    @Test
    fun `ignores invalid assets`() {
        val selected = selectCompatibleAssets(
            assets = listOf(asset("legado-1.0.0.apk", state = "new")),
            variantSuffix = ""
        )

        assertEquals(emptyList<Asset>(), selected)
    }

    private fun asset(name: String, state: String = "uploaded") = Asset(
        apkUrl = "https://example.com/$name",
        contentType = "application/vnd.android.package-archive",
        createdAt = "2026-07-13T00:00:00Z",
        downloadCount = 0,
        id = name.hashCode(),
        name = name,
        state = state,
        url = "https://api.example.com/$name"
    )
}
