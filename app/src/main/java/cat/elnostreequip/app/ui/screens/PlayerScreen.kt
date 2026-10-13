package cat.elnostreequip.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cat.elnostreequip.app.ElNostreEquipApp
import cat.elnostreequip.app.data.TeamMapper
import cat.elnostreequip.app.data.db.PlayerEntity
import coil.compose.AsyncImage

/**
 * Fitxa de jugadora: capçalera (foto, dorsal, nom, posició, equip) +
 * pestanyes Fitxa / Estadístiques.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(playerId: Long) {
    val app = LocalContext.current.applicationContext as ElNostreEquipApp
    var player by remember { mutableStateOf<PlayerEntity?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(playerId) {
        player = try {
            app.repository.player(playerId)
        } catch (_: Exception) {
            null
        }
        loaded = true
    }

    Scaffold(
        topBar = {
            EneTopBar(player?.name ?: "Jugadora")
        }
    ) { padding ->
        when {
            !loaded -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            player == null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Jugadora no trobada") }

            else -> {
                val p = player!!
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Capçalera
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = p.photoUrl,
                            contentDescription = p.name,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                if (p.dorsal < 999) "#${p.dorsal}" else "",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(p.name, style = MaterialTheme.typography.headlineSmall)
                            if (p.position.isNotBlank()) {
                                Text(
                                    p.position,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                TeamMapper.fullName(p.teamId),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Pestanyes
                    TabRow(selectedTabIndex = tab) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Fitxa") })
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Estadístiques") })
                    }
                    when (tab) {
                        0 -> {
                            if (p.bioHtml.isBlank()) {
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) { Text("Sense fitxa disponible") }
                            } else {
                                HtmlWebView(html = p.bioHtml, modifier = Modifier.fillMaxSize())
                            }
                        }
                        1 -> PlayerStatsTab(playerId = p.id)
                    }
                }
            }
        }
    }
}

/**
 * Pestanya d'estadístiques: tabla por partido + totales.
 * TODO (fase 3): llegir de Firestore matches/{id}/events (DESIGN §5).
 */
@Composable
private fun PlayerStatsTab(playerId: Long) {
    // De momento sin datos: estado vacío según diseño.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Les estadístiques apareixeran quan el partit es cobrisca en directe",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Ací veuràs, per cada partit: data, rival, minuts, gols, " +
                    "assistències, titular/suplent i targetes, més els totals de la temporada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
