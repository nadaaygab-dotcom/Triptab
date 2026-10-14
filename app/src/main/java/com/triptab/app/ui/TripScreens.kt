package com.triptab.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.triptab.app.data.AppData
import com.triptab.app.data.Billing
import com.triptab.app.data.Trip
import com.triptab.app.data.formatEgp

fun Trip.datesLabel(): String = when {
    startDate.isBlank() && endDate.isBlank() -> ""
    endDate.isBlank() -> startDate
    else -> "$startDate – $endDate"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    data: AppData,
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onDelete: (String) -> Unit,
) {
    var confirmDelete by remember { mutableStateOf<Trip?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("TripTab", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNew, icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New trip") })
        },
    ) { pad ->
        if (data.trips.isEmpty()) {
            Column(
                Modifier.padding(pad).fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.Luggage, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Track who ordered what on your trip", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Create a trip, add your family, and tap a restaurant to order from its menu. " +
                        "Totals include service and tax.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onNew) { Text("Start a trip") }
            }
        } else {
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(data.trips, key = { it.id }) { trip ->
                    val total = Billing.billTotal(trip, null).total
                    Card(Modifier.fillMaxWidth().clickable { onOpen(trip.id) }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(trip.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                if (trip.datesLabel().isNotEmpty())
                                    Text(trip.datesLabel(), style = MaterialTheme.typography.bodySmall)
                                Text("${trip.members.size} people · ${formatEgp(total)} due",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { confirmDelete = trip }) {
                                Icon(Icons.Default.DeleteOutline, "Delete trip")
                            }
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete \"${t.name}\"?") },
            text = { Text("All people, menus and orders in this trip will be removed.") },
            confirmButton = { TextButton(onClick = { onDelete(t.id); confirmDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTripScreen(
    onBack: () -> Unit,
    onCreate: (name: String, start: String, end: String, owner: String, family: List<String>, fayoum: Boolean) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("Fayoum") }
    var start by rememberSaveable { mutableStateOf("22 Oct 2026") }
    var end by rememberSaveable { mutableStateOf("24 Oct 2026") }
    var owner by rememberSaveable { mutableStateOf("") }
    var fayoum by rememberSaveable { mutableStateOf(true) }
    val family: SnapshotStateList<String> = remember { mutableStateListOf("") }

    val canCreate = name.isNotBlank() && owner.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New trip") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { onCreate(name, start, end, owner, family.toList(), fayoum) },
                    enabled = canCreate,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                ) { Text("Create trip") }
            }
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(name, { name = it }, label = { Text("Trip name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(start, { start = it }, label = { Text("From") }, singleLine = true,
                    modifier = Modifier.weight(1f))
                OutlinedTextField(end, { end = it }, label = { Text("To") }, singleLine = true,
                    modifier = Modifier.weight(1f))
            }
            OutlinedTextField(owner, { owner = it }, label = { Text("Your name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())

            Text("Family members", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            family.forEachIndexed { i, n ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(n, { family[i] = it }, label = { Text("Name ${i + 1}") }, singleLine = true,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = { if (family.size > 1) family.removeAt(i) else family[0] = "" }) {
                        Icon(Icons.Default.Close, "Remove")
                    }
                }
            }
            TextButton(onClick = { family.add("") }) {
                Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("Add another person")
            }

            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { fayoum = !fayoum }) {
                Checkbox(fayoum, { fayoum = it })
                Column {
                    Text("Include Fayoum places")
                    Text("Kom El Dikka, Ibis, Tibtunis and Safari with their menus",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
