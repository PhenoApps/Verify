package org.phenoapps.verify.utilities

import android.util.TypedValue
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.bottomnavigation.BottomNavigationView

object InsetHandler {

    private val systemBarOrDisplayCutout =
        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()

    /**
     * Standard inset handling for activities with a top toolbar.
     * Applies top padding to the toolbar and bottom padding to the root view.
     */
    @JvmOverloads
    fun setupStandardInsets(rootView: View, toolbar: Toolbar? = null) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(systemBarOrDisplayCutout)

            toolbar?.updatePadding(top = systemBars.top)

            v.updatePadding(bottom = systemBars.bottom)

            insets
        }

        ViewCompat.requestApplyInsets(rootView)
    }

    /**
     * Handles insets for a view with only top insets (e.g. fragments inside
     * an activity that already handles bottom insets).
     * Consumes bottom insets to prevent double-spacing.
     */
    @JvmOverloads
    fun setupFragmentWithTopInsetsOnly(rootView: View, toolbar: Toolbar? = null) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBars = insets.getInsets(systemBarOrDisplayCutout)

            toolbar?.updatePadding(top = systemBars.top)

            WindowInsetsCompat.Builder(insets)
                .setInsets(
                    WindowInsetsCompat.Type.systemBars(),
                    androidx.core.graphics.Insets.of(
                        systemBars.left, systemBars.top, systemBars.right, 0
                    )
                )
                .build()
        }

        ViewCompat.requestApplyInsets(rootView)
    }

    /**
     * Handles insets for the main activity layout that has a top toolbar and a
     * BottomNavigationView. The bottom nav extends behind the gesture/button
     * navigation area so there is no gap below it.
     */
    @JvmOverloads
    fun setupInsetsWithBottomNav(
        rootView: View,
        toolbar: Toolbar? = null,
        bottomNav: BottomNavigationView
    ) {
        val typedValue = TypedValue()
        rootView.context.theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)
        val actionBarPx = TypedValue.complexToDimensionPixelSize(
            typedValue.data,
            rootView.resources.displayMetrics
        )

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBars = insets.getInsets(systemBarOrDisplayCutout)

            toolbar?.updatePadding(top = systemBars.top)

            // Expand the bottom nav height so it draws under the system nav bar
            val desiredHeight = actionBarPx + systemBars.bottom
            bottomNav.updateLayoutParams {
                height = desiredHeight
            }

            // Pad the bottom nav content upward so icons remain above the system nav bar
            bottomNav.updatePadding(bottom = systemBars.bottom)

            insets
        }

        ViewCompat.requestApplyInsets(rootView)
    }
}
