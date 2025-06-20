package com.example.progressbar10

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import kotlin.math.roundToInt

class YearProgressWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_CLICK = "com.example.progressbar10.WIDGET_CLICK"
        const val ACTION_HOURLY_UPDATE = "com.example.progressbar10.HOURLY_UPDATE"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_WIDGET_CLICK -> {
                // Handle widget click - update all widgets
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    intent.component ?: return
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
            ACTION_HOURLY_UPDATE -> {
                // Handle hourly update
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    intent.component ?: return
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
                // Schedule next hourly update
                scheduleHourlyUpdate(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Reschedule updates after device boot
                scheduleHourlyUpdate(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleHourlyUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelHourlyUpdate(context)
    }

    private fun scheduleHourlyUpdate(context: Context) {
        try {
            val intent = Intent(context, YearProgressWidgetProvider::class.java).apply {
                action = ACTION_HOURLY_UPDATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.HOUR_OF_DAY, 1) // Schedule for next hour
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Use setExactAndAllowWhileIdle for more reliable scheduling
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            // Fallback to system widget update if alarm scheduling fails
            e.printStackTrace()
        }
    }

    private fun cancelHourlyUpdate(context: Context) {
        try {
            val intent = Intent(context, YearProgressWidgetProvider::class.java).apply {
                action = ACTION_HOURLY_UPDATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        try {
            val views = RemoteViews(context.packageName, R.layout.year_progress_widget)

            val yearProgress = calculateYearProgress()
            val currentYear = LocalDate.now().year

            // Create and set the title bitmap
            val bitmap = createTitleBitmap(context, currentYear)
            views.setImageViewBitmap(R.id.title_image, bitmap)

            // Update progress bar and bottom text
            views.setProgressBar(R.id.year_progress, 100, yearProgress.percentage.roundToInt(), false)
            views.setTextViewText(R.id.percentage_done, "${yearProgress.percentage.roundToInt()}% DONE")
            views.setTextViewText(R.id.days_left, "${yearProgress.daysLeft} DAYS LEFT")

            // Set up click listener for the entire widget
            val clickIntent = Intent(context, YearProgressWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_CLICK
            }
            val clickPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, clickIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, clickPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateYearProgress(): YearProgress {
        val now = LocalDate.now()
        val startOfYear = LocalDate.of(now.year, 1, 1)
        val endOfYear = LocalDate.of(now.year, 12, 31)

        val totalDaysInYear = ChronoUnit.DAYS.between(startOfYear, endOfYear) + 1
        val daysPassed = ChronoUnit.DAYS.between(startOfYear, now) + 1 // Include today as passed
        val daysLeft = ChronoUnit.DAYS.between(now, endOfYear) // Do not add +1 here

        val percentage = (daysPassed.toDouble() / totalDaysInYear.toDouble()) * 100

        return YearProgress(percentage, daysLeft.toInt())
    }

    private fun createTitleBitmap(context: Context, year: Int): Bitmap {
        val width = 400
        val height = 120
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER

        // Load custom font from assets
        val typeface = Typeface.createFromAsset(context.assets, "ndot_55.otf")
        paint.typeface = typeface

        val yearY = 50f
        val spacing = 50f // space between the two lines
        val progressBarY = yearY + spacing + paint.textSize

        paint.textSize = 64f
        canvas.drawText("YEAR $year", width / 2f, yearY, paint)

        paint.textSize = 48f
        canvas.drawText("Progress Bar", width / 2f, progressBarY, paint)

        return bitmap
    }

    data class YearProgress(val percentage: Double, val daysLeft: Int)
}
