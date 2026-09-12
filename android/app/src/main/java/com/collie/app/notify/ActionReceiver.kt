package com.collie.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.collie.app.data.HerdRepository
import com.collie.app.widget.HerdWidget

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val agentId = intent.getStringExtra(CollieNotifications.EXTRA_AGENT) ?: return
        when (intent.action) {
            CollieNotifications.ACTION_APPROVE -> HerdRepository.answer(agentId, "yes")
            CollieNotifications.ACTION_DENY -> HerdRepository.answer(agentId, "no")
        }
        val notifId = intent.getIntExtra("notif_id", -1)
        if (notifId > 0) NotificationManagerCompat.from(context).cancel(notifId)
        CollieNotifications.syncAsks(context, HerdRepository.snapshot())
        HerdWidget.refresh(context)
    }
}
