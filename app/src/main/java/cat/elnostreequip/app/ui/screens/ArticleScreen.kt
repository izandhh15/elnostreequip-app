package cat.elnostreequip.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.elnostreequip.app.ElNostreEquipApp
import cat.elnostreequip.app.R
import cat.elnostreequip.app.data.TeamMapper
import cat.elnostreequip.app.data.db.ArticleEntity
import coil.compose.AsyncImage
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Bloc de contingut d'un article: titular de secció o paràgraf. */
private sealed interface ArticleBlock {
    data class Heading(val text: AnnotatedString) : ArticleBlock
    data class Paragraph(val text: AnnotatedString) : ArticleBlock
}

/** Converteix <strong> en negreta dins un fragment HTML simple. */
private fun styledText(html: String): AnnotatedString {
    var clean = html.replace(Regex("<span[^>]*>.*?</span>", RegexOption.DOT_MATCHES_ALL), "")
    clean = clean.replace(Regex("<i[^>]*>.*?</i>", RegexOption.DOT_MATCHES_ALL), "")
    return buildAnnotatedString {
        val parts = clean.split(Regex("(?i)</?strong>"))
        var bold = false
        parts.forEachIndexed { i, part ->
            val text = part.replace(Regex("<[^>]+>"), "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&#8217;", "'")
                .replace("&#8216;", "'")
                .replace("&#8220;", "\"")
                .replace("&#8221;", "\"")
                .replace("&#8211;", "–")
                .replace("&#8230;", "…")
                .trim()
            if (text.isNotEmpty()) {
                if (bold) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text) }
                else append(text)
                if (i < parts.size - 1) append(" ")
            }
            bold = !bold
        }
    }
}

/** Parteix el HTML de WordPress en blocs (h2 / p). */
private fun parseArticle(html: String): List<ArticleBlock> {
    val blocks = mutableListOf<ArticleBlock>()
    val regex = Regex("(?i)<(h2|p)[^>]*>(.*?)</\\1>", RegexOption.DOT_MATCHES_ALL)
    regex.findAll(html).forEach { m ->
        val tag = m.groupValues[1].lowercase()
        val text = styledText(m.groupValues[2])
        if (text.isBlank()) return@forEach
        if (tag == "h2") blocks.add(ArticleBlock.Heading(text))
        else blocks.add(ArticleBlock.Paragraph(text))
    }
    return blocks
}

/** Paleta de colors de píndoles com al web. */
private val PILL_COLORS = listOf(
    Color(0xFFE2001A), // roig
    Color(0xFF1A56DB), // blau
    Color(0xFF16803C), // verd
    Color(0xFF7C3AED), // lila
    Color(0xFFB45309), // taronja fosc
    Color(0xFF0E7490)  // turquesa
)

/** Píndoles amb les categories reals del WordPress. */
private fun categoryPills(article: ArticleEntity): List<Pair<String, Color>> {
    val names = article.categoryNames.split("|").filter { it.isNotBlank() }
    if (names.isEmpty()) {
        // Fallback: nom de l'equip
        val fallback = when (article.teamId) {
            TeamMapper.PRIMER_EQUIP -> "València CF Femení"
            TeamMapper.EQUIP_B -> "València CF Femení B"
            TeamMapper.EQUIP_C -> "València CF Femení C"
            TeamMapper.EQUIP_D -> "València CF Femení D"
            else -> "El Nostre Equip"
        }
        return listOf(fallback to PILL_COLORS[0])
    }
    return names.mapIndexed { i, n -> n to PILL_COLORS[i % PILL_COLORS.size] }
}

private fun prettyDate(iso: String): String = try {
    OffsetDateTime.parse(iso).format(
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale("ca"))
    )
} catch (_: Exception) {
    iso.take(10)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArticleScreen(articleId: Long, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as ElNostreEquipApp
    var article by remember { mutableStateOf<ArticleEntity?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(articleId) {
        article = try {
            app.repository.fetchArticle(articleId)
        } catch (_: Exception) {
            null
        }
        loaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tornar")
                    }
                },
                title = {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.ene_logo),
                            contentDescription = "El Nostre Equip",
                            modifier = Modifier.height(44.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                },
                actions = { Spacer(Modifier.width(48.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        when {
            !loaded -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            article == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Article no trobat") }

            else -> {
                val a = article!!
                val blocks = remember(a.contentHtml) { parseArticle(a.contentHtml) }
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(8.dp))

                    // Breadcrumbs com al web
                    Text(
                        "Inici / ${a.categoryNames.split("|").firstOrNull { it.isNotBlank() } ?: "Notícies"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))

                    // Píndoles de categoria
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryPills(a).forEach { (label, color) ->
                            Surface(
                                color = color,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    label,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // Titular
                    Text(
                        a.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 32.sp
                    )
                    Spacer(Modifier.height(10.dp))

                    // Línia d'autor/data com al web
                    Text(
                        "Per Redacció El Nostre Equip  •  ${prettyDate(a.date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))

                    // Imatge destacada 16:9 amb cantonades arrodonides
                    if (!a.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = a.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    // Cos de l'article
                    blocks.forEach { block ->
                        when (block) {
                            is ArticleBlock.Heading -> {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    block.text,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 26.sp
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            is ArticleBlock.Paragraph -> {
                                Text(
                                    block.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 27.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
