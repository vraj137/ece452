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
        last?.takeIf {
            System.currentTimeMillis() - it.time <= 2 * 60 * 1000 &&
                it.accuracy <= 200f
        }?.let { return Pair(it.latitude, it.longitude) }

        val cancellation = CancellationTokenSource()
        val fresh = runCatching {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token).await()
        }.getOrNull()
        return fresh?.let { Pair(it.latitude, it.longitude) }
    }
}
