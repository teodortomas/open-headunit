package com.andrerinas.openheadunit.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewDisplayPolicyTest {

    // A Galaxy Z Fold's two screens.
    private val cover = 968 to 2376
    private val inner = 1812 to 2176

    @Test
    fun `the other half of a foldable is a new display`() {
        assertTrue(NewDisplayPolicy.isNewDisplay(cover.first, cover.second, inner.first, inner.second))
        assertTrue(NewDisplayPolicy.isNewDisplay(inner.first, inner.second, cover.first, cover.second))
    }

    @Test
    fun `the display the wait started on is not`() {
        assertFalse(NewDisplayPolicy.isNewDisplay(inner.first, inner.second, inner.first, inner.second))
    }

    @Test
    fun `turning the device does not make a new display`() {
        assertFalse(NewDisplayPolicy.isNewDisplay(inner.first, inner.second, inner.second, inner.first))
    }

    @Test
    fun `the same screen behind the system bars is not a new display`() {
        // The settings screen reads the panel, the connect path reads the window.
        assertFalse(NewDisplayPolicy.isNewDisplay(1812, 2176, 1740, 2080))
    }

    @Test
    fun `an unknown size is never a display`() {
        assertFalse(NewDisplayPolicy.isNewDisplay(0, 0, inner.first, inner.second))
        assertFalse(NewDisplayPolicy.isNewDisplay(inner.first, inner.second, 0, 0))
    }
}
