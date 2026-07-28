package com.appetizers.spotra.domain.model

data class Review(
    val id: String,
    val spotSlug: String,
    val reviewerName: String,
    val reviewerInitials: String,
    val reviewerId: String,
    val rating: Int,
    val noiseLevel: String? = null,
    val lighting: String? = null,
    val wifiQuality: String? = null,
    val occupancyPercent: Int? = null,
    val comment: String? = null,
    val anonymous: Boolean = false,
    val isOwnedByCurrentUser: Boolean = false,
)

data class ReviewDraft(
    val spotSlug: String,
    val rating: Int,
    val noiseLevel: String? = null,
    val lighting: String? = null,
    val wifiQuality: String? = null,
    val occupancyPercent: Int? = null,
    val comment: String? = null,
    val anonymous: Boolean = false,
    val qualityScore: Int = 0,
)
