package com.dedm.dns

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast

class DnsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val isEnabled = try {
            DnsManager.isDnsEnabled(context)
        } catch (e: Exception) {
            false
        }
        val color = if (isEnabled) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setInt(R.id.widget_background, "setColorFilter", color)

            val intent = Intent(context, DnsWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_DNS
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_DNS) {
            val repository = DnsRepository(context)
            val history = repository.getDnsHistory()
            val targetDns = history.firstOrNull() ?: "dns.adguard-dns.com"

            try {
                DnsManager.toggleDns(context, targetDns)
            } catch (e: SecurityException) {
                Toast.makeText(context, "Ошибка: нет WRITE_SECURE_SETTINGS. Выдайте через ADB.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, DnsWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_TOGGLE_DNS = "com.example.dns_switcher.TOGGLE_DNS"
    }
}
