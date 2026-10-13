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
    val title: Rendered = Rendered(),
    val content: Rendered = Rendered(),
    val excerpt: Rendered = Rendered(),
    val categories: List<Int> = emptyList(),
    val tags: List<Int> = emptyList(),
    @SerializedName("featured_media") val featuredMedia: Long = 0,
    @SerializedName("_embedded") val embedded: WpEmbedded? = null
)

data class WpJugadora(
    val id: Long,
    val slug: String = "",
    val link: String = "",
    val title: Rendered = Rendered(),
    val content: Rendered = Rendered(),
    /** IDs de la taxonomía `equip` (39=Primer Equip, 40=B, 41=C, 42=D) */
    val equip: List<Int> = emptyList(),
    @SerializedName("featured_media") val featuredMedia: Long = 0,
    /**
     * Meta/ACF: l'API de elnostreequip.cat retorna [] (array buit) quan no hi ha camps,
     * així que s'usa JsonElement per tolerar-ho (un Map petaria amb JsonSyntaxException).
     * El dorsal/posició no estan exposats a la REST API de moment.
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
