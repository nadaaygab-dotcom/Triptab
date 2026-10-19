package com.triptab.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.triptab.app.data.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MenuScreen(
    trip: Trip,
    member: Member,
    place: Place,
    repo: TripRepository,
    day: Int,
    onDay: (Int) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSwitchMember: (String) -> Unit,
    onScan: () -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    var orderedOnly by rememberSaveable { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    val allDays = day == ALL_DAYS
    val mineHere = trip.orders.filter { it.memberId == member.id && it.placeId == place.id }
    // In a single day: that day's quantities (editable). In All days: the sum (read-only).
    val qty: Map<String, Int> = mineHere
        .filter { allDays || it.day == day }
        .groupBy { it.itemId }.mapValues { (_, l) -> l.sumOf { it.quantity } }
    // For All days: which days each item was ordered on, e.g. "Thu 22: 1 · Fri 23: 2".
    val perDay: Map<String, String> = if (!allDays) emptyMap() else mineHere
        .groupBy { it.itemId }
        .mapValues { (_, l) -> l.sortedBy { it.day }.joinToString(" · ") { "${TripDays.shortLabel(trip, it.day)}: ${it.quantity}" } }
    val bill = Billing.billAt(trip, place, member.id, dayOrNull(day))

    val visible = place.items.filter { item ->
        (!orderedOnly || (qty[item.id] ?: 0) > 0) &&
            (query.isBlank() || item.nameEn.contains(query, true) || item.nameAr.contains(query) ||
                item.category.contains(query, true))
    }
    val grouped = visible.groupBy { it.category }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Index of each category header in the list (item 0 is the controls block).
    val headerIndex = buildMap {
        var i = 1
        grouped.forEach { (cat, list) -> put(cat, i); i += 1 + list.size }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(place.name, fontWeight = FontWeight.SemiBold)
                        Text(if (allDays) "${member.name} · all days" else "${member.name} · ${TripDays.label(trip, day)}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (qty.isNotEmpty() && !allDays) IconButton(onClick = { confirmClear = true }) {
                        Icon(Icons.Default.RemoveShoppingCart, "Clear ${member.name}'s order here")
                    }
                    IconButton(onClick = onScan) { Icon(Icons.Default.DocumentScanner, "Scan menu") }
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit menu") }
                },
            )
        },
        bottomBar = { BillBar(place, bill, if (allDays) "Due here · all days" else "Due here · ${dayCaption(trip, day)}") },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                // Switch person without leaving the table.
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(trip.membersOf(trip.familyOf(member).id), key = { it.id }) { m ->
                        val t = Billing.billAt(trip, place, m.id, dayOrNull(day)).total
                        FilterChip(
                            selected = m.id == member.id,
                            onClick = { if (m.id != member.id) onSwitchMember(m.id) },
                            label = { Text(if (t > 0) "${m.name} · ${formatEgp(t).removePrefix("EGP ")}" else m.name) },
                        )
                    }
                }
                DayPicker(trip, day, onDay, Modifier.padding(bottom = 8.dp))
                if (allDays) {
                    Surface(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f), shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()) {
                        Text("Showing totals for all days. Pick a day above to add or change items.",
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                    }
                }
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        query, { query = it }, singleLine = true,
                        placeholder = { Text("Search menu") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear") } },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = orderedOnly, onClick = { orderedOnly = !orderedOnly },
                        label = { Text("Ordered") })
                }
                if (grouped.size > 1) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(grouped.keys.toList()) { cat ->
                            SuggestionChip(onClick = {
                                scope.launch { listState.animateScrollToItem(headerIndex[cat] ?: 0) }
                            }, label = { Text(cat) })
                        }
                    }
                }
                if (place.note.isNotBlank()) {
                    Text(place.note, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }

            if (place.items.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No menu items yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Snap the menu and the app fills it in for you.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onScan) {
                        Icon(Icons.Default.DocumentScanner, null); Spacer(Modifier.width(8.dp)); Text("Scan menu")
                    }
                    TextButton(onClick = onEdit) { Text("Add items by hand") }
                }
            } else if (visible.isEmpty()) item {
                Text("Nothing matches", Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center)
            }

            grouped.forEach { (category, list) ->
                stickyHeader(key = "h_$category") {
                    Text(category, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp, vertical = 8.dp))
                }
                items(list, key = { it.id }) { item ->
                    MenuRow(item, qty[item.id] ?: 0, editable = !allDays, detail = perDay[item.id]) { n ->
                        repo.setQuantity(trip.id, member.id, place.id, item.id, day, n)
                    }
                }
            }
        }
    }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Clear ${member.name}'s order at ${place.name} on ${dayCaption(trip, day)}?") },
        confirmButton = { TextButton(onClick = { repo.clearOrders(trip.id, member.id, place.id, day); confirmClear = false }) { Text("Clear") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
    )
}

@Composable
private fun MenuRow(item: MenuItem, quantity: Int, editable: Boolean, detail: String?, onQuantity: (Int) -> Unit) {
    val selected = quantity > 0
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.nameEn, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                if (item.nameAr.isNotBlank())
                    Text(item.nameAr, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (item.price == 0.0) "Price not set" else if (selected && quantity > 1)
                        "${formatEgp(item.price)} × $quantity = ${formatEgp(item.price * quantity)}"
                    else formatEgp(item.price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.price == 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!editable) {
                if (selected) Text("×$quantity", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
            } else if (selected) {
                IconButton(onClick = { onQuantity(quantity - 1) }) { Icon(Icons.Default.RemoveCircleOutline, "Less") }
                Text("$quantity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(min = 20.dp), textAlign = TextAlign.Center)
                IconButton(onClick = { onQuantity(quantity + 1) }) { Icon(Icons.Default.AddCircle, "More") }
            } else {
                FilledTonalIconButton(onClick = { onQuantity(1) }) { Icon(Icons.Default.Add, "Add") }
            }
        }
    }
}

@Composable
private fun BillBar(place: Place, bill: Bill, label: String) {
    Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
            if (bill.subtotal > 0 && (place.servicePct > 0 || place.taxPct > 0)) {
                BillLine("Subtotal", bill.subtotal)
                if (place.servicePct > 0) BillLine("Service ${place.servicePct.pct()}", bill.service)
                if (place.taxPct > 0) BillLine("Tax ${place.taxPct.pct()}", bill.tax)
                Spacer(Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(formatEgp(bill.total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun BillLine(label: String, v: Double) {
    Row {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatEgp(v), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
