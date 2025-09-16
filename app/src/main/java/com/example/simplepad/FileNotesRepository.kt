package com.example.simplepad

import android.content.Context
import org.json.JSONObject
import java.io.File

class FileNotesRepository(private val context: Context) {

    private val notesDir: File by lazy {
        File(context.filesDir, "notes").apply { if (!exists()) mkdirs() }
    }

    fun listNotes(): List<Note> {
        val files = notesDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        val list = files.mapNotNull { f -> readFileAsNote(f) }
        return list.sortedByDescending { it.updatedAt }
    }

    fun readNote(id: String): Note? {
        val f = File(notesDir, "$id.json")
        if (!f.exists()) return null
        return readFileAsNote(f)
    }

    fun saveNote(note: Note) {
        val f = File(notesDir, "${note.id}.json")
        val obj = JSONObject().apply {
            put("id", note.id)
            put("title", note.title)
            put("content", note.content)
            put("createdAt", note.createdAt)
            put("updatedAt", note.updatedAt)
        }
        f.writeText(obj.toString())
    }

    fun deleteNote(id: String) {
        File(notesDir, "$id.json").delete()
    }

    private fun readFileAsNote(f: File): Note? = try {
        val obj = JSONObject(f.readText())
        Note(
            id = obj.optString("id", f.nameWithoutExtension),
            title = obj.optString("title", ""),
            content = obj.optString("content", ""),
            createdAt = obj.optLong("createdAt", f.lastModified()),
            updatedAt = obj.optLong("updatedAt", f.lastModified())
        )
    } catch (_: Throwable) {
        null
    }
}
