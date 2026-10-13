package cat.elnostreequip.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cat.elnostreequip.app.ElNostreEquipApp
import cat.elnostreequip.app.data.db.ArticleEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(articleId: Long) {
    val app = LocalContext.current.applicationContext as ElNostreEquipApp
    var article by remember { mutableStateOf<ArticleEntity?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(articleId) {
        article = try {
            app.repository.article(articleId)
        } catch (_: Exception) {
            null
        }
        loaded = true
    }

    Scaffold(
        topBar = {
            EneTopBar(article?.title ?: "Article")
        }
    ) { padding ->
        when {
            !loaded -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            article == null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Article no trobat") }

            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                HtmlWebView(
                    html = "<h1>${article!!.title}</h1>${article!!.contentHtml}",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
