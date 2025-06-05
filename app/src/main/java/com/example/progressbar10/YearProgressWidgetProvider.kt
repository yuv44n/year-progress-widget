package com.example.progressbar10

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import kotlin.math.roundToInt

class YearProgressWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleDailyUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelDailyUpdate(context)
    }

    private fun scheduleDailyUpdate(context: Context) {
        val intent = Intent(context, YearProgressWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 5) // Small offset after midnight
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelDailyUpdate(context: Context) {
        val intent = Intent(context, YearProgressWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
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

        appWidgetManager.updateAppWidget(appWidgetId, views)
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
