package com.andrerinas.openheadunit.main

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.R
import com.andrerinas.openheadunit.utils.DensityScreenProfile
import com.google.android.material.appbar.MaterialToolbar

/**
 * The DPI screen for a device with more than one display: one card per screen size, plus the shared
 * value a display without its own falls back to. Only reachable while the device is marked as
 * having more than one display (Device-Specific settings).
 */
class DpiScreensFragment : Fragment() {

    private val settings by lazy { App.provide(requireContext()).settings }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_dpi_screens, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            if (!findNavController().navigateUp()) requireActivity().finish()
        }
    }

    // Rebuilt on every return, so a value just saved is the one the card shows.
    override fun onResume() {
        super.onResume()
        view?.findViewById<LinearLayout>(R.id.content)?.let { fill(it) }
    }

    private fun fill(content: LinearLayout) {
        content.removeAllViews()
        val (panelWidth, panelHeight) = currentPanel()
        val entries = DensityScreenProfile.parse(settings.dpiScreenProfiles)

        content.addView(card(
            title = getString(R.string.dpi_screens_shared),
            value = if (settings.dpiPixelDensity > 0) {
                getString(R.string.dpi_screens_shared_value, settings.dpiPixelDensity)
            } else {
                getString(R.string.dpi_automatic_desc)
            },
            onClick = { open(null) }
        ))

        content.addView(sectionTitle(getString(R.string.dpi_screens_section)))

        entries.forEach { entry ->
            content.addView(card(
                title = getString(R.string.dpi_screens_size, entry.screen.width, entry.screen.height),
                value = getString(R.string.dpi_screens_row_value, entry.dpi),
                preview = entry.screen.width to entry.screen.height,
                isCurrent = DensityScreenProfile.find(listOf(entry), panelWidth, panelHeight) != null,
                onClick = { open(entry.screen.width to entry.screen.height) }
            ))
        }

        content.addView(card(
            title = getString(R.string.dpi_screens_add),
            value = null,
            onClick = {
                findNavController().navigate(
                    R.id.dpiAddDisplayFragment,
                    bundleOf(
                        DpiAddDisplayFragment.ARG_WIDTH to panelWidth,
                        DpiAddDisplayFragment.ARG_HEIGHT to panelHeight,
                        // The wait also watches the window, which is what changes when the app is
                        // running in a window rather than fullscreen.
                        DpiAddDisplayFragment.ARG_WINDOW_WIDTH to resources.displayMetrics.widthPixels,
                        DpiAddDisplayFragment.ARG_WINDOW_HEIGHT to resources.displayMetrics.heightPixels
                    )
                )
            }
        ))

        content.addView(note(getString(R.string.dpi_screens_note)))
    }

    /** Opens the DPI screen for one display; null edits the shared value. */
    private fun open(screen: Pair<Int, Int>?) {
        val args = screen?.let {
            bundleOf(
                DpiSettingsFragment.ARG_WIDTH to it.first,
                DpiSettingsFragment.ARG_HEIGHT to it.second
            )
        }
        findNavController().navigate(R.id.dpiSettingsFragment, args)
    }

    private fun card(
        title: String,
        value: String?,
        preview: Pair<Int, Int>? = null,
        isCurrent: Boolean = false,
        onClick: () -> Unit
    ): View {
        val view = layoutInflater.inflate(R.layout.item_dpi_display, null, false)
        view.setOnClickListener { onClick() }
        view.findViewById<TextView>(R.id.displayTitle).text = title

        val valueView = view.findViewById<TextView>(R.id.displayValue)
        valueView.text = value
        valueView.visibility = if (value == null) View.GONE else View.VISIBLE

        view.findViewById<TextView>(R.id.displayBadge).visibility =
            if (isCurrent) View.VISIBLE else View.GONE

        val rect = view.findViewById<View>(R.id.previewRect)
        if (preview == null) {
            // Kept in place so every card's text starts at the same edge.
            rect.visibility = View.INVISIBLE
        } else {
            sizePreview(rect, preview.first, preview.second)
        }
        return view
    }

    /** Draws the panel's proportions inside the 40dp box. */
    private fun sizePreview(rect: View, width: Int, height: Int) {
        val longest = maxOf(width, height)
        if (longest <= 0) return
        val scale = PREVIEW_SIZE_DP / longest.toFloat()
        rect.layoutParams = rect.layoutParams.apply {
            this.width = (width * scale).toInt().coerceAtLeast(6)
            this.height = (height * scale).toInt().coerceAtLeast(6)
        }
    }

    private fun sectionTitle(label: String): View = TextView(requireContext()).apply {
        text = label
        setTextColor(ContextCompat.getColor(requireContext(), R.color.text_second))
        textSize = 12f
        setPadding(4.dp, 8.dp, 4.dp, 8.dp)
    }

    private fun note(label: String): View = TextView(requireContext()).apply {
        text = label
        setTextColor(ContextCompat.getColor(requireContext(), R.color.text_second))
        textSize = 12f
        setPadding(4.dp, 12.dp, 4.dp, 0)
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

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
        private const val PREVIEW_SIZE_DP = 40f
    }
}
