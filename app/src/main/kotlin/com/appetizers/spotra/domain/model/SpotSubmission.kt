package com.appetizers.spotra.domain.model

data class SpotSubmission(
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val building: String,
    val floor: String,
    val submittedByEmail: String,
    val submittedByUserId: String?,
    val bookingUrl: String? = null,
)
