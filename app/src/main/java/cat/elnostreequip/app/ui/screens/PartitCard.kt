package cat.elnostreequip.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.elnostreequip.app.ElNostreEquipApp
import cat.elnostreequip.app.data.ElNostreEquipRepository
import cat.elnostreequip.app.data.api.MatchLiveData
import java.time.Duration
import java.time.OffsetDateTime
import kotlinx.coroutines.delay

/**
 * Targeta del partit creat per Izan (tipus "partit" del WP).
 * Si el partit s'ha creat fa poc (< 6h), mostra "EN JOC" en roig + marcador en directe.
 * Fitxer separat perquè HomeScreen.kt no accepta funcions noves (trenca el build).
 */
@Composable
fun PartitDestacatCard(partit: ElNostreEquipRepository.PartitPost) {
    val enJoc = isEnJoc(partit.date)
    val app = LocalContext.current.applicationContext as ElNostreEquipApp
    var live by remember { mutableStateOf<MatchLiveData?>(null) }

    // Si està en joc, carregar dades en directe i refrescar cada 30s
    if (enJoc) {
        LaunchedEffect(partit.id) {
            while (true) {
                try {
                    live = app.repository.matchLive(partit.id)
                } catch (_: Exception) {
                }
                delay(10_000)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enJoc) Color(0xFFFFEBEE)
            else MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (enJoc) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    if (live != null && live!!.minutDirecte.isNotEmpty()) {
                        Spacer(Modifier.weight(1f))
                        Text(
                            live!!.minutDirecte,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Text(
                    "PARTIT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.tertiary,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                partit.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Marcador en directe
            if (live != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "${live!!.golsLocal}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "-",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Text(
                        "${live!!.golsRival}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (live!!.estat.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        estatText(live!!.estat),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Text(
                    relativeTime(partit.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun isEnJoc(dateStr: String): Boolean {
    return try {
        if (dateStr.isEmpty()) return false
        val dt = OffsetDateTime.parse(dateStr)
        val hours = Duration.between(dt, OffsetDateTime.now()).toHours()
        hours in 0..6
    } catch (_: Exception) {
        false
    }
}

private fun estatText(estat: String): String = when (estat) {
    "primera" -> "Primera part"
    "segona" -> "Segona part"
    "descans" -> "Descans"
    "final" -> "Final del partit"
    "pre" -> "Prèvia"
    else -> estat
}
