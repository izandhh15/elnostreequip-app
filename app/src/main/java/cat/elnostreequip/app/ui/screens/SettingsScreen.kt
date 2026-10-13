package cat.elnostreequip.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cat.elnostreequip.app.ElNostreEquipApp
import cat.elnostreequip.app.data.ElNostreEquipRepository
import cat.elnostreequip.app.data.TeamMapper
import cat.elnostreequip.app.notifications.TopicManager
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private val LIVE_LABELS = listOf(
    TopicManager.LiveKind.XI to "XI disponible",
    TopicManager.LiveKind.START to "Inici del partit",
    TopicManager.LiveKind.GOAL to "Gols",
    TopicManager.LiveKind.YELLOW to "Targetes grogues",
    TopicManager.LiveKind.RED to "Targetes roges",
    TopicManager.LiveKind.HT to "Descans",
    TopicManager.LiveKind.FT to "Final",
    TopicManager.LiveKind.ET to "Pròrroga"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenAdmin: () -> Unit) {
    val app = LocalContext.current.applicationContext as ElNostreEquipApp

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ajustos") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Notificacions", style = MaterialTheme.typography.titleMedium)
            }
            if (!app.topicManager.isPushAvailable()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            "Les notificacions push encara no estan actives: falta crear el projecte Firebase. Els teus ajustos es guarden igualment i s'activaran sols quan estiga configurat.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            items(TeamMapper.ALL.size) { i ->
                TeamNotificationCard(equipId = TeamMapper.ALL[i])
            }
            item {
                Text(
                    "Jugadores seguides",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item { FollowedPlayersCard() }
            item {
                TextButton(onClick = onOpenAdmin) {
                    Text("Mode directe (Izan)")
                }
            }
        }
    }
}

@Composable
private fun TeamNotificationCard(equipId: Int) {
    val app = LocalContext.current.applicationContext as ElNostreEquipApp
    val topics = app.topicManager
    val scope = rememberCoroutineScope()
    val followed by topics.isTeamFollowed(equipId).collectAsState(initial = false)

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    TeamMapper.fullName(equipId),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = followed,
                    onCheckedChange = { scope.launch { topics.setTeamFollowed(equipId, it) } }
                )
            }
            if (followed) {
                val articles by topics.articlesPref(equipId).collectAsState(initial = false)
                val matchday by topics.matchdayPref(equipId).collectAsState(initial = false)
                PrefRow("Articles", articles) {
                    scope.launch { topics.setArticles(equipId, it) }
                }
                PrefRow("Dia de partit", matchday) {
                    scope.launch { topics.setMatchday(equipId, it) }
                }
                Text(
                    "En directe",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                LIVE_LABELS.forEach { (kind, label) ->
                    val on by topics.livePref(equipId, kind).collectAsState(initial = false)
                    PrefRow(label, on) {
                        scope.launch { topics.setLivePref(equipId, kind, it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrefRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun FollowedPlayersCard() {
    val app = LocalContext.current.applicationContext as ElNostreEquipApp
    val topics = app.topicManager
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ElNostreEquipRepository.PlayerHit>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val followedIds by topics.followedPlayers().collectAsState(initial = emptySet())
    var names by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }

    LaunchedEffect(followedIds) {
        names = followedIds.associateWith { id ->
            try {
                app.repository.player(id)?.name ?: "Jugadora #$id"
            } catch (_: Exception) {
                "Jugadora #$id"
            }
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Cerca jugadores…") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        scope.launch {
                            searching = true
                            results = try {
                                app.repository.searchPlayers(query)
                            } catch (_: Exception) {
                                emptyList()
                            }
                            searching = false
                        }
                    },
                    enabled = query.isNotBlank() && !searching
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Cerca")
                }
            }
            results.forEach { hit ->
                val isFollowed = hit.id in followedIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!hit.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = hit.photoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(hit.name, style = MaterialTheme.typography.bodyMedium)
                        hit.teamId?.let {
                            Text(
                                TeamMapper.fullName(it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            scope.launch { topics.setPlayerFollowed(hit.id, !isFollowed) }
                        }
                    ) {
                        Icon(
                            if (isFollowed) Icons.Filled.Close else Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(if (isFollowed) "Deixar" else "Seguir")
                    }
                }
            }
            if (followedIds.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Seguides ara:", style = MaterialTheme.typography.labelLarge)
                names.forEach { (id, name) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            scope.launch { topics.setPlayerFollowed(id, false) }
                        }) { Text("Deixar de seguir") }
                    }
                }
            } else if (results.isEmpty()) {
                Text(
                    "Encara no segueixes cap jugadora.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
