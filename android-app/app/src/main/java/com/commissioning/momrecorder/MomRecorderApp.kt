package com.commissioning.momrecorder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class MomRecorderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    private fun createChannels() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("recording_channel", "Recording", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Active recording notifications"
            }
        )
    }
}
