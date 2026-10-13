package cat.elnostreequip.app.ui.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.elnostreequip.app.ElNostreEquipApp
import cat.elnostreequip.app.data.ElNostreEquipRepository
import cat.elnostreequip.app.data.TeamMapper
import cat.elnostreequip.app.data.api.CalendarMatch
import cat.elnostreequip.app.data.db.ArticleEntity
import cat.elnostreequip.app.notifications.TopicManager
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeViewModel(app: ElNostreEquipApp) : ViewModel() {
    private val repo: ElNostreEquipRepository = app.repository
    private val topics: TopicManager = app.topicManager

    val followedTeams: StateFlow<Set<Int>> = topicsFollowedTeams()

    private fun topicsFollowedTeams(): StateFlow<Set<Int>> {
        return kotlinx.coroutines.flow.combine(
            topics.isTeamFollowed(TeamMapper.PRIMER_EQUIP),
            topics.isTeamFollowed(TeamMapper.EQUIP_B),
            topics.isTeamFollowed(TeamMapper.EQUIP_C),
            topics.isTeamFollowed(TeamMapper.EQUIP_D)
        ) { arr ->
            TeamMapper.ALL.filterIndexed { i, _ -> arr[i] }.toSet()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    }

    val articles: StateFlow<List<ArticleEntity>> =
        repo.observeArticles().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _nextMatch = MutableStateFlow<Pair<Int, CalendarMatch>?>(null)
    val nextMatch: StateFlow<Pair<Int, CalendarMatch>?> = _nextMatch

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
                val teams = followedTeams.value.ifEmpty { TeamMapper.ALL.toSet() }
                _nextMatch.value = repo.nextMatch(teams.toList())
            } catch (_: Exception) {
            }
            _refreshing.value = false
        }
    }

    fun toggleTeam(equipId: Int, on: Boolean) {
        viewModelScope.launch {
            try {
                topics.setTeamFollowed(equipId, on)
                val teams = (if (on) followedTeams.value + equipId else followedTeams.value - equipId)
                    .ifEmpty { TeamMapper.ALL.toSet() }
                _nextMatch.value = repo.nextMatch(teams.toList())
            } catch (e: Exception) {
                _error.value = "Error guardant la preferència"
            }
        }
    }
}

private fun greeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when (hour) {
        in 6..12 -> "Bon dia"
        in 13..20 -> "Bona vesprada"
        else -> "Bona nit"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onArticleClick: (Long) -> Unit,
    onMatchesClick: () -> Unit
) {
    val vm: HomeViewModel = appViewModel { HomeViewModel(it) }
    val articles by vm.articles.collectAsState()
    val followed by vm.followedTeams.collectAsState()
    val nextMatch by vm.nextMatch.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val error by vm.error.collectAsState()

    val pullState = rememberPullRefreshState(refreshing, { vm.refresh() })
    val today = LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("ca"))
    ).replaceFirstChar { it.uppercaseChar() }

    Scaffold(
        topBar = { EneTopBar("El Nostre Equip") }
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
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Salutació
                    item(key = "greeting") {
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            Text(
                                greeting(),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                today,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Pròxim partit (heroi)
                    if (nextMatch != null) {
                        item(key = "nextmatch") {
                            NextMatchHeroCard(
                                match = nextMatch!!.second,
                                teamId = nextMatch!!.first,
                                onClick = onMatchesClick
                            )
                        }
                    }

                    // Equips seguits
                    item(key = "teams") {
                        Column {
                            SectionHeader("Els teus equips")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(TeamMapper.ALL) { equipId ->
                                    FilterChip(
                                        selected = equipId in followed,
                                        onClick = { vm.toggleTeam(equipId, equipId !in followed) },
                                        label = { Text(TeamMapper.shortName(equipId)) }
                                    )
                                }
                            }
                        }
                    }

                    // Notícies
                    if (articles.isNotEmpty()) {
                        item(key = "newsheader") {
                            SectionHeader("Últimes notícies")
                        }
                        item(key = "hero") {
                            HeroArticleCard(
                                article = articles[0],
                                onClick = { onArticleClick(articles[0].id) }
                            )
                        }
                        items(articles.drop(1), key = { it.id }) { article ->
                            CompactArticleCard(
                                article = article,
                                onClick = { onArticleClick(article.id) }
                            )
                        }
                    } else if (!refreshing) {
                        item {
                            Text(
                                "Encara no hi ha articles. Estira per actualitzar.",
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
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }

                    item { Spacer(Modifier.height(12.dp)) }
                }
                PullRefreshIndicator(refreshing, pullState, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

/** Targeta protagonista: imatge gran amb degradat i titular a sobre. */
@Composable
private fun HeroArticleCard(article: ArticleEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pressScale(onClick),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            if (!article.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.82f)
                            )
                        )
                    )
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "DESTACAT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    article.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    article.date.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/** Targeta compacta: miniatura + titular. */
@Composable
private fun CompactArticleCard(article: ArticleEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pressScale(onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!article.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    article.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    article.excerpt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    article.date.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
