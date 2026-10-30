package com.andrerinas.openheadunit.main

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.andrerinas.openheadunit.R
import com.andrerinas.openheadunit.utils.NewDisplayPolicy
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

/**
 * Waits for a display other than the one the wait started on, then opens the DPI screen for it.
 *
 * Two readings are watched, because the two ways a second screen arrives look different. A monitor
 * shows up in the display list at a size of its own; a foldable, or the app in a resizable window,
 * leaves the display list alone and only changes the size of the window. Both starting points come
 * in the arguments, so turning the device mid-wait — which recreates this screen — cannot move them.
 */
class DpiAddDisplayFragment : Fragment() {

    private val handler = Handler(Looper.getMainLooper())
    private var startWidth = 0
    private var startHeight = 0
    private var startWindowWidth = 0
    private var startWindowHeight = 0
    private var done = false

    private val poll = object : Runnable {
        override fun run() {
            if (!isAdded || done) return
            if (!lookForDisplay()) handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_dpi_add_display, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        view.findViewById<MaterialButton>(R.id.cancel).setOnClickListener {
            findNavController().navigateUp()
        }
        startWidth = requireArguments().getInt(ARG_WIDTH)
        startHeight = requireArguments().getInt(ARG_HEIGHT)
        startWindowWidth = requireArguments().getInt(ARG_WINDOW_WIDTH)
        startWindowHeight = requireArguments().getInt(ARG_WINDOW_HEIGHT)
    }

    override fun onResume() {
        super.onResume()
        handler.post(poll)
    }

    override fun onPause() {
        handler.removeCallbacks(poll)
        super.onPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // A fold or a resized window reaches the app this way, so there is no reason to wait for
        // the next tick of the poll once it has.
        if (!done) lookForDisplay()
    }

    /** True when a display was found and its DPI screen opened. */
    private fun lookForDisplay(): Boolean {
        val window = windowSize()
        showSizes(window)
        val found = NewDisplayPolicy.pick(
            startWidth,
            startHeight,
            startWindowWidth,
            startWindowHeight,
            panelSizes(),
            window
        ) ?: return false

        done = true
        handler.removeCallbacks(poll)
        openFoundDisplay(found.width, found.height)
        return true
    }

    /**
     * Opens the DPI screen for the display just found. The waiting screen is popped on the way, in
     * the same transaction, so Back from the editor returns to the list rather than here.
     */
    internal fun openFoundDisplay(width: Int, height: Int) {
        findNavController().navigate(
            R.id.dpiSettingsFragment,
            bundleOf(
                DpiSettingsFragment.ARG_WIDTH to width,
                DpiSettingsFragment.ARG_HEIGHT to height
            ),
            navOptions { popUpTo(R.id.dpiAddDisplayFragment) { inclusive = true } }
        )
    }

    /** The size of every display the device reports, its own included. */
    private fun panelSizes(): List<NewDisplayPolicy.Size> {
        val manager = requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        return manager.displays.mapNotNull { sizeOf(it) }
            .map { NewDisplayPolicy.Size(it.first, it.second) }
    }

    /** Shows what the wait is comparing against, so nothing about it has to be guessed. */
    private fun showSizes(window: NewDisplayPolicy.Size) {
        val text = view?.findViewById<TextView>(R.id.sizes) ?: return
        text.text = getString(
            R.string.dpi_add_sizes,
            startWidth, startHeight, window.width, window.height
        )
    }

    /** The space this app is drawn into: the whole panel fullscreen, part of it in a window. */
    private fun windowSize(): NewDisplayPolicy.Size {
        val metrics = resources.displayMetrics
        return NewDisplayPolicy.Size(metrics.widthPixels, metrics.heightPixels)
    }

    private fun sizeOf(display: Display): Pair<Int, Int>? {
        display.mode?.let { mode ->
            if (mode.physicalWidth > 0 && mode.physicalHeight > 0) {
                return mode.physicalWidth to mode.physicalHeight
            }
        }
        val point = Point()
        @Suppress("DEPRECATION")
        display.getRealSize(point)
        return if (point.x > 0 && point.y > 0) point.x to point.y else null
    }

    companion object {
        const val ARG_WIDTH = "startWidth"
        const val ARG_HEIGHT = "startHeight"
        const val ARG_WINDOW_WIDTH = "startWindowWidth"
        const val ARG_WINDOW_HEIGHT = "startWindowHeight"
        private const val POLL_MS = 600L
    }
}
