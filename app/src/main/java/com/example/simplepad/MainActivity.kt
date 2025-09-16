package com.example.simplepad

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.text.KeyboardOptions

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vm = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[NotesViewModel::class.java]

        setContent {
            MaterialTheme {
                SimplePadApp(vm)
            }
        }
    }
}

private sealed interface Screen {
    data object List : Screen
    data class Edit(val noteId: String?, val isNew: Boolean) : Screen
}

@Composable
private fun SimplePadApp(vm: NotesViewModel) {
    var screen by remember { mutableStateOf<Screen>(Screen.List) }

    when (val s = screen) {
        is Screen.List -> NotesListScreen(
            vm = vm,
            onAdd = {
                val id = vm.newDraftId()
                screen = Screen.Edit(noteId = id, isNew = true)
            },
            onOpen = { noteId ->
                screen = Screen.Edit(noteId = noteId, isNew = false)
            }
        )
        is Screen.Edit -> NoteEditorScreen(
            vm = vm,
            noteId = s.noteId,
            isNew = s.isNew,
            onDone = { screen = Screen.List },
            onDeleted = { screen = Screen.List }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesListScreen(
    vm: NotesViewModel,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit
) {
    val notes by vm.notesState // SnapshotStateList<Note>
    val query by vm.queryState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SimplePad", fontWeight = FontWeight.SemiBold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Text("+") }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            OutlinedTextField(
                value = query,
                onValueChange = { vm.setQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search notes…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search
                )
            )

            Spacer(Modifier.height(12.dp))

            val filtered = remember(notes, query) {
                if (query.isBlank()) notes
                else notes.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.content.contains(query, ignoreCase = true)
                }
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No notes yet. Tap + to add one.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(filtered, key = { it.id }) { note ->
                        NoteRow(note = note, onClick = { onOpen(note.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteRow(note: Note, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = if (note.title.isBlank()) "(Untitled)" else note.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = note.content.replace("\n", " "),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Last edited ${TimeUtils.formatRelative(note.updatedAt)}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorScreen(
    vm: NotesViewModel,
    noteId: String?,
    isNew: Boolean,
    onDone: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current

    // Pull the working copy for this editor instance
    val working = remember(noteId) { vm.getWorkingCopy(noteId, isNew) }
    var title by remember(working.id) { mutableStateOf(working.title) }
    var content by remember(working.id) { mutableStateOf(working.content) }
    var showDelete by remember { mutableStateOf(false) }

    BackHandler {
        // Prompt save on back if anything changed; otherwise just go back
        if (title != working.title || content != working.content) {
            vm.upsert(
                working.copy(
                    title = title,
                    content = content,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isNew) "New Note" else "Edit Note")
                },
                actions = {
                    TextButton(onClick = {
                        vm.upsert(
                            working.copy(
                                title = title,
                                content = content,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        onDone()
                    }) { Text("Save") }

                    IconButton(onClick = { showDelete = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete"
                        )
                    }

                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, if (title.isBlank()) "Note" else title)
                            putExtra(Intent.EXTRA_TEXT, content)
                        }
                        context.startActivity(
                            Intent.createChooser(send, "Share note")
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Start typing…") },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                maxLines = Int.MAX_VALUE
            )
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete note?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(working.id)
                    showDelete = false
                    onDeleted()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            }
        )
    }
}

// Lightweight vector icons used above (delete/share) without extra deps
// (Material3 supplies these in the base artifact as of recent versions)
private object Icons {
    object Default {
        val Delete = androidx.compose.material.icons.Icons.Default.Delete
        val Share = androidx.compose.material.icons.Icons.Default.Share
    }
}
