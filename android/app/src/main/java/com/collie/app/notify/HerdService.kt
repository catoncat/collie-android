package com.collie.app.notify

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.collie.app.data.HerdRepository
import com.collie.app.widget.HerdWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HerdService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        CollieNotifications.ensureChannels(this)
        startForeground(CollieNotifications.ONGOING_ID, CollieNotifications.ongoing(this, HerdRepository.snapshot()))
        scope.launch {
            while (isActive) {
                delay(2800)
                HerdRepository.tick()
                val snap = HerdRepository.snapshot()
                CollieNotifications.syncAsks(this@HerdService, snap)
                HerdWidget.refresh(this@HerdService)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
