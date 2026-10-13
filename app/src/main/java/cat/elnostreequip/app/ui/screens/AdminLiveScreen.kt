package cat.elnostreequip.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cat.elnostreequip.app.ElNostreEquipApp
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// TODO: substituir per un PIN real (ara mateix és temporal per a la fase alfa).
private const val TEMP_PIN = "1234"

private val EVENT_TYPES = listOf(
    "xi" to "XI inicial",
    "start" to "Inici",
    "goal" to "Gol",
    "yellow" to "Groga",
    "red" to "Roja",
    "ht" to "Descans",
    "ft" to "Final",
    "et" to "Pròrroga"
)

/**
 * Mode directe (ocult): Izan marca els esdeveniments del partit des del mòbil
 * i l'endpoint els converteix en push per als seguidors (DESIGN §6).
 * De moment l'endpoint és configurable i fa de stub fins que existisca el backend Wasmer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLiveScreen() {
    var unlocked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { EneTopBar("Mode directe") }
    ) { padding ->
        if (!unlocked) {
            PinGate(
                modifier = Modifier.padding(padding),
                onUnlock = { unlocked = true }
            )
        } else {
            AdminPanel(modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun PinGate(modifier: Modifier = Modifier, onUnlock: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Zona restringida", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Introdueix el PIN per obrir el mode directe.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it; error = false },
            label = { Text("PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = error
        )
        if (error) {
            Text("PIN incorrecte", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            if (pin == TEMP_PIN) onUnlock() else error = true
        }) { Text("Entrar") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminPanel(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as ElNostreEquipApp
    val topics = app.topicManager
    val scope = rememberCoroutineScope()

    val savedEndpoint by topics.adminEndpoint().collectAsState(initial = "")
    val savedSecret by topics.adminSecret().collectAsState(initial = "")

    var endpoint by remember(savedEndpoint) { mutableStateOf(savedEndpoint) }
    var secret by remember(savedSecret) { mutableStateOf(savedSecret) }
    var matchId by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf("goal") }
    var minute by remember { mutableStateOf("") }
    var playerWpId by remember { mutableStateOf("") }
    var playerName by remember { mutableStateOf("") }
    var assist by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Backend", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = { endpoint = it },
                        label = { Text("URL de l'endpoint") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = secret,
                        onValueChange = { secret = it },
                        label = { Text("X-Admin-Secret") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        scope.launch {
                            topics.setAdminEndpoint(endpoint)
                            topics.setAdminSecret(secret)
                            result = "Configuració guardada"
                        }
                    }) { Text("Guardar") }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Partit", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = matchId,
                        onValueChange = { matchId = it },
                        label = { Text("ID del partit (Firestore)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Esdeveniment", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(EVENT_TYPES) { (value, label) ->
                            FilterChip(
                                selected = eventType == value,
                                onClick = { eventType = value },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        OutlinedTextField(
                            value = minute,
                            onValueChange = { minute = it.filter(Char::isDigit) },
                            label = { Text("Minut") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = playerWpId,
                            onValueChange = { playerWpId = it.filter(Char::isDigit) },
                            label = { Text("ID jugadora (WP)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it },
                        label = { Text("Nom de la jugadora") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = assist,
                        onValueChange = { assist = it },
                        label = { Text("Assistència (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                sending = true
                                result = publishEvent(
                                    endpoint = endpoint.ifBlank { savedEndpoint },
                                    secret = secret,
                                    matchId = matchId,
                                    tipo = eventType,
                                    minute = minute.toIntOrNull(),
                                    playerWpId = playerWpId.toLongOrNull(),
                                    playerName = playerName,
                                    assist = assist.ifBlank { null }
                                )
                                sending = false
                            }
                        },
                        enabled = !sending && matchId.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (sending) "Publicant…" else "Publicar esdeveniment")
                    }
                    result?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/**
 * POST al endpoint del backend (DESIGN §6). Stub funcional: quan l'endpoint
 * Wasmer existisca, publicarà l'esdeveniment a Firestore + FCM topics.
 */
private suspend fun publishEvent(
    endpoint: String,
    secret: String,
    matchId: String,
    tipo: String,
    minute: Int?,
    playerWpId: Long?,
    playerName: String,
    assist: String?
): String = withContext(Dispatchers.IO) {
    try {
        val json = JSONObject().apply {
            put("matchId", matchId)
            put("tipo", tipo)
            minute?.let { put("minuto", it) }
            playerWpId?.let { put("jugadorWpId", it) }
            put("jugadorNom", playerName)
            assist?.let { put("asistente", it) }
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("X-Admin-Secret", secret)
            .post(body)
            .build()
        OkHttpClient().newCall(request).execute().use { resp ->
            if (resp.isSuccessful) "Publicat ✓ (${resp.code})"
            else "Error ${resp.code}: ${resp.message}"
        }
    } catch (e: Exception) {
        "No s'ha pogut publicar: ${e.message}"
    }
}
