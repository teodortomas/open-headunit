package com.andrerinas.openheadunit.utils

/**
 * A stored DPI per screen size, so a device that has two screens of its own keeps one density for
 * each instead of one for both. A foldable is the case that matters: the cover and the inner
 * display are the same phone at two very different pixel sizes, and one density cannot suit both.
 *
 * Nothing here asks the platform whether the device is folded. The projection already reads the
 * panel it is about to draw into, and that reading alone tells the two states apart, so the
 * density can be keyed to it: the screen the user set a value on is the screen that gets it.
 */
object DensityScreenProfile {

    /** A panel size in its canonical orientation, so turning the device finds the same entry. */
    data class Screen(val width: Int, val height: Int)

    data class Entry(val screen: Screen, val dpi: Int)

    /**
     * How far a live panel reading may sit from a stored one and still count as the same screen.
     * The two readings come from different APIs — the full panel in the settings screen, the
     * window's own metrics at connect — so the bars between them must not cost the match, while
     * the smallest gap between two real screens of one device (a foldable's halves, hundreds of
     * pixels apart) stays far outside it.
     */
    const val MATCH_TOLERANCE_PERMILLE = 120

    private const val SEPARATOR = ';'
    private const val PAIR = '='
    private const val TIMES = 'x'

    /** The size in the canonical order, or null when either side is unknown. */
    fun screen(width: Int, height: Int): Screen? {
        if (width <= 0 || height <= 0) return null
        return if (width <= height) Screen(width, height) else Screen(height, width)
    }

    /** Reads the stored string; anything unreadable is dropped rather than costing the rest. */
    fun parse(raw: String?): List<Entry> {
        if (raw.isNullOrBlank()) return emptyList()
        val byScreen = LinkedHashMap<Screen, Int>()
        raw.split(SEPARATOR).forEach { token ->
            val pair = token.trim().split(PAIR)
            if (pair.size != 2) return@forEach
            val dims = pair[0].trim().split(TIMES)
            if (dims.size != 2) return@forEach
            val width = dims[0].trim().toIntOrNull() ?: return@forEach
            val height = dims[1].trim().toIntOrNull() ?: return@forEach
            val dpi = pair[1].trim().toIntOrNull() ?: return@forEach
            val screen = screen(width, height) ?: return@forEach
            if (dpi <= 0) return@forEach
            byScreen[screen] = dpi
        }
        return byScreen.map { Entry(it.key, it.value) }
    }

    /** The stored string; the inverse of [parse]. */
    fun format(entries: List<Entry>): String = entries.joinToString(SEPARATOR.toString()) { entry ->
        "${entry.screen.width}$TIMES${entry.screen.height}$PAIR${entry.dpi}"
    }

    /** The entry for a screen this size: an exact reading first, then the nearest close enough. */
    fun find(entries: List<Entry>, width: Int, height: Int): Entry? {
        val panel = screen(width, height) ?: return null
        entries.firstOrNull { it.screen == panel }?.let { return it }
        val nearest = entries.minByOrNull { distance(it.screen, panel) } ?: return null
        return if (distance(nearest.screen, panel) <= MATCH_TOLERANCE_PERMILLE) nearest else null
    }

    /** The DPI stored for a screen this size, or null when it has none of its own. */
    fun select(entries: List<Entry>, width: Int, height: Int): Int? =
        find(entries, width, height)?.dpi

    /** Writes a DPI for this screen; 0 or less removes it, which leaves that screen on Automatic. */
    fun put(entries: List<Entry>, width: Int, height: Int, dpi: Int): List<Entry> {
        val panel = screen(width, height) ?: return entries
        val kept = mutableListOf<Entry>()
        var written = false
        entries.forEach { entry ->
            if (distance(entry.screen, panel) > MATCH_TOLERANCE_PERMILLE) {
                kept.add(entry)
            } else if (!written && dpi > 0) {
                // In place, so saving does not reorder the other screens' entries.
                kept.add(Entry(panel, dpi))
                written = true
            }
        }
        if (!written && dpi > 0) kept.add(Entry(panel, dpi))
        return kept
    }

    /** The largest relative difference of the two dimensions, in thousandths. */
    fun distance(a: Screen, b: Screen): Int = maxOf(relative(a.width, b.width), relative(a.height, b.height))

    private fun relative(a: Int, b: Int): Int {
        val largest = maxOf(a, b)
        if (largest <= 0) return 0
        return Math.abs(a - b) * 1000 / largest
    }
}
