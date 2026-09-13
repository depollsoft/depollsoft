package depollsoft.tagmaster

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
class BarberPoleLoadingViewTest {
    @Test fun stripes_move_but_caps_and_frame_do_not() =
        withPole { pole ->
            fun phase(value: Float) = Bitmap.createBitmap(56, 104, Bitmap.Config.ARGB_8888).also { pole.drawPole(Canvas(it), value) }
            val first = phase(0f)
            val next = phase(0.25f)
            var changes = 0
            for (y in 0 until 104) {
                for (x in 0 until 56) {
                    if (y < 20 || y >= 84 || x < 10 || x >= 46) {
                        assertEquals(first.getPixel(x, y), next.getPixel(x, y))
                    } else if (first.getPixel(x, y) != next.getPixel(x, y)) {
                        changes++
                    }
                }
            }
            assertTrue("Stripes change inside cylinder", changes > 500)
            first.recycle()
            next.recycle()
        }

    @Test fun loading_host_visibility_detach_and_remove_animations_gate_loop() =
        withPole { pole ->
            assertFalse(pole.isAnimating)
            pole.loading = true
            pole.hostResumed = true
            assertTrue(pole.isAnimating)
            pole.hostResumed = false
            assertFalse(pole.isAnimating)
            pole.hostResumed = true
            assertTrue(pole.isAnimating)
            pole.visibility = View.GONE
            assertFalse(pole.isAnimating)
            pole.visibility = View.VISIBLE
            assertTrue(pole.isAnimating)
            pole.loading = false
            assertFalse(pole.isAnimating)
            val resolver = pole.context.contentResolver
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
            pole.loading = true
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse(pole.isAnimating)
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            resolver.notifyChange(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), null)
            // Deliver the setting observer without draining an intentionally recurring frame callback.
            shadowOf(Looper.getMainLooper()).runOneTask()
            pole.viewTreeObserver.dispatchOnPreDraw() // Geometry becomes valid after window layout.
            assertTrue(
                "Live setting: shown=${pole.isShown} size=${pole.width}x${pole.height} attached=${pole.isAttachedToWindow} bounds=${pole.getGlobalVisibleRect(
                    android.graphics.Rect(),
                )}",
                pole.isAnimating,
            )
            (pole.parent as ViewGroup).removeView(pole)
            assertFalse(pole.isAnimating)
        }

    @Test fun xml_loading_setter_and_accessibility_contract_exist() =
        withPole { pole ->
            val root = LayoutInflater.from(pole.context).inflate(R.layout.tagqueryview, null)
            val loader = root.findViewById<BarberPoleLoadingView>(R.id.loadingProgressBar)
            assertEquals("Loading tags", loader.contentDescription)
            assertEquals(View.GONE, loader.visibility)
            loader.javaClass.getMethod("setLoading", Boolean::class.javaPrimitiveType).invoke(loader, true)
            assertTrue(loader.loading)
            assertFalse(loader.isAnimating)
            assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_YES, loader.importantForAccessibility)
        }

    @Test fun query_loader_stays_inside_parent_when_visibility_changes() =
        withPole { pole ->
            val root = LayoutInflater.from(pole.context).inflate(R.layout.tagqueryview, null) as ViewGroup
            val loader = root.findViewById<BarberPoleLoadingView>(R.id.loadingProgressBar)
            for (visible in listOf(false, true, false, true)) {
                loader.visibility = if (visible) View.VISIBLE else View.GONE
                root.measure(
                    View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY),
                )
                root.layout(0, 0, 360, 640)
                if (visible) {
                    assertTrue(loader.top >= 0)
                    assertTrue(loader.bottom <= root.height)
                    assertTrue(root.findViewById<View>(R.id.queryResultListView).bottom <= loader.top)
                }
            }
        }

    private fun withPole(block: (BarberPoleLoadingView) -> Unit) {
        val controller = Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.AppTheme)
        controller.setup().visible()
        val pole = BarberPoleLoadingView(controller.get())
        controller.get().setContentView(pole)
        pole.layout(0, 0, 56, 104)
        try {
            block(pole)
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
