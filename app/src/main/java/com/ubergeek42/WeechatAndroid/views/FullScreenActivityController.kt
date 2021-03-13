package com.ubergeek42.WeechatAndroid.views

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.ubergeek42.WeechatAndroid.R
import com.ubergeek42.WeechatAndroid.WeechatActivity
import com.ubergeek42.WeechatAndroid.service.P
import com.ubergeek42.WeechatAndroid.upload.f
import com.ubergeek42.WeechatAndroid.utils.ThemeFix
import com.ubergeek42.WeechatAndroid.utils.WeaselMeasuringViewPager
import kotlin.math.sign


// this can technically work on earlier Android versions,
// e.g. on api 24 (7.0) it works perfectly in dark mode,
// but in light mode the status bar icons remain light
val FULL_SCREEN_DRAWER_ENABLED = Build.VERSION.SDK_INT >= 26    // 8.0, Oreo


object SystemWindowInsets {
    var top = 0
    var bottom = 0
}


fun interface InsetListener {
    fun onInsetsChanged()
}


private val insetListeners = mutableListOf<InsetListener>()


////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////// activity
////////////////////////////////////////////////////////////////////////////////////////////////////

class WeechatActivityFullScreenController(val activity: WeechatActivity) : DefaultLifecycleObserver {
    fun observeLifecycle() {
        activity.lifecycle.addObserver(this)
    }

    private lateinit var navigationPadding: View

    override fun onCreate(owner: LifecycleOwner) {
        if (!FULL_SCREEN_DRAWER_ENABLED) return

        val toolbarContainer = activity.findViewById<View>(R.id.toolbar_container)
        val viewPager = activity.findViewById<WeaselMeasuringViewPager>(R.id.main_viewpager)
        val rootView = viewPager.rootView

        navigationPadding = activity.findViewById(R.id.navigation_padding)
        navigationPadding.visibility = View.VISIBLE

        // todo use WindowCompat.setDecorFitsSystemWindows(window, false)
        // todo needs api 30+? and/or androidx.core:core-ktx:1.5.0-beta02
        rootView.systemUiVisibility = rootView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        rootView.setOnApplyWindowInsetsListener listener@{ _, insets ->
            SystemWindowInsets.top = insets.systemWindowInsetTop
            SystemWindowInsets.bottom = insets.systemWindowInsetBottom

            insetListeners.forEach { it.onInsetsChanged() }

            insets
        }

        val weechatActivityInsetsListener = InsetListener {
            toolbarContainer.updatePadding(top = SystemWindowInsets.top)
            navigationPadding.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                height = SystemWindowInsets.bottom }
            viewPager.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = SystemWindowInsets.bottom }
        }

        insetListeners.add(weechatActivityInsetsListener)
    }

    // status bar can be colored since api 21 and have dark icons since api 23
    // navigation bar can be colored since api 21 and can have dark icons since api 26 via
    // SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR, which the theming engine seems to be setting
    // automatically, and since api 27 via android:navigationBarColor
    override fun onStart(owner: LifecycleOwner) {
        if (FULL_SCREEN_DRAWER_ENABLED) {
            navigationPadding.setBackgroundColor(P.colorPrimaryDark)
        } else {

            val systemAreaBackgroundColorIsDark = !ThemeFix.isColorLight(P.colorPrimaryDark)
            val statusBarIconCanBeDark = Build.VERSION.SDK_INT >= 23
            val navigationBarIconsCanBeDark = Build.VERSION.SDK_INT >= 26

            if (systemAreaBackgroundColorIsDark || statusBarIconCanBeDark)
                activity.window.statusBarColor = P.colorPrimaryDark

            if (systemAreaBackgroundColorIsDark || navigationBarIconsCanBeDark)
                activity.window.navigationBarColor = P.colorPrimaryDark
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        insetListeners.clear()
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////// buffer list
////////////////////////////////////////////////////////////////////////////////////////////////////

class BufferListFragmentFullScreenController(val fragment: Fragment) : DefaultLifecycleObserver {
    fun observeLifecycle() {
        fragment.lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (!FULL_SCREEN_DRAWER_ENABLED) return

        val bufferListView = fragment.requireView()
        val navigationPadding = bufferListView.findViewById<View>(R.id.navigation_padding)
        val layoutManager = bufferListView.findViewById<RecyclerView>(R.id.recycler)
                .layoutManager as FullScreenDrawerLinearLayoutManager

        navigationPadding.setBackgroundColor(P.colorPrimaryDark)

        val filterBar = bufferListView.findViewById<View>(R.id.filter_bar)

        fun applyInsets() {
            if (P.showBufferFilter) {
                navigationPadding.visibility = View.VISIBLE
                navigationPadding.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    height = SystemWindowInsets.bottom }
                filterBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = SystemWindowInsets.bottom }
                layoutManager.setInsets(SystemWindowInsets.top, 0)
            } else {
                navigationPadding.visibility = View.GONE
                layoutManager.setInsets(SystemWindowInsets.top, SystemWindowInsets.bottom)
            }
        }

        insetListeners.add(InsetListener { applyInsets() })

        applyInsets()
    }
}


////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////// buffer fragment
////////////////////////////////////////////////////////////////////////////////////////////////////


class BufferFragmentFullScreenController(val fragment: Fragment) : DefaultLifecycleObserver {
    fun observeLifecycle() {
        fragment.lifecycle.addObserver(this)
    }

    private var uiLines: RecyclerView? = null

    override fun onStart(owner: LifecycleOwner) {
        if (!FULL_SCREEN_DRAWER_ENABLED) return
        uiLines = fragment.requireView().findViewById(R.id.chatview_lines)
        insetListeners.add(insetListener)
        insetListener.onInsetsChanged()
    }

    override fun onStop(owner: LifecycleOwner) {
        if (!FULL_SCREEN_DRAWER_ENABLED) return
        insetListeners.remove(insetListener)
        uiLines = null
    }

    private var uiLinesPaddingTop = 0
    private val insetListener = InsetListener {
        val desiredPadding = if (P.autoHideActionbar) SystemWindowInsets.top else 0

        if (uiLinesPaddingTop != desiredPadding) {
            uiLinesPaddingTop = desiredPadding
            uiLines?.updatePadding(top = uiLinesPaddingTop)
        }
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////// height observer
////////////////////////////////////////////////////////////////////////////////////////////////////


fun interface SystemAreaHeightObserver {
    fun onSystemAreaHeightChanged(systemAreaHeight: Int)
}


private abstract class SystemAreaHeightExaminer(
        val activity: AppCompatActivity,
) : DefaultLifecycleObserver {
    fun observeLifecycle() {
        activity.lifecycle.addObserver(this)
    }

    var observer: SystemAreaHeightObserver? = null

    companion object {
        @JvmStatic fun obtain(activity: AppCompatActivity) = if (FULL_SCREEN_DRAWER_ENABLED)
            NewSystemAreaHeightExaminer(activity) else OldSystemAreaHeightExaminer(activity)
    }
}


private class OldSystemAreaHeightExaminer(
        activity: AppCompatActivity,
) : SystemAreaHeightExaminer(activity) {
    private lateinit var content: View
    private lateinit var rootView: View

    override fun onCreate(owner: LifecycleOwner) {
        content = activity.findViewById(android.R.id.content)
        rootView = content.rootView
        content.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        content.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }

    // windowHeight is the height of the activity that includes the height of the status bar and the
    // navigation bar. if the activity is split, this height seems to be only including the system
    // bar that the activity is “touching”. this height doesn't include the keyboard height per se,
    // but if the activity changes size due to the keyboard, this number remains the same.
    // activityHeight is the height of the activity not including any of the system stuff.
    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener listener@{
        // on android 7, if changing the day/night theme in settings, the activity can be recreated
        // right away but with a wrong window height. so we wait until it's actually resumed
        if (activity.lifecycle.currentState != Lifecycle.State.RESUMED) return@listener

        val windowHeight = rootView.height
        val activityHeight = content.height
        val systemAreaHeight = windowHeight - activityHeight

        // weed out some insanity that's happening when the window is in split screen mode.
        // it seems that while resizing some elements can temporarily have the height 0.
        if (windowHeight <= 0 || activityHeight <= 0 || systemAreaHeight <= 0) return@listener

        observer?.onSystemAreaHeightChanged(systemAreaHeight)
    }
}


private class NewSystemAreaHeightExaminer(
        activity: AppCompatActivity,
) : SystemAreaHeightExaminer(activity) {
    override fun onCreate(owner: LifecycleOwner) {
        insetListeners.add(insetListener)
    }

    private val insetListener = InsetListener {
        observer?.onSystemAreaHeightChanged(SystemWindowInsets.bottom)
    }
}


////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////// toolbar controller
////////////////////////////////////////////////////////////////////////////////////////////////////


class ToolbarController(val activity: WeechatActivity) : DefaultLifecycleObserver, SystemAreaHeightObserver {
    fun observeLifecycle() {
        activity.lifecycle.addObserver(this)
        SystemAreaHeightExaminer.obtain(activity).also { it.observer = this }.observeLifecycle()
    }

    private lateinit var toolbarContainer: View
    private lateinit var uiPager: View

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        toolbarContainer = activity.findViewById(R.id.toolbar_container)
        uiPager = activity.findViewById(R.id.main_viewpager)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        autoHideEnabled = P.autoHideActionbar
    }

    private var autoHideEnabled = true
        set(enabled) {
            if (field != enabled) {
                field = enabled
                toolbarContainer.post {
                    uiPager.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        topMargin = if (enabled) 0 else toolbarContainer.height
                    }
                }
            }
        }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private var toolbarShown = true
    private var keyboardVisible = false

    private fun show() {
        if (!toolbarShown) {
            toolbarShown = true
            toolbarContainer.animate()
                    .translationY(0f)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
        }
    }

    private fun hide() {
        if (!autoHideEnabled) return

        if (toolbarShown) {
            toolbarShown = false
            toolbarContainer.animate()
                    .translationY(-toolbarContainer.bottom.f)
                    .setInterpolator(AccelerateInterpolator())
                    .start()
        }

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private var cumDy = 0

    fun onChatLinesScrolled(dy: Int, touchingTop: Boolean, touchingBottom: Boolean) {
        if (!autoHideEnabled || keyboardVisible || dy == 0) return

        if (cumDy.sign != dy.sign) cumDy = 0
        cumDy += dy

        if (cumDy < -hideToolbarScrollThreshold || cumDy < 0 && touchingTop) hide()
        if (cumDy > showToolbarScrollThreshold || cumDy > 0 && touchingBottom) show()
    }

    fun onPageChangedOrSelected() {
        cumDy = 0
        show()
    }

    private fun onSoftwareKeyboardStateChanged(visible: Boolean) {
        if (keyboardVisible != visible) {
            keyboardVisible = visible

            if (autoHideEnabled && activity.isChatInputOrSearchInputFocused) {
                if (visible) hide() else show()
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private var initialSystemAreaHeight = -1

    override fun onSystemAreaHeightChanged(systemAreaHeight: Int) {
        // note the initial system area (assuming keyboard closed) and return. we should be getting
        // a few more calls to this method without any changes to the height numbers

        // note the initial system area (assuming keyboard closed) and return. we should be getting
        // a few more calls to this method without any changes to the height numbers
        if (initialSystemAreaHeight == -1) {
            initialSystemAreaHeight = systemAreaHeight
            return
        }

        // weed out some insanity that's happening when the window is in split screen mode. it seems
        // that while resizing some elements can temporarily have the height 0.
        if (systemAreaHeight < initialSystemAreaHeight) return

        val keyboardVisible = systemAreaHeight - initialSystemAreaHeight >
                sensibleMinimumSoftwareKeyboardHeight

        onSoftwareKeyboardStateChanged(keyboardVisible)
    }
}


private val sensibleMinimumSoftwareKeyboardHeight = 50 * P._1dp
private val hideToolbarScrollThreshold = P._200dp
private val showToolbarScrollThreshold = P._200dp