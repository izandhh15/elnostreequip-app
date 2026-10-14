package cat.elnostreequip.app.data.api

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/** DTOs mínimos del WordPress REST API de elnostreequip.cat */

data class Rendered(
    val rendered: String = ""
)

data class WpPost(
    val id: Long,
    val date: String = "",
    val slug: String = "",
    val link: String = "",
    val title: Rendered? = null,
    val content: Rendered? = null,
    val excerpt: Rendered? = null,
    val categories: List<Int> = emptyList(),
    val tags: List<Int> = emptyList(),
    @SerializedName("featured_media") val featuredMedia: Long = 0,
    @SerializedName("_embedded") val embedded: WpEmbedded? = null
)

data class WpJugadora(
    val id: Long,
    val slug: String = "",
    val link: String = "",
    val title: Rendered? = null,
    val content: Rendered? = null,
    /** IDs de la taxonomía `equip` (39=Primer Equip, 40=B, 41=C, 42=D) */
    val equip: List<Int> = emptyList(),
    /** Dorsal i posició exposats pel plugin «El Nostre Equip — Push & Directe». */
    @SerializedName("ene_dorsal") val eneDorsal: JsonElement? = null,
    @SerializedName("ene_posicio") val enePosicio: JsonElement? = null,
    @SerializedName("ene_stats") val eneStats: List<PlayerStatRow>? = null,
    @SerializedName("featured_media") val featuredMedia: Long = 0,
    /**
     * Meta/ACF: l'API de elnostreequip.cat retorna [] (array buit) quan no hi ha camps,
     * així que s'usa JsonElement per tolerar-ho (un Map petaria amb JsonSyntaxException).
     * El dorsal/posició venen del plugin (ene_dorsal/ene_posicio); açò és fallback.
     */
    val meta: JsonElement? = null,
    val acf: JsonElement? = null,
    @SerializedName("_embedded") val embedded: WpEmbedded? = null
)

data class WpTerm(
    val id: Int,
    val name: String = "",
    val slug: String = "",
    val count: Int = 0
)

data class WpMedia(
    val id: Long,
    @SerializedName("source_url") val sourceUrl: String = "",
    @SerializedName("media_details") val mediaDetails: WpMediaDetails? = null
)

data class WpMediaDetails(
    val sizes: Map<String, WpMediaSize>? = null
)

data class WpMediaSize(
    @SerializedName("source_url") val sourceUrl: String = ""
)

data class WpEmbedded(
    @SerializedName("wp:featuredmedia") val featuredMedia: List<WpMedia>? = null
)

/** Foto destacada: prefiere el tamaño mediano si existe, si no la original. */
fun WpEmbedded?.featuredImageUrl(): String? {
    val media = this?.featuredMedia?.firstOrNull() ?: return null
    val medium = media.mediaDetails?.sizes?.get("medium")?.sourceUrl
    return medium?.takeIf { it.isNotBlank() } ?: media.sourceUrl.takeIf { it.isNotBlank() }
}

/** Fila d'estadístiques del plugin (taula ene-stats-table). */
data class PlayerStatRow(
    val team: String = "",
    val label: String = "",
    /** comp = competició oficial, pre = pretemporada, total = total oficial */
    val kind: String = "comp",
    val pj: Int = 0,
    val pt: Int = 0,
    val min: Int = 0,
    val gols: Int = 0,
    val assists: Int = 0,
    val grogues: Int = 0,
    val roges: Int = 0
)

/** Dorsal del plugin (pot venir com a número o com a text). */
fun WpJugadora.dorsal(): Int? {
    val el = eneDorsal ?: return null
    if (!el.isJsonPrimitive) return null
    val p = el.asJsonPrimitive
    return when {
        p.isNumber -> p.asInt
        p.isString -> p.asString.filter { it.isDigit() }.toIntOrNull()
        else -> null
    }
}

/** Posició del plugin. */
fun WpJugadora.posicio(): String? {
    val el = enePosicio ?: return null
    if (!el.isJsonPrimitive) return null
    return el.asString.trim().takeIf { it.isNotBlank() }
}

/** Extrae un campo de texto de meta o acf (claves candidatas). Tolera arrays buits []. */
fun WpJugadora.field(vararg keys: String): String? {
    for (el in listOfNotNull(meta, acf)) {
        if (!el.isJsonObject) continue
        val obj = el.asJsonObject
        for (k in keys) {
            val v = obj.get(k) ?: continue
            if (!v.isJsonPrimitive) continue
            val s = v.asString.trim()
            if (s.isNotBlank() && s != "null") return s
        }
    }
    return null
}

/** Dades en directe del Match Center (via admin-ajax.php action=ene_get_match_data). */
data class MatchLiveResponse(
    val success: Boolean = false,
    val data: MatchLiveData? = null
)

data class MatchLiveData(
    @SerializedName("gols_local") val golsLocal: Int = 0,
    @SerializedName("goles_rival") val golsRival: Int = 0,
    @SerializedName("minut_directe") val minutDirecte: String = "",
    val estat: String = "",
    @SerializedName("valencia_es_local") val valenciaEsLocal: Boolean = true
)
