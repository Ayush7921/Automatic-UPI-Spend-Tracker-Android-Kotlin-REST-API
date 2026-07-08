package com.example.upipaymenttracker

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit

object LocationHelper {
    
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context): Location? {
        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            // Try last location first (fast)
            val lastLocationTask = fusedLocationClient.lastLocation
            val lastLocation = Tasks.await(lastLocationTask, 2, TimeUnit.SECONDS)
            
            if (lastLocation != null) return lastLocation

            // If no last location, request current (slower)
            val currentTask = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            Tasks.await(currentTask, 3, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.e("LOCATION", "Failed to get location: ${e.message}")
            null
        }
    }
}