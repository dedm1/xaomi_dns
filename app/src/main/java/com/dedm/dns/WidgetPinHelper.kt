package com.dedm.dns

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import com.dedm.dns.widget.DashboardWidgetProvider

object WidgetPinHelper {

    fun pinDnsWidget(context: Context) {
        pinWidget(context, DnsWidgetProvider::class.java, "Виджет DNS 1×1")
    }

    fun pinDashboardWidget(context: Context) {
        pinWidget(context, DashboardWidgetProvider::class.java, "Дашборд 1×3")
    }

    private fun pinWidget(context: Context, providerClass: Class<*>, widgetName: String) {
        val manager = AppWidgetManager.getInstance(context)

        if (manager.isRequestPinAppWidgetSupported) {
            val provider = ComponentName(context, providerClass)
            val success = manager.requestPinAppWidget(provider, null, null)
            if (!success) {
                Toast.makeText(context, "Не удалось добавить $widgetName", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                context,
                "Добавьте $widgetName вручную с рабочего стола",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
