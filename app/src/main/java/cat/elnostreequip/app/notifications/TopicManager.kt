package cat.elnostreequip.app.notifications

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore("ene_prefs")

/**
 * Gestiona las suscripciones a topics FCM según las preferencias del usuario.
 * No hay servidor de tokens: todo va por topics (ver DESIGN §4).
 *
 * Equipos E ∈ {39,40,41,42}:
 *   articles_E, matchday_E,
 *   live_E_xi, live_E_start, live_E_goal, live_E_yellow, live_E_red,
 *   live_E_ht, live_E_ft, live_E_et
 * Jugadora seguida (id WP J):
 *   player_J_starter, player_J_goal, player_J_yellow, player_J_red
 *
 * NOTA: sin google-services.json (proyecto Firebase aún no creado por Izan),
 * FirebaseMessaging lanza excepción; se captura y se registra para no romper la app.
 */
class TopicManager(private val context: Context) {

    enum class LiveKind(val suffix: String) {
        XI("xi"),
        START("start"),
        GOAL("goal"),
        YELLOW("yellow"),
        RED("red"),
        HT("ht"),
        FT("ft"),
        ET("et")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * True si Firebase està configurat (google-services.json present).
     * Sense el projecte Firebase d'Izan, les subscripcions a topics no poden funcionar;
     * la UI ho mostra amb un avís en comptes de fallar en silenci.
     */
    fun isPushAvailable(): Boolean = try {
        FirebaseMessaging.getInstance()
        true
    } catch (e: Exception) {
        Log.w(TAG, "FCM no disponible (falta configurar Firebase): ${e.message}")
        false
    }

    private fun teamKey(e: Int) = booleanPreferencesKey("team_$e")
    private fun articlesKey(e: Int) = booleanPreferencesKey("articles_$e")
    private fun matchdayKey(e: Int) = booleanPreferencesKey("matchday_$e")
    private fun liveKey(e: Int, k: LiveKind) = booleanPreferencesKey("live_${e}_${k.suffix}")
    private val playersKey = stringSetPreferencesKey("players")
    private val adminEndpointKey = stringPreferencesKey("admin_endpoint")
    private val adminSecretKey = stringPreferencesKey("admin_secret")

    // ---- Lectura (Flows para la UI) ----

    fun isTeamFollowed(equipId: Int): Flow<Boolean> =
        context.dataStore.data.map { it[teamKey(equipId)] ?: false }

    fun articlesPref(equipId: Int): Flow<Boolean> =
        context.dataStore.data.map { it[articlesKey(equipId)] ?: false }

    fun matchdayPref(equipId: Int): Flow<Boolean> =
        context.dataStore.data.map { it[matchdayKey(equipId)] ?: false }

    fun livePref(equipId: Int, kind: LiveKind): Flow<Boolean> =
        context.dataStore.data.map { it[liveKey(equipId, kind)] ?: false }

    fun followedPlayers(): Flow<Set<Long>> =
        context.dataStore.data.map { prefs ->
            prefs[playersKey]?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
        }

    fun isPlayerFollowed(wpId: Long): Flow<Boolean> =
        followedPlayers().map { wpId in it }

    fun adminEndpoint(): Flow<String> =
        context.dataStore.data.map { it[adminEndpointKey] ?: DEFAULT_ENDPOINT }

    fun adminSecret(): Flow<String> =
        context.dataStore.data.map { it[adminSecretKey] ?: "" }

    // ---- Escritura ----

    /** Seguir/dejar de seguir un equipo (baseline: artículos + día de partido). */
    suspend fun setTeamFollowed(equipId: Int, on: Boolean) {
        setArticles(equipId, on)
        setMatchday(equipId, on)
        if (!on) {
            // Al dejar de seguir, desuscribir también todo el directo de ese equipo.
            LiveKind.values().forEach { setLivePref(equipId, it, false) }
        }
        context.dataStore.edit { it[teamKey(equipId)] = on }
    }

    suspend fun setArticles(equipId: Int, on: Boolean) {
        setTopic("articles_$equipId", on)
        context.dataStore.edit { it[articlesKey(equipId)] = on }
    }

    suspend fun setMatchday(equipId: Int, on: Boolean) {
        setTopic("matchday_$equipId", on)
        context.dataStore.edit { it[matchdayKey(equipId)] = on }
    }

    suspend fun setLivePref(equipId: Int, kind: LiveKind, on: Boolean) {
        setTopic("live_${equipId}_${kind.suffix}", on)
        context.dataStore.edit { it[liveKey(equipId, kind)] = on }
    }

    /** Seguir una jugadora: suscribe player_J_starter/goal/yellow/red. */
    suspend fun setPlayerFollowed(wpId: Long, on: Boolean) {
        listOf("starter", "goal", "yellow", "red").forEach { suffix ->
            setTopic("player_${wpId}_$suffix", on)
        }
        context.dataStore.edit { prefs ->
            val current = prefs[playersKey]?.toMutableSet() ?: mutableSetOf()
            if (on) current.add(wpId.toString()) else current.remove(wpId.toString())
            prefs[playersKey] = current
        }
    }

    suspend fun setAdminEndpoint(url: String) {
        context.dataStore.edit { it[adminEndpointKey] = url.trim() }
    }

    suspend fun setAdminSecret(secret: String) {
        context.dataStore.edit { it[adminSecretKey] = secret }
    }

    // ---- FCM ----

    private fun setTopic(topic: String, subscribe: Boolean) {
        scope.launch {
            try {
                val fm = FirebaseMessaging.getInstance()
                val task = if (subscribe) fm.subscribeToTopic(topic) else fm.unsubscribeFromTopic(topic)
                task.addOnFailureListener { e ->
                    Log.w(TAG, "Topic $topic (on=$subscribe) falló: ${e.message}")
                }
            } catch (e: Exception) {
                // Sin google-services.json Firebase no está inicializado: no romper la app.
                Log.w(TAG, "FCM no disponible (falta proyecto Firebase): $topic (${e.message})")
            }
        }
    }

    companion object {
        private const val TAG = "TopicManager"
        // Endpoint del plugin WP "El Nostre Equip — Push & Directe" (la web esta en Nominalia).
        const val DEFAULT_ENDPOINT = "https://elnostreequip.cat/wp-json/ene/v1/live"
    }
}
