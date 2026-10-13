package cat.elnostreequip.app.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * WordPress REST API — https://elnostreequip.cat/wp-json/wp/v2/
 * Todo es lectura pública, sin autenticación.
 */
interface WpApi {

    @GET("posts")
    suspend fun posts(
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1,
        @Query("orderby") orderBy: String = "date",
        @Query("order") order: String = "desc",
        @Query("_embed") embed: Boolean = true,
        @Query("categories") categories: List<Int>? = null,
        @Query("search") search: String? = null,
        @Query("_fields") fields: String? = null
    ): List<WpPost>

    @GET("posts/{id}")
    suspend fun post(
        @Path("id") id: Long,
        @Query("_embed") embed: Boolean = true
    ): WpPost

    @GET("jugadora")
    suspend fun jugadoras(
        @Query("equip") equip: Int,
        @Query("per_page") perPage: Int = 100,
        @Query("orderby") orderBy: String = "title",
        @Query("order") order: String = "asc",
        @Query("_embed") embed: Boolean = true
    ): List<WpJugadora>

    /** Búsqueda de jugadoras por nombre (para seguir jugadoras en Ajustos). */
    @GET("jugadora")
    suspend fun searchJugadoras(
        @Query("search") query: String,
        @Query("per_page") perPage: Int = 20,
        @Query("_embed") embed: Boolean = true
    ): List<WpJugadora>

    @GET("equip")
    suspend fun equips(
        @Query("per_page") perPage: Int = 20
    ): List<WpTerm>

    @GET("categories")
    suspend fun categories(
        @Query("per_page") perPage: Int = 100
    ): List<WpTerm>

    @GET("media/{id}")
    suspend fun media(@Path("id") id: Long): WpMedia
}
