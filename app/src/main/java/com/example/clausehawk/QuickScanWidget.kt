package com.example.clausehawk

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.clausehawk.R

class QuickScanWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Perform setup when the first widget is added to the home screen
    }

    override fun onDisabled(context: Context) {
        // Clean up resources when the last widget instance is removed
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    // Intent to open MainActivity when the widget action is triggered
    val intent = Intent(context, MainActivity::class.java).apply {
        action = "ACTION_QUICK_SCAN"
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Inflate layout using the explicit project R reference
    val views = RemoteViews(context.packageName, R.layout.widget_quick_scan)

    // Set the intent to trigger when clicking the scan button/view
    views.setOnClickPendingIntent(R.id.btn_scan, pendingIntent)

    // Apply updates to the widget manager
    appWidgetManager.updateAppWidget(appWidgetId, views)
}