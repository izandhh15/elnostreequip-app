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
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.unit.sp
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
    "rcd", "deportivo", "sporting"
)

/** Àlies català/castellà per a noms d'equip (bidireccionals). */
private val TEAM_ALIASES = mapOf(
    "barcelona" to listOf("barcelona", "barca"),
    "barca" to listOf("barcelona", "barca"),
    "atletico" to listOf("atletico", "atletic"),
    "atletic" to listOf("atletico", "atletic"),
    "reial" to listOf("reial", "real"),
    "real" to listOf("reial", "real")
)

/**
 * Troba articles de Partits relacionats amb un partit: coincideix el rival
 * (l'equip que no és el València) pel seu nom al títol.
 * Si el rival té 2+ paraules distintives, CALEN TOTES (així "Atlètic de Madrid"
 * no agafa notícies del "Reial Madrid").
 */
private fun relatedFor(
    match: CalendarMatch,
    articles: List<ElNostreEquipRepository.RelatedArticle>
): List<ElNostreEquipRepository.RelatedArticle> {
    val homeN = norm(match.home)
    val rival = if ("valencia" in homeN) match.away else match.home
    val rivalN = norm(rival)
    // Designacio de filial (B, C, D...): forma part de la identitat de l'equip.
    // "Llevant B" != "Llevant C".
    val designation = Regex("\\b([a-z])\\b\\s*$").find(rivalN)?.groupValues?.get(1)
    val words = rivalN.split(Regex("[^a-z0-9]+"))
        .filter { it.length > 2 && it !in TEAM_STOPWORDS }
    if (words.isEmpty()) return emptyList()
    return articles.filter { a ->
        val t = norm(a.title)
        val baseOk = baseMatches(words, t)
        if (!baseOk) return@filter false
        // Si el rival te designacio (B/C/D), el titular l'ha de mencionar
        // amb la MATEIXA lletra: "llevant ud c", "barca c", etc.
        if (designation != null) {
            val base = words[0]
            val aliases = TEAM_ALIASES[base] ?: listOf(base)
            val found = aliases.any { alias ->
                Regex(
                    Regex.escape(alias) +
                        "(?:\\s+[a-z]{1,4}){0,2}\\s+" +
                        Regex.escape(designation) + "\\b"
                ).containsMatchIn(t)
            }
            if (!found) return@filter false
        }
        true
    }.take(2)
}

private fun baseMatches(words: List<String>, t: String): Boolean {
    return if (words.size >= 2) {
        words.all { w ->
            val variants = TEAM_ALIASES[w] ?: listOf(w)
            variants.any { it in t }
        }
    } else {
        val w = words[0]
        val variants = TEAM_ALIASES[w] ?: listOf(w)
        variants.any { v ->
            Regex("(?<![a-z0-9])" + Regex.escape(v) + "(?![a-z0-9])").containsMatchIn(t)
        }
    }
}

/**
 * Colors per competició (demanats per Izan):
 * Liga F blau fosc, Copa lila, Segona RFEF lila, Tercera RFEF taronja, Autonòmica roig corall.
 */
fun competitionColor(comp: String): androidx.compose.ui.graphics.Color {
    val cc = comp.lowercase()
    return when {
        "lliga f" in cc || "ligaf" in cc || "liga f" in cc -> androidx.compose.ui.graphics.Color(0xFF1E3A8A)
        "copa" in cc -> androidx.compose.ui.graphics.Color(0xFF7C3AED)
        "2rfef" in cc || "segona" in cc || "segunda" in cc -> androidx.compose.ui.graphics.Color(0xFFA855F7)
        "3rfef" in cc || "tercera" in cc -> androidx.compose.ui.graphics.Color(0xFFFF7B00)
        "autonom" in cc || "llcom" in cc || "preferent" in cc -> androidx.compose.ui.graphics.Color(0xFFFF6B6B)
        else -> androidx.compose.ui.graphics.Color(0xFF6B7280)
    }
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            // Capçalera: competició + data (estil professional subtil)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(competitionColor(match.competition))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        match.competition.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    match.prettyDate(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                if (!match.past) {
                    Text(
                        match.prettyTime(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = cat.elnostreequip.app.ui.theme.EneOrange
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // Equips: layout de partit professional
            MatchTeamsRow(match = match)

            // Articles vinculats del web (cròniques / prèvies) — estil subtil
            if (related.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    thickness = 1.dp
                )
                Spacer(Modifier.height(4.dp))
                related.forEach { a ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onArticleClick(a.id) }
                            .padding(vertical = 7.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Article,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            a.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Fila d'equips estil professional: local vs visitant amb separador. */
@Composable
private fun MatchTeamsRow(match: CalendarMatch) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(
                match.home,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (match.past) MaterialTheme.colorScheme.surfaceVariant
                    else cat.elnostreequip.app.ui.theme.EneOrange.copy(alpha = 0.12f)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "VS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = if (match.past) MaterialTheme.colorScheme.onSurfaceVariant
                else cat.elnostreequip.app.ui.theme.EneOrange,
                letterSpacing = 1.sp
            )
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(
                match.away,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start
            )
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
                else cat.elnostreequip.app.ui.theme.EneOrange
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val fg = if (match.past) MaterialTheme.colorScheme.onSurfaceVariant
        else androidx.compose.ui.graphics.Color.White
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
