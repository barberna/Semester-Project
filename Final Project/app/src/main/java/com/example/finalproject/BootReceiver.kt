package com.example.finalproject

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device rebooted, re-registering geofences...")
            // Need to call viewModel.setupGeofencing in order to reregister geofencing on device reboot

            // create app and DB instance
            val app = context.applicationContext as HuntHealth
            val appDB = app.appDB.getInstance()

            CoroutineScope(Dispatchers.IO).launch {
                // If there are stands create the geofences so we do not have to open the app, reregister them
                try {
                    val stands = appDB.getStands().first()

                    if (stands.isNotEmpty()) {
                        // We need an instance of the viewModel in order to call setupGeofences
                        val viewModel = AppViewModel(app)
                        viewModel.setupGeofencing(context, stands)

                        Log.d("BootReceiver", "Successfully re-registered ${stands.size} stands.")
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to re-register geofences after reboot", e)
                }
            }
        }
    }
}