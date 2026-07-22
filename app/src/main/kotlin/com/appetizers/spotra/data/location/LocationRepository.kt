package com.appetizers.spotra.data.location

interface LocationRepository {
    suspend fun getLastLocation(): Pair<Double, Double>?  // (latitude, longitude)
}
