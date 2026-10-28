package io.legado.app.help.storage

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadStyleAssetRefsTest {

    @Test
    fun `只收集图片类型的背景与四种字体`() {
        val refs = collectReadStyleAssetRefs(
            """
            [{"bgType":2,"bgStr":"/bg/day.jpg","bgTypeNight":0,"bgStrNight":"#000000",
              "bgTypeEInk":2,"bgStrEInk":"ink.png","textFont":"file:///f/a.ttf","titleFont":"",
              "headerFont":"file:///f/h.ttf","footerFont":"file:///f/f.ttf"}]
            """.trimIndent()
        )
        assertEquals(listOf("/bg/day.jpg", "ink.png"), refs.backgrounds)
        assertEquals(listOf("file:///f/a.ttf", "file:///f/h.ttf", "file:///f/f.ttf"), refs.fonts)
    }

    @Test
    fun `单对象共享排版同样收集且去重`() {
        val json = """{"bgType":2,"bgStr":"bg/a.jpg","textFont":"a.ttf"}"""
        val refs = collectReadStyleAssetRefs(json, json)
        assertEquals(listOf("bg/a.jpg"), refs.backgrounds)
        assertEquals(listOf("a.ttf"), refs.fonts)
    }

    @Test
    fun `空值与非法 json 不抛异常`() {
        val refs = collectReadStyleAssetRefs("not json", "[]", "")
        assertTrue(refs.backgrounds.isEmpty())
        assertTrue(refs.fonts.isEmpty())
        assertEquals("not json", rewriteReadStyleAssetRefs("not json", { null }, { null }))
    }

    @Test
    fun `命中目标文件时改写背景与字体引用`() {
        val json = """
            [{"bgType":2,"bgStr":"/old/bg/a.jpg","bgTypeNight":0,"bgStrNight":"#000000",
              "textFont":"content://font/a.ttf","titleFont":""}]
        """.trimIndent()
        val rewritten = rewriteReadStyleAssetRefs(
            json = json,
            resolveBackground = { "/new/bg/" + it.substringAfterLast('/') },
            resolveFont = { "file:///new/font/" + it.substringAfterLast('/') },
        )
        val config = JsonParser.parseString(rewritten).asJsonArray[0].asJsonObject
        assertEquals("/new/bg/a.jpg", config.get("bgStr").asString)
        assertEquals("#000000", config.get("bgStrNight").asString)
        assertEquals("file:///new/font/a.ttf", config.get("textFont").asString)
        assertEquals("", config.get("titleFont").asString)
    }

    @Test
    fun `解析不到文件时保持原引用`() {
        val json = """[{"bgType":2,"bgStr":"content://img/a.jpg","textFont":"a.ttf"}]"""
        val rewritten = rewriteReadStyleAssetRefs(json, { null }, { null })
        val config = JsonParser.parseString(rewritten).asJsonArray[0].asJsonObject
        assertEquals("content://img/a.jpg", config.get("bgStr").asString)
        assertEquals("a.ttf", config.get("textFont").asString)
    }

    @Test
    fun `颜色背景不受改写影响`() {
        val json = """[{"bgType":0,"bgStr":"#EEEEEE"}]"""
        val rewritten = rewriteReadStyleAssetRefs(json, { "/new/bg/x" }, { null })
        val config = JsonParser.parseString(rewritten).asJsonArray[0].asJsonObject
        assertEquals("#EEEEEE", config.get("bgStr").asString)
    }
}
