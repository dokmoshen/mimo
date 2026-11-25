package app.mimo.notes.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [
        NoteEntity::class,
        ChecklistItemEntity::class,
        LabelEntity::class,
        NoteLabelCrossRef::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class MimoDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao

    companion object {
        @Volatile
        private var instance: MimoDatabase? = null

        fun get(context: Context): MimoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MimoDatabase::class.java,
                    "mimo.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
