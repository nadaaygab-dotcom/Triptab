package com.triptab.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
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
    onChecklist: () -> Unit = {},
) {
    val families = trip.familyList
    var familyId by rememberSaveable { mutableStateOf(families.first().id) }
    val family = families.find { it.id == familyId } ?: families.first()
    val famMembers = trip.membersOf(family.id)
    var selectedId by rememberSaveable { mutableStateOf(famMembers.firstOrNull()?.id) }
    val selected = famMembers.find { it.id == selectedId } ?: famMembers.firstOrNull()
    var showPeople by remember { mutableStateOf(false) }
    var addingPerson by remember { mutableStateOf(false) }
    var addingFamily by remember { mutableStateOf(false) }

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
                    IconButton(onClick = onChecklist) { Icon(Icons.Default.Checklist, "Packing checklist") }
                    IconButton(onClick = { showPeople = true }) { Icon(Icons.Default.Groups, "Families and people") }
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
                        Text("${if (families.size > 1) family.name else "Family"} · ${dayCaption(trip, day)}",
                            style = MaterialTheme.typography.labelMedium)
                        Text(formatEgp(Billing.familyBill(trip, family.id, dayOrNull(day)).total),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                val total = trip.checklist.size; val packed = trip.checklist.count { it.done }
                OutlinedCard(onClick = onChecklist, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Luggage, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Packing checklist", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(if (total == 0) "Add things to bring" else if (packed == total) "All packed ✓" else "$packed of $total packed",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (total > 0) CircularProgressIndicator(progress = { packed.toFloat() / total }, modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
            item {
                Text("Family", style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(families, key = { "f" + it.id }) { f ->
                        FilterChip(
                            selected = f.id == family.id,
                            onClick = { familyId = f.id; selectedId = trip.membersOf(f.id).firstOrNull()?.id },
                            label = { Text(f.name) },
                            leadingIcon = { Icon(Icons.Default.Groups, null, Modifier.size(18.dp)) },
                        )
                    }
                    item {
                        AssistChip(onClick = { addingFamily = true }, label = { Text("Add family") },
                            leadingIcon = { Icon(Icons.Default.GroupAdd, null, Modifier.size(18.dp)) })
                    }
                }
                Text("Who is ordering?", style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(famMembers, key = { it.id }) { m ->
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
                if (famMembers.isEmpty()) Text("Add a person to ${family.name} to start ordering.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(start = 16.dp, top = 6.dp))
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

    if (addingPerson) NameDialog("Add person to ${family.name}", "") { name ->
        if (name != null && name.isNotBlank()) repo.addMember(trip.id, name, family.id)
        addingPerson = false
    }
    if (addingFamily) AddFamilyDialog(onDismiss = { addingFamily = false }) { name, people ->
        val id = repo.addFamily(trip.id, name, people)
        familyId = id
        selectedId = repo.data.value.trips.find { it.id == trip.id }?.let { t -> t.membersOf(id).firstOrNull()?.id }
        addingFamily = false
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
fun AddFamilyDialog(onDismiss: () -> Unit, onAdd: (name: String, people: List<String>) -> Unit) {
    var head by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var nameTouched by remember { mutableStateOf(false) }
    val others = remember { mutableStateListOf("") }
    val shownName = if (nameTouched) name else if (head.isBlank()) "" else defaultFamilyName(head)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a family") },
        text = {
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Track another family's orders on this phone, with their own bill.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(head, { head = it }, singleLine = true, label = { Text("First person (e.g. Sara)") })
                others.forEachIndexed { i, n ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(n, { others[i] = it }, singleLine = true, label = { Text("Person ${i + 2}") },
                            modifier = Modifier.weight(1f))
                        IconButton(onClick = { if (others.size > 1) others.removeAt(i) else others[0] = "" }) { Icon(Icons.Default.Close, "Remove") }
                    }
                }
                TextButton(onClick = { others.add("") }) {
                    Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(6.dp)); Text("Add another person")
                }
                OutlinedTextField(shownName, { name = it; nameTouched = true }, singleLine = true, label = { Text("Family name") },
                    placeholder = { Text("e.g. Sara's family") })
            }
        },
        confirmButton = {
            TextButton(enabled = head.isNotBlank(), onClick = {
                onAdd(shownName.ifBlank { defaultFamilyName(head) }, listOf(head) + others)
            }) { Text("Add family") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PeopleDialog(trip: Trip, repo: TripRepository, onClose: () -> Unit) {
    var renaming by remember { mutableStateOf<Member?>(null) }
    var removing by remember { mutableStateOf<Member?>(null) }
    var renamingFamily by remember { mutableStateOf<Family?>(null) }
    var removingFamily by remember { mutableStateOf<Family?>(null) }
    var addingTo by remember { mutableStateOf<Family?>(null) }
    var addingFamily by remember { mutableStateOf(false) }
    val families = trip.familyList

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Families and people") },
        text = {
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                for (f in families) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Icon(Icons.Default.Groups, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(f.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { renamingFamily = f }) { Icon(Icons.Default.Edit, "Rename family") }
                        if (families.size > 1) IconButton(onClick = { removingFamily = f }) { Icon(Icons.Default.DeleteOutline, "Remove family") }
                    }
                    trip.membersOf(f.id).forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 28.dp)) {
                            Text(m.name, Modifier.weight(1f))
                            IconButton(onClick = { renaming = m }) { Icon(Icons.Default.Edit, "Rename", Modifier.size(20.dp)) }
                            IconButton(onClick = { removing = m }, enabled = trip.members.size > 1) {
                                Icon(Icons.Default.DeleteOutline, "Remove", Modifier.size(20.dp))
                            }
                        }
                    }
                    TextButton(onClick = { addingTo = f }, modifier = Modifier.padding(start = 20.dp)) {
                        Icon(Icons.Default.PersonAdd, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Add person")
                    }
                    HorizontalDivider()
                }
                TextButton(onClick = { addingFamily = true }, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.GroupAdd, null); Spacer(Modifier.width(6.dp)); Text("Add a family")
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
    renamingFamily?.let { f ->
        NameDialog("Rename family", f.name) { n ->
            if (n != null && n.isNotBlank()) repo.renameFamily(trip.id, f.id, n)
            renamingFamily = null
        }
    }
    addingTo?.let { f ->
        NameDialog("Add person to ${f.name}", "") { n ->
            if (n != null && n.isNotBlank()) repo.addMember(trip.id, n, f.id)
            addingTo = null
        }
    }
    if (addingFamily) AddFamilyDialog(onDismiss = { addingFamily = false }) { name, people ->
        repo.addFamily(trip.id, name, people); addingFamily = false
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
    removingFamily?.let { f ->
        AlertDialog(
            onDismissRequest = { removingFamily = null },
            title = { Text("Remove ${f.name}?") },
            text = { Text("Everyone in this family and all their orders will be removed from this trip.") },
            confirmButton = { TextButton(onClick = { repo.removeFamily(trip.id, f.id); removingFamily = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { removingFamily = null }) { Text("Cancel") } },
        )
    }
}
