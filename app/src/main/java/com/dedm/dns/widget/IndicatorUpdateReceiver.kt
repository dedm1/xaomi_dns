package com.dedm.dns.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.RemoteViews
import com.dedm.dns.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IndicatorUpdateReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isInteractive) {
            // Экран выключен — останавливаем обновления, чтобы не будить систему
            return
        }

        // Обновляем только цвет индикатора (partial update)
        updateIndicatorColor(context)
        
        // Проверяем, нужно ли запустить фоновую синхронизацию
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                checkAndScheduleSyncIfNeeded(context)
            } finally {
                pendingResult.finish()
            }
        }
        
        // Планируем следующее обновление через 5 секунд
        scheduleNextUpdate(context)
    }
    
    companion object {
        private const val TAG = "IndicatorReceiver"
        const val ACTION_UPDATE_INDICATOR = "com.dedm.dns.UPDATE_INDICATOR"
        private const val UPDATE_INTERVAL_MS = 5000L // 5 секунд

        private suspend fun checkAndScheduleSyncIfNeeded(context: Context) {
            val lastSync = FreshnessIndicator.getLastSyncTime(context)
            val elapsedMs = System.currentTimeMillis() - lastSync
            if (elapsedMs >= 300_000L) {
                if (StepsReader.hasPermissionAsync(context)) {
                    Log.d(TAG, "Sync is stale (elapsed ${elapsedMs / 1000}s) and permission granted, scheduling update")
                    DashboardWidgetProvider.updateStepsAsync(context)
                } else {
                    Log.d(TAG, "Sync is stale but Health Connect permission not granted, skipping update")
                }
            }
        }
        
        fun updateIndicatorColor(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DashboardWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isEmpty()) {
                cancelUpdates(context)
                return
            }
            
            val color = FreshnessIndicator.getColorForCurrentTime(context)
            
            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.dashboard_widget)
                views.setInt(R.id.sync_indicator, "setColorFilter", color)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
            }
        }
        
        fun scheduleNextUpdate(context: Context) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isInteractive) {
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, IndicatorUpdateReceiver::class.java).apply {
                action = ACTION_UPDATE_INDICATOR
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val triggerTime = System.currentTimeMillis() + UPDATE_INTERVAL_MS
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC,
                            triggerTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC,
                            triggerTime,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC,
                        triggerTime,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Cannot schedule exact alarm, using inexact", e)
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC,
                    triggerTime,
                    pendingIntent
                )
            }
        }
        
        fun startUpdates(context: Context) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isInteractive) {
                return
            }
            updateIndicatorColor(context)
            scheduleNextUpdate(context)
        }
        
        fun cancelUpdates(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, IndicatorUpdateReceiver::class.java).apply {
                action = ACTION_UPDATE_INDICATOR
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
