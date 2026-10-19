package com.triptab.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.triptab.app.data.ClaudeMenuReader
import com.triptab.app.scan.ScanSettings

/** Lets the user save their own Anthropic API key so menu scanning can use Claude. */
@Composable
fun ClaudeSettingsDialog(settings: ScanSettings, onClose: () -> Unit) {
    var key by remember { mutableStateOf(settings.apiKey) }
    var model by remember { mutableStateOf(settings.model) }
    var show by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Menu scanning with Claude") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Without a key, menus are read on the phone for free (English text only). " +
                        "With your own Anthropic API key, Claude reads Arabic and English menus in any layout. " +
                        "Photos are sent only to Anthropic; the key stays on this phone.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    key, { key = it }, singleLine = true, label = { Text("Anthropic API key") },
                    placeholder = { Text("sk-ant-…") },
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { TextButton(onClick = { show = !show }) { Text(if (show) "Hide" else "Show") } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Model", style = MaterialTheme.typography.labelLarge)
                listOf(
                    ClaudeMenuReader.DEFAULT_MODEL to "Sonnet 5.5 — most accurate (about 5¢ per menu page)",
                    ClaudeMenuReader.CHEAP_MODEL to "Haiku 5.5 — cheapest (well under 1¢ per page)",
                ).forEach { (id, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { model = id }) {
                        RadioButton(selected = model == id, onClick = { model = id })
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text("Get a key at console.anthropic.com → API keys.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = { settings.apiKey = key; settings.model = model; onClose() }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (settings.hasClaude) TextButton(onClick = { settings.apiKey = ""; onClose() }) { Text("Remove key") }
                TextButton(onClick = onClose) { Text("Cancel") }
            }
        },
    )
}

/** App-wide settings kept on this phone. */
class AppSettings(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
    var admin: Boolean
        get() = prefs.getBoolean("admin_mode", false)
        set(v) = prefs.edit().putBoolean("admin_mode", v).apply()
}

@Composable
fun AppSettingsDialog(admin: Boolean, onAdmin: (Boolean) -> Unit, onClaude: () -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { onAdmin(!admin) }) {
                    Column(Modifier.weight(1f)) {
                        Text("Admin mode", style = MaterialTheme.typography.titleSmall)
                        Text("Compile the group's orders from the Excel bills other families send you.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(admin, onAdmin)
                }
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(onClick = onClaude)) {
                    Column(Modifier.weight(1f)) {
                        Text("Menu scanning with Claude", style = MaterialTheme.typography.titleSmall)
                        Text("API key for reading Arabic menus", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("›", style = MaterialTheme.typography.titleLarge)
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } },
    )
}
