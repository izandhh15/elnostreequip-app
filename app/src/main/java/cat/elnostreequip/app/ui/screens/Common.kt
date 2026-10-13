package cat.elnostreequip.app.ui.screens

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import cat.elnostreequip.app.ElNostreEquipApp

/** Factoría simple de ViewModels sin Hilt. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    crossinline creator: (ElNostreEquipApp) -> VM
): VM {
    val app = LocalContext.current.applicationContext as ElNostreEquipApp
    return viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = creator(app) as T
    })
}

/** Renderiza HTML (contenido de WordPress) en un WebView. */
@Composable
fun HtmlWebView(html: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = false
                settings.loadsImagesAutomatically = true
            }
        },
        update = { webView ->
            val wrapped = """
                <html><head>
                <meta name="viewport" content="width=device-width, initial-scale=1"/>
                <style>
                  body { font-family: sans-serif; padding: 12px; line-height: 1.55; }
                  img { max-width: 100%; height: auto; }
                  figure { margin: 12px 0; }
                </style>
                </head><body>$html</body></html>
            """.trimIndent()
            webView.loadDataWithBaseURL("https://elnostreequip.cat", wrapped, "text/html", "UTF-8", null)
        }
    )
}
