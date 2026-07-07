package com.dedm.dns.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.dedm.dns.DnsManager
import com.dedm.dns.DnsRepository
import com.dedm.dns.DnsWidgetProvider
import com.dedm.dns.R
import java.time.LocalDate
import java.time.ZoneId

class DashboardWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_TOGGLE_DNS -> {
                toggleDns(context)
            }
            ACTION_OPEN_BATTERY -> {
                openBatterySettings(context)
            }
            ACTION_OPEN_STEPS -> {
                openStepsApp(context)
            }
            ACTION_REFRESH -> {
                updateStepsAsync(context)
            }
            ACTION_SYNC -> {
                forceSync(context)
            }
        }
    }
    
    private fun forceSync(context: Context) {
        // Устанавливаем индикатор в оранжевый (синхронизация в процессе)
        showPendingSyncState(context)
        // Запускаем полное обновление данных
        updateStepsAsync(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Запускаем периодическое фоновое обновление данных (15 минут)
        schedulePeriodicUpdate(context)
        // И делаем первый немедленный запуск при создании
        updateStepsAsync(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork(DashboardUpdateWorker.WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork("dashboard_periodic_update")
    }

    private fun toggleDns(context: Context) {
        showPendingDnsState(context)
        
        DnsToggleWorker.enqueue(context)
    }
    
    private fun openBatterySettings(context: Context) {
        try {
            val intent = Intent().apply {
                setClassName("com.miui.securitycenter", "com.miui.powercenter.PowerMainActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    private fun openStepsApp(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.mi.globalminusscreen")
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent?.let { context.startActivity(it) }
        } catch (e: Exception) {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage("com.mi.health")
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent?.let { context.startActivity(it) }
            } catch (e2: Exception) {
                Log.e(TAG, "Cannot open steps app", e2)
            }
        }
    }

    companion object {
        private const val TAG = "DashboardWidget"
        
        const val ACTION_TOGGLE_DNS = "com.dedm.dns.DASHBOARD_TOGGLE_DNS"
        const val ACTION_OPEN_BATTERY = "com.dedm.dns.DASHBOARD_OPEN_BATTERY"
        const val ACTION_OPEN_STEPS = "com.dedm.dns.DASHBOARD_OPEN_STEPS"
        const val ACTION_REFRESH = "com.dedm.dns.DASHBOARD_REFRESH"
        const val ACTION_SYNC = "com.dedm.dns.DASHBOARD_SYNC"
        
        private const val COLOR_PENDING = 0xFFFF9800.toInt()  // Оранжевый/жёлтый
        private const val COLOR_ON = 0xFF4CAF50.toInt()       // Зелёный
        private const val COLOR_OFF = 0xFFF44336.toInt()      // Красный

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.dashboard_widget)
            
            // Получаем данные
            val batteryTemp = BatteryTemperatureReader.read(context)
            val dnsEnabled = try { DnsManager.isDnsEnabled(context) } catch (e: Exception) { false }
            
            // Sync indicator (top-left)
            val indicatorColor = FreshnessIndicator.getColorForCurrentTime(context)
            views.setInt(R.id.sync_indicator, "setColorFilter", indicatorColor)
            views.setOnClickPendingIntent(R.id.sync_indicator, createPendingIntent(context, ACTION_SYNC, 4))
            
            // Battery block
            val batteryColor = BatteryTemperatureReader.getColorForTemp(batteryTemp)
            views.setInt(R.id.battery_bg, "setBackgroundColor", batteryColor)
            views.setTextViewText(R.id.battery_text, BatteryTemperatureReader.formatTemp(batteryTemp))
            views.setOnClickPendingIntent(R.id.block_battery, createPendingIntent(context, ACTION_OPEN_BATTERY, 1))

            // Steps block — обновляется асинхронно через Worker
            val cachedSteps = getStepsFromCache(context)
            views.setTextViewText(R.id.steps_text, cachedSteps?.toString() ?: "—")
            
            // Расстояние
            val distanceText = if (cachedSteps != null && cachedSteps > 0) {
                val meters = StepsCalculator.calculateDistanceMeters(context, cachedSteps)
                StepsCalculator.formatDistanceShort(meters)
            } else {
                "0м"
            }
            views.setTextViewText(R.id.distance_text, distanceText)
            views.setOnClickPendingIntent(R.id.block_steps, createPendingIntent(context, ACTION_OPEN_STEPS, 2))

            // DNS block
            val dnsColor = if (dnsEnabled) COLOR_ON else COLOR_OFF
            views.setInt(R.id.dns_bg, "setBackgroundColor", dnsColor)
            views.setTextViewText(R.id.dns_text, if (dnsEnabled) "ON" else "OFF")
            views.setOnClickPendingIntent(R.id.block_dns, createPendingIntent(context, ACTION_TOGGLE_DNS, 3))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun showPendingDnsState(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DashboardWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                // partiallyUpdateAppWidget — меняет ТОЛЬКО указанные view,
                // НЕ сбрасывает батарею и шаги
                val views = RemoteViews(context.packageName, R.layout.dashboard_widget)
                views.setInt(R.id.dns_bg, "setBackgroundColor", COLOR_PENDING)
                views.setTextViewText(R.id.dns_text, "...")
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
            }
        }

        fun showPendingSyncState(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DashboardWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.dashboard_widget)
                views.setInt(R.id.sync_indicator, "setColorFilter", COLOR_PENDING)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
            }
        }

        fun refreshAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DashboardWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun updateStepsAsync(context: Context) {
            DashboardUpdateWorker.scheduleImmediateUpdate(context)
        }

        private fun createPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, DashboardWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private const val CACHE_PREFS = "dashboard_widget_cache"

        private fun getStepsFromCache(context: Context): Long? {
            val prefs = context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
            val today = LocalDate.now(ZoneId.systemDefault()).toString()
            val todaySteps = prefs.getLong("steps_$today", -1L)
            
            if (todaySteps >= 0) {
                return todaySteps
            }
            
            // Если данных за сегодня ещё нет в кэше, но разрешение выдано, показываем 0 вместо прочерка
            return if (StepsReader.hasPermission(context)) 0L else null
        }

        fun saveStepsToCache(context: Context, steps: Long) {
            val prefs = context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
            val today = LocalDate.now(ZoneId.systemDefault()).toString()
            val minDateToKeep = LocalDate.now(ZoneId.systemDefault()).minusDays(4)
            
            prefs.edit().apply {
                putLong("steps_$today", steps)
                // Храним только последние 5 дней, чтобы не раздувать кэш
                prefs.all.keys.forEach { key ->
                    if (!key.startsWith("steps_") || key == "steps_$today") {
                        return@forEach
                    }

                    val cachedDate = runCatching {
                        LocalDate.parse(key.removePrefix("steps_"))
                    }.getOrNull()

                    if (cachedDate == null || cachedDate.isBefore(minDateToKeep)) {
                        remove(key)
                    }
                }
                apply()
            }
        }

        fun schedulePeriodicUpdate(context: Context) {
            val request = PeriodicWorkRequestBuilder<DashboardUpdateWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "dashboard_periodic_update",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
