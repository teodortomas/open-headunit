package com.andrerinas.openheadunit.main

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.andrerinas.openheadunit.R
import com.andrerinas.openheadunit.utils.NewDisplayPolicy
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

/**
 * Waits for a display other than the one the wait started on, then opens the DPI screen for it.
 * The started size comes in the arguments, so turning or folding the device mid-wait — which
 * recreates the screen — cannot move the mark. Polling covers both a foldable changing the size of
 * the display it already has and a monitor appearing as a new one.
 */
class DpiAddDisplayFragment : Fragment() {

    private val handler = Handler(Looper.getMainLooper())
    private var startWidth = 0
    private var startHeight = 0
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
    }

    override fun onResume() {
        super.onResume()
        handler.post(poll)
    }

    override fun onPause() {
        handler.removeCallbacks(poll)
        super.onPause()
    }

    /** True when a display was found and its DPI screen opened. */
    private fun lookForDisplay(): Boolean {
        val sizes = mutableListOf<Pair<Int, Int>>()
        val manager = requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        manager.displays.forEach { display -> sizeOf(display)?.let { sizes.add(it) } }
        sizes.add(currentPanel())

        val found = sizes.firstOrNull {
            NewDisplayPolicy.isNewDisplay(startWidth, startHeight, it.first, it.second)
        } ?: return false

        done = true
        handler.removeCallbacks(poll)
        val nav = findNavController()
        nav.navigate(
            R.id.dpiSettingsFragment,
            bundleOf(
                DpiSettingsFragment.ARG_WIDTH to found.first,
                DpiSettingsFragment.ARG_HEIGHT to found.second
            )
        )
        // Drops this waiting screen, so Back returns to the list and not here.
        nav.popBackStack(R.id.dpiAddDisplayFragment, true)
        return true
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

    private fun currentPanel(): Pair<Int, Int> {
        val metrics = DisplayMetrics()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requireContext().display?.getRealMetrics(metrics)
            } else {
                @Suppress("DEPRECATION")
                (requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                    .defaultDisplay.getRealMetrics(metrics)
            }
        } catch (_: Exception) {
        }
        return metrics.widthPixels to metrics.heightPixels
    }

    companion object {
        const val ARG_WIDTH = "startWidth"
        const val ARG_HEIGHT = "startHeight"
        private const val POLL_MS = 600L
    }
}
