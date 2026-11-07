package io.legado.app.data.repository.manga.translation

import android.app.Application
import android.graphics.*
import io.legado.app.domain.model.manga.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MangaInpaintPixelsTest {
    @Test fun `mask extraction leaves source untouched and protects neighbors`() {
        val bitmap = Bitmap.createBitmap(200, 150, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        try {
            val paint = Paint().apply { color = Color.BLACK }
            Canvas(bitmap).apply { drawRect(55f, 50f, 65f, 70f, paint); drawRect(90f, 50f, 100f, 70f, paint) }
            val box = MangaTextBox(250f, 300f, 550f, 500f)
            val region = MangaTranslatedRegion("a", "two", "字", box, typesetting = MangaAiTypesetting(listOf("字"), 100f))
            val before = IntArray(200 * 150).also { bitmap.getPixels(it, 0, 200, 0, 0, 200, 150) }
            val mask = MangaPageComposer.erasureMask(bitmap, MangaTranslationPlan(listOf(region)), listOf(Rect(85, 45, 105, 75)))
            assertTrue(mask[60 * 200 + 60]); assertFalse(mask[60 * 200 + 95]); assertFalse(mask[0])
            val after = IntArray(before.size).also { bitmap.getPixels(it, 0, 200, 0, 0, 200, 150) }
            assertArrayEquals(before, after)
            val repaired = mangaCompositeInpainting(before, IntArray(before.size) { Color.RED }, mask)
            assertEquals(Color.RED, repaired[60 * 200 + 60]); assertEquals(Color.BLACK, repaired[60 * 200 + 95])
            assertEquals(Color.WHITE, repaired[0])
        } finally { bitmap.recycle() }
    }

    @Test fun `old saved settings default to local and every downloadable model pins immutable bytes`() {
        assertEquals(MangaInpaintEngine.LOCAL, MangaTranslationPlan.json.decodeFromString<MangaTranslationSettings>("{}").inpaintEngine)
        assertEquals(MangaInpaintEngine.entries.filter { it.downloadable }.toSet(), MangaInpaintModelCatalog.files.keys)
        MangaInpaintModelCatalog.files.values.forEach {
            assertTrue(it.sha256.matches(Regex("[0-9a-f]{64}"))); assertTrue(it.bytes > 0)
            assertFalse(it.url.contains("/resolve/main/"))
        }
    }
}
