package com.example.finalproject

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.finalproject.data.Sit
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return

        // This ONLY triggers when crossing the boundary from OUTSIDE to INSIDE
        if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

            val app = context.applicationContext as HuntHealth
            val appDAO = app.appDB.getInstance()


            triggeringGeofences.forEach { geofence ->
                val standId = geofence.requestId.toInt()
                // Update your DB here. Since this is a receiver, it's best to
                // call a function in your Repository or use a WorkManager.


                CoroutineScope(Dispatchers.IO).launch {
                    try{
                        val lastSit = appDAO.getLatestSitForStand(standId)
                        val now = LocalDateTime.now()

                        // TIME-GATE: Only add a sit if:
                        // - No sit exists yet OR The last sit was more than 3 hours ago
                        // This allows Morning/Evening hunts but ignores GPS jitter (the "bounce")
                        val isNewVisit = lastSit == null || lastSit.date.isBefore(now.minusMinutes(2))

                        if (isNewVisit) {
                            val stand = appDAO.getStandId(standId) ?: return@launch

                            // Create the sit record
                            val sit = Sit(
                                standId = standId,
                                standName = stand.name,
                                date = now
                            )

                            // Add sit record to DB
                            appDAO.addSitRecord(sit)

                            val tenDaysAgo = LocalDateTime.now().minusDays(10)
                            val count = appDAO.getStandSitCount(standId, tenDaysAgo)
                            val newHealthStatus = AppViewModel.determineHealthStatus(count)

                            val updatedStand = stand.copy(
                                sitCount = stand.sitCount + 1,
                                healthStatus = newHealthStatus
                            )

                            appDAO.updateStand(updatedStand)
                            Log.d("Geofence", "Recorded visit for ${stand.name}")
                        } else {
                            Log.d("Geofence", "GPS Jitter or user still at stand $standId. Skipping.")
                        }
                        // Get stand from DB

                    } catch (e: Exception) {
                        Log.e("Geofence", "Unable to add sit record to Sits table", e)
                    }
                }
            }
        }
    }
}