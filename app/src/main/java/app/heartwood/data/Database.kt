package app.heartwood.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** What happened at the tree. Several can happen in one meeting; a meeting day is one ring. */
enum class Kind {
    /** The first meeting: the pith of the cross-section. */
    MEET,

    /** Came, stood there, nothing else. */
    VISIT,

    /** Phone to the chest, chest to the trunk, until the heartbeat slowed. */
    HUG,

    /** Ten seconds of the bark's sound: the voice of the day. */
    LISTEN,

    /** Sat beside it with the phone face down. */
    SIT,

    /** A fallen leaf pressed into the herbarium. */
    LEAF,

    /** A rubbing of the bark. */
    RUB,

    /** A photo from the same spot as the last one, for the chronicle. */
    PHOTO;

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: VISIT
    }
}

/**
 * A real tree you are friends with. [photo] is the first picture (a file in the app's storage,
 * empty if none), the chronicle's first frame. [seed] is the random number its drawing grows from.
 * [from] is the friend who passed it on, if it came as a gift.
 */
@Entity(tableName = "trees")
data class Tree(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val species: String,
    /** Circumference at chest height, in centimetres. */
    val girth: Int,
    val lat: Double,
    val lon: Double,
    val photo: String = "",
    val why: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val seed: Long = 0,
    val from: String = "",
)

/**
 * One thing done at the tree. [sky] and [temp] are the weather then (empty / [NO_TEMP] when it
 * couldn't be fetched). [seconds] is how long a hug or a sit lasted, [bpm] where a hug's heartbeat
 * ended. [file] is the recording, the photo or the rubbing; [wave] the recording's loudness, one
 * byte per 50 ms, base64. [align] is a chronicle photo's fit over the previous one: "scale,dx,dy,rotation".
 */
@Entity(tableName = "visits", indices = [Index("treeId")])
data class Visit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val treeId: Long,
    val at: Long = System.currentTimeMillis(),
    val kind: String = Kind.VISIT.name,
    val sky: String = "",
    val temp: Int = NO_TEMP,
    val seconds: Int = 0,
    val bpm: Int = 0,
    val file: String = "",
    val wave: String = "",
    val align: String = "",
) {
    val kindOf: Kind get() = Kind.of(kind)

    companion object {
        const val NO_TEMP = -999
    }
}

/** A note left in the hollow: it opens only at the tree, a year later. */
@Entity(tableName = "notes", indices = [Index("treeId")])
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val treeId: Long,
    val leftAt: Long,
    val openAt: Long,
    val text: String,
    val openedAt: Long = 0,
)

@Dao
interface GroveDao {
    @Query("SELECT * FROM trees ORDER BY createdAt") fun observeTrees(): Flow<List<Tree>>
    @Query("SELECT * FROM visits ORDER BY at") fun observeVisits(): Flow<List<Visit>>
    @Query("SELECT * FROM notes ORDER BY leftAt") fun observeNotes(): Flow<List<Note>>

    @Query("SELECT * FROM trees ORDER BY createdAt") suspend fun trees(): List<Tree>
    @Query("SELECT * FROM visits ORDER BY at") suspend fun visits(): List<Visit>
    @Query("SELECT * FROM notes ORDER BY leftAt") suspend fun notes(): List<Note>
    @Query("SELECT * FROM trees WHERE id = :id") suspend fun tree(id: Long): Tree?

    @Insert suspend fun insert(t: Tree): Long
    @Insert suspend fun insert(v: Visit): Long
    @Insert suspend fun insert(n: Note): Long
    @Update suspend fun update(t: Tree)
    @Update suspend fun update(n: Note)

    @Query("DELETE FROM trees WHERE id = :id") suspend fun deleteTreeRow(id: Long)
    @Query("DELETE FROM visits WHERE treeId = :id") suspend fun deleteVisitsOf(id: Long)
    @Query("DELETE FROM notes WHERE treeId = :id") suspend fun deleteNotesOf(id: Long)

    @Transaction
    suspend fun deleteTree(id: Long) {
        deleteVisitsOf(id)
        deleteNotesOf(id)
        deleteTreeRow(id)
    }
}

@Database(entities = [Tree::class, Visit::class, Note::class], version = 1, exportSchema = true)
abstract class GroveDatabase : RoomDatabase() {
    abstract fun dao(): GroveDao

    companion object {
        fun create(context: Context): GroveDatabase =
            Room.databaseBuilder(context.applicationContext, GroveDatabase::class.java, "grove.db").build()
    }
}
