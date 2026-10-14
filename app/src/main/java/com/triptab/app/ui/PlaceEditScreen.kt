package com.triptab.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.triptab.app.data.*

private fun Double.plain(): String = if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceEditScreen(trip: Trip, placeId: String, repo: TripRepository, onBack: () -> Unit,
                    onScan: (placeId: String) -> Unit = {}) {
    // For a new place we mint the id once; after the first save it exists in the trip.
    val id = rememberSaveable { if (placeId == "new") newId() else placeId }
    val existing = trip.places.find { it.id == id }

    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var nameAr by rememberSaveable { mutableStateOf(existing?.nameAr ?: "") }
    var type by rememberSaveable { mutableStateOf(existing?.type ?: PlaceType.RESTAURANT) }
    var service by rememberSaveable { mutableStateOf(existing?.servicePct?.plain() ?: "0") }
    var tax by rememberSaveable { mutableStateOf(existing?.taxPct?.plain() ?: "0") }
    var taxOnService by rememberSaveable { mutableStateOf(existing?.taxOnService ?: true) }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }

    var editingItem by remember { mutableStateOf<MenuItem?>(null) }
    var addingItem by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val dirty = existing == null || name != existing.name || nameAr != existing.nameAr || type != existing.type ||
        service.toDoubleOrNull() != existing.servicePct || tax.toDoubleOrNull() != existing.taxPct ||
        taxOnService != existing.taxOnService || note != existing.note
    val valid = name.isNotBlank() && service.toDoubleOrNull() != null && tax.toDoubleOrNull() != null

    fun save() {
        val base = existing ?: Place(id = id, name = name)
        repo.upsertPlace(trip.id, base.copy(
            name = name.trim(), nameAr = nameAr.trim(), type = type,
            servicePct = service.toDouble(), taxPct = tax.toDouble(), taxOnService = taxOnService, note = note.trim(),
        ))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New place" else "Edit ${existing.name}") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (existing != null) IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.DeleteOutline, "Delete place")
                    }
                },
            )
        },
        floatingActionButton = {
            if (existing != null) ExtendedFloatingActionButton(
                onClick = { addingItem = true },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add item") },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(nameAr, { nameAr = it }, label = { Text("Arabic name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    PlaceType.entries.forEachIndexed { i, t ->
                        SegmentedButton(selected = type == t, onClick = { type = t },
                            shape = SegmentedButtonDefaults.itemShape(i, PlaceType.entries.size)) {
                            Text(if (t == PlaceType.RESTAURANT) "Restaurant / café" else "Activity")
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(service, { service = it }, label = { Text("Service %") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                        isError = service.toDoubleOrNull() == null)
                    OutlinedTextField(tax, { tax = it }, label = { Text("Tax %") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                        isError = tax.toDoubleOrNull() == null)
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { taxOnService = !taxOnService }) {
                    Column(Modifier.weight(1f)) {
                        Text("Charge tax on service too")
                        Text("Usual on Egyptian bills: tax is applied after service is added",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(taxOnService, { taxOnService = it })
                }
            }
            item {
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                Button(onClick = { save() }, enabled = valid && dirty, modifier = Modifier.fillMaxWidth()) {
                    Text(if (existing == null) "Create place" else "Save changes")
                }
            }

            if (existing != null) {
                item {
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Menu (${existing.items.size} items)", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        FilledTonalButton(onClick = { onScan(existing.id) }) {
                            Icon(Icons.Default.DocumentScanner, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                            Text("Scan menu")
                        }
                    }
                }
                existing.items.groupBy { it.category }.forEach { (cat, list) ->
                    item(key = "c_$cat") {
                        Text(cat, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                    items(list, key = { it.id }) { it ->
                        Row(Modifier.fillMaxWidth().clickable { editingItem = it }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(it.nameEn)
                                if (it.nameAr.isNotBlank()) Text(it.nameAr, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(formatEgp(it.price), color = if (it.price == 0.0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface)
                            Icon(Icons.Default.Edit, "Edit", Modifier.padding(start = 8.dp).size(18.dp))
                        }
                    }
                }
            } else {
                item {
                    Text("Create the place first, then scan its menu or add items.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (existing != null) {
        val categories = existing.items.map { it.category }.distinct()
        if (addingItem) ItemDialog(null, categories,
            onSave = { repo.upsertItem(trip.id, existing.id, it); addingItem = false },
            onDelete = null, onDismiss = { addingItem = false })
        editingItem?.let { item ->
            ItemDialog(item, categories,
                onSave = { repo.upsertItem(trip.id, existing.id, it); editingItem = null },
                onDelete = { repo.removeItem(trip.id, existing.id, item.id); editingItem = null },
                onDismiss = { editingItem = null })
        }
        if (confirmDelete) AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${existing.name}?") },
            text = { Text("Its menu and everyone's orders there will be removed.") },
            confirmButton = { TextButton(onClick = { repo.removePlace(trip.id, existing.id); confirmDelete = false; onBack() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItemDialog(
    item: MenuItem?,
    categories: List<String>,
    onSave: (MenuItem) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var category by remember { mutableStateOf(item?.category ?: categories.lastOrNull() ?: "Food") }
    var en by remember { mutableStateOf(item?.nameEn ?: "") }
    var ar by remember { mutableStateOf(item?.nameAr ?: "") }
    var price by remember { mutableStateOf(item?.price?.plain() ?: "") }
    var catMenu by remember { mutableStateOf(false) }

    val valid = en.isNotBlank() && category.isNotBlank() && (price.toDoubleOrNull() ?: -1.0) >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add item" else "Edit item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = catMenu, onExpandedChange = { catMenu = it }) {
                    OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catMenu) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable))
                    if (categories.isNotEmpty()) ExposedDropdownMenu(expanded = catMenu, onDismissRequest = { catMenu = false }) {
                        categories.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = { category = c; catMenu = false })
                        }
                    }
                }
                OutlinedTextField(en, { en = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(ar, { ar = it }, label = { Text("Arabic name (optional)") }, singleLine = true)
                OutlinedTextField(price, { price = it }, label = { Text("Price (EGP)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                if (onDelete != null) TextButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, null); Spacer(Modifier.width(6.dp)); Text("Delete item")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                val base = item ?: MenuItem(category = category, nameEn = en, price = 0.0)
                onSave(base.copy(category = category.trim(), nameEn = en.trim(), nameAr = ar.trim(), price = price.toDouble()))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
