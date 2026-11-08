package io.legado.app.domain.model.settings

/** 漫画导出的落盘形态：单个 CBZ 文件，或按章节目录的文件夹。 */
enum class MangaExportFormat(val value: String) {
    Cbz("cbz"),
    Folder("folder");

    companion object {
        fun fromValue(value: String?): MangaExportFormat =
            entries.firstOrNull { it.value == value } ?: Cbz
    }
}
