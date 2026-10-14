package com.triptab.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.triptab.app.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHomeScreen(
    trip: Trip,
    repo: TripRepository,
    day: Int,
    onDay: (Int) -> Unit,
    onBack: () -> Unit,
    onOpenPlace: (memberId: String, placeId: String) -> Unit,
    onSummary: () -> Unit,
    onEditPlace: (placeId: String) -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf(trip.members.firstOrNull()?.id) }
    val selected = trip.members.find { it.id == selectedId } ?: trip.members.firstOrNull()
    var showPeople by remember { mutableStateOf(false) }
    var addingPerson by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(trip.name, fontWeight = FontWeight.SemiBold)
                        if (trip.datesLabel().isNotEmpty())
                            Text(trip.datesLabel(), style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { showPeople = true }) { Icon(Icons.Default.Groups, "Manage people") }
                    IconButton(onClick = onSummary) { Icon(Icons.AutoMirrored.Filled.ReceiptLong, "Summary") }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        if (selected != null) {
                            Text("${selected.name} · ${dayCaption(trip, day)}", style = MaterialTheme.typography.labelMedium)
                            Text(formatEgp(Billing.billTotal(trip, selected.id, dayOrNull(day)).total),
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.clickable(onClick = onSummary)) {
                        Text("Family · ${dayCaption(trip, day)}", style = MaterialTheme.typography.labelMedium)
                        Text(formatEgp(Billing.billTotal(trip, null, dayOrNull(day)).total),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Text("Who is ordering?", style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(trip.members, key = { it.id }) { m ->
                        FilterChip(
                            selected = m.id == selected?.id,
                            onClick = { selectedId = m.id },
                            label = { Text(m.name) },
                            leadingIcon = if (m.id == selected?.id) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null,
                        )
                    }
                    item {
                        AssistChip(onClick = { addingPerson = true }, label = { Text("Add") },
                            leadingIcon = { Icon(Icons.Default.PersonAdd, null, Modifier.size(18.dp)) })
                    }
                }
                Text("Which day?", style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))
                DayPicker(trip, day, onDay)
            }

            for (type in PlaceType.entries) {
                val places = trip.places.filter { it.type == type }
                item {
                    Text(if (type == PlaceType.RESTAURANT) "Restaurants & cafés" else "Activities",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp))
                }
                items(places, key = { it.id }) { p ->
                    PlaceCard(trip, p, selected, day,
                        onClick = { if (selected != null) onOpenPlace(selected.id, p.id) })
                }
                if (places.isEmpty()) item {
                    Text("None yet", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            item {
                OutlinedButton(onClick = { onEditPlace("new") },
                    modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Icon(Icons.Default.AddBusiness, null); Spacer(Modifier.width(8.dp)); Text("Add a place or activity")
                }
            }
        }
    }

    if (addingPerson) NameDialog("Add person", "") { name ->
        if (name != null && name.isNotBlank()) repo.addMember(trip.id, name)
        addingPerson = false
    }
    if (showPeople) PeopleDialog(trip, repo) { showPeople = false }
}

@Composable
private fun PlaceCard(trip: Trip, place: Place, member: Member?, day: Int, onClick: () -> Unit) {
    val mine = if (member != null) Billing.billAt(trip, place, member.id, dayOrNull(day)) else Bill.ZERO
    val lines = if (member != null) trip.orders.filter {
        it.placeId == place.id && it.memberId == member.id && (day == ALL_DAYS || it.day == day)
    } else emptyList()
    val count = lines.sumOf { it.quantity }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (place.type == PlaceType.ACTIVITY) Icons.Default.Landscape else Icons.Default.Restaurant,
                null, tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (place.nameAr.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(place.nameAr, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val sub = buildList {
                    add(if (place.items.isEmpty()) "No menu yet" else "${place.items.size} item${if (place.items.size == 1) "" else "s"}")
                    if (place.servicePct > 0) add("+${place.servicePct.pct()} service")
                    if (place.taxPct > 0) add("+${place.taxPct.pct()} tax")
                }.joinToString(" · ")
                Text(sub, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (count > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatEgp(mine.total), fontWeight = FontWeight.Bold)
                    Text("$count item${if (count == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Icon(Icons.Default.ChevronRight, null)
            }
        }
    }
}

/** "Thu 22", or "all days". */
fun dayCaption(trip: Trip, day: Int): String =
    if (day == ALL_DAYS) "all days" else TripDays.label(trip, day).substringAfter(" · ", "Day $day")

fun Double.pct(): String = if (this % 1.0 == 0.0) "${this.toInt()}%" else "$this%"

@Composable
fun NameDialog(title: String, initial: String, onDone: (String?) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = { onDone(null) },
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it }, singleLine = true, label = { Text("Name") }) },
        confirmButton = { TextButton(onClick = { onDone(text) }, enabled = text.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = { onDone(null) }) { Text("Cancel") } },
    )
}

@Composable
private fun PeopleDialog(trip: Trip, repo: TripRepository, onClose: () -> Unit) {
    var renaming by remember { mutableStateOf<Member?>(null) }
    var removing by remember { mutableStateOf<Member?>(null) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("People on this trip") },
        text = {
            Column {
                trip.members.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(m.name, Modifier.weight(1f))
                        IconButton(onClick = { renaming = m }) { Icon(Icons.Default.Edit, "Rename") }
                        IconButton(onClick = { removing = m }, enabled = trip.members.size > 1) {
                            Icon(Icons.Default.DeleteOutline, "Remove")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } },
    )

    renaming?.let { m ->
        NameDialog("Rename", m.name) { n ->
            if (n != null && n.isNotBlank()) repo.renameMember(trip.id, m.id, n)
            renaming = null
        }
    }
    removing?.let { m ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove ${m.name}?") },
            text = { Text("Their orders will be removed too.") },
            confirmButton = { TextButton(onClick = { repo.removeMember(trip.id, m.id); removing = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } },
        )
    }
}
