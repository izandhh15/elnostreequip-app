package cat.elnostreequip.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.elnostreequip.app.ElNostreEquipApp
import cat.elnostreequip.app.R
import cat.elnostreequip.app.data.ElNostreEquipRepository
import cat.elnostreequip.app.data.ElNostreEquipRepository.PartitPost
import cat.elnostreequip.app.data.TeamMapper
import cat.elnostreequip.app.data.api.CalendarMatch
import cat.elnostreequip.app.data.db.ArticleEntity
import cat.elnostreequip.app.ui.theme.EneBlue
import cat.elnostreequip.app.ui.theme.EneOrange
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class HomeViewModel(app: ElNostreEquipApp) : ViewModel() {
    private val repo: ElNostreEquipRepository = app.repository

    val articles: StateFlow<List<ArticleEntity>> =
        repo.observeArticles().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _teamFilter = MutableStateFlow<Int?>(null)
    val teamFilter: StateFlow<Int?> = _teamFilter

    private val _nextMatch = MutableStateFlow<Pair<Int, CalendarMatch>?>(null)
    val nextMatch: StateFlow<Pair<Int, CalendarMatch>?> = _nextMatch

    private val _partitPosts = MutableStateFlow<List<ElNostreEquipRepository.PartitPost>>(emptyList())
    val partitPosts: StateFlow<List<ElNostreEquipRepository.PartitPost>> = _partitPosts

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            _error.value = null
            try {
                repo.refreshArticles()
            } catch (e: Exception) {
                _error.value = "No s'ha pogut actualitzar: ${e.message}"
            }
            try {
                _nextMatch.value = repo.nextMatch(TeamMapper.ALL)
            } catch (_: Exception) {
            }
            try {
                _partitPosts.value = repo.partitPosts()
            } catch (_: Exception) {
            }
            _refreshing.value = false
        }
    }

    fun setFilter(equipId: Int?) {
        _teamFilter.value = equipId
    }
}

/** Temps relatiu estil premsa: "fa 2 h", "ahir", "12 oct". */
fun relativeTime(iso: String): String {
    return try {
        val dt = OffsetDateTime.parse(iso)
        val now = OffsetDateTime.now()
        val dur = Duration.between(dt, now)
        when {
            dur.isNegative || dur.toMinutes() < 1 -> "ara mateix"
            dur.toMinutes() < 60 -> "fa ${dur.toMinutes()} min"
            dur.toHours() < 24 -> "fa ${dur.toHours()} h"
            dur.toDays() == 1L -> "ahir"
            dur.toDays() < 7 -> "fa ${dur.toDays()} dies"
            else -> dt.format(DateTimeFormatter.ofPattern("d MMM", java.util.Locale("ca")))
        }
    } catch (_: Exception) {
        iso.take(10)
    }
}

/** Etiqueta de secció/equip per a una notícia. */
fun articleLabel(a: ArticleEntity): String = when (a.teamId) {
    TeamMapper.PRIMER_EQUIP -> "PRIMER EQUIP"
    TeamMapper.EQUIP_B -> "FILIAL"
    TeamMapper.EQUIP_C -> "JUVENIL"
    TeamMapper.EQUIP_D -> "CADET"
    else -> "EL NOSTRE EQUIP"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onArticleClick: (Long) -> Unit,
    onMatchesClick: () -> Unit
) {
    val vm: HomeViewModel = appViewModel { HomeViewModel(it) }
    val articles by vm.articles.collectAsState()
    val teamFilter by vm.teamFilter.collectAsState()
    val nextMatch by vm.nextMatch.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val error by vm.error.collectAsState()

    val pullState = rememberPullRefreshState(refreshing, { vm.refresh() })

    val filtered = if (teamFilter == null) articles
    else articles.filter { it.teamId == teamFilter }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(R.drawable.ene_logo),
                        contentDescription = "El Nostre Equip",
                        modifier = Modifier.height(40.dp),
                        contentScale = ContentScale.Fit
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullState)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Chips d'equip (estil Marca: seccions horitzontals)
                    item(key = "chips") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = teamFilter == null,
                                    onClick = { vm.setFilter(null) },
                                    label = { Text("Totes", fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EneOrange,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                            items(TeamMapper.ALL) { id ->
                                FilterChip(
                                    selected = teamFilter == id,
                                    onClick = { vm.setFilter(if (teamFilter == id) null else id) },
                                    label = { Text(TeamMapper.shortName(id), fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EneOrange,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    if (vm.partitPosts.value.isNotEmpty() && teamFilter == null) {
                        item(key = "partitdestacat") {
                            PartitDestacatCard(partit = vm.partitPosts.value.first())
                        }
                    }

                    // Pròxim partit (franja compacta)
                    if (nextMatch != null && teamFilter == null) {
                        item(key = "nextmatch") {
                            SlimNextMatchCard(
                                match = nextMatch!!.second,
                                teamId = nextMatch!!.first,
                                onClick = onMatchesClick
                            )
                        }
                    }

                    // Hero
                    if (filtered.isNotEmpty()) {
                        item(key = "hero") {
                            PressHeroCard(
                                article = filtered[0],
                                onClick = { onArticleClick(filtered[0].id) }
                            )
                        }
                        // Files d'estil diari esportiu
                        items(filtered.drop(1), key = { it.id }) { article ->
                            PressRow(
                                article = article,
                                onClick = { onArticleClick(article.id) }
                            )
                        }
                    } else if (!refreshing) {
                        item {
                            Text(
                                "Encara no hi ha notícies. Estira per actualitzar.",
                                modifier = Modifier.padding(24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (error != null) {
                        item {
                            Text(
                                error!!,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
                PullRefreshIndicator(refreshing, pullState, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

/** Franja compacta del pròxim partit (sota els chips). */
@Composable
private fun PartitDestacatCard(
    partit: PartitPost
) {
    Text(partit.title)
}

private fun SlimNextMatchCard(
    match: CalendarMatch,
    teamId: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .pressScale(onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "PRÒXIM PARTIT · ${TeamMapper.shortName(teamId).uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${match.home} - ${match.away}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    match.prettyDateTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CountdownMini(match.start)
        }
    }
}

/** Compte enrere mini per a la franja. */
@Composable
private fun CountdownMini(startIso: String) {
    val text = try {
        val dur = Duration.between(OffsetDateTime.now(), OffsetDateTime.parse(startIso))
        if (dur.isNegative) null
        else {
            val d = dur.toDays()
            val h = dur.toHours() % 24
            val m = dur.toMinutes() % 60
            if (d > 0) "${d}d ${h}h" else "${h}h ${m}m"
        }
    } catch (_: Exception) {
        null
    } ?: return
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.tertiary
    )
}

/** Hero estil portada esportiva: foto a sang + etiqueta + titular. */
@Composable
private fun PressHeroCard(article: ArticleEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(onClick)
            .padding(bottom = 8.dp)
    ) {
        Box {
            if (!article.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .background(EneBlue)
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                        )
                    )
            )
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                articleLabel(article),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = EneOrange
            )
            Spacer(Modifier.height(4.dp))
            Text(
                article.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                relativeTime(article.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

/** Fila de diari esportiu: etiqueta + titular + hora, miniatura a la dreta. */
@Composable
private fun PressRow(article: ArticleEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    articleLabel(article),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = EneOrange
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    relativeTime(article.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!article.imageUrl.isNullOrBlank()) {
                Spacer(Modifier.width(12.dp))
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}
