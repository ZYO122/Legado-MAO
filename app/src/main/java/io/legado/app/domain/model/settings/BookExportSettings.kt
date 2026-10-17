package io.legado.app.domain.model.settings

data class BookExportSettings(
    val bookExportFileName: String? = null,
    val episodeExportFileName: String = "",
    val exportCharset: String = "UTF-8",
    val exportUseReplace: Boolean = true,
    val exportToWebDav: Boolean = false,
    val exportNoChapterName: Boolean = false,
    val enableCustomExport: Boolean = false,
    val exportType: Int = 0,
    val exportPictureFile: Boolean = false,
    val parallelExportBook: Boolean = false,
    /** 缓存目录：起始章/结束章（1-based），null 表示该端不限。 */
    val exportChapterStart: Int? = null,
    val exportChapterEnd: Int? = null,
)
