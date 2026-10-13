package cat.elnostreequip.app.data

/**
 * Mapeo entre taxonomías de WordPress y los 4 equipos de la app.
 *
 * IDs de la taxonomía `equip` (verificados el 2026-10-13 contra /wp-json/wp/v2/equip):
 * 39 = Primer Equip, 40 = València CF Femení B, 41 = C, 42 = D.
 */
object TeamMapper {

    const val PRIMER_EQUIP = 39
    const val EQUIP_B = 40
    const val EQUIP_C = 41
    const val EQUIP_D = 42

    val ALL: List<Int> = listOf(PRIMER_EQUIP, EQUIP_B, EQUIP_C, EQUIP_D)

    fun shortName(equipId: Int): String = when (equipId) {
        PRIMER_EQUIP -> "Primer Equip"
        EQUIP_B -> "B"
        EQUIP_C -> "C"
        EQUIP_D -> "D"
        else -> "Equip $equipId"
    }

    fun fullName(equipId: Int): String = when (equipId) {
        PRIMER_EQUIP -> "València CF Femení"
        EQUIP_B -> "València CF Femení B"
        EQUIP_C -> "València CF Femení C"
        EQUIP_D -> "València CF Femení D"
        else -> "Equip $equipId"
    }

    // TODO: verificar el mapeo real categoría -> equipo contra
    // https://elnostreequip.cat/wp-json/wp/v2/categories
    // Los posts NO llevan la taxonomía `equip` (solo category/post_tag), así que
    // el filtrado del feed por equipo depende de este mapa. Hasta que se rellene,
    // los artículos se muestran sin filtrar por equipo.
    private val CATEGORY_TO_TEAM: Map<Int, Int> = emptyMap()

    /** Devuelve el equipId del artículo según sus categorías, o null si no se sabe. */
    fun teamForCategories(categoryIds: List<Int>): Int? {
        for (cat in categoryIds) {
            CATEGORY_TO_TEAM[cat]?.let { return it }
        }
        return null
    }
}
