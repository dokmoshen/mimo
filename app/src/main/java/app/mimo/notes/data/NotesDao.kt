package app.mimo.notes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {

    @Transaction
    @Query("SELECT * FROM notes WHERE isTrashed = 0 ORDER BY isPinned DESC, modifiedAt DESC")
    fun observeLive(): Flow<List<NoteWithDetails>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun observeTrash(): Flow<List<NoteWithDetails>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeNote(id: Long): Flow<NoteWithDetails?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNote(id: Long): NoteWithDetails?

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteEntity(id: Long): NoteEntity?

    @Insert
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM notes WHERE isTrashed = 1")
    suspend fun emptyTrash()

    @Query("SELECT * FROM notes WHERE isTrashed = 1 AND trashedAt < :cutoff")
    suspend fun trashedBefore(cutoff: Long): List<NoteEntity>

    @Query("UPDATE notes SET isTrashed = 1, trashedAt = :now, isArchived = 0, isPinned = 0 WHERE id IN (:ids)")
    suspend fun moveToTrash(ids: List<Long>, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isTrashed = 0, trashedAt = NULL WHERE id IN (:ids)")
    suspend fun restoreFromTrash(ids: List<Long>)

    @Query("UPDATE notes SET isArchived = :archived, isPinned = CASE WHEN :archived = 1 THEN 0 ELSE isPinned END WHERE id IN (:ids)")
    suspend fun setArchived(ids: List<Long>, archived: Boolean)

    @Query("UPDATE notes SET isPinned = :pinned, modifiedAt = :now WHERE id IN (:ids)")
    suspend fun setPinned(ids: List<Long>, pinned: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET modifiedAt = :now WHERE id IN (:ids)")
    suspend fun touch(ids: List<Long>, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isLocked = :locked, modifiedAt = :now WHERE id = :id")
    suspend fun setLocked(id: Long, locked: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET reminderAt = :at, reminderRepeat = :repeat WHERE id = :id")
    suspend fun setReminder(id: Long, at: Long?, repeat: String)

    @Query("SELECT * FROM notes WHERE reminderAt IS NOT NULL AND isTrashed = 0 AND isArchived = 0 ORDER BY reminderAt ASC")
    fun observeReminderNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE reminderAt IS NOT NULL AND isTrashed = 0")
    suspend fun notesWithReminders(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND isArchived = 0 AND title = '' AND content = '' AND images = '[]' AND drawing IS NULL AND audioPath IS NULL AND id NOT IN (SELECT noteId FROM checklist_items) AND createdAt < :cutoff")
    suspend fun emptyNotesOlderThan(cutoff: Long): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(items: List<ChecklistItemEntity>)

    @Query("DELETE FROM checklist_items WHERE noteId = :noteId")
    suspend fun deleteChecklist(noteId: Long)

    @Query("UPDATE checklist_items SET isChecked = :checked WHERE id = :itemId")
    suspend fun setChecklistChecked(itemId: String, checked: Boolean)

    @Transaction
    suspend fun replaceChecklist(noteId: Long, items: List<ChecklistItemEntity>) {
        deleteChecklist(noteId)
        if (items.isNotEmpty()) insertChecklist(items)
    }

    @Query("SELECT * FROM labels ORDER BY name COLLATE NOCASE ASC")
    fun observeLabels(): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels ORDER BY name COLLATE NOCASE ASC")
    suspend fun labels(): List<LabelEntity>

    @Insert
    suspend fun insertLabel(label: LabelEntity): Long

    @Query("UPDATE labels SET name = :name, colorId = :colorId WHERE id = :id")
    suspend fun updateLabel(id: Long, name: String, colorId: Int)

    @Query("DELETE FROM labels WHERE id = :id")
    suspend fun deleteLabel(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLabelLinks(links: List<NoteLabelCrossRef>)

    @Query("DELETE FROM note_labels WHERE noteId = :noteId")
    suspend fun clearLabelLinks(noteId: Long)

    @Transaction
    suspend fun setLabelLinks(noteId: Long, labelIds: List<Long>) {
        clearLabelLinks(noteId)
        if (labelIds.isNotEmpty()) {
            insertLabelLinks(labelIds.map { NoteLabelCrossRef(noteId, it) })
        }
    }

    @Transaction
    @Query("SELECT labels.*, (SELECT COUNT(*) FROM note_labels nl INNER JOIN notes n ON n.id = nl.noteId WHERE nl.labelId = labels.id AND n.isTrashed = 0) AS noteCount FROM labels ORDER BY name COLLATE NOCASE ASC")
    fun observeLabelsWithCount(): Flow<List<LabelWithCount>>
}
