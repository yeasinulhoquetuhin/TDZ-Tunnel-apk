package com.example.utils

import android.view.HapticFeedbackConstants
import android.view.View
import kotlinx.coroutines.delay

class AppHapticManager(private val view: View, private val isEnabled: () -> Boolean) {

    fun triggerSuccess() {
        if (!isEnabled()) return
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun triggerError() {
        if (!isEnabled()) return
        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    }

    suspend fun triggerConnectionPulse() {
        if (!isEnabled()) return
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        delay(150)
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
    
    fun triggerClick() {
        if (!isEnabled()) return
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}