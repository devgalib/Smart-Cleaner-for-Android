package com.d4rk.cleaner.app.clean.scanner.domain.usecases

import kotlinx.coroutines.flow.StateFlow

/**
 * Exposes streak related data and actions.
 */
interface StreakUseCase {
    val cleanStreak: StateFlow<Int>
    val streakRecord: StateFlow<Int>
    val showStreakCard: StateFlow<Boolean>

    fun setHideStreakDialogVisibility(isVisible: Boolean)
    fun hideStreakForNow()
    fun hideStreakPermanently()
}
