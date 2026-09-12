package com.collie.app

import android.app.Application
import com.collie.app.notify.CollieNotifications

class CollieApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CollieNotifications.ensureChannels(this)
    }
}
