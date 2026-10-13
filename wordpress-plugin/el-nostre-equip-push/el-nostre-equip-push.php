<?php
/**
 * Plugin Name: El Nostre Equip — Push & Directe
 * Description: Connecta elnostreequip.cat amb l'app Android: envia notificacions push via Firebase Cloud Messaging (topics), exposa dorsal i posició de les jugadores a la REST API i rep els esdeveniments del mode directe de l'app.
 * Version: 1.0.0
 * Author: El Nostre Equip
 * License: GPLv2 or later
 * Text Domain: ene-push
 */

defined('ABSPATH') || exit;

define('ENE_PUSH_VERSION', '1.0.0');
define('ENE_PUSH_OPT', 'ene_push_settings');
define('ENE_PUSH_NS', 'ene/v1');

/* ==========================================================================
 * Configuració
 * ========================================================================== */

function ene_push_defaults() {
    return array(
        'service_account' => '',   // JSON del compte de servei de Firebase
        'secret'          => '',   // secret per a POST /ene/v1/live (mode directe de l'app)
        'cat_map'         => '',   // línies "id_categoria=id_equip" (p. ex. "12=40")
        'auto_articles'   => 1,    // envia push automàtic en publicar un article
    );
}

function ene_push_get($key) {
    $s = get_option(ENE_PUSH_OPT, array());
    $s = wp_parse_args($s, ene_push_defaults());
    return isset($s[$key]) ? $s[$key] : ene_push_defaults()[$key];
}

add_action('admin_menu', function () {
    add_options_page(
        'El Nostre Equip — Push',
        'ENE Push',
        'manage_options',
        'ene-push',
        'ene_push_settings_page'
    );
});

add_action('admin_init', function () {
    register_setting('ene_push_group', ENE_PUSH_OPT, array(
        'sanitize_callback' => 'ene_push_sanitize',
    ));
});

function ene_push_sanitize($in) {
    $out = ene_push_defaults();
    // El JSON del compte de servei es guarda tal qual (es valida en usar-lo).
    if (isset($in['service_account'])) {
        $out['service_account'] = trim(wp_unslash($in['service_account']));
    }
    if (isset($in['secret'])) {
        $out['secret'] = sanitize_text_field($in['secret']);
    }
    if (isset($in['cat_map'])) {
        $out['cat_map'] = sanitize_textarea_field($in['cat_map']);
    }
    $out['auto_articles'] = !empty($in['auto_articles']) ? 1 : 0;
    return $out;
}

function ene_push_settings_page() {
    if (!current_user_can('manage_options')) return;
    $s = wp_parse_args(get_option(ENE_PUSH_OPT, array()), ene_push_defaults());
    ?>
    <div class="wrap">
        <h1>El Nostre Equip — Push &amp; Directe</h1>
        <p>Connecta la web amb l'app Android. Cal un projecte Firebase amb Cloud Messaging activat.</p>
        <form method="post" action="options.php">
            <?php settings_fields('ene_push_group'); ?>
            <table class="form-table">
                <tr>
                    <th scope="row"><label for="ene_sa">Compte de servei de Firebase (JSON)</label></th>
                    <td>
                        <textarea id="ene_sa" name="<?php echo esc_attr(ENE_PUSH_OPT); ?>[service_account]" rows="8" cols="70" class="large-text code" placeholder='{"type": "service_account", ...}'><?php echo esc_textarea($s['service_account']); ?></textarea>
                        <p class="description">Firebase Console → Configuració del projecte → Comptes de servei → Genera una nova clau privada. Enganxa ací el contingut del JSON.</p>
                    </td>
                </tr>
                <tr>
                    <th scope="row"><label for="ene_secret">Secret del mode directe</label></th>
                    <td>
                        <input id="ene_secret" type="text" name="<?php echo esc_attr(ENE_PUSH_OPT); ?>[secret]" value="<?php echo esc_attr($s['secret']); ?>" class="regular-text" />
                        <p class="description">L'app l'envia en la capçalera <code>X-ENE-Secret</code> quan publiques esdeveniments en directe. Fes-lo llarg i aleatori.</p>
                    </td>
                </tr>
                <tr>
                    <th scope="row"><label for="ene_catmap">Categories → equip</label></th>
                    <td>
                        <textarea id="ene_catmap" name="<?php echo esc_attr(ENE_PUSH_OPT); ?>[cat_map]" rows="5" cols="30" class="code"><?php echo esc_textarea($s['cat_map']); ?></textarea>
                        <p class="description">Una línia per categoria: <code>id_categoria=id_equip</code> (equips: 39 = Primer Equip, 40 = B, 41 = C, 42 = D).<br>En publicar un article s'envia push al topic <code>articles_{id_equip}</code> de cada equip mapejat.</p>
                    </td>
                </tr>
                <tr>
                    <th scope="row">Articles automàtics</th>
                    <td>
                        <label><input type="checkbox" name="<?php echo esc_attr(ENE_PUSH_OPT); ?>[auto_articles]" value="1" <?php checked($s['auto_articles'], 1); ?> /> Envia push quan es publique un article</label>
                    </td>
                </tr>
            </table>
            <?php submit_button('Desa els canvis'); ?>
        </form>
        <hr>
        <h2>Prova d'enviament</h2>
        <form method="post">
            <?php wp_nonce_field('ene_push_test', 'ene_push_test_nonce'); ?>
            <input type="hidden" name="ene_push_test_send" value="1" />
            <p><label>Topic <input type="text" name="ene_test_topic" value="articles_40" class="regular-text" /></label></p>
            <p><?php submit_button('Envia notificació de prova', 'secondary'); ?></p>
        </form>
    </div>
    <?php
}

add_action('admin_init', function () {
    if (!empty($_POST['ene_push_test_send']) && check_admin_referer('ene_push_test', 'ene_push_test_nonce') && current_user_can('manage_options')) {
        $topic = sanitize_text_field(wp_unslash($_POST['ene_test_topic']));
        $ok = ene_push_send($topic, 'Prova — El Nostre Equip', 'Si reps açò al mòbil, el push funciona.', array('kind' => 'test'));
        add_settings_error('ene_push_group', 'ene_push_test', $ok ? "Notificació enviada al topic $topic." : 'Ha fallat l\'enviament. Revisa el JSON del compte de servei.', $ok ? 'updated' : 'error');
    }
});

/* ==========================================================================
 * Firebase Cloud Messaging (HTTP v1, sense dependències externes)
 * ========================================================================== */

function ene_push_b64url($data) {
    return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
}

/** Access token OAuth2 a partir del JSON del compte de servei (cachejat 55 min). */
function ene_push_access_token() {
    $cached = get_transient('ene_push_fcm_token');
    if ($cached) return $cached;

    $sa_raw = ene_push_get('service_account');
    if (!$sa_raw) return new WP_Error('ene_no_sa', 'Falta el JSON del compte de servei.');
    $sa = json_decode($sa_raw, true);
    if (!is_array($sa) || empty($sa['client_email']) || empty($sa['private_key']) || empty($sa['project_id'])) {
        return new WP_Error('ene_bad_sa', 'El JSON del compte de servei no és vàlid.');
    }
    if (!function_exists('openssl_sign')) {
        return new WP_Error('ene_no_ssl', 'Cal l\'extensió OpenSSL de PHP.');
    }

    $now = time();
    $jwt = ene_push_b64url(wp_json_encode(array('alg' => 'RS256', 'typ' => 'JWT')))
        . '.' . ene_push_b64url(wp_json_encode(array(
            'iss'   => $sa['client_email'],
            'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
            'aud'   => 'https://oauth2.googleapis.com/token',
            'iat'   => $now,
            'exp'   => $now + 3600,
        )));
    $ok = openssl_sign($jwt, $sig, $sa['private_key'], OPENSSL_ALGO_SHA256);
    if (!$ok) return new WP_Error('ene_sign', 'No s\'ha pogut signar el JWT.');
    $jwt .= '.' . ene_push_b64url($sig);

    $res = wp_remote_post('https://oauth2.googleapis.com/token', array(
        'timeout' => 15,
        'body'    => array(
            'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
            'assertion'  => $jwt,
        ),
    ));
    if (is_wp_error($res)) return $res;
    $body = json_decode(wp_remote_retrieve_body($res), true);
    if (empty($body['access_token'])) {
        return new WP_Error('ene_token', 'Google no ha tornat access_token.');
    }
    set_transient('ene_push_fcm_token', array($body['access_token'], $sa['project_id']), 55 * MINUTE_IN_SECONDS);
    return array($body['access_token'], $sa['project_id']);
}

/**
 * Envia una notificació a un topic FCM.
 * Torna true si FCM respon OK, WP_Error en cas contrari.
 */
function ene_push_send($topic, $title, $body, $data = array()) {
    $tok = ene_push_access_token();
    if (is_wp_error($tok)) return $tok;
    list($access_token, $project_id) = $tok;

    $data_str = array();
    foreach ((array) $data as $k => $v) {
        $data_str[(string) $k] = (string) $v; // FCM data només accepta strings
    }

    $res = wp_remote_post(
        'https://fcm.googleapis.com/v1/projects/' . rawurlencode($project_id) . '/messages:send',
        array(
            'timeout' => 15,
            'headers' => array(
                'Authorization' => 'Bearer ' . $access_token,
                'Content-Type'  => 'application/json; charset=utf-8',
            ),
            'body' => wp_json_encode(array(
                'message' => array(
                    'topic'        => $topic,
                    'notification' => array('title' => $title, 'body' => $body),
                    'data'         => $data_str,
                    'android'      => array('priority' => 'high'),
                ),
            )),
        )
    );
    if (is_wp_error($res)) return $res;
    $code = wp_remote_retrieve_response_code($res);
    if ($code < 200 || $code >= 300) {
        return new WP_Error('ene_fcm', 'FCM ha respost ' . $code . ': ' . wp_remote_retrieve_body($res));
    }
    return true;
}

/* ==========================================================================
 * REST API: mode directe (l'app publica, el plugin reparteix)
 * ========================================================================== */

add_action('rest_api_init', function () {
    register_rest_route(ENE_PUSH_NS, '/live', array(
        'methods'             => 'POST',
        'callback'            => 'ene_push_rest_live',
        'permission_callback' => '__return_true', // s'autentica amb el secret
    ));
});

/**
 * POST /wp-json/ene/v1/live
 * Capçalera: X-ENE-Secret: <secret>
 * Cos JSON: { "equip": 40, "kind": "goal", "title": "...", "body": "...",
 *             "player_id": 123 (opcional), "minute": 67 (opcional) }
 * kinds: xi, start, goal, yellow, red, ht, ft, et
 */
function ene_push_rest_live(WP_REST_Request $req) {
    $secret = ene_push_get('secret');
    if (!$secret) {
        return new WP_Error('ene_no_secret', 'El secret del mode directe no està configurat.', array('status' => 503));
    }
    $given = $req->get_header('x-ene-secret');
    if (!is_string($given) || !hash_equals($secret, $given)) {
        return new WP_Error('ene_forbidden', 'Secret incorrecte.', array('status' => 403));
    }

    $p = $req->get_json_params();
    $equip = isset($p['equip']) ? absint($p['equip']) : 0;
    $kind  = isset($p['kind']) ? sanitize_key($p['kind']) : '';
    $valid = array('xi', 'start', 'goal', 'yellow', 'red', 'ht', 'ft', 'et');
    if (!$equip || !in_array($kind, $valid, true)) {
        return new WP_Error('ene_bad_params', 'Cal "equip" (39-42) i "kind" vàlid.', array('status' => 400));
    }
    $title = isset($p['title']) ? sanitize_text_field($p['title']) : '';
    $body  = isset($p['body']) ? sanitize_text_field($p['body']) : '';
    if ($title === '' && $body === '') {
        return new WP_Error('ene_bad_params', 'Cal "title" o "body".', array('status' => 400));
    }

    $data = array('equip' => (string) $equip, 'kind' => $kind, 'ts' => (string) time());
    if (isset($p['minute'])) $data['minute'] = (string) absint($p['minute']);
    $player_id = isset($p['player_id']) ? absint($p['player_id']) : 0;
    if ($player_id) $data['player_id'] = (string) $player_id;

    $topics = array("live_{$equip}_{$kind}");
    // També avisa les seguidores de la jugadora quan l'esdeveniment és seu.
    $player_map = array('xi' => 'starter', 'goal' => 'goal', 'yellow' => 'yellow', 'red' => 'red');
    if ($player_id && isset($player_map[$kind])) {
        $topics[] = "player_{$player_id}_{$player_map[$kind]}";
    }

    $errors = array();
    foreach ($topics as $t) {
        $r = ene_push_send($t, $title, $body, $data);
        if (is_wp_error($r)) $errors[] = $t . ': ' . $r->get_error_message();
    }
    if ($errors) {
        return new WP_Error('ene_partial', implode(' | ', $errors), array('status' => 502));
    }
    return array('ok' => true, 'topics' => $topics);
}

/* ==========================================================================
 * Push automàtic en publicar articles
 * ========================================================================== */

add_action('transition_post_status', function ($new, $old, $post) {
    if ($new !== 'publish' || $old === 'publish') return;
    if ($post->post_type !== 'post') return;
    if (!ene_push_get('auto_articles')) return;

    $equips = ene_push_equips_for_post($post->ID);
    if (!$equips) return;

    $title = get_the_title($post->ID);
    $excerpt = has_excerpt($post->ID) ? get_the_excerpt($post->ID) : wp_trim_words(wp_strip_all_tags($post->post_content), 18);
    foreach ($equips as $e) {
        ene_push_send(
            "articles_{$e}",
            'Nou article — El Nostre Equip',
            $title,
            array('post_id' => (string) $post->ID, 'equip' => (string) $e, 'link' => get_permalink($post->ID))
        );
    }
}, 10, 3);

/** Llig les categories de l'article amb equips segons el mapa de la configuració. */
function ene_push_equips_for_post($post_id) {
    $map_raw = ene_push_get('cat_map');
    if (trim($map_raw) === '') return array();
    $map = array();
    foreach (preg_split('/\r?\n/', $map_raw) as $line) {
        $line = trim($line);
        if ($line === '' || strpos($line, '=') === false) continue;
        list($cat, $equip) = array_map('trim', explode('=', $line, 2));
        if (is_numeric($cat) && is_numeric($equip)) $map[(int) $cat] = (int) $equip;
    }
    $cats = wp_get_post_categories($post_id);
    $out = array();
    foreach ($cats as $c) {
        if (isset($map[$c])) $out[] = $map[$c];
    }
    return array_values(array_unique($out));
}

/* ==========================================================================
 * REST API: dorsal i posició de les jugadores
 * ========================================================================== */

add_action('rest_api_init', function () {
    $keys_dorsal  = apply_filters('ene_jugadora_dorsal_keys', array('dorsal', 'numero', 'shirt_number'));
    $keys_posicio = apply_filters('ene_jugadora_posicio_keys', array('posicio', 'posicion', 'position', 'demarcacio', 'demarcacion'));

    register_rest_field('jugadora', 'ene_dorsal', array(
        'get_callback' => function ($post) use ($keys_dorsal) {
            return ene_push_first_meta($post['id'], $keys_dorsal);
        },
        'schema' => array('type' => array('integer', 'null')),
    ));
    register_rest_field('jugadora', 'ene_posicio', array(
        'get_callback' => function ($post) use ($keys_posicio) {
            return ene_push_first_meta($post['id'], $keys_posicio);
        },
        'schema' => array('type' => array('string', 'null')),
    ));
});

/** Torna el primer post meta no buit d'una llista de claus candidates. */
function ene_push_first_meta($post_id, $keys) {
    foreach ((array) $keys as $k) {
        $v = get_post_meta($post_id, $k, true);
        if ($v !== '' && $v !== false && $v !== null) return $v;
    }
    return null;
}
