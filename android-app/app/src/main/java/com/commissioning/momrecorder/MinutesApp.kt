package com.commissioning.momrecorder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class MinutesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                "recording_channel",
                "Minutes Recording",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active while Minutes is recording a meeting"
            }
        )
    }
}
