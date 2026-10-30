package com.andrerinas.openheadunit.utils

/**
 * Decides when a live panel reading is the other display the user has just brought up, while the
 * "add a display" screen waits. Turning the device does not count: the sizes are compared in their
 * canonical order, so portrait and landscape are the same screen.
 */
object NewDisplayPolicy {

    /** True when this panel is a different screen from the one the wait started on. */
    fun isNewDisplay(startWidth: Int, startHeight: Int, width: Int, height: Int): Boolean {
        val started = DensityScreenProfile.screen(startWidth, startHeight) ?: return false
        val live = DensityScreenProfile.screen(width, height) ?: return false
        return DensityScreenProfile.distance(started, live) > DensityScreenProfile.MATCH_TOLERANCE_PERMILLE
    }
}
