package com.triptab.app.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.triptab.app.data.*
import com.triptab.app.scan.MenuScanner
import com.triptab.app.scan.ScanEngine
import com.triptab.app.scan.ScanSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** One extracted item in the review list. */
data class ReviewRow(val item: ScannedItem, val selected: Boolean, val existing: MenuItem?) {
    val samePrice get() = existing != null && existing.price == item.price
}

fun buildReview(place: Place, result: ScanResult): List<ReviewRow> =
    result.items.distinctBy { MenuMerge.key(it.nameEn) + "|" + it.price }.map { s ->
        val ex = MenuMerge.findExisting(place, s)
        ReviewRow(s, selected = ex == null || ex.price != s.price, existing = ex)
    }

/** Applies the reviewed rows to the place: new items are added, matched items get the new price. */
fun applyReview(repo: TripRepository, tripId: String, place: Place, rows: List<ReviewRow>,
                charges: Pair<Double?, Double?>?) {
    if (charges != null) repo.upsertPlace(tripId, place.copy(
        servicePct = charges.first ?: place.servicePct, taxPct = charges.second ?: place.taxPct))
    for (r in rows.filter { it.selected }) {
        val s = r.item
        if (r.existing != null) repo.upsertItem(tripId, place.id, r.existing.copy(
            price = s.price, nameAr = r.existing.nameAr.ifBlank { s.nameAr }))
        else repo.upsertItem(tripId, place.id, MenuItem(category = s.category, nameEn = s.nameEn, nameAr = s.nameAr, price = s.price))
    }
}

private sealed interface ScanState {
    data object Photos : ScanState
    data class Working(val message: String) : ScanState
    data class Error(val message: String) : ScanState
    data object Review : ScanState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanMenuScreen(
    trip: Trip,
    place: Place,
    repo: TripRepository,
    onBack: () -> Unit,
    // For previews/tests: start straight in review with this result.
    initialResult: ScanResult? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { ScanSettings(context) }
    var hasClaude by remember { mutableStateOf(settings.hasClaude) }
    var showSettings by remember { mutableStateOf(false) }

    val photos = rememberSaveable(saver = listSaver<SnapshotStateList<Uri>, String>(
        save = { list -> list.map { it.toString() } },
        restore = { saved -> saved.map { Uri.parse(it) }.toMutableStateList() },
    )) { mutableStateListOf() }
    var engine by rememberSaveable { mutableStateOf(if (settings.hasClaude) ScanEngine.CLAUDE else ScanEngine.ON_DEVICE) }
    var state by remember { mutableStateOf<ScanState>(if (initialResult != null) ScanState.Review else ScanState.Photos) }
    val rows = remember { mutableStateListOf<ReviewRow>().apply { initialResult?.let { addAll(buildReview(place, it)) } } }
    var charges by remember { mutableStateOf(initialResult?.let { it.servicePct to it.taxPct }) }
    var applyCharges by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf<Int?>(null) }

    var pendingCamera by rememberSaveable { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        pendingCamera?.let { if (ok) photos.add(Uri.parse(it)) }
        pendingCamera = null
    }
    val pickPhotos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { uris ->
        photos.addAll(uris.filter { it !in photos })
    }
    fun openCamera() {
        val dir = File(context.cacheDir, "scans").apply { mkdirs() }
        val f = File(dir, "menu_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", f)
        pendingCamera = uri.toString()
        takePicture.launch(uri)
    }

    fun run() {
        val useClaude = engine == ScanEngine.CLAUDE && settings.hasClaude
        state = ScanState.Working(if (useClaude) "Claude is reading ${photos.size} photo${if (photos.size > 1) "s" else ""}…"
            else "Reading the menu on your phone…")
        scope.launch {
            try {
                val res = if (useClaude) MenuScanner.readWithClaude(context, photos.toList(), settings)
                else MenuScanner.readOnDevice(context, photos.toList())
                rows.clear(); rows.addAll(buildReview(place, res))
                charges = (res.servicePct to res.taxPct).takeIf { it.first != null || it.second != null }
                state = if (rows.isEmpty()) ScanState.Error(
                    if (useClaude) "No priced items were found in these photos."
                    else "No priced items were found. Try a straighter, closer photo" +
                        (if (settings.hasClaude) ", or switch to Claude." else ". For Arabic menus, set up Claude.")
                ) else ScanState.Review
            } catch (e: Exception) {
                state = ScanState.Error(e.message ?: "Something went wrong reading the menu.")
            }
        }
    }

    val selectedCount = rows.count { it.selected }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Scan menu", fontWeight = FontWeight.SemiBold)
                        Text(place.name, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "Scan settings") } },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                    when (state) {
                        ScanState.Photos -> Button(onClick = { run() }, enabled = photos.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.DocumentScanner, null); Spacer(Modifier.width(8.dp))
                            Text(if (photos.isEmpty()) "Add a photo first" else "Read menu (${photos.size} photo${if (photos.size > 1) "s" else ""})")
                        }
                        ScanState.Review -> Button(
                            onClick = {
                                applyReview(repo, trip.id, place, rows,
                                    charges?.takeIf { applyCharges && chargesDiffer(place, it) })
                                onBack()
                            },
                            enabled = selectedCount > 0 || (charges != null && applyCharges && chargesDiffer(place, charges!!)),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Add $selectedCount item${if (selectedCount == 1) "" else "s"} to ${place.name}") }
                        is ScanState.Error -> Button(onClick = { state = ScanState.Photos }, modifier = Modifier.fillMaxWidth()) {
                            Text("Back to photos")
                        }
                        is ScanState.Working -> {}
                    }
                }
            }
        },
    ) { pad ->
        when (val s = state) {
            ScanState.Photos -> PhotosStep(
                Modifier.padding(pad), photos, engine, hasClaude,
                onCamera = ::openCamera,
                onGallery = { pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemove = { photos.remove(it) },
                onEngine = { engine = it },
                onSetup = { showSettings = true },
            )
            is ScanState.Working -> Column(Modifier.padding(pad).fillMaxSize(), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(); Spacer(Modifier.height(16.dp)); Text(s.message)
            }
            is ScanState.Error -> Column(Modifier.padding(pad).fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ErrorOutline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp)); Text(s.message, style = MaterialTheme.typography.bodyLarge)
            }
            ScanState.Review -> ReviewStep(
                Modifier.padding(pad), place, rows, charges, applyCharges,
                onToggleCharges = { applyCharges = it },
                onToggle = { i -> rows[i] = rows[i].copy(selected = !rows[i].selected) },
                onAll = { sel -> for (i in rows.indices) rows[i] = rows[i].copy(selected = sel) },
                onEdit = { editing = it },
            )
        }
    }

    editing?.let { i ->
        val r = rows[i]
        ItemDialog(
            item = MenuItem(category = r.item.category, nameEn = r.item.nameEn, nameAr = r.item.nameAr, price = r.item.price),
            categories = rows.map { it.item.category }.distinct(),
            onSave = { m ->
                val s = ScannedItem(m.category, m.nameEn, m.nameAr, m.price)
                rows[i] = ReviewRow(s, true, MenuMerge.findExisting(place, s)); editing = null
            },
            onDelete = { rows.removeAt(i); editing = null },
            onDismiss = { editing = null },
        )
    }
    if (showSettings) ClaudeSettingsDialog(settings) {
        showSettings = false
        hasClaude = settings.hasClaude
        if (hasClaude && state == ScanState.Photos) engine = ScanEngine.CLAUDE
        if (!hasClaude) engine = ScanEngine.ON_DEVICE
    }
}

private fun chargesDiffer(place: Place, c: Pair<Double?, Double?>) =
    (c.first != null && c.first != place.servicePct) || (c.second != null && c.second != place.taxPct)

@Composable
private fun PhotosStep(
    modifier: Modifier,
    photos: List<Uri>,
    engine: ScanEngine,
    hasClaude: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemove: (Uri) -> Unit,
    onEngine: (ScanEngine) -> Unit,
    onSetup: () -> Unit,
) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Photograph each page of the menu, flat and well lit. You'll check every item before it's added.",
            style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = onCamera, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PhotoCamera, null); Spacer(Modifier.width(8.dp)); Text("Take photo")
            }
            OutlinedButton(onClick = onGallery, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PhotoLibrary, null); Spacer(Modifier.width(8.dp)); Text("From gallery")
            }
        }
        if (photos.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(photos, key = { it.toString() }) { uri -> Thumb(uri) { onRemove(uri) } }
        }

        Text("Read with", style = MaterialTheme.typography.titleSmall)
        EngineOption(
            selected = engine == ScanEngine.ON_DEVICE, enabled = true, onClick = { onEngine(ScanEngine.ON_DEVICE) },
            title = "On this phone", detail = "Free and offline. English text only — Arabic dish names won't be read.",
        )
        EngineOption(
            selected = engine == ScanEngine.CLAUDE, enabled = hasClaude, onClick = { onEngine(ScanEngine.CLAUDE) },
            title = "Claude", detail = if (hasClaude) "Reads Arabic and English, any layout. Needs internet."
                else "Reads Arabic and English menus. Needs your Anthropic API key.",
        )
        if (!hasClaude) TextButton(onClick = onSetup) { Icon(Icons.Default.Key, null); Spacer(Modifier.width(6.dp)); Text("Set up Claude") }
    }
}

@Composable
private fun EngineOption(selected: Boolean, enabled: Boolean, onClick: () -> Unit, title: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Column {
            Text(title, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Thumb(uri: Uri, onRemove: () -> Unit) {
    val context = LocalContext.current
    val bmp by produceState<Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) { runCatching { MenuScanner.loadBitmap(context, uri, 320) }.getOrNull() }
    }
    Box(Modifier.size(96.dp, 128.dp).clip(RoundedCornerShape(10.dp))) {
        bmp?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            ?: Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxSize()) {}
        FilledTonalIconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(28.dp)) {
            Icon(Icons.Default.Close, "Remove photo", Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ReviewStep(
    modifier: Modifier,
    place: Place,
    rows: List<ReviewRow>,
    charges: Pair<Double?, Double?>?,
    applyCharges: Boolean,
    onToggleCharges: (Boolean) -> Unit,
    onToggle: (Int) -> Unit,
    onAll: (Boolean) -> Unit,
    onEdit: (Int) -> Unit,
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Found ${rows.size} items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Tap an item to fix its name or price. Untick anything wrong.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onAll(rows.any { !it.selected }) }) {
                    Text(if (rows.any { !it.selected }) "Select all" else "Select none")
                }
            }
            if (charges != null && chargesDiffer(place, charges)) {
                Card(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()
                    .clickable { onToggleCharges(!applyCharges) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(applyCharges, onToggleCharges)
                        Text(buildString {
                            append("Also set ")
                            append(listOfNotNull(charges.first?.let { "service ${it.pct()}" }, charges.second?.let { "tax ${it.pct()}" })
                                .joinToString(" and "))
                            append(" — printed on the menu")
                        }, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        var lastCat: String? = null
        rows.forEachIndexed { i, r ->
            if (r.item.category != lastCat) {
                val cat = r.item.category; lastCat = cat
                item(key = "h$i") {
                    Text(cat, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))
                }
            }
            item(key = "r$i") {
                Row(Modifier.fillMaxWidth().clickable { onEdit(i) }.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(r.selected, { onToggle(i) })
                    Column(Modifier.weight(1f)) {
                        Text(r.item.nameEn, style = MaterialTheme.typography.bodyLarge)
                        if (r.item.nameAr.isNotBlank() && r.item.nameAr != r.item.nameEn)
                            Text(r.item.nameAr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        when {
                            r.existing == null -> {}
                            r.samePrice -> Text("Already on the menu", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            else -> Text("Updates price from ${formatEgp(r.existing.price)}", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(formatEgp(r.item.price), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 12.dp))
                }
            }
        }
    }
}
