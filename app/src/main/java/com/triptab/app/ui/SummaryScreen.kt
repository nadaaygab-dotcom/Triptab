package com.triptab.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.triptab.app.data.*

private fun Trip.linesFor(memberId: String, placeId: String, day: Int): List<OrderLine> =
    orders.filter { it.memberId == memberId && it.placeId == placeId && (day == ALL_DAYS || it.day == day) }
        .sortedBy { it.day }

/** Plain-text summary suitable for WhatsApp, for one day or all days. */
fun buildShareText(trip: Trip, day: Int = ALL_DAYS): String = buildString {
    val d = dayOrNull(day)
    appendLine("🧾 ${trip.name}${if (trip.datesLabel().isNotEmpty()) " (${trip.datesLabel()})" else ""}")
    appendLine(if (day == ALL_DAYS) "All days" else TripDays.label(trip, day))
    for (m in trip.members) {
        val total = Billing.billTotal(trip, m.id, d)
        if (total.subtotal == 0.0) continue
        appendLine()
        appendLine("👤 ${m.name}: ${formatEgp(total.total)}")
        for (p in trip.places) {
            val lines = trip.linesFor(m.id, p.id, day)
            if (lines.isEmpty()) continue
            appendLine("  • ${p.name}: ${formatEgp(Billing.billAt(trip, p, m.id, d).total)}")
            for ((ld, dl) in lines.groupBy { it.day }) {
                if (day == ALL_DAYS) appendLine("    ${TripDays.label(trip, ld)}")
                for (l in dl) {
                    val item = p.items.find { it.id == l.itemId } ?: continue
                    appendLine("     ${l.quantity}× ${item.nameEn}")
                }
            }
        }
    }
    appendLine()
    val fam = Billing.billTotal(trip, null, d)
    if (day == ALL_DAYS) {
        for (x in TripDays.days(trip)) {
            val t = Billing.billTotal(trip, null, x).total
            if (t > 0) appendLine("${TripDays.label(trip, x)}: ${formatEgp(t)}")
        }
    }
    appendLine("Family total: ${formatEgp(fam.total)}")
    if (fam.service + fam.tax > 0) appendLine("(includes ${formatEgp(fam.service)} service + ${formatEgp(fam.tax)} tax)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(trip: Trip, day: Int, onDay: (Int) -> Unit, onBack: () -> Unit) {
    fun exportExcel(context: android.content.Context) {
        val o = BillExport.fromTrip(trip)
        ExcelFiles.share(context, BillExport.fileName("TripTab", trip.name, trip.ownerName), BillExport.familyWorkbook(o),
            "Send your bill")
    }
    val context = LocalContext.current
    val d = dayOrNull(day)
    val family = Billing.billTotal(trip, null, d)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Summary") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { exportExcel(context) }) { Icon(Icons.Default.TableView, "Export Excel") }
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, buildShareText(trip, day))
                        }
                        context.startActivity(Intent.createChooser(send, "Share summary"))
                    }) { Icon(Icons.Default.Share, "Share as text") }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Button(onClick = { exportExcel(context) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.TableView, null); Spacer(Modifier.width(8.dp)); Text("Send bill as Excel")
                    }
                    Text("All days, every person. Send it on WhatsApp to whoever is compiling the group's orders.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { DayPicker(trip, day, onDay, Modifier.padding(top = 8.dp)) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Family total · ${dayCaption(trip, day)}", style = MaterialTheme.typography.labelLarge)
                        Text(formatEgp(family.total), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Menu prices ${formatEgp(family.subtotal)} · service ${formatEgp(family.service)} · tax ${formatEgp(family.tax)}",
                            style = MaterialTheme.typography.bodySmall)
                        if (day == ALL_DAYS) {
                            Spacer(Modifier.height(8.dp))
                            Text("By day", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            for (x in TripDays.days(trip)) Row {
                                Text(TripDays.label(trip, x), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text(formatEgp(Billing.billTotal(trip, null, x).total), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("By place", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        var any = false
                        for (p in trip.places) {
                            val b = Billing.billAt(trip, p, null, d)
                            if (b.subtotal > 0) {
                                any = true
                                Row {
                                    Text(p.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    Text(formatEgp(b.total), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        if (!any) Text("Nothing ordered", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            items(trip.members, key = { it.id }) { m ->
                val total = Billing.billTotal(trip, m.id, d)
                ElevatedCard(Modifier.padding(horizontal = 16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(m.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f))
                            Text(formatEgp(total.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (total.subtotal == 0.0) {
                            Text("Nothing ordered", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        for (p in trip.places) {
                            val lines = trip.linesFor(m.id, p.id, day)
                            if (lines.isEmpty()) continue
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Row {
                                Text(p.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(formatEgp(Billing.billAt(trip, p, m.id, d).total), fontWeight = FontWeight.SemiBold)
                            }
                            for ((ld, dayLines) in lines.groupBy { it.day }) {
                                if (day == ALL_DAYS) Text(TripDays.label(trip, ld), style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp))
                                for (l in dayLines) {
                                    val item = p.items.find { it.id == l.itemId } ?: continue
                                    Row {
                                        Text("${l.quantity}× ${item.nameEn}", style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f))
                                        Text(formatEgp(item.price * l.quantity), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            val b = Billing.billAt(trip, p, m.id, d)
                            if (b.service + b.tax > 0) Text(
                                "+ ${formatEgp(b.service + b.tax)} service & tax",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
