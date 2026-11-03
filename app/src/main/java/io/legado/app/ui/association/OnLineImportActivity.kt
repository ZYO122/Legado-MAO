package io.legado.app.ui.association

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.R
import io.legado.app.base.BaseComposeActivity
import io.legado.app.feature.rss.source.RssSourceImportDialog
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.main.MainActivity
import io.legado.app.utils.showDialogFragment
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

/**
 * 网络一键导入
 * 格式: legado://import/{path}?src={url}
 */
class OnLineImportActivity : BaseComposeActivity(transparent = true, imageBg = false) {

    private val viewModel by viewModels<OnLineImportViewModel>()
    private val rssImportSource = MutableStateFlow<String?>(null)

    @Composable
    override fun Content() {
        val source by rssImportSource.collectAsStateWithLifecycle()
        source?.let { RssSourceImportDialog(it, onClose = ::finish) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.successLive.observe(this) {
            when (it.first) {
                "bookSource" -> openComposeBookSourceImport(it.second)
                "rssSource" -> rssImportSource.value = it.second
                "replaceRule" -> showDialogFragment(
                    ImportReplaceRuleDialog(it.second, true)
                )
                "httpTts" -> showDialogFragment(
                    ImportHttpTtsDialog(it.second, true)
                )
                "theme" -> showDialogFragment(
                    ImportThemeDialog(it.second, true)
                )
                "txtRule" -> showDialogFragment(
                    ImportTxtTocRuleDialog(it.second, true)
                )
                "dictRule" -> showDialogFragment(
                    ImportDictRuleDialog(it.second, true)
                )
            }
        }
        viewModel.errorLive.observe(this) {
            finallyDialog(getString(R.string.error), it)
        }
        intent.data?.let {
            val url = it.getQueryParameter("src")
            if (url.isNullOrEmpty()) {
                finish()
                return
            }
            when (it.path) {
                "/bookSource" -> openComposeBookSourceImport(url)

                "/rssSource" -> rssImportSource.value = url

                "/replaceRule" -> showDialogFragment(
                    ImportReplaceRuleDialog(url, true)
                )
                "/textTocRule" -> showDialogFragment(
                    ImportTxtTocRuleDialog(url, true)
                )
                "/httpTTS" -> showDialogFragment(
                    ImportHttpTtsDialog(url, true)
                )
                "/dictRule" -> showDialogFragment(
                    ImportDictRuleDialog(url, true)
                )
                "/theme" -> showDialogFragment(
                    ImportThemeDialog(url, true)
                )
                "/readConfig" -> viewModel.getBytes(url) { bytes ->
                    viewModel.importReadConfig(bytes, this::finallyDialog)
                }
                "/addToBookshelf" -> showDialogFragment(
                    AddToBookshelfDialog(url, true)
                )
                "/importonline" -> when (it.host) {
                    "booksource" -> openComposeBookSourceImport(url)
                    "rsssource" -> rssImportSource.value = url
                    "replace" -> showDialogFragment(
                        ImportReplaceRuleDialog(url, true)
                    )
                    else -> {
                        viewModel.determineType(url, this::finallyDialog)
                    }
                }
                else -> viewModel.determineType(url, this::finallyDialog)
            }
        }
    }

    private fun finallyDialog(title: String, msg: String) {
        alert(title, msg) {
            okButton()
            onDismiss {
                finish()
            }
        }
    }

    private fun openComposeBookSourceImport(source: String) {
        startActivity(MainActivity.createBookSourceManageIntent(this, source))
        finish()
    }

}
