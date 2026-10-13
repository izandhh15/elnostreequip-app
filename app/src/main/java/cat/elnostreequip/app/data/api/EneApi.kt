package cat.elnostreequip.app.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/** Resposta de GET /ene/v1/calendari */
data class CalendarResponse(
    val equip: Int = 0,
    val matches: List<CalendarMatch> = emptyList()
)

data class CalendarMatch(
    val id: String = "",
    val title: String = "",
    val competition: String = "",
    val home: String = "",
    val away: String = "",
    val start: String = "",
    val end: String? = null,
    val past: Boolean = false
)

/**
 * API del plugin «El Nostre Equip — Push & Directe» (wp-json/ene/v1/).
 * Només lectura: els partits s'editen al WordPress.
 */
interface EneApi {
    @GET("calendari")
    suspend fun calendari(@Query("equip") equip: Int): CalendarResponse
}

object EneClient {
    const val BASE_URL = "https://elnostreequip.cat/wp-json/ene/v1/"

    val api: EneApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(RetrofitClient.client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EneApi::class.java)
    }
}
