# El Nostre Equip App — Diseño técnico (v1)
Fecha: 2026-10-13. Plataforma elegida por Izan: **Android nativa (APK)**.

## 1. Proyecto Android
- Kotlin + Jetpack Compose, `package cat.elnostreequip.app`, minSdk 26.
- Entorno (verificado en esta VM, reutilizar de AndorraTV):
  - JDK: `~/workspace/.tools/jdk-17.0.20.1+1` (persistente; NO apt tras reemplazo de VM)
  - Android SDK: `~/workspace/android/sdk/`, `GRADLE_USER_HOME=/home/hatch/.gradle`
  - Gradle 8.7 + AGP 8.5.2. Proxy local puerto 8888 si hace falta (`~/workspace/android/proxy.py`).
  - Si la VM se reemplaza: relanzar proxy, reimportar CA de Hatch al cacerts del JDK, reiniciar daemon Gradle.
- Dependencias: Retrofit + Moshi/Gson, Room (caché), Coil (fotos), Firebase BOM (messaging, firestore).
- **NO aplicar el plugin `com.google.gms.google-services` todavía** (no hay `google-services.json`; se integra cuando Izan cree el proyecto Firebase). El SDK de FCM compila sin él.

## 2. Fuente de datos — WordPress REST API
Base: `https://elnostreequip.cat/wp-json/wp/v2/`
- `posts?per_page=…` → artículos (previas, crónicas, noticias). Incluyen `categories`, `tags`, `featured_media`, `content.rendered`, `date`.
- `jugadora?equip={id}&per_page=100` → plantillas. IDs de taxonomía `equip` (verificar): 39=Primer Equip, 40=València CF Femení B, 41=C, 42=D.
- `equip` → nombres de los 4 equipos.
- `media/{id}` → fotos (featured de jugadoras y artículos).
- Pendiente de verificar en el REST: qué campos expone cada `jugadora` (¿`meta`/ACF con dorsal, posición, fecha nacimiento? ¿foto destacada en todas?). Si el dorsal no viene en el API, plan B: parsear el HTML de la ficha o pedir a Izan que lo añada como campo.
- Filtrado de artículos por equipo: comprobar si los posts llevan taxonomía `equip` o hay que mapear `categories`→equipo (revisar `/wp-json/wp/v2/categories`). Definir el mapeo en `TeamMapper.kt` y documentarlo.

## 3. Pantallas
1. **Inici**: selector del equipo seguido (chips: Primer Equip/B/C/D, multi-selección) + feed de artículos filtrado. Pull-to-refresh, caché Room.
2. **Article**: render del `content.rendered` (WebView o Compose HTML).
3. **Equip**: lista de plantilla ordenada por dorsal — fila `dorsal · foto · nom` (+ posición pequeña). Tap → ficha.
4. **Jugadora**: capçalera (foto, dorsal, nom, posición, equipo) + pestanya **Estadístiques**: tabla por partido (data, rival, minuts, gols, assistències, titular/suplent, targetes) + totales de temporada. Datos desde Firestore (sección 5). Sin datos → texto: "Les estadístiques apareixeran quan el partit es cobrisca en directe".
5. **Partits**: pròxims (fixtures) + en directe (estado desde Firestore).
6. **Ajustos → Notificacions**: matriz de toggles (sección 4) + seguir/dejar de seguir jugadoras (búsqueda por nombre).
7. **Admin — Mode directe** (oculto, acceso con PIN): elegir partido de hoy → botones de evento. Llama al endpoint Wasmer (sección 6).

Idioma de la UI: **valenciano** (como la web).

## 4. Notificaciones — FCM topics (sin servidor de tokens)
La app suscribe/desuscribe topics según los toggles del usuario. Equipos E ∈ {39,40,41,42}:
- `articles_E` — artículo nuevo de ese equipo
- `matchday_E` — día de partido (aviso por la mañana)
- `live_E_xi` — XI disponible
- `live_E_start` — inicio del partido
- `live_E_goal` — goles (`[minuto] Gol de [jugadora] (asistencia: [X])`)
- `live_E_yellow` — amarillas
- `live_E_red` — rojas
- `live_E_ht` — descanso
- `live_E_ft` — final
- `live_E_et` — prórroga
Por jugadora seguida (id WP J):
- `player_J_starter` — "`[nombre]` es titular en `[local] - [visitante]`"
- `player_J_goal` — "`[minuto] Gol de [nombre]`"
- `player_J_yellow`, `player_J_red`
Caso de uso de Izan: sigue al B (topics `articles_40`, `live_40_*`…) + a una jugadora del C (`player_J_*`) sin seguir al C entero.
`TopicManager.kt`: `setTeamFollowed(equipId, on)`, `setLivePref(equipId, tipo, on)`, `setPlayerFollowed(wpId, on)` → subscribe/unsubscribe + persistencia en DataStore.

## 5. Firestore (cuando exista el proyecto Firebase)
- `matches/{id}`: `{ equipId, competicion, fecha, local, visitante, estado: scheduled|live|finished, marcadorLocal, marcadorVisitante }`
- `matches/{id}/events/{eventId}`: `{ tipo: xi|start|goal|yellow|red|sub|ht|ft|et, minuto, jugadorWpId, asistenteWpId, texto }`
- `players/{wpId}`: agregados de temporada `{ partidos, titularidades, minutos, goles, asistencias, amarillas, rojas }` (recalculados por el endpoint al escribir eventos; la app también puede derivar por partido leyendo `events`).
La pestaña de estadísticas lee `matches` (filtrando por equipo y por `events` con ese `jugadorWpId`).

## 6. Backend
- **Watcher de artículos** (cron en el runtime): poll a `/wp-json/wp/v2/posts?orderby=date&order=desc` cada ~10 min; si hay nuevos → FCM HTTP v1 al topic `articles_E` (mapeo categoría→equipo). Estado (último `date` visto) en `hidden_files/` del goal.
- **Día de partido** (cron diario ~08:00): lee fixtures (JSON generado del calendario de elnostreequip@gmail.com o del repo `~/workspace/elnostreequip/`) → si hoy juega E → FCM a `matchday_E`.
- **Endpoint Wasmer** (PHP, app gratuita nueva p. ej. `elnostreequip-push.wasmer.app`): `POST /api/publish` con `X-Admin-Secret`. Body: `{ matchId, tipo, minuto, jugadorWpId, ... }`. Hace dos cosas: (1) escribe el evento en Firestore vía REST, (2) publica FCM HTTP v1 a los topics que toquen (equipo + jugadora si aplica). El secreto y la service account van en variables de entorno de Wasmer, nunca en la app.
- **Modo directo**: Izan abre la pantalla admin en la app (PIN), elige el partido de hoy, toca botones (XI → pega la alineación o la elige de la plantilla; Gol → minuto + jugadora + asistencia opcional; etc.). Cada toque → `POST /api/publish` → push instantáneo a los seguidores.

## 7. Fases
1. Scaffold + cliente WP + pantallas de lectura (feed, equipo, jugadora sin stats) → APK alfa instalable sin push.
2. TopicManager + ajustes de notificaciones + FCM (requiere proyecto Firebase de Izan).
3. Firestore + stats por partido + modo directo + endpoint Wasmer.
4. Crons watcher (artículos, día de partido). Backfill opcional: parsear crónicas publicadas para sembrar eventos de la temporada.
