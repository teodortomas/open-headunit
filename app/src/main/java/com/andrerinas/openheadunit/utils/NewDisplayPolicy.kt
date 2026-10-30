package com.andrerinas.openheadunit.utils

/**
 * Decides when a live reading is the other display the user has just brought up, while the
 * "add a display" screen waits. Turning the device does not count: the sizes are compared in their
 * canonical order, so portrait and landscape are the same screen.
 */
object NewDisplayPolicy {

    data class Size(val width: Int, val height: Int)

    /** True when this panel is a different screen from the one the wait started on. */
    fun isNewDisplay(startWidth: Int, startHeight: Int, width: Int, height: Int): Boolean {
        val started = DensityScreenProfile.screen(startWidth, startHeight) ?: return false
        val live = DensityScreenProfile.screen(width, height) ?: return false
        return DensityScreenProfile.distance(started, live) > DensityScreenProfile.MATCH_TOLERANCE_PERMILLE
    }

    /**
     * The size the DPI screen should open for, or null while nothing has changed.
     *
     * A display that was not there before comes first: that is a monitor being plugged in. Otherwise
     * the window itself is the second display — a foldable that just opened, or the app running in a
     * resizable window, where the panel keeps its size but the space Android Auto is drawn into does
     * not. The projection reads the window at connect, so that is the size worth a DPI of its own.
     */
    fun pick(
        startWidth: Int,
        startHeight: Int,
        startWindowWidth: Int,
        startWindowHeight: Int,
        displays: List<Size>,
        window: Size
    ): Size? {
        displays.firstOrNull { isNewDisplay(startWidth, startHeight, it.width, it.height) }
            ?.let { return it }
        return window.takeIf {
            isNewDisplay(startWindowWidth, startWindowHeight, it.width, it.height)
        }
    }
}
