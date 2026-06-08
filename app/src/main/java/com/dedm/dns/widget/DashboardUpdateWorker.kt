package com.dedm.dns.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class DashboardUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork started")
        
        // Читаем шаги асинхронно и сохраняем в кэш
        try {
            val steps = StepsReader.readTodaySteps(context)
            Log.d(TAG, "Steps read: $steps")
            if (steps != null) {
                saveStepsToCache(context, steps)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read steps", e)
        }

        // Обновляем все виджеты
        DashboardWidgetProvider.refreshAllWidgets(context)

        // Планируем следующее обновление если есть виджеты
        if (hasDashboardWidgets(context)) {
            scheduleNextUpdate(context)
        }

        Log.d(TAG, "doWork completed")
        return Result.success()
    }

    companion object {
        private const val TAG = "DashboardWorker"
        const val WORK_NAME = "dashboard_update_work"
        private const val UPDATE_INTERVAL_MINUTES = 5L

        fun scheduleNextUpdate(context: Context) {
            val request = OneTimeWorkRequestBuilder<DashboardUpdateWorker>()
                .setInitialDelay(UPDATE_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun scheduleImmediateUpdate(context: Context) {
            val request = OneTimeWorkRequestBuilder<DashboardUpdateWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        private fun saveStepsToCache(context: Context, steps: Long) {
            DashboardWidgetProvider.saveStepsToCache(context, steps)
        }

        private fun hasDashboardWidgets(context: Context): Boolean {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DashboardWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            return appWidgetIds.isNotEmpty()
        }
    }
}
