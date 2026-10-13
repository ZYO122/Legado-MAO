package io.legado.app.help.update

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import io.legado.app.BuildConfig
import io.legado.app.exception.NoStackTraceException
import java.time.Instant

data class AppReleaseInfo(
    val appVariant: AppVariant,
    val createdAt: Long,
    val note: String,
    val name: String,
    val downloadUrl: String,
    val assetUrl: String,
    val versionName: String
)

enum class AppVariant {
    OFFICIAL,
    BETA_RELEASE,
    ALL,
    UNKNOWN;

}

@Keep
data class GithubRelease(
    val assets: List<Asset>?,
    val body: String?,
    @SerializedName("prerelease")
    val isPreRelease: Boolean,
    @SerializedName("tag_name")
    val tagName: String,
    val name: String?,
    @SerializedName("created_at")
    val createdAt: String?
) {
    fun gitReleaseToAppReleaseInfo(
        variantSuffix: String = BuildConfig.UPDATE_ASSET_SUFFIX
    ): List<AppReleaseInfo> {
        assets ?: throw NoStackTraceException("获取新版本出错")

        val version = tagName
        return selectCompatibleAssets(assets, variantSuffix)
            .map { it.assetToAppReleaseInfo(isPreRelease, body.orEmpty(), version) }
    }
}

internal fun selectCompatibleAssets(
    assets: List<Asset>,
    variantSuffix: String
): List<Asset> {
    return assets.filter { it.isValid && it.variantSuffix == variantSuffix }
}

// 资产名形如 legado-<版本>.apk / legado-<版本>-A.apk / -B.apk；末尾 A/B 标记共存版
private val Asset.variantSuffix: String
    get() = name.substringBeforeLast('.').substringAfterLast('-').let { last ->
        if (last.equals("a", ignoreCase = true) || last.equals("b", ignoreCase = true)) {
            "-${last.uppercase()}"
        } else {
            ""
        }
    }

@Keep
data class Asset(
    @SerializedName("browser_download_url")
    val apkUrl: String,
    @SerializedName("content_type")
    val contentType: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("download_count")
    val downloadCount: Int,
    val id: Int,
    val name: String,
    val state: String,
    val url: String
) {
    val isValid: Boolean
        get() = (contentType == "application/vnd.android.package-archive") && (state == "uploaded")

    fun assetToAppReleaseInfo(preRelease: Boolean, note: String, version: String): AppReleaseInfo {
        val instant = Instant.parse(createdAt)
        val timestamp: Long = instant.toEpochMilli()
        val appVariant = if (preRelease) AppVariant.BETA_RELEASE else AppVariant.OFFICIAL

        return AppReleaseInfo(
            appVariant = appVariant,
            createdAt = timestamp,
            note = note,
            name = name,
            downloadUrl = apkUrl,
            assetUrl = url,
            versionName = version
        )
    }
}
