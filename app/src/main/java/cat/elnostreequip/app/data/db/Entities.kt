package cat.elnostreequip.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Artículo cacheado del feed. */
@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val excerpt: String,
    val contentHtml: String,
    val imageUrl: String?,
    val date: String,
    val link: String,
    /** equipId según TeamMapper, o null si no se pudo mapear la categoría. */
    val teamId: Int?,
    /** Noms de categories del WP separats per | (per a les píndoles). */
    val categoryNames: String = ""
)

/** Jugadora cacheada de una plantilla. dorsal=999 si se desconoce (va al final). */
@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val dorsal: Int,
    val position: String,
    val photoUrl: String?,
    val bioHtml: String,
    val teamId: Int,
    /** Files d'estadístiques (JSON de PlayerStatRow), o "[]" si no n'hi ha. */
    val statsJson: String = "[]"
)
