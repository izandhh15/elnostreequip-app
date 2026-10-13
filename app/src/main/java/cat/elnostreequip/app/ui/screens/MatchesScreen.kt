package cat.elnostreequip.app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cat.elnostreequip.app.ElNostreEquipApp
import cat.elnostreequip.app.data.TeamMapper
import cat.elnostreequip.app.data.api.CalendarMatch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Partits: calendari oficial de cada equip (Google Calendar via plugin ene/v1/calendari).
 * Només lectura: els partits s'editen al WordPress.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen() {
    val app = LocalContext.current.applicationContext as ElNostreEquipApp
    var selectedTeam by remember { mutableIntStateOf(TeamMapper.PRIMER_EQUIP) }
    var matches by remember { mutableStateOf<List<CalendarMatch>?>(null) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTeam) {
        matches = null
        error = false
        try {
            matches = app.repository.matches(selectedTeam)
        } catch (_: Exception) {
            error = true
        }
    }

    Scaffold(
        topBar = { EneTopBar("Partits") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Selector d'equip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TeamMapper.ALL) { equipId ->
                    FilterChip(
                        selected = equipId == selectedTeam,
                        onClick = { selectedTeam = equipId },
                        label = { Text(TeamMapper.shortName(equipId)) }
                    )
                }
            }

            when {
                error -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No s'ha pogut carregar el calendari.\nComprova la connexió i torna-ho a provar.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                matches == null -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Carregant calendari…")
                    }
                }
                matches!!.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hi ha partits al calendari.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    val upcoming = matches!!.filter { !it.past }
                    val past = matches!!.filter { it.past }.reversed()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (upcoming.isNotEmpty()) {
                            item {
                                Text(
                                    "Pròxims",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(upcoming, key = { it.id }) { match ->
                                MatchCard(match)
                            }
                        }
                        if (past.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Anteriors",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(past, key = { it.id }) { match ->
                                MatchCard(match)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchCard(match: CalendarMatch) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (match.competition.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            match.competition,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    formatMatchDate(match.start),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${match.home} – ${match.away}".trim(' ', '–'),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private val dateFmt = DateTimeFormatter.ofPattern("EEEE d MMMM · HH:mm", Locale("ca"))

private fun formatMatchDate(iso: String): String {
    return try {
        val dt = OffsetDateTime.parse(iso)
        dateFmt.format(dt).replaceFirstChar { it.uppercase() }
    } catch (_: Exception) {
        iso.take(16).replace("T", " ")
    }
}
