package cat.elnostreequip.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ArticleDao {
    @Query("SELECT * FROM articles ORDER BY date DESC")
    abstract fun observeAll(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id")
    abstract suspend fun get(id: Long): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(items: List<ArticleEntity>)

    @Query("DELETE FROM articles")
    abstract suspend fun clear()

    @Transaction
    open suspend fun replaceAll(items: List<ArticleEntity>) {
        clear()
        insertAll(items)
    }
}

@Dao
abstract class PlayerDao {
    @Query("SELECT * FROM players WHERE teamId = :team ORDER BY dorsal ASC")
    abstract fun observeByTeam(team: Int): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id = :id")
    abstract suspend fun get(id: Long): PlayerEntity?

    @Query("DELETE FROM players WHERE teamId = :team")
    abstract suspend fun clearByTeam(team: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(items: List<PlayerEntity>)

    @Transaction
    open suspend fun replaceByTeam(team: Int, items: List<PlayerEntity>) {
        clearByTeam(team)
        insertAll(items)
    }
}
