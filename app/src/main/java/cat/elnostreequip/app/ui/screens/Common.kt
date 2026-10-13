package cat.elnostreequip.app.ui.screens

import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import cat.elnostreequip.app.ElNostreEquipApp
import cat.elnostreequip.app.R

/** Barra superior oficial: logo d'El Nostre Equip + títol, amb el blau corporatiu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EneTopBar(title: String) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ene_logo),
                    contentDescription = "El Nostre Equip",
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

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
