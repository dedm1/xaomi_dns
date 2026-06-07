package com.dedm.dns

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DnsSwitcherScreen()
                }
            }
        }
    }
}

@Composable
fun DnsSwitcherScreen() {
    val context = LocalContext.current
    val repository = remember { DnsRepository(context) }
    var dnsInput by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(repository.getDnsHistory()) }
    var isDnsEnabled by remember { mutableStateOf(false) }
    var activeSpecifier by remember { mutableStateOf<String?>(null) }

    fun refreshState() {
        isDnsEnabled = try { DnsManager.isDnsEnabled(context) } catch (e: Exception) { false }
        activeSpecifier = try { DnsManager.getActiveSpecifier(context) } catch (e: Exception) { null }
    }

    val updateWidget = {
        val widgetIntent = Intent(context, DnsWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, DnsWidgetProvider::class.java))
        widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(widgetIntent)
    }

    val contentResolver = context.contentResolver
    DisposableEffect(contentResolver) {
        val uri = Settings.Global.getUriFor("private_dns_mode")
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                refreshState()
            }
        }
        try {
            contentResolver.registerContentObserver(uri, false, observer)
        } catch (_: Exception) {}

        refreshState()
        history = repository.getDnsHistory()

        onDispose {
            contentResolver.unregisterContentObserver(observer)
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Private DNS Switcher",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        val statusColor = if (isDnsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        val statusText = if (isDnsEnabled) "Включен" else "Выключен"
        Text(
            text = "Статус: $statusText",
            style = MaterialTheme.typography.titleMedium,
            color = statusColor
        )

        if (isDnsEnabled && !activeSpecifier.isNullOrBlank()) {
            Text(
                text = activeSpecifier!!,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isDnsEnabled) "DNS включен" else "DNS выключен",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = isDnsEnabled,
                onCheckedChange = { checked ->
                    try {
                        if (checked) {
                            val activeDns = dnsInput.trim().ifEmpty {
                                history.firstOrNull() ?: "dns.adguard-dns.com"
                            }
                            DnsManager.enableDns(context, activeDns)
                            if (dnsInput.trim().isNotEmpty()) {
                                repository.saveDns(dnsInput.trim())
                                history = repository.getDnsHistory()
                            }
                        } else {
                            DnsManager.disableDns(context)
                        }
                        refreshState()
                        updateWidget()
                    } catch (e: SecurityException) {
                        Toast.makeText(context, "Нет разрешения WRITE_SECURE_SETTINGS. Выдайте через ADB.", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = dnsInput,
            onValueChange = { dnsInput = it },
            label = { Text("Введите DNS") },
            placeholder = { Text("dns.adguard-dns.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val trimmed = dnsInput.trim()
                if (trimmed.isNotEmpty()) {
                    try {
                        repository.saveDns(trimmed)
                        DnsManager.enableDns(context, trimmed)
                        refreshState()
                        history = repository.getDnsHistory()
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        updateWidget()
                        Toast.makeText(context, "Сохранено и включено", Toast.LENGTH_SHORT).show()
                    } catch (e: SecurityException) {
                        Toast.makeText(context, "Нет разрешения WRITE_SECURE_SETTINGS. Выдайте через ADB.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Введите DNS-адрес", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить и включить")
        }

        if (history.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Последние DNS",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                history.forEach { address ->
                    AssistChip(
                        onClick = { dnsInput = address },
                        label = { Text(address, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }
    }
}

