package com.collie.app.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.collie.app.MainActivity
import com.collie.app.R
import com.collie.app.data.AgentStatus
import com.collie.app.data.HerdState

object CollieNotifications {
    const val ONGOING_ID = 1
    const val CHANNEL_ONGOING = "collie_ongoing"
    const val CHANNEL_ASK = "collie_ask"
    const val ACTION_APPROVE = "com.collie.app.APPROVE"
    const val ACTION_DENY = "com.collie.app.DENY"
    const val EXTRA_AGENT = "agent_id"

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ONGOING, ctx.getString(R.string.channel_ongoing), NotificationManager.IMPORTANCE_LOW),
        )
        val ask = NotificationChannel(CHANNEL_ASK, ctx.getString(R.string.channel_needs_you), NotificationManager.IMPORTANCE_HIGH)
        ask.enableVibration(true)
        ask.setShowBadge(true)
        nm.createNotificationChannel(ask)
    }

    fun ongoing(ctx: Context, s: HerdState): Notification {
        val blocked = s.agents.count { it.status == AgentStatus.Blocked }
        val working = s.agents.count { it.status == AgentStatus.Working }
        val title = if (s.localeZh) "Collie · $blocked 需要你" else "Collie · $blocked need you"
        val text = if (s.localeZh) "$working 工作中 · herdr" else "$working working · herdr"
        return NotificationCompat.Builder(ctx, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_collie)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openApp(ctx, null))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun syncAsks(ctx: Context, s: HerdState) {
        val nm = NotificationManagerCompat.from(ctx)
        val blocked = s.agents.filter { it.status == AgentStatus.Blocked && it.ask != null }
        val liveIds = blocked.map { 100 + (it.id.hashCode() and 0x7fffffff) }.toSet()
        // Cancel stale ask notifications — keep ongoing (id 1) and widget noise out.
        if (Build.VERSION.SDK_INT >= 23) {
            ctx.getSystemService(NotificationManager::class.java).activeNotifications
                .filter { it.id != ONGOING_ID && it.id !in liveIds }
                .forEach { nm.cancel(it.id) }
        }
        for (agent in blocked) {
            val id = 100 + (agent.id.hashCode() and 0x7fffffff)
            val body = agent.ask?.command ?: agent.ask?.title ?: agent.cwd
            val b = NotificationCompat.Builder(ctx, CHANNEL_ASK)
                .setSmallIcon(R.drawable.ic_stat_collie)
                .setContentTitle("${agent.name} needs you")
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(openApp(ctx, agent.id))
                .addAction(0, ctx.getString(R.string.approve), action(ctx, ACTION_APPROVE, agent.id, id))
                .addAction(0, ctx.getString(R.string.deny), action(ctx, ACTION_DENY, agent.id, id))
                .addAction(0, ctx.getString(R.string.open), openApp(ctx, agent.id))
                .setColor(0xFFE07070.toInt())
            nm.notify(id, b.build())
        }
        nm.notify(ONGOING_ID, ongoing(ctx, s))
    }

    private fun openApp(ctx: Context, paneId: String?): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (paneId != null) putExtra(EXTRA_AGENT, paneId)
        }
        return PendingIntent.getActivity(
            ctx,
            paneId?.hashCode() ?: 0,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun action(ctx: Context, action: String, agentId: String, notifId: Int): PendingIntent {
        val i = Intent(ctx, ActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_AGENT, agentId)
            putExtra("notif_id", notifId)
        }
        return PendingIntent.getBroadcast(
            ctx,
            (action + agentId).hashCode(),
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
