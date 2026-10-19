package com.triptab.app.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.triptab.app.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(trip: Trip, repo: TripRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    var hidePacked by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ChecklistItem?>(null) }
    var adding by remember { mutableStateOf<String?>(null) }   // section to add into ("" = choose)

    val items = trip.checklist
    val done = items.count { it.done }
    val sections = items.map { it.section.ifBlank { "Other" } }.distinct()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Packing checklist", fontWeight = FontWeight.SemiBold)
                        Text(trip.name, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, PackingList.shareText(trip)) }
                        context.startActivity(Intent.createChooser(send, "Share packing list"))
                    }) { Icon(Icons.Default.Share, "Share what's left to pack") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text(if (hidePacked) "Show packed items" else "Hide packed items") },
                                leadingIcon = { Icon(if (hidePacked) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) },
                                onClick = { hidePacked = !hidePacked; menu = false })
                            DropdownMenuItem(text = { Text("Uncheck all (packing to go home)") },
                                leadingIcon = { Icon(Icons.Default.RestartAlt, null) },
                                enabled = done > 0, onClick = { menu = false; confirmReset = true })
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { adding = "" }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add item") })
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Card(Modifier.padding(16.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (items.isNotEmpty() && done == items.size)
                        MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (items.isNotEmpty() && done == items.size) "All packed!" else "$done of ${items.size} packed",
                                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Icon(if (items.isNotEmpty() && done == items.size) Icons.Default.CheckCircle else Icons.Default.Luggage, null,
                                tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(progress = { if (items.isEmpty()) 0f else done.toFloat() / items.size },
                            modifier = Modifier.fillMaxWidth())
                        if (hidePacked && done > 0) Text("$done packed item${if (done == 1) "" else "s"} hidden",
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            if (items.isEmpty()) item {
                Text("Your list is empty. Tap Add item to start.", Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            for (section in sections) {
                val inSection = items.filter { it.section.ifBlank { "Other" } == section }
                val shown = if (hidePacked) inSection.filter { !it.done } else inSection
                item(key = "h_$section") {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(section, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        Text("${inSection.count { it.done }}/${inSection.size}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(onClick = { adding = section }) { Icon(Icons.Default.Add, "Add to $section", Modifier.size(20.dp)) }
                    }
                }
                items(shown, key = { it.id }) { it ->
                    Row(Modifier.fillMaxWidth().clickable { repo.setChecked(trip.id, it.id, !it.done) }.padding(start = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(it.done, { v -> repo.setChecked(trip.id, it.id, v) })
                        Text(it.text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (it.done) TextDecoration.LineThrough else null,
                            color = if (it.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { editing = it }) {
                            Icon(Icons.Default.Edit, "Edit", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Uncheck everything?") },
        text = { Text("Handy when packing to go home. Your items stay on the list.") },
        confirmButton = { TextButton(onClick = { repo.uncheckAll(trip.id); confirmReset = false }) { Text("Uncheck all") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
    )
    adding?.let { section ->
        ChecklistItemDialog(null, sections, section.ifEmpty { sections.lastOrNull() ?: "Other" },
            onSave = { repo.upsertChecklistItem(trip.id, it); adding = null }, onDelete = null, onDismiss = { adding = null })
    }
    editing?.let { item ->
        ChecklistItemDialog(item, sections, item.section,
            onSave = { repo.upsertChecklistItem(trip.id, it); editing = null },
            onDelete = { repo.removeChecklistItem(trip.id, item.id); editing = null },
            onDismiss = { editing = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistItemDialog(
    item: ChecklistItem?,
    sections: List<String>,
    initialSection: String,
    onSave: (ChecklistItem) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(item?.text ?: "") }
    var section by remember { mutableStateOf(initialSection) }
    var open by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add item" else "Edit item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(text, { text = it }, label = { Text("Item") }, singleLine = true)
                ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
                    OutlinedTextField(section, { section = it }, label = { Text("Section") }, singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable))
                    if (sections.isNotEmpty()) ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        sections.forEach { s -> DropdownMenuItem(text = { Text(s) }, onClick = { section = s; open = false }) }
                    }
                }
                if (onDelete != null) TextButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, null); Spacer(Modifier.width(6.dp)); Text("Delete item")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = {
                val s = section.trim().ifEmpty { "Other" }
                onSave((item ?: ChecklistItem(text = "")).copy(text = text.trim(), section = s))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
