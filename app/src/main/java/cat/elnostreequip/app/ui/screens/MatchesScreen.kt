package cat.elnostreequip.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.elnostreequip.app.ElNostreEquipApp
import cat.elnostreequip.app.data.ElNostreEquipRepository
import cat.elnostreequip.app.data.TeamMapper
import cat.elnostreequip.app.data.api.CalendarMatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MatchesViewModel(app: ElNostreEquipApp) : ViewModel() {
    private val repo: ElNostreEquipRepository = app.repository

    private val _equip = MutableStateFlow(TeamMapper.EQUIP_B)
    val equip: StateFlow<Int> = _equip

    private val _matches = MutableStateFlow<List<CalendarMatch>>(emptyList())
    val matches: StateFlow<List<CalendarMatch>> = _matches

    private val _articles = MutableStateFlow<List<ElNostreEquipRepository.RelatedArticle>>(emptyList())
    val articles: StateFlow<List<ElNostreEquipRepository.RelatedArticle>> = _articles

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val cache = mutableMapOf<Int, List<CalendarMatch>>()
    private var articlesLoaded = false

    init {
        load(TeamMapper.EQUIP_B)
    }

    fun select(equipId: Int) {
        _equip.value = equipId
        load(equipId)
    }

    private fun load(equipId: Int) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _matches.value = cache.getOrPut(equipId) { repo.matches(equipId) }
                if (!articlesLoaded) {
                    _articles.value = repo.partitsArticles()
                    articlesLoaded = true
                }
            } catch (e: Exception) {
                _error.value = "No s'han pogut carregar els partits"
            } finally {
                _loading.value = false
            }
        }
    }

    fun refresh() {
        cache.remove(_equip.value)
        load(_equip.value)
    }
}

/**
 * Normalitza un text per comparar (minúscules, sense accents).
 */
private fun norm(s: String): String =
    Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

private val TEAM_STOPWORDS = setOf(
    "valencia", "femeni", "femenino", "femenina", "club", "cf", "de",
    "la", "el", "les", "dels", "del", "afc", "ud", "sd", "cd", "ce", "fc",
    "rcd", "atletico", "atletic", "reial", "real", "deportivo", "sporting"
)

/**
 * Troba articles de Partits relacionats amb un partit: coincideix el rival
 * (l'equip que no és el València) pel seu nom al títol.
 */
private fun relatedFor(
    match: CalendarMatch,
    articles: List<ElNostreEquipRepository.RelatedArticle>
): List<ElNostreEquipRepository.RelatedArticle> {
    val homeN = norm(match.home)
    val awayN = norm(match.away)
    val rival = if ("valencia" in homeN) match.away else match.home
    val keywords = norm(rival).split(Regex("[^a-z0-9]+"))
        .filter { it.length > 3 && it !in TEAM_STOPWORDS }
    if (keywords.isEmpty()) return emptyList()
    return articles.filter { a ->
        val t = norm(a.title)
        keywords.any { it in t }
    }.take(2)
}

private val VALENCIAN_DATE = DateTimeFormatter.ofPattern("EEEE d MMM", Locale("ca"))

private fun CalendarMatch.prettyDate(): String = try {
    OffsetDateTime.parse(start).format(VALENCIAN_DATE)
        .replaceFirstChar { it.uppercaseChar() }
} catch (_: Exception) {
    start.take(10)
}

private fun CalendarMatch.prettyTime(): String = try {
    OffsetDateTime.parse(start).format(DateTimeFormatter.ofPattern("HH:mm"))
} catch (_: Exception) {
    ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(onArticleClick: (Long) -> Unit) {
    val vm: MatchesViewModel = appViewModel { MatchesViewModel(it) }
    val equip by vm.equip.collectAsState()
    val matches by vm.matches.collectAsState()
    val articles by vm.articles.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    val upcoming = matches.filter { !it.past }
    val past = matches.filter { it.past }

    Scaffold(
        topBar = { EneTopBar("Partits") }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TeamMapper.ALL) { id ->
                    FilterChip(
                        selected = id == equip,
                        onClick = { vm.select(id) },
                        label = { Text(TeamMapper.shortName(id)) }
                    )
                }
            }
            when {
                loading && matches.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                error != null && matches.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Torna-ho a provar",
                            modifier = Modifier.clickable { vm.refresh() },
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (upcoming.isNotEmpty()) {
                        item {
                            SectionTitle("Pròxims")
                        }
                        items(upcoming, key = { it.id }) { m ->
                            MatchCard(
                                match = m,
                                related = relatedFor(m, articles),
                                onArticleClick = onArticleClick
                            )
                        }
                    }
                    if (past.isNotEmpty()) {
                        item {
                            SectionTitle("Anteriors")
                        }
                        items(past, key = { it.id }) { m ->
                            MatchCard(
                                match = m,
                                related = relatedFor(m, articles),
                                onArticleClick = onArticleClick
                            )
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun MatchCard(
    match: CalendarMatch,
    related: List<ElNostreEquipRepository.RelatedArticle>,
    onArticleClick: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Insígnia de data
                DateBadge(match)
                Column(Modifier.weight(1f)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            match.competition,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        match.home,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        match.away,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!match.past) {
                    Text(
                        match.prettyTime(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Articles vinculats del web (cròniques / prèvies)
            if (related.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                related.forEach { a ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onArticleClick(a.id) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Article,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            a.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateBadge(match: CalendarMatch) {
    val parts = try {
        val dt = OffsetDateTime.parse(match.start)
        Triple(
            dt.dayOfMonth.toString(),
            dt.format(DateTimeFormatter.ofPattern("MMM", Locale("ca")))
                .replaceFirstChar { it.uppercaseChar() }.removeSuffix("."),
            dt.format(DateTimeFormatter.ofPattern("EEE", Locale("ca")))
                .replaceFirstChar { it.uppercaseChar() }.removeSuffix(".")
        )
    } catch (_: Exception) {
        Triple("–", "", "")
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (match.past) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.primary
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val fg = if (match.past) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onPrimary
        Text(
            parts.first,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = fg
        )
        Text(parts.second, style = MaterialTheme.typography.labelSmall, color = fg)
        Text(parts.third, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.75f))
    }
}
