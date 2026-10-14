package cat.elnostreequip.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Esdeveniment de la cronologia d'un partit (parsejat del HTML del web).
 * Ex: minut "76'", icona "🔄", text "Canvi - Ix Saray López, entra Blanca Garrido"
 */
data class TimelineEvent(
    val minute: String,
    val icon: String,
    val text: String
)

/**
 * Parsea la cronologia del Match Center des del HTML públic de la pàgina del partit.
 * El web renderitza la cronologia al servidor (div.ene-timeline), així que es pot
 * llegir sense accedir a l'admin.
 */
object TimelineParser {
    private val client = OkHttpClient()

    suspend fun fetchTimeline(partitUrl: String): List<TimelineEvent> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(partitUrl).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext emptyList()
                val html = response.body?.string() ?: return@withContext emptyList()
                parseTimeline(html)
            } catch (_: Exception) {
                emptyList()
            }
        }

    fun parseTimeline(html: String): List<TimelineEvent> {
        val events = mutableListOf<TimelineEvent>()
        val parts = html.split("<div class=\"ene-event\">").drop(1)
        for (part in parts) {
            try {
                val minuteMatch = Regex("""<div class="ene-minute">([^<]*)</div>""").find(part)
                val minute = minuteMatch?.groupValues?.get(1)?.trim() ?: ""
                val iconMatch = Regex("""<span class="ene-icon">([^<]*)</span>""").find(part)
                val icon = iconMatch?.groupValues?.get(1)?.trim() ?: ""
                val strongMatch = Regex("""<strong>(.*?)</strong>""", RegexOption.DOT_MATCHES_ALL).find(part)
                var text = strongMatch?.groupValues?.get(1)?.trim() ?: ""
                text = text.replace(Regex("<[^>]+>"), "").trim()
                val afterStrong = part.substringAfter("</strong>", "")
                    .substringBefore("</div>")
                    .replace(Regex("<[^>]+>"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                if (afterStrong.isNotEmpty() && afterStrong != text) {
                    text = if (text.isNotEmpty()) "$text — $afterStrong" else afterStrong
                }
                if (text.isNotEmpty() || minute.isNotEmpty()) {
                    events.add(TimelineEvent(minute, icon, text))
                }
            } catch (_: Exception) {
                continue
            }
        }
        return events
    }
}
