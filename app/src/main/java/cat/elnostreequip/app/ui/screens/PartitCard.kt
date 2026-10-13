package cat.elnostreequip.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.elnostreequip.app.data.ElNostreEquipRepository
import java.time.Duration
import java.time.OffsetDateTime

/**
 * Targeta del partit creat per Izan (tipus "partit" del WP).
 * Si el partit s'ha modificat fa poc (< 4h), mostra "EN JOC" en roig.
 * Fitxer separat perquè HomeScreen.kt no accepta funcions noves (trenca el build).
 */
@Composable
fun PartitDestacatCard(partit: ElNostreEquipRepository.PartitPost) {
    val enJoc = isEnJoc(partit)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enJoc)
                Color(0xFFFFEBEE) // roig clar de fons si està en joc
            else
                MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (enJoc) {
                // Insígnia EN JOC en roig
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFD32F2F))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Punt blanc polsant (estàtic)
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
            Text(
                relativeTime(partit.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Un partit està "en joc" si s'ha creat o modificat en les últimes 4 hores.
 * Izan actualitza el partit durant el directe, així que modified recent = en joc.
 */
private fun isEnJoc(partit: ElNostreEquipRepository.PartitPost): Boolean {
    return try {
        val ref = if (partit.modified.isNotEmpty()) partit.modified else partit.date
        if (ref.isEmpty()) return false
        val dt = OffsetDateTime.parse(ref)
        val now = OffsetDateTime.now()
        val hours = Duration.between(dt, now).toHours()
        hours in 0..4
    } catch (_: Exception) {
        false
    }
}
