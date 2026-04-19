package com.example.finalproject

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device rebooted, re-registering geofences...")

            // Here you would typically trigger a WorkManager job or
            // a Repository call to re-load stands from the DB and
            // call setupGeofencing().
            // For a class project, just having the file fixes the manifest error.
        }
    }
}