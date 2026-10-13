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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.elnostreequip.app.ElNostreEquipApp
import cat.elnostreequip.app.data.TeamMapper
import cat.elnostreequip.app.data.db.PlayerEntity
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TeamViewModel(app: ElNostreEquipApp, private val equipId: Int) : ViewModel() {
    private val repo = app.repository

    val squad: StateFlow<List<PlayerEntity>> =
        repo.observeSquad(equipId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            try {
                repo.refreshSquad(equipId)
            } catch (_: Exception) {
                // Se muestra la caché; el error se ignora en fase 1.
            }
        }
    }
}

/**
 * Plantilla ordenada por dorsal: fila dorsal · foto · nom (+ posició).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(equipId: Int, onPlayerClick: (Long) -> Unit) {
    val vm: TeamViewModel = appViewModel { TeamViewModel(it, equipId) }
    val squad by vm.squad.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(TeamMapper.fullName(equipId)) }) }
    ) { padding ->
        if (squad.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Carregant plantilla…")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(squad, key = { it.id }) { player ->
                    PlayerRow(player = player, onClick = { onPlayerClick(player.id) })
                }
            }
        }
    }
}

@Composable
private fun PlayerRow(player: PlayerEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dorsal
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (player.dorsal >= 999) "–" else player.dorsal.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.width(12.dp))
            // Foto
            AsyncImage(
                model = player.photoUrl,
                contentDescription = player.name,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            // Nom + posició
            Column(Modifier.weight(1f)) {
                Text(player.name, style = MaterialTheme.typography.titleSmall)
                if (player.position.isNotBlank()) {
                    Text(
                        player.position,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
