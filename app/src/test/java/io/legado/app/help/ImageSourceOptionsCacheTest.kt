package io.legado.app.help

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class ImageSourceOptionsCacheTest {

    @Test
    fun reusesParsedOptionsForSameSource() {
        val source = """https://example.com/r.png,{"click":"java.toast('ok')"}"""
        val first = ImageSourceOptions.parse(source)
        val second = ImageSourceOptions.parse(source)
        assertSame("同一 src 应命中缓存", first, second)
        assertEquals("https://example.com/r.png", first?.source)
        assertEquals("java.toast('ok')", first?.click)
    }

    @Test
    fun skipsCacheForLongSources() {
        val source = "data:image/svg+xml," + "a".repeat(4096)
        val first = ImageSourceOptions.parse(source)
        val second = ImageSourceOptions.parse(source)
        assertNotSame("超长 src 不应进缓存", first, second)
        assertEquals(source, first?.source)
    }

    @Test
    fun keepsParsingSemantics() {
        val styled = ImageSourceOptions.parse("""img.png,{"style":"TEXT"}""")
        assertEquals("img.png", styled?.source)
        assertEquals("TEXT", styled?.style)
        assertEquals(emptyMap<String, String>(), ImageSourceOptions.parse("plain.png")?.options)
        assertEquals(null, ImageSourceOptions.parse("  "))
    }
}
