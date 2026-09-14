package depollsoft.tagmaster

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CompactLoadingBindingsTest {
    private class Owner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test fun every_compact_xml_slot_uses_shared_size_and_retains_space() {
        val controller = Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.AppTheme)
        controller.setup().visible()
        try {
            val activity = controller.get()
            val ids =
                mapOf(
                    R.layout.meviewheader to listOf(R.id.randomTagProgress),
                    R.layout.savedtagitemview to listOf(R.id.loadingBar),
                    R.layout.mediaplayerview to listOf(R.id.trackLoadingIndicator),
                    R.layout.sheetmusicview to listOf(R.id.sheetMusicLoading),
                    R.layout.tagsummaryview to listOf(R.id.ratingSubmitProgress, R.id.sheetMusicProgress),
                )
            for ((layout, loaders) in ids) {
                val root = LayoutInflater.from(activity).inflate(layout, FrameLayout(activity), false)
                for (id in loaders) {
                    val pole = root.findViewById<BarberPoleLoadingView>(id)
                    assertNotNull(pole.contentDescription)
                    assertEquals(activity.resources.getDimensionPixelSize(R.dimen.barberpole_compact_width), pole.layoutParams.width)
                    assertEquals(activity.resources.getDimensionPixelSize(R.dimen.barberpole_compact_height), pole.layoutParams.height)
                    pole.loading = true
                    pole.loading = false
                    assertEquals(View.INVISIBLE, pole.visibility)
                    assertFalse(pole.isAnimating)
                }
            }
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test fun compact_does_not_expand_with_parent_and_observes_view_tree_lifecycle() {
        val controller = Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.AppTheme)
        controller.setup().visible()
        try {
            val activity = controller.get()
            val owner = Owner()
            owner.registry.currentState = Lifecycle.State.RESUMED
            val root = FrameLayout(activity)
            root.setViewTreeLifecycleOwner(owner)
            val inflated = LayoutInflater.from(activity).inflate(R.layout.meviewheader, root, false)
            val pole = inflated.findViewById<BarberPoleLoadingView>(R.id.randomTagProgress)
            (pole.parent as android.view.ViewGroup).removeView(pole)
            root.addView(pole)
            activity.setContentView(root)
            root.layout(0, 0, 600, 600)
            pole.layout(0, 0, 300, 200) // Mis-sized host must not magnify compact art.
            pole.loading = true
            assertTrue(pole.hostResumed)
            assertTrue(pole.isAnimating)
            val image = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888)
            pole.drawPole(Canvas(image), 0f)
            var top = 200
            var bottom = 0
            for (y in 0 until 200) {
                for (x in 0 until 300) {
                    if (image.getPixel(x, y) ushr 24 >
                        0
                    ) {
                        top = minOf(top, y)
                        bottom = maxOf(bottom, y)
                    }
                }
            }
            assertTrue(
                bottom - top + 1 <= kotlin.math.ceil(BarberPoleLogo.COMPACT_HEIGHT * activity.resources.displayMetrics.density).toInt() + 1,
            )
            owner.registry.currentState = Lifecycle.State.STARTED
            assertFalse(pole.isAnimating)
            owner.registry.currentState = Lifecycle.State.RESUMED
            assertTrue(pole.isAnimating)
            root.visibility = View.INVISIBLE
            assertFalse(pole.isAnimating)
            root.visibility = View.VISIBLE
            pole.viewTreeObserver.dispatchOnPreDraw()
            assertTrue(pole.isAnimating)
            pole.translationY = 700f
            pole.viewTreeObserver.dispatchOnPreDraw()
            assertFalse(pole.isAnimating)
            pole.translationY = 0f
            val resolver = activity.contentResolver
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
            pole.viewTreeObserver.dispatchOnPreDraw()
            assertFalse(pole.isAnimating)
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            pole.viewTreeObserver.dispatchOnPreDraw()
            assertTrue(pole.isAnimating)
            root.removeView(pole)
            assertFalse(pole.isAnimating)
            pole.loading = false
            root.addView(pole)
            pole.viewTreeObserver.dispatchOnPreDraw()
            assertFalse(pole.isAnimating)
            assertFalse(pole.loading)
            image.recycle()
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
