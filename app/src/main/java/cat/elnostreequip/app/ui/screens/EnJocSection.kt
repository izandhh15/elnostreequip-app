package cat.elnostreequip.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.elnostreequip.app.ElNostreEquipApp
import cat.elnostreequip.app.data.ElNostreEquipRepository
import cat.elnostreequip.app.data.LineupPlayer
import cat.elnostreequip.app.data.TimelineEvent
import cat.elnostreequip.app.data.api.MatchLiveData
import kotlinx.coroutines.delay

/**
 * Secció EN JOC: mostra tots els partits (tipus "partit" del WP) que estan
 * jugant-se, en prèvia o en descans, amb marcador, minut, cronologia.
 * Fitxer separat per no tocar MatchesScreen.kt (com HomeScreen.kt, és sensible).
 */
@Composable
fun EnJocSection(partits: List<ElNostreEquipRepository.PartitPost>) {
    if (partits.isEmpty()) return

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFD32F2F))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "EN JOC",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "Partits en directe",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(12.dp))
        partits.forEach { partit ->
            EnJocMatchCard(partit = partit)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun EnJocMatchCard(partit: ElNostreEquipRepository.PartitPost) {
    val app = LocalContext.current.applicationContext as ElNostreEquipApp
    var live by remember { mutableStateOf<MatchLiveData?>(null) }
    var timeline by remember { mutableStateOf<List<TimelineEvent>>(emptyList()) }
    var lineup by remember { mutableStateOf<List<LineupPlayer>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(partit.id) {
        while (true) {
            try {
                live = app.repository.matchLive(partit.id)
                if (expanded) {
                    timeline = app.repository.matchTimeline(partit.link)
                    if (lineup.isEmpty()) {
                        lineup = app.repository.matchLineup(partit.link)
                    }
                }
            } catch (_: Exception) {
            }
            delay(10_000)
        }
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            try {
                if (timeline.isEmpty()) timeline = app.repository.matchTimeline(partit.link)
                if (lineup.isEmpty()) lineup = app.repository.matchLineup(partit.link)
            } catch (_: Exception) {
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    partit.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (live != null && live!!.minutDirecte.isNotEmpty()) {
                    Text(
                        live!!.minutDirecte,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFD32F2F)
                    )
                }
            }

            if (live != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "${live!!.golsLocal}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "-",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Text(
                        "${live!!.golsRival}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (live!!.estat.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        estatTextLive(live!!.estat),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (expanded && timeline.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Cronologia",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                timeline.forEach { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            event.minute,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.width(40.dp)
                        )
                        Text(
                            event.icon,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(28.dp)
                        )
                        Text(
                            event.text,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // XI Inicial
            if (expanded && lineup.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "XI Inicial",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                lineup.forEach { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${player.dorsal}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.width(32.dp)
                        )
                        Text(
                            player.name,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (expanded && timeline.isEmpty() && lineup.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Carregant cronologia i alineació...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!expanded) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Toca per veure la cronologia i l'XI ▼",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun estatTextLive(estat: String): String = when (estat) {
    "primera" -> "Primera part"
    "segona" -> "Segona part"
    "descans" -> "Descans"
    "final" -> "Final del partit"
    "finalitzat" -> "Final del partit"
    "pre" -> "Prèvia"
    else -> estat
}
