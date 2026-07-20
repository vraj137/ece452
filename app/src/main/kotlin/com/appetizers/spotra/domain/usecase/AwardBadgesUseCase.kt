package com.appetizers.spotra.domain.usecase

import com.appetizers.spotra.domain.model.BadgeId
import com.appetizers.spotra.domain.repository.BadgeRepository
import com.appetizers.spotra.domain.repository.ReviewRepository

class AwardBadgesUseCase(
    private val badgeRepository: BadgeRepository,
    private val reviewRepository: ReviewRepository,
) {
    suspend fun onLogin(userId: String, newStreak: Int) {
        val milestone = mapOf(
            2 to BadgeId.LOGIN_STREAK_2,
            5 to BadgeId.LOGIN_STREAK_5,
            14 to BadgeId.LOGIN_STREAK_14,
        )[newStreak] ?: return
        badgeRepository.awardBadge(userId, milestone)
    }

    suspend fun onCheckout(userId: String, newCheckoutCount: Int) {
        when (newCheckoutCount) {
            1  -> badgeRepository.awardBadge(userId, BadgeId.FIRST_CHECKOUT)
            10 -> badgeRepository.awardBadge(userId, BadgeId.SESSION_VETERAN)
        }
    }

    suspend fun onReview(userId: String, qualityScore: Int) {
        val reviewCount = reviewRepository.getReviewCount(userId)
        if (reviewCount == 1) badgeRepository.awardBadge(userId, BadgeId.FIRST_REVIEW)
        if (qualityScore >= 5) {
            badgeRepository.awardBadge(userId, BadgeId.QUALITY_REVIEWER)
            if (reviewRepository.getQualityReviewCount(userId) >= 5) {
                badgeRepository.awardBadge(userId, BadgeId.PROLIFIC_REVIEWER)
            }
        }
    }
}
