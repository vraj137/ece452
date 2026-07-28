package com.appetizers.spotra.presentation.home

internal fun rootSpotPath(spotId: String): List<String> = listOf(spotId)

internal fun childSpotPath(currentPath: List<String>, childSpotId: String): List<String> =
    currentPath + childSpotId

internal fun previousSpotPath(currentPath: List<String>): List<String> =
    currentPath.dropLast(1)
