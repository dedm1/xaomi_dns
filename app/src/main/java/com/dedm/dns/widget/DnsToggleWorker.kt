package com.dedm.dns.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dedm.dns.DnsManager
import com.dedm.dns.DnsRepository
import com.dedm.dns.DnsWidgetProvider

class DnsToggleWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = DnsRepository(context)
        val history = repository.getDnsHistory()
        val targetDns = history.firstOrNull() ?: "dns.adguard-dns.com"

        try {
            DnsManager.toggleDns(context, targetDns)
        } catch (e: SecurityException) {
            Log.e(TAG, "No WRITE_SECURE_SETTINGS permission", e)
        } catch (e: Exception) {
            Log.e(TAG, "DNS toggle error", e)
        }

        DashboardWidgetProvider.refreshAllWidgets(context)

        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, DnsWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            val updateIntent = Intent(context, DnsWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(updateIntent)
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "DnsToggleWorker"
        private const val WORK_NAME = "dns_toggle_work"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<DnsToggleWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
