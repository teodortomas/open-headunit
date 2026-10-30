package com.andrerinas.openheadunit.utils

import com.andrerinas.openheadunit.utils.DensityScreenProfile.Entry
import com.andrerinas.openheadunit.utils.DensityScreenProfile.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DensityScreenProfileTest {

    // A Galaxy Z Fold's two screens, the case the per-screen DPI exists for.
    private val cover = 968 to 2376
    private val inner = 1812 to 2176

    private fun entries(vararg pairs: Pair<Pair<Int, Int>, Int>): List<Entry> =
        pairs.map { (screen, dpi) -> Entry(DensityScreenProfile.screen(screen.first, screen.second)!!, dpi) }

    @Test
    fun `a stored string reads back unchanged`() {
        val stored = entries(inner to 420, cover to 300)
        val raw = DensityScreenProfile.format(stored)
        assertEquals("1812x2176=420;968x2376=300", raw)
        assertEquals(stored, DensityScreenProfile.parse(raw))
    }

    @Test
    fun `an unreadable entry does not cost the others`() {
        val parsed = DensityScreenProfile.parse("1812x2176=420;garbage;0x0=200;968x2376=0;968x2376=300")
        assertEquals(entries(inner to 420, cover to 300), parsed)
    }

    @Test
    fun `an empty or missing string stores nothing`() {
        assertEquals(emptyList<Entry>(), DensityScreenProfile.parse(null))
        assertEquals(emptyList<Entry>(), DensityScreenProfile.parse(""))
        assertEquals(emptyList<Entry>(), DensityScreenProfile.parse("   "))
    }

    @Test
    fun `each screen of a foldable gets its own value`() {
        val stored = entries(inner to 420, cover to 300)
        assertEquals(420, DensityScreenProfile.select(stored, inner.first, inner.second))
        assertEquals(300, DensityScreenProfile.select(stored, cover.first, cover.second))
    }

    @Test
    fun `a stored value is found whichever way the screen is turned`() {
        val stored = entries(inner to 420)
        assertEquals(420, DensityScreenProfile.select(stored, inner.second, inner.first))
    }

    @Test
    fun `the system bars between two readings of one screen still match`() {
        // The settings screen reads the whole panel; the projection reads its window. The bars
        // come off one of them, so the same screen must still find its value.
        val stored = entries(inner to 420)
        assertEquals(420, DensityScreenProfile.select(stored, inner.first - 60, inner.second - 96))
    }

    @Test
    fun `a screen never set says so instead of borrowing the other's value`() {
        val onlyInner = entries(inner to 420)
        assertNull(DensityScreenProfile.select(onlyInner, cover.first, cover.second))
        assertNull(DensityScreenProfile.select(emptyList(), inner.first, inner.second))
        assertNull(DensityScreenProfile.select(onlyInner, 0, 0))
    }

    @Test
    fun `a screen already stored is replaced in place`() {
        val stored = entries(inner to 420, cover to 300)
        val updated = DensityScreenProfile.put(stored, inner.first, inner.second, 480)
        assertEquals(entries(inner to 480, cover to 300), updated)
    }

    @Test
    fun `a new screen is appended, and turning it does not add a second entry`() {
        val once = DensityScreenProfile.put(entries(inner to 420), cover.first, cover.second, 300)
        assertEquals(entries(inner to 420, cover to 300), once)
        val turned = DensityScreenProfile.put(once, cover.second, cover.first, 320)
        assertEquals(entries(inner to 420, cover to 320), turned)
    }

    @Test
    fun `automatic removes the screen's value instead of storing a zero`() {
        val stored = entries(inner to 420, cover to 300)
        assertEquals(entries(inner to 420), DensityScreenProfile.put(stored, cover.first, cover.second, 0))
        assertEquals(emptyList<Entry>(), DensityScreenProfile.put(entries(cover to 300), cover.first, cover.second, 0))
    }

    @Test
    fun `an unknown screen keeps what was stored`() {
        val stored = entries(inner to 420)
        assertEquals(stored, DensityScreenProfile.put(stored, 0, 0, 480))
    }

    @Test
    fun `the two screens of a fold are far apart, not a tolerance question`() {
        val a = DensityScreenProfile.screen(inner.first, inner.second)!!
        val b = DensityScreenProfile.screen(cover.first, cover.second)!!
        assertEquals(0, DensityScreenProfile.distance(a, a))
        assertTrue(DensityScreenProfile.distance(a, b) > DensityScreenProfile.MATCH_TOLERANCE_PERMILLE)
    }
}
