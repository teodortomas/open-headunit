package com.andrerinas.openheadunit.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    private fun pick(
        displays: List<Pair<Int, Int>>,
        window: Pair<Int, Int>,
        startPanel: Pair<Int, Int> = inner,
        startWindow: Pair<Int, Int> = inner
    ) = NewDisplayPolicy.pick(
        startPanel.first, startPanel.second,
        startWindow.first, startWindow.second,
        displays.map { NewDisplayPolicy.Size(it.first, it.second) },
        NewDisplayPolicy.Size(window.first, window.second)
    )

    @Test
    fun `nothing is picked while the screens stay the same`() {
        assertNull(pick(displays = listOf(inner), window = inner))
    }

    @Test
    fun `a monitor that appeared is picked over the window`() {
        val found = pick(displays = listOf(inner, 1920 to 1080), window = inner)
        assertEquals(NewDisplayPolicy.Size(1920, 1080), found)
    }

    @Test
    fun `a window that changed size is picked when no display appeared`() {
        // A foldable unfolding, or the app in a resizable window: the display list is unchanged.
        val found = pick(displays = listOf(inner), window = 1200 to 900)
        assertEquals(NewDisplayPolicy.Size(1200, 900), found)
    }

    @Test
    fun `a window squeezed by the system bars is not a second display`() {
        assertNull(pick(displays = listOf(inner), window = 1740 to 2080))
    }

    @Test
    fun `a wait that never learned its starting size picks nothing`() {
        assertNull(pick(displays = listOf(inner), window = inner, startPanel = 0 to 0, startWindow = 0 to 0))
    }
}
