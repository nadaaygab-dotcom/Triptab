package com.triptab.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.triptab.app.data.*
import kotlinx.coroutines.launch

/** Plain-language result of an import, for the dialog. */
fun importMessage(results: List<ExcelFiles.ReadResult>, before: Compilation?): String = buildString {
    val ok = results.flatMap { it.orders }
    val known = before?.imports?.map { it.sourceId }?.toSet().orEmpty()
    val added = ok.filter { it.sourceId !in known }; val updated = ok.filter { it.sourceId in known }
    if (added.isNotEmpty()) appendLine("Added: " + added.joinToString { "${it.familyLabel} (${it.allMembers.size} people, ${formatEgp(it.bill().total)})" })
    if (updated.isNotEmpty()) appendLine("Updated with their newer file: " + updated.joinToString { it.familyLabel })
    results.filter { !it.ok }.forEach { appendLine("Couldn't import ${it.fileName}: ${it.error}") }
    if (ok.isEmpty() && results.isEmpty()) append("No files were chosen.")
}.trim()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompiledScreen(
    compilation: Compilation,
    trips: List<Trip>,
    repo: TripRepository,
    onBack: () -> Unit,
    initialMessage: String? = null,
    initialTab: Int = 0,
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var kPlace by rememberSaveable { mutableStateOf<String?>(null) }
    var kDay by rememberSaveable { mutableStateOf<String?>(null) }
    var kNames by rememberSaveable { mutableStateOf(false) }
    var kArabic by rememberSaveable { mutableStateOf(true) }
    var kNote by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var day by rememberSaveable { mutableStateOf<String?>(null) }   // null = all days
    var message by remember { mutableStateOf(initialMessage) }
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<ImportedOrder?>(null) }
    var pickTrip by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val results = ExcelFiles.readBills(context, uris)
            val before = compilation
            repo.addImports(compilation.id, results.flatMap { it.orders })
            message = importMessage(results, before)
            busy = false
        }
    }
    fun addTrip(t: Trip) {
        val o = BillExport.fromTrip(t)
        val before = compilation
        repo.addImports(compilation.id, o)
        message = importMessage(listOf(ExcelFiles.ReadResult(t.name, o, null)), before)
    }

    val days = compilation.days()
    if (day != null && days.none { it.first == day }) day = null
    val group = compilation.bill(day)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(compilation.name, fontWeight = FontWeight.SemiBold)
                        Text("Compiled · ${compilation.imports.size} famil${if (compilation.imports.size == 1) "y" else "ies"} · " +
                            "${compilation.imports.sumOf { it.allMembers.size }} people", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (compilation.imports.isNotEmpty()) IconButton(onClick = {
                        ExcelFiles.share(context, BillExport.fileName("TripTab", compilation.name, "compiled"),
                            BillExport.compiledWorkbook(compilation), "Share compiled order")
                    }) { Icon(Icons.Default.TableView, "Export compiled Excel") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; renaming = true },
                                leadingIcon = { Icon(Icons.Default.Edit, null) })
                            DropdownMenuItem(text = { Text("Delete compiled order") }, onClick = { menu = false; deleting = true },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, null) })
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                if (tab == 1 && compilation.imports.isNotEmpty()) {
                    val place = kPlace
                    val k = place?.let { Kitchen.order(compilation, it, kDay) }
                    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(enabled = k != null && k.lines.isNotEmpty(), modifier = Modifier.weight(1f), onClick = {
                            val text = Kitchen.message(compilation, k!!, kNames, kArabic, kNote)
                            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(android.content.Intent.createChooser(send, "Send order to ${k.place}"))
                        }) { Icon(Icons.AutoMirrored.Filled.Send, null); Spacer(Modifier.width(8.dp)); Text("Send to restaurant") }
                        OutlinedButton(enabled = k != null && k.lines.isNotEmpty(), onClick = {
                            ExcelFiles.share(context, BillExport.fileName("Order", k!!.place, if (k.dayKey == null) "all days" else k.dayLabel),
                                Kitchen.workbook(compilation, if (k.dayKey == null) Kitchen.all(compilation).filter { it.place == k.place } else listOf(k)),
                                "Send order as Excel")
                        }) { Icon(Icons.Default.TableView, "As Excel") }
                    }
                    return@Surface
                }
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { picker.launch(arrayOf(BillExport.MIME, "application/octet-stream")) },
                        enabled = !busy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.FileOpen, null); Spacer(Modifier.width(8.dp)); Text(if (busy) "Reading…" else "Import bills")
                    }
                    if (trips.isNotEmpty()) OutlinedButton(onClick = { if (trips.size == 1) addTrip(trips[0]) else pickTrip = true }) {
                        Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("Add my trip")
                    }
                }
            }
        },
    ) { pad ->
      Column(Modifier.padding(pad).fillMaxSize()) {
        if (compilation.imports.isNotEmpty()) {
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Bills") }, icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, null) })
                Tab(tab == 1, { tab = 1 }, text = { Text("Kitchen order") }, icon = { Icon(Icons.Default.RestaurantMenu, null) })
            }
        }
        if (tab == 1 && compilation.imports.isNotEmpty()) {
            KitchenTab(compilation, kPlace, { kPlace = it }, kDay, { kDay = it }, kNames, { kNames = it },
                kArabic, { kArabic = it }, kNote, { kNote = it })
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (compilation.imports.isEmpty()) {
                item { EmptyHelp(Modifier.padding(16.dp)) }
                return@LazyColumn
            }
            if (days.size > 1) item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(day == null, { day = null }, label = { Text("All days") },
                            leadingIcon = { Icon(Icons.Default.Functions, null, Modifier.size(18.dp)) })
                    }
                    items(days) { (k, label) -> FilterChip(day == k, { day = k }, label = { Text(label) }) }
                }
            }
            item {
                Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Everyone · ${day?.let { k -> days.first { it.first == k }.second } ?: "all days"}",
                            style = MaterialTheme.typography.labelLarge)
                        Text(formatEgp(group.total), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Menu prices ${formatEgp(group.subtotal)} · service ${formatEgp(group.service)} · tax ${formatEgp(group.tax)}",
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        for (p in compilation.places()) {
                            val b = compilation.imports.fold(Bill.ZERO) { acc, o -> acc + o.bill(dayKey = day, place = p) }
                            if (b.subtotal > 0) Row {
                                Text(p, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text(formatEgp(b.total), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            items(compilation.imports, key = { it.sourceId }) { o ->
                FamilyCard(o, day, onRemove = { removing = o }, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
      }
    }

    message?.let { m ->
        AlertDialog(onDismissRequest = { message = null }, title = { Text("Import") }, text = { Text(m) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } })
    }
    if (renaming) NameDialog("Rename", compilation.name) { n ->
        if (n != null && n.isNotBlank()) repo.renameCompilation(compilation.id, n); renaming = false
    }
    if (deleting) AlertDialog(
        onDismissRequest = { deleting = false },
        title = { Text("Delete \"${compilation.name}\"?") },
        text = { Text("The imported bills are removed from this phone. Families keep their own orders on their phones.") },
        confirmButton = { TextButton(onClick = { deleting = false; onBack(); repo.deleteCompilation(compilation.id) }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancel") } },
    )
    removing?.let { o ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove ${o.familyLabel}?") },
            text = { Text("You can import their file again later.") },
            confirmButton = { TextButton(onClick = { repo.removeImport(compilation.id, o.sourceId); removing = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } },
        )
    }
    if (pickTrip) AlertDialog(
        onDismissRequest = { pickTrip = false },
        title = { Text("Add which trip?") },
        text = {
            Column {
                trips.forEach { t ->
                    Text("${t.name} — ${t.ownerName}'s family", Modifier.fillMaxWidth().clickable { pickTrip = false; addTrip(t) }.padding(vertical = 12.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = { pickTrip = false }) { Text("Cancel") } },
    )
}

@Composable
private fun EmptyHelp(modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.Groups, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text("No bills yet", style = MaterialTheme.typography.titleLarge)
        Text("1. Each family opens their trip in TripTab → Summary → Send bill as Excel, and sends it to you on WhatsApp.",
            style = MaterialTheme.typography.bodyMedium)
        Text("2. In WhatsApp, tap the file and choose Share or Open with → TripTab. Or download the files and tap Import bills below.",
            style = MaterialTheme.typography.bodyMedium)
        Text("3. Tap Add my trip to include your own family's orders.", style = MaterialTheme.typography.bodyMedium)
        Text("If a family sends a newer file later, importing it replaces their old one.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FamilyCard(o: ImportedOrder, day: String?, onRemove: () -> Unit, modifier: Modifier) {
    var expanded by rememberSaveable(o.sourceId) { mutableStateOf<String?>(null) }
    val fam = o.bill(dayKey = day)
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(o.familyLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(listOfNotNull(o.tripName.takeIf { it.isNotBlank() }, o.exportedAt.takeIf { it.isNotBlank() }?.let { "sent $it" })
                        .joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatEgp(fam.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onRemove) { Icon(Icons.Default.DeleteOutline, "Remove family") }
            }
            for (m in o.allMembers) {
                val b = o.bill(m, day)
                val open = expanded == m
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Row(Modifier.fillMaxWidth().clickable { expanded = if (open) null else m }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(m, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text(if (b.subtotal == 0.0) "—" else formatEgp(b.total), fontWeight = FontWeight.SemiBold)
                }
                if (open) MemberDetail(o, m, day)
            }
        }
    }
}

@Composable
private fun MemberDetail(o: ImportedOrder, member: String, day: String?) {
    val lines = o.lines.filter { it.member == member && (day == null || it.dayKey == day) }
    if (lines.isEmpty()) {
        Text("Nothing ordered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 26.dp, bottom = 4.dp))
        return
    }
    Column(Modifier.padding(start = 26.dp, bottom = 4.dp)) {
        for ((dk, dl) in lines.groupBy { it.dayKey }) {
            if (day == null) Text(BillExport.dayLabel(dl[0].day, dl[0].date), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 6.dp))
            for ((p, pl) in dl.groupBy { it.place }) {
                val b = o.bill(member, dk, p)
                Row(Modifier.padding(top = 4.dp)) {
                    Text(p, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text(formatEgp(b.total), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }
                for (l in pl) Row {
                    Text("${l.quantity}× ${l.item}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text(formatEgp(l.lineTotal), style = MaterialTheme.typography.bodySmall)
                }
                if (b.service + b.tax > 0) Text("+ ${formatEgp(b.service + b.tax)} service & tax", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Total dishes to prepare at one restaurant, from everyone's orders. */
@Composable
private fun KitchenTab(
    c: Compilation,
    place: String?, onPlace: (String) -> Unit,
    day: String?, onDay: (String?) -> Unit,
    names: Boolean, onNames: (Boolean) -> Unit,
    arabic: Boolean, onArabic: (Boolean) -> Unit,
    note: String, onNote: (String) -> Unit,
) {
    val places = c.places()
    // Default to the first restaurant and its first day with orders.
    LaunchedEffect(places) { if (place == null || place !in places) places.firstOrNull()?.let(onPlace) }
    val p = place?.takeIf { it in places } ?: places.firstOrNull() ?: return
    val days = c.days().filter { (k, _) -> c.imports.any { o -> o.lines.any { it.place == p && it.dayKey == k } } }
    LaunchedEffect(p, days) { if (day != null && days.none { it.first == day }) onDay(days.firstOrNull()?.first) }
    LaunchedEffect(p) { if (day == null && days.size == 1) onDay(days[0].first) }
    val k = Kitchen.order(c, p, day?.takeIf { d -> days.any { it.first == d } })
    val hasArabic = k.lines.any { it.itemAr.isNotBlank() && it.itemAr != it.item }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text("Restaurant", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(places) { pl -> FilterChip(pl == p, { onPlace(pl) }, label = { Text(pl) }) }
            }
            Text("Day", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days) { (dk, label) -> FilterChip(day == dk, { onDay(dk) }, label = { Text(label) }) }
                if (days.size > 1) item { FilterChip(day == null, { onDay(null) }, label = { Text("All days") }) }
            }
        }
        item {
            Card(Modifier.padding(16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("To prepare at $p", style = MaterialTheme.typography.labelLarge)
                        Text("${k.dishes} items", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("${if (k.lines.size == 1) "1 dish" else "${k.lines.size} different dishes"} · ${k.people} ${if (k.people == 1) "person" else "people"} · ${if (k.dayKey == null) "all days" else k.dayLabel}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.SoupKitchen, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (k.dayKey == null && days.size > 1) Text("Showing every day together. Pick a day to send one meal's order.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        for ((section, ls) in k.sections) {
            item(key = "s_$section") {
                Text(section, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp))
            }
            items(ls, key = { "i_" + it.item }) { l ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.Top) {
                    Text("${l.quantity}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(min = 40.dp))
                    Column(Modifier.weight(1f)) {
                        Text(l.item, style = MaterialTheme.typography.bodyLarge)
                        if (l.itemAr.isNotBlank() && l.itemAr != l.item) Text(l.itemAr, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(l.who.joinToString(", ") { (n, q) -> if (q > 1) "$n ×$q" else n }, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
        item {
            HorizontalDivider(Modifier.padding(top = 16.dp))
            Text("Message to the restaurant", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
            OutlinedTextField(note, onNote, label = { Text("Note (optional)") },
                placeholder = { Text("e.g. We'll arrive around 2 pm, 11 people") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            Row(Modifier.fillMaxWidth().clickable { onNames(!names) }.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(names, onNames); Text("Include who ordered each dish")
            }
            if (hasArabic) Row(Modifier.fillMaxWidth().clickable { onArabic(!arabic) }.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(arabic, onArabic); Text("Include Arabic dish names")
            }
        }
    }
}
