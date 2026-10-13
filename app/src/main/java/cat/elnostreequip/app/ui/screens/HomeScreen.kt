package cat.elnostreequip.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.elnostreequip.app.ElNostreEquipApp
import cat.elnostreequip.app.data.ElNostreEquipRepository
import cat.elnostreequip.app.data.TeamMapper
import cat.elnostreequip.app.data.db.ArticleEntity
import cat.elnostreequip.app.notifications.TopicManager
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: ElNostreEquipApp) : ViewModel() {
    private val repo: ElNostreEquipRepository = app.repository
    private val topics: TopicManager = app.topicManager

    val followedTeams: StateFlow<Set<Int>> = topicsFollowedTeams()

    private fun topicsFollowedTeams(): StateFlow<Set<Int>> {
        // Derivado: un equipo se considera seguido si articles o matchday están activos.
        // Simplificación fase 1: observamos isTeamFollowed por equipo.
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
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun toggleTeam(equipId: Int, on: Boolean) {
        viewModelScope.launch {
            try {
                topics.setTeamFollowed(equipId, on)
            } catch (e: Exception) {
                _error.value = "Error guardant la preferència"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(onArticleClick: (Long) -> Unit) {
    val vm: HomeViewModel = appViewModel { HomeViewModel(it) }
    val articles by vm.articles.collectAsState()
    val followed by vm.followedTeams.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val error by vm.error.collectAsState()

    val pullState = rememberPullRefreshState(refreshing, { vm.refresh() })

    // TODO: filtrar por equipo cuando TeamMapper.teamForCategories tenga el mapa real.
    val visible = articles

    Scaffold(
        topBar = { TopAppBar(title = { Text("El Nostre Equip") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullState)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                if (visible.isEmpty() && !refreshing) {
                    Text(
                        "Encara no hi ha articles. Estira per actualitzar.",
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(visible, key = { it.id }) { article ->
                            ArticleCard(article = article, onClick = { onArticleClick(article.id) })
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
                PullRefreshIndicator(refreshing, pullState, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun ArticleCard(article: ArticleEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            if (!article.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(Modifier.padding(12.dp)) {
                Text(article.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    article.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    Text(
                        article.date.take(10),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
