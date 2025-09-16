package com.example.simplepad

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FileNotesRepository(app)
    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // Compose state exposed to UI
    private val _notes = mutableStateListOf<Note>()
    val notesState get() = mutableStateOf(_notes) // wrapper to trigger recomposition when ref changes
    private val _query = mutableStateOf("")
    val queryState get() = _query

    init {
        refresh()
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun refresh() {
        io.execute {
            val items = repo.listNotes()
            mainHandler.post {
                _notes.clear()
                _notes.addAll(items)
            }
        }
    }

    fun newDraftId(): String = Note.newId()

    fun getWorkingCopy(noteId: String?, isNew: Boolean): Note {
        if (isNew) {
            // A draft that isn't persisted yet
            return Note(
                id = noteId ?: Note.newId(),
                title = "",
                content = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        // For existing notes: try memory first, then disk
        val existing = _notes.find { it.id == noteId }
        if (existing != null) return existing
        return repo.readNote(noteId!!)?.also { loaded ->
            // keep memory in sync
            val idx = _notes.indexOfFirst { it.id == loaded.id }
            if (idx >= 0) _notes[idx] = loaded else _notes.add(0, loaded)
        } ?: Note(
            id = noteId ?: Note.newId(),
            title = "",
            content = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun upsert(note: Note) {
        io.execute {
            repo.saveNote(note)
            val updated = repo.readNote(note.id) ?: note
            mainHandler.post {
                val idx = _notes.indexOfFirst { it.id == updated.id }
                if (idx >= 0) {
                    _notes[idx] = updated
                } else {
                    _notes.add(0, updated)
                }
                // keep list sorted by updatedAt desc
                _notes.sortByDescending { it.updatedAt }
            }
        }
    }

    fun delete(id: String) {
        io.execute {
            repo.deleteNote(id)
            mainHandler.post {
                val idx = _notes.indexOfFirst { it.id == id }
                if (idx >= 0) _notes.removeAt(idx)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        io.shutdown()
    }
}
