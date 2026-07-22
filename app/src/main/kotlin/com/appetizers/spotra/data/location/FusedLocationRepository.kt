package com.appetizers.spotra.data.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

class FusedLocationRepository(private val context: Context) : LocationRepository {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun getLastLocation(): Pair<Double, Double>? {
        val last = runCatching { client.lastLocation.await() }.getOrNull()
        if (last != null) return Pair(last.latitude, last.longitude)

        val cancellation = CancellationTokenSource()
        val fresh = runCatching {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token).await()
        }.getOrNull()
        return fresh?.let { Pair(it.latitude, it.longitude) }
    }
}

class DebugLocationRepository : LocationRepository {
    override suspend fun getLastLocation() = Pair(43.4720, -80.5430)
}
