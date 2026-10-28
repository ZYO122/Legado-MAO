package io.legado.app.help.storage

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.legado.app.utils.GSON

/**
 * 阅读样式 json 里指向本地文件的引用。
 *
 * 整体备份只写 json，背景图与字体文件本身不跟着进包，换机恢复后就只剩「配置还在、
 * 文件没有」的空白背景与默认字体，所以备份/恢复两侧都要按这份引用搬运文件。
 */
internal data class ReadStyleAssetRefs(
    val backgrounds: List<String>,
    val fonts: List<String>,
)

/** 背景图引用只在类型为 2（其它图片）时指向本地文件，颜色与 assets 图不需要搬运。 */
private val backgroundRefFields = listOf(
    "bgType" to "bgStr",
    "bgTypeNight" to "bgStrNight",
    "bgTypeEInk" to "bgStrEInk",
)

private val fontRefFields = listOf("textFont", "titleFont", "headerFont", "footerFont")

/** 收集 [jsons]（readConfig.json 与 shareReadConfig.json 的原文）里引用到的背景图与字体。 */
internal fun collectReadStyleAssetRefs(vararg jsons: String): ReadStyleAssetRefs {
    val backgrounds = LinkedHashSet<String>()
    val fonts = LinkedHashSet<String>()
    jsons.forEach { json ->
        configObjects(json).forEach { config ->
            backgroundRefFields.forEach { (typeKey, valueKey) ->
                if (config.intValue(typeKey) == 2) {
                    config.stringValue(valueKey).takeIf(String::isNotBlank)?.let(backgrounds::add)
                }
            }
            fontRefFields.forEach { key ->
                config.stringValue(key).takeIf(String::isNotBlank)?.let(fonts::add)
            }
        }
    }
    return ReadStyleAssetRefs(backgrounds.toList(), fonts.toList())
}

/**
 * 把 json 里的文件引用改写成新设备上的值。
 *
 * [resolveBackground] / [resolveFont] 返回 null 表示目标机上没有对应文件，原引用保持不动，
 * 交给阅读器现有的回落逻辑。
 */
internal fun rewriteReadStyleAssetRefs(
    json: String,
    resolveBackground: (String) -> String?,
    resolveFont: (String) -> String?,
): String {
    val root = parseJson(json) ?: return json
    configObjects(root).forEach { config ->
        backgroundRefFields.forEach { (typeKey, valueKey) ->
            if (config.intValue(typeKey) == 2) {
                rewriteRef(config, valueKey, resolveBackground)
            }
        }
        fontRefFields.forEach { key -> rewriteRef(config, key, resolveFont) }
    }
    return GSON.toJson(root)
}

private fun rewriteRef(config: JsonObject, key: String, resolve: (String) -> String?) {
    val reference = config.stringValue(key)
    if (reference.isBlank()) return
    resolve(reference)?.let { config.addProperty(key, it) }
}

private fun JsonObject.stringValue(key: String): String {
    val element = get(key) ?: return ""
    return if (element.isJsonPrimitive) element.asString else ""
}

private fun JsonObject.intValue(key: String): Int {
    val element = get(key) ?: return 0
    return runCatching { element.asInt }.getOrDefault(0)
}

private fun parseJson(json: String): JsonElement? =
    runCatching { JsonParser.parseString(json) }.getOrNull()

private fun configObjects(json: String): List<JsonObject> =
    parseJson(json)?.let(::configObjects).orEmpty()

private fun configObjects(element: JsonElement): List<JsonObject> = when {
    element.isJsonArray -> element.asJsonArray.mapNotNull { it as? JsonObject }
    element.isJsonObject -> listOf(element.asJsonObject)
    else -> emptyList()
}
