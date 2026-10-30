package com.andrerinas.openheadunit.main

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.R
import com.google.android.material.card.MaterialCardView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Every way into the display list and the DPI screen, through the real navigation graph. Each of
 * these was a crash once: a destination without arguments threw on open, which sent the whole
 * settings screen back to the launcher.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class)
class DpiScreensNavigationTest {

    private lateinit var activity: FragmentActivity

    private fun navHost(): NavController {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java)
        activity = controller.get()
        activity.setTheme(R.style.AppTheme)
        controller.setup()
        val host = NavHostFragment.create(R.navigation.nav_graph)
        activity.supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, host)
            .setPrimaryNavigationFragment(host)
            .commitNow()
        return host.navController
    }

    private fun settings() = App.provide(activity).settings

    /** Lets the fragment lifecycle and the list building run before anything is read off it. */
    private fun settle() = shadowOf(Looper.getMainLooper()).idle()

    /** The screen the graph is showing, not the host holding it. */
    private fun currentFragment(): Fragment? =
        (activity.supportFragmentManager.primaryNavigationFragment as? NavHostFragment)
            ?.childFragmentManager?.fragments?.lastOrNull { it.view != null }

    private fun currentView(): View? = currentFragment()?.view

    /** The cards on the list screen, in the order they are shown. */
    private fun cards(): List<MaterialCardView> {
        settle()
        val content = currentView()?.findViewById<LinearLayout>(R.id.content) ?: return emptyList()
        val found = mutableListOf<MaterialCardView>()
        fun walk(view: View) {
            if (view is MaterialCardView) found.add(view)
            if (view is ViewGroup) for (i in 0 until view.childCount) walk(view.getChildAt(i))
        }
        walk(content)
        return found
    }

    @Test
    fun `the dpi screen opens without a display in the arguments`() {
        val nav = navHost()
        nav.navigate(R.id.settingsFragment)
        nav.navigate(R.id.action_settingsFragment_to_dpiSettingsFragment)
        settle()
        assertEquals(R.id.dpiSettingsFragment, nav.currentDestination?.id)
        assertNotNull(currentView())
    }

    @Test
    fun `the dpi screen opens for one display`() {
        val nav = navHost()
        nav.navigate(R.id.settingsFragment)
        nav.navigate(R.id.action_settingsFragment_to_dpiScreensFragment)
        settle()
        assertEquals(R.id.dpiScreensFragment, nav.currentDestination?.id)
        assertNotNull(currentView())
    }

    @Test
    fun `the list shows the shared value, one card per display, and the add row`() {
        val nav = navHost()
        settings().multiScreenDpi = true
        settings().dpiScreenProfiles = "1812x2176=420;968x2376=300"
        nav.navigate(R.id.dpiScreensFragment)
        // The shared value, one card per stored display, and the add row.
        assertEquals(4, cards().size)
    }

    @Test
    fun `a card opens the dpi screen for that display`() {
        val nav = navHost()
        settings().multiScreenDpi = true
        settings().dpiScreenProfiles = "1812x2176=420"
        nav.navigate(R.id.dpiScreensFragment)

        cards()[1].performClick()
        settle()

        assertEquals(R.id.dpiSettingsFragment, nav.currentDestination?.id)
        val editor = (activity.supportFragmentManager.primaryNavigationFragment as NavHostFragment)
            .childFragmentManager.fragments.filterIsInstance<DpiSettingsFragment>().single()
        assertEquals(1812, editor.arguments?.getInt(DpiSettingsFragment.ARG_WIDTH))
        assertEquals(2176, editor.arguments?.getInt(DpiSettingsFragment.ARG_HEIGHT))
    }

    @Test
    fun `a found display opens its dpi screen, and back returns to the list`() {
        val nav = navHost()
        settings().multiScreenDpi = true
        settings().dpiScreenProfiles = "1812x2176=420"
        nav.navigate(R.id.dpiScreensFragment)
        settle()

        // The wait starts on this panel, so nothing it can see yet counts as the other display.
        val panel = activity.resources.displayMetrics
        nav.navigate(
            R.id.dpiAddDisplayFragment,
            android.os.Bundle().apply {
                putInt(DpiAddDisplayFragment.ARG_WIDTH, panel.widthPixels)
                putInt(DpiAddDisplayFragment.ARG_HEIGHT, panel.heightPixels)
            }
        )
        settle()
        assertEquals(R.id.dpiAddDisplayFragment, nav.currentDestination?.id)

        val waiting = (activity.supportFragmentManager.primaryNavigationFragment as NavHostFragment)
            .childFragmentManager.fragments.filterIsInstance<DpiAddDisplayFragment>().single()
        waiting.openFoundDisplay(968, 2376)
        settle()

        assertEquals(R.id.dpiSettingsFragment, nav.currentDestination?.id)
        nav.popBackStack()
        settle()
        assertEquals(R.id.dpiScreensFragment, nav.currentDestination?.id)
    }

    @Test
    fun `the add row opens the waiting screen with the starting panel`() {
        val nav = navHost()
        settings().multiScreenDpi = true
        settings().dpiScreenProfiles = "1812x2176=420"
        nav.navigate(R.id.dpiScreensFragment)

        cards().last().performClick()
        settle()

        assertEquals(R.id.dpiAddDisplayFragment, nav.currentDestination?.id)
    }
}
