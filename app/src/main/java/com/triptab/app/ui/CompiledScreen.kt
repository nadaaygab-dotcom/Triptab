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
    val ok = results.mapNotNull { it.order }
    val known = before?.imports?.map { it.sourceId }?.toSet().orEmpty()
    val added = ok.filter { it.sourceId !in known }; val updated = ok.filter { it.sourceId in known }
    if (added.isNotEmpty()) appendLine("Added: " + added.joinToString { "${it.familyLabel} (${it.allMembers.size} people, ${formatEgp(it.bill().total)})" })
    if (updated.isNotEmpty()) appendLine("Updated with their newer file: " + updated.joinToString { it.familyLabel })
    results.filter { it.order == null }.forEach { appendLine("Couldn't import ${it.fileName}: ${it.error}") }
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
) {
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
            repo.addImports(compilation.id, results.mapNotNull { it.order })
            message = importMessage(results, before)
            busy = false
        }
    }
    fun addTrip(t: Trip) {
        val o = BillExport.fromTrip(t)
        val before = compilation
        repo.addImports(compilation.id, listOf(o))
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
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp),
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
