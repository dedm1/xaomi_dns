package com.dedm.dns.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
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
        return try {
            // Читаем шаги асинхронно и сохраняем в кэш
            try {
                val steps = StepsReader.readTodaySteps(context)
                if (steps != null) {
                    saveStepsToCache(context, steps)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read steps", e)
            }

            // Помечаем время синхронизации (сбрасывает индикатор в зелёный)
            FreshnessIndicator.markSynced(context)

            // Обновляем все виджеты
            DashboardWidgetProvider.refreshAllWidgets(context)
            
            // Планируем следующее обновление через Handler.postDelayed,
            // чтобы это произошло ПОСЛЕ полного завершения doWork() и освобождения
            // WorkManager-ресурсов. Иначе REPLACE отменяет текущий "живой" воркер.
            if (hasDashboardWidgets(context)) {
                Handler(Looper.getMainLooper()).postDelayed({
                    scheduleNextUpdate(context)
                }, 200)
            }
            
            Result.success()
        } catch (t: Throwable) {
            Log.e(TAG, "doWork failed", t)
            
            // Даже при ошибке планируем следующий запуск
            if (hasDashboardWidgets(context)) {
                Handler(Looper.getMainLooper()).postDelayed({
                    scheduleNextUpdate(context)
                }, 200)
            }
            
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "DashboardWorker"
        const val WORK_NAME = "dashboard_update_work"
        private const val UPDATE_INTERVAL_SECONDS = 300L

        fun scheduleNextUpdate(context: Context) {
            val request = OneTimeWorkRequestBuilder<DashboardUpdateWorker>()
                .setInitialDelay(UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS)
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
