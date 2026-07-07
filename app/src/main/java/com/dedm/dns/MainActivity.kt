package com.dedm.dns

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.dedm.dns.widget.DashboardUpdateWorker
import com.dedm.dns.widget.DashboardWidgetProvider
import com.dedm.dns.widget.StepsCalculator
import com.dedm.dns.widget.StepsReader
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DnsSwitcherApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsSwitcherApp() {
    val context = LocalContext.current
    var showWidgetMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val healthPermissions = remember {
        setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
        )
    }

    fun pinDashboardAndSync() {
        WidgetPinHelper.pinDashboardWidget(context)
        DashboardUpdateWorker.scheduleImmediateUpdate(context)
    }

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        scope.launch {
            val hasBasic = StepsReader.hasBasicPermissionAsync(context)
            val hasBg = StepsReader.hasBackgroundPermissionAsync(context)
            if (hasBasic) {
                pinDashboardAndSync()
                if (!hasBg) {
                    Toast.makeText(
                        context,
                        "Включите «Доступ к данным в фоновом режиме» в разделе «Дополнительный доступ»",
                        Toast.LENGTH_LONG
                    ).show()
                    try {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS")
                                .putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                        } else {
                            Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {}
                }
            } else {
                Toast.makeText(
                    context,
                    "Для шагов нужно разрешение Health Connect",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun ensureHealthPermissionThenAddWidget() {
        if (!StepsReader.isHealthConnectAvailable(context)) {
            Toast.makeText(context, "Health Connect недоступен на устройстве", Toast.LENGTH_LONG).show()
            return
        }

        scope.launch {
            val hasBasic = StepsReader.hasBasicPermissionAsync(context)
            val hasBg = StepsReader.hasBackgroundPermissionAsync(context)
            if (hasBasic && hasBg) {
                pinDashboardAndSync()
            } else if (!hasBasic) {
                healthPermissionLauncher.launch(healthPermissions)
            } else {
                pinDashboardAndSync()
                Toast.makeText(
                    context,
                    "Включите «Доступ к данным в фоновом режиме» в разделе «Дополнительный доступ»",
                    Toast.LENGTH_LONG
                ).show()
                try {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS")
                            .putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                    } else {
                        Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {}
            }
        }
    }

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        ensureHealthPermissionThenAddWidget()
    }

    fun addDashboardWidget() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            ensureHealthPermissionThenAddWidget()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Private DNS Switcher") },
                actions = {
                    Box {
                        IconButton(onClick = { showWidgetMenu = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Добавить виджет")
                        }
                        DropdownMenu(
                            expanded = showWidgetMenu,
                            onDismissRequest = { showWidgetMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Виджет DNS 1×1") },
                                onClick = {
                                    showWidgetMenu = false
                                    WidgetPinHelper.pinDnsWidget(context)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Дашборд 1×3") },
                                onClick = {
                                    showWidgetMenu = false
                                    addDashboardWidget()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            DnsSwitcherScreen()
        }
    }
}

private val ColorPending = Color(0xFFFF9800)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DnsSwitcherScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { DnsRepository(context) }
    var dnsInput by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(repository.getDnsHistory()) }
    var isDnsEnabled by remember { mutableStateOf(false) }
    var activeSpecifier by remember { mutableStateOf<String?>(null) }
    var isDnsToggling by remember { mutableStateOf(false) }

    fun refreshState() {
        isDnsEnabled = try { DnsManager.isDnsEnabled(context) } catch (e: Exception) { false }
        activeSpecifier = try { DnsManager.getActiveSpecifier(context) } catch (e: Exception) { null }
    }

    fun runDnsAction(action: () -> Unit, successMessage: String? = null) {
        if (isDnsToggling) return

        isDnsToggling = true
        DashboardWidgetProvider.showPendingDnsState(context)

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    action()
                }
                refreshState()
                history = repository.getDnsHistory()
                updateWidgets(context)
                successMessage?.let {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }
            } catch (e: SecurityException) {
                Toast.makeText(
                    context,
                    "Нет разрешения WRITE_SECURE_SETTINGS. Выдайте через ADB.",
                    Toast.LENGTH_LONG
                ).show()
                refreshState()
                updateWidgets(context)
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                refreshState()
                updateWidgets(context)
            } finally {
                isDnsToggling = false
            }
        }
    }

    // Обновляем состояние при КАЖДОМ возврате в приложение (onResume)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshState()
        history = repository.getDnsHistory()
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
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        val statusColor = when {
            isDnsToggling -> ColorPending
            isDnsEnabled -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.error
        }
        val statusText = when {
            isDnsToggling -> "Переключаем..."
            isDnsEnabled -> "Включен"
            else -> "Выключен"
        }
        Text(
            text = "Статус: $statusText",
            style = MaterialTheme.typography.titleMedium,
            color = statusColor
        )

        if (isDnsToggling) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = ColorPending
            )
        }

        if (isDnsEnabled && !activeSpecifier.isNullOrBlank() && !isDnsToggling) {
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
                text = when {
                    isDnsToggling -> "Подождите..."
                    isDnsEnabled -> "DNS включен"
                    else -> "DNS выключен"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDnsToggling) ColorPending else MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = isDnsEnabled,
                enabled = !isDnsToggling,
                onCheckedChange = { checked ->
                    runDnsAction(action = {
                        if (checked) {
                            val activeDns = dnsInput.trim().ifEmpty {
                                history.firstOrNull() ?: "dns.adguard-dns.com"
                            }
                            DnsManager.enableDns(context, activeDns)
                            if (dnsInput.trim().isNotEmpty()) {
                                repository.saveDns(dnsInput.trim())
                            }
                        } else {
                            DnsManager.disableDns(context)
                        }
                    })
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
            enabled = !isDnsToggling,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val trimmed = dnsInput.trim()
                if (trimmed.isEmpty()) {
                    Toast.makeText(context, "Введите DNS-адрес", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                keyboardController?.hide()
                focusManager.clearFocus()
                runDnsAction(
                    action = {
                        repository.saveDns(trimmed)
                        DnsManager.enableDns(context, trimmed)
                    },
                    successMessage = "Сохранено и включено"
                )
            },
            enabled = !isDnsToggling,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isDnsToggling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Переключаем...")
            } else {
                Text("Сохранить и включить")
            }
        }

        if (history.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Последние DNS",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                history.forEach { address ->
                    AssistChip(
                        onClick = { dnsInput = address },
                        enabled = !isDnsToggling,
                        label = { Text(address, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }

        // Настройки для расчёта расстояния
        UserSettingsSection(context)
    }
}

@Composable
private fun UserSettingsSection(context: Context) {
    var heightCm by remember { mutableIntStateOf(StepsCalculator.getHeightCm(context)) }
    var isMale by remember { mutableStateOf(StepsCalculator.isMale(context)) }
    var hasBasicPermission by remember { mutableStateOf(false) }
    var hasBackgroundPermission by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Проверяем разрешение Health Connect при входе
    LaunchedEffect(Unit) {
        hasBasicPermission = StepsReader.hasBasicPermissionAsync(context)
        hasBackgroundPermission = StepsReader.hasBackgroundPermissionAsync(context)
    }

    // Обновляем состояние разрешений при возвращении в приложение
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch {
            hasBasicPermission = StepsReader.hasBasicPermissionAsync(context)
            hasBackgroundPermission = StepsReader.hasBackgroundPermissionAsync(context)
        }
    }

    // Лаунчер для запроса разрешений Health Connect
    val healthConnectLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        scope.launch {
            hasBasicPermission = StepsReader.hasBasicPermissionAsync(context)
            hasBackgroundPermission = StepsReader.hasBackgroundPermissionAsync(context)
            if (hasBasicPermission && hasBackgroundPermission) {
                DashboardUpdateWorker.scheduleImmediateUpdate(context)
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    HorizontalDivider()

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Настройки шагомера",
        style = MaterialTheme.typography.titleMedium
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Рост
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Рост",
            style = MaterialTheme.typography.bodyLarge
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    if (heightCm > 100) {
                        heightCm--
                        StepsCalculator.setHeightCm(context, heightCm)
                        DashboardWidgetProvider.refreshAllWidgets(context)
                    }
                }
            ) {
                Text("−", style = MaterialTheme.typography.headlineSmall)
            }

            Text(
                text = "$heightCm см",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.width(70.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            IconButton(
                onClick = {
                    if (heightCm < 250) {
                        heightCm++
                        StepsCalculator.setHeightCm(context, heightCm)
                        DashboardWidgetProvider.refreshAllWidgets(context)
                    }
                }
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }

    // Пол
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Пол",
            style = MaterialTheme.typography.bodyLarge
        )

        Row {
            FilterChip(
                selected = isMale,
                onClick = {
                    isMale = true
                    StepsCalculator.setIsMale(context, true)
                    DashboardWidgetProvider.refreshAllWidgets(context)
                },
                label = { Text("Мужской") }
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilterChip(
                selected = !isMale,
                onClick = {
                    isMale = false
                    StepsCalculator.setIsMale(context, false)
                    DashboardWidgetProvider.refreshAllWidgets(context)
                },
                label = { Text("Женский") }
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Показать расчётную длину шага
    val stepLength = StepsCalculator.getStepLengthMeters(context)
    Text(
        text = "Длина шага: ${String.format("%.0f", stepLength * 100)} см",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Кнопка разрешения Health Connect
    if (StepsReader.isHealthConnectAvailable(context)) {
        val buttonColor = when {
            hasBasicPermission && hasBackgroundPermission -> MaterialTheme.colorScheme.primary
            hasBasicPermission -> Color(0xFFFF9800) // Оранжевый/Жёлтый (требуется доступ в фоне)
            else -> MaterialTheme.colorScheme.error // Красный (нет базовых разрешений)
        }

        val buttonText = when {
            hasBasicPermission && hasBackgroundPermission -> "✓ Health Connect подключён"
            hasBasicPermission -> "Разрешить работу в фоне"
            else -> "Подключить Health Connect"
        }

        Button(
            onClick = {
                scope.launch {
                    if (!hasBasicPermission) {
                        val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))
                        healthConnectLauncher.launch(permissions)
                    } else if (!hasBackgroundPermission) {
                        Toast.makeText(
                            context,
                            "Включите «Доступ к данным в фоновом режиме» в разделе «Дополнительный доступ»",
                            Toast.LENGTH_LONG
                        ).show()
                        try {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS")
                                    .putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                            } else {
                                Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Не удалось открыть настройки", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Если всё уже подключено, просто открываем настройки для управления
                        try {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS")
                                    .putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                            } else {
                                Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
        ) {
            Text(buttonText)
        }
    } else {
        Text(
            text = "Health Connect недоступен на этом устройстве",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

private fun updateWidgets(context: android.content.Context) {
    val widgetIntent = Intent(context, DnsWidgetProvider::class.java).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    }
    val ids = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, DnsWidgetProvider::class.java))
    widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    context.sendBroadcast(widgetIntent)
    DashboardWidgetProvider.refreshAllWidgets(context)
}

