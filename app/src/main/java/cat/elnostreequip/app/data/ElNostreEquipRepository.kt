package cat.elnostreequip.app.data

import android.text.Html
import cat.elnostreequip.app.data.api.CalendarMatch
import cat.elnostreequip.app.data.api.EneApi
import cat.elnostreequip.app.data.api.WpApi
import cat.elnostreequip.app.data.api.WpPost
import cat.elnostreequip.app.data.api.featuredImageUrl
import cat.elnostreequip.app.data.api.dorsal
import cat.elnostreequip.app.data.api.field
import cat.elnostreequip.app.data.api.posicio
import com.google.gson.Gson
import cat.elnostreequip.app.data.db.AppDatabase
import cat.elnostreequip.app.data.db.ArticleEntity
import cat.elnostreequip.app.data.db.PlayerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio: WordPress REST API + caché Room.
 * Estrategia: la UI observa Room; refresh() trae de red y reemplaza la caché.
 */
class ElNostreEquipRepository(
    private val api: WpApi,
    private val db: AppDatabase,
    private val eneApi: EneApi
) {

    fun observeArticles(): Flow<List<ArticleEntity>> = db.articleDao().observeAll()

    suspend fun refreshArticles() {
        val posts = api.posts(perPage = 30)
        db.articleDao().replaceAll(posts.map { postToEntity(it) })
    }

    private fun postToEntity(p: WpPost) = ArticleEntity(
        id = p.id,
        title = (p.title?.rendered ?: "").htmlToText(),
        excerpt = (p.excerpt?.rendered ?: "").htmlToText().take(220),
        contentHtml = p.content?.rendered ?: "",
        imageUrl = p.embedded.featuredImageUrl(),
        date = p.date,
        link = p.link,
        teamId = TeamMapper.teamForCategories(p.categories)
    )

    suspend fun article(id: Long): ArticleEntity? = db.articleDao().get(id)

    /** Article per id: caché, i si no hi és, el baixa del web i el guarda. */
    suspend fun fetchArticle(id: Long): ArticleEntity? {
        db.articleDao().get(id)?.let { return it }
        return try {
            val entity = postToEntity(api.post(id))
            db.articleDao().insertAll(listOf(entity))
            entity
        } catch (_: Exception) {
            null
        }
    }

    fun observeSquad(equipId: Int): Flow<List<PlayerEntity>> =
        db.playerDao().observeByTeam(equipId)

    suspend fun refreshSquad(equipId: Int) {
        val list = api.jugadoras(equip = equipId)
        val entities = list.map { j ->
            // El plugin exposa ene_dorsal/ene_posicio; fallback a meta/ACF antics.
            val dorsal = j.dorsal()
                ?: j.field("dorsal", "numero", "dorsal_numero", "shirt_number")
                    ?.filter { it.isDigit() }?.toIntOrNull()
                ?: 999
            val position = j.posicio()
                ?: j.field("posicion", "posicio", "position", "demarcacion", "demarcacio")
                ?: ""
            PlayerEntity(
                id = j.id,
                name = (j.title?.rendered ?: "").htmlToText().ifBlank { "Jugadora" },
                dorsal = dorsal,
                position = position,
                photoUrl = j.embedded.featuredImageUrl(),
                bioHtml = j.content?.rendered ?: "",
                teamId = equipId,
                statsJson = Gson().toJson(j.eneStats ?: emptyList<Any>())
            )
        }
        db.playerDao().replaceByTeam(equipId, entities)
    }

    suspend fun player(id: Long): PlayerEntity? = db.playerDao().get(id)

    /** Búsqueda remota para seguir jugadoras (devuelve id + nombre + equipo aprox). */
    suspend fun searchPlayers(query: String): List<PlayerHit> =
        api.searchJugadoras(query).map { j ->
            PlayerHit(
                id = j.id,
                name = (j.title?.rendered ?: "").htmlToText().ifBlank { "Jugadora" },
                teamId = j.equip.firstOrNull(),
                photoUrl = j.embedded.featuredImageUrl()
            )
        }

    data class PlayerHit(val id: Long, val name: String, val teamId: Int?, val photoUrl: String?)

    /** Article lleuger de la categoria Partits (per vincular partits ↔ cròniques/prèvies). */
    data class RelatedArticle(val id: Long, val title: String, val link: String, val date: String)

    suspend fun partitsArticles(): List<RelatedArticle> =
        api.posts(perPage = 40, categories = listOf(PARTITS_CATEGORY), embed = false).map { p ->
            RelatedArticle(
                id = p.id,
                title = (p.title?.rendered ?: "").htmlToText(),
                link = p.link,
                date = p.date
            )
        }

    companion object {
        /** Categoria «Partits» del web (cròniques i prèvies). */
        const val PARTITS_CATEGORY = 8
    }

    /**
     * Partits del calendari (Google Calendar via plugin ene/v1/calendari).
     * Només lectura: els partits s'editen al WordPress.
     */
    suspend fun matches(equipId: Int): List<CalendarMatch> =
        eneApi.calendari(equipId).matches
}

private fun String.htmlToText(): String {
    return try {
        @Suppress("DEPRECATION")
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString().trim()
    } catch (_: Exception) {
        this
    }
}
