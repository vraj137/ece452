package com.appetizers.spotra.domain.model

// A submitted review for a study spot. noise/lighting/wifi are self-reported
// (per the design decision to avoid mic/light sensors) and feed the spot's
// crowdsourced status. Reviews can only be created for a spot the user has
// checked into — that gate is enforced in the database (RLS).

data class Review(
    val id: String,
    val spotSlug: String,
    val reviewerName: String,        // "Anonymous" when the review was submitted anonymously
    val reviewerInitials: String,
    val reviewerId: String,
    val rating: Int,                 // 1..5
    val noiseLevel: String? = null,  // Silent / Low / Moderate / Lively
    val lighting: String? = null,    // Poor / Good / Bright / Natural
    val wifiQuality: String? = null, // Poor / OK / Good / Fast
    val occupancyPercent: Int? = null,
    val comment: String? = null,
    val anonymous: Boolean = false
)

data class ReviewDraft(
    val spotSlug: String,
    val rating: Int,
    val noiseLevel: String? = null,
    val lighting: String? = null,
    val wifiQuality: String? = null,
    val occupancyPercent: Int? = null,
    val comment: String? = null,
    val anonymous: Boolean = false
)
