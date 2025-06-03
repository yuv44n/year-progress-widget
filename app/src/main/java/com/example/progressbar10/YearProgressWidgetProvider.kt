package com.example.progressbar10

import android.graphics.*
import android.content.Context
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Intent
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

class YearProgressWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
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

        // Set click intent on the title image instead
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
//        views.setOnClickPendingIntent(R.id.title_image, pendingIntent)

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

    fun createTitleBitmap(context: Context, year: Int): Bitmap {
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
