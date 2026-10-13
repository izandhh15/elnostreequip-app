package cat.elnostreequip.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.elnostreequip.app.ElNostreEquipApp
import cat.elnostreequip.app.data.TeamMapper
import cat.elnostreequip.app.data.api.PlayerStatRow
import cat.elnostreequip.app.data.db.PlayerEntity
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Fitxa de jugadora 2026: capçalera amb degradat + pestanyes Fitxa / Estadístiques.
 * Les estadístiques venen del plugin (taula ene-stats-table del web).
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
        topBar = { EneTopBar(player?.name ?: "Jugadora") }
    ) { padding ->
        when {
            !loaded -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            player == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Jugadora no trobada") }

            else -> {
                val p = player!!
                Column(Modifier.fillMaxSize().padding(padding)) {
                    PlayerHeader(p)
                    TabRow(selectedTabIndex = tab) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Fitxa") })
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Estadístiques") })
                    }
                    when (tab) {
                        0 -> {
                            if (p.bioHtml.isBlank()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "La fitxa completa és al web.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                HtmlWebView(html = p.bioHtml, modifier = Modifier.fillMaxSize())
                            }
                        }
                        1 -> PlayerStatsTab(p)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerHeader(p: PlayerEntity) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFF7B00),
                        Color(0xFFFFA726)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = p.photoUrl,
                    contentDescription = p.name,
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentScale = ContentScale.Crop
                )
                if (p.dorsal < 999) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                    ) {
                        Text(
                            p.dorsal.toString(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    p.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (p.position.isNotBlank()) {
                    Text(
                        p.position,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Text(
                    TeamMapper.fullName(p.teamId),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/** Pestanya d'estadístiques: taula per competició + total (del web). */
@Composable
private fun PlayerStatsTab(p: PlayerEntity) {
    val rows: List<PlayerStatRow> = remember(p.statsJson) {
        try {
            val type = object : TypeToken<List<PlayerStatRow>>() {}.type
            Gson().fromJson<List<PlayerStatRow>>(p.statsJson, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    if (rows.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "Encara no hi ha estadístiques publicades per a aquesta jugadora.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Totals destacats
        val total = rows.firstOrNull { it.kind == "total" }
        if (total != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatHighlight(total.pj.toString(), "Partits")
                    StatHighlight(total.gols.toString(), "Gols")
                    StatHighlight(total.assists.toString(), "Assist.")
                    StatHighlight("${total.min}'", "Minuts")
                }
            }
        }

        // Barres visuals de les estadístiques totals
        if (total != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Temporada",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val maxVal = maxOf(total.pj, total.pt, total.min, total.gols, total.assists, 1)
                    StatBar("Partits jugats", total.pj, maxVal)
                    StatBar("Titularitats", total.pt, maxVal)
                    StatBar("Gols", total.gols, maxOf(total.gols, total.assists, 1))
                    StatBar("Assistències", total.assists, maxOf(total.gols, total.assists, 1))
                }
            }
        }

        // Taula per competició
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    "Per competició",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Column {
                        StatHeaderRow()
                        var lastTeam = ""
                        rows.forEach { r ->
                            if (r.kind == "total") return@forEach
                            if (r.team.isNotBlank() && r.team != lastTeam) {
                                lastTeam = r.team
                                Text(
                                    lastTeam,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                )
                            }
                            StatDataRow(r)
                        }
                    }
                }
                Text(
                    "Dades d'elnostreequip.cat",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StatHighlight(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun StatHeaderRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatCell("Competició", 130.dp, header = true)
        StatCell("PJ", 44.dp, header = true)
        StatCell("PT", 44.dp, header = true)
        StatCell("Min", 56.dp, header = true)
        StatCell("⚽", 44.dp, header = true)
        StatCell("🅰️", 44.dp, header = true)
        StatCell("🟨", 44.dp, header = true)
        StatCell("🟥", 44.dp, header = true)
    }
}

@Composable
private fun StatDataRow(r: PlayerStatRow) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 5.dp)
    ) {
        StatCell(if (r.kind == "pre") "${r.label} *" else r.label, 130.dp)
        StatCell(r.pj.toString(), 44.dp)
        StatCell(r.pt.toString(), 44.dp)
        StatCell("${r.min}'", 56.dp)
        StatCell(r.gols.toString(), 44.dp)
        StatCell(r.assists.toString(), 44.dp)
        StatCell(r.grogues.toString(), 44.dp)
        StatCell(r.roges.toString(), 44.dp)
    }
}

@Composable
private fun StatCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    header: Boolean = false
) {
    Text(
        text,
        modifier = Modifier.width(width),
        style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
        fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
        color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        textAlign = if (width == 130.dp) TextAlign.Start else TextAlign.Center,
        maxLines = 1
    )
}
