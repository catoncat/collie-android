package com.collie.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.collie.app.data.HerdRepository
import com.collie.app.notify.CollieNotifications
import com.collie.app.notify.HerdService
import com.collie.app.ui.CollieRoot
import com.collie.app.ui.theme.CollieTheme

class MainActivity : FragmentActivity() {
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF1C1C1C.toInt()
        window.navigationBarColor = 0xFF1C1C1C.toInt()
        CollieNotifications.ensureChannels(this)
        startForegroundService(Intent(this, HerdService::class.java))
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        handle(intent)
        setContent {
            CollieTheme { CollieRoot() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent == null) return
        val pane = intent.getStringExtra(CollieNotifications.EXTRA_AGENT)
        if (pane != null) {
            HerdRepository.unlock()
            HerdRepository.openPane(pane)
        }
        if (intent.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
                ?: intent.type
            if (!text.isNullOrBlank()) {
                HerdRepository.unlock()
                HerdRepository.receiveShare(text)
            }
        }
    }
}
