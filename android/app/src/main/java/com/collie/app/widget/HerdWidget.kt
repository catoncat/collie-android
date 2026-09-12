package com.collie.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.collie.app.MainActivity
import com.collie.app.R
import com.collie.app.data.AgentStatus
import com.collie.app.data.HerdRepository

class HerdWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val s = HerdRepository.snapshot()
        val blocked = s.agents.filter { it.status == AgentStatus.Blocked }
        val first = blocked.firstOrNull()
        val detail = first?.let { "${it.name} · ${it.ask?.command ?: it.cwd}" }
            ?: if (s.localeZh) "没有人在等你" else "Nobody is waiting"
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_herd)
            views.setTextViewText(R.id.widget_count, blocked.size.toString())
            views.setTextViewText(R.id.widget_detail, detail)
            views.setOnClickPendingIntent(R.id.widget_root, open)
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    companion object {
        fun refresh(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, HerdWidget::class.java))
            if (ids.isEmpty()) return
            val i = Intent(context, HerdWidget::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(i)
        }
    }
}
