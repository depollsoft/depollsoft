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
    @Test fun stripes_move_but_logo_finials_collars_and_alpha_do_not() =
        withPole { pole ->
            for (size in listOf(68 to 116, 340 to 580, 400 to 580)) {
                pole.layout(0, 0, size.first, size.second)
                val mask = shaftMask(pole)
                val first = render(pole, 0f)
                for (phase in listOf(0.001f, 0.25f, 0.5f, 0.999f, 1f)) {
                    val next = render(pole, phase)
                    var changes = 0
                    val colors = mutableSetOf<Int>()
                    for (y in 0 until first.height) {
                        for (x in 0 until first.width) {
                            val a = first.getPixel(x, y)
                            val b = next.getPixel(x, y)
                            assertEquals("Silhouette alpha at $phase", android.graphics.Color.alpha(a), android.graphics.Color.alpha(b))
                            if (mask.getPixel(x, y) == 0) assertEquals("Stationary metal at $x,$y phase $phase", a, b)
                            if (a != b) changes++
                            colors.add(b)
                        }
                    }
                    assertTrue(colors.contains(android.graphics.Color.WHITE))
                    assertTrue(colors.contains(android.graphics.Color.rgb(190, 42, 53)))
                    assertTrue(colors.contains(android.graphics.Color.rgb(0, 99, 165)))
                    when (phase) {
                        1f -> assertEquals("Exact seamless loop endpoint", 0, changes)
                        0.25f, 0.5f -> assertTrue("Axial stripe movement", changes > first.width * first.height / 20)
                        else -> assertTrue("Continuous wrap, not a jump", changes < first.width * first.height / 20)
                    }
                    next.recycle()
                }
                first.recycle()
                mask.recycle()
            }
        }

    @Test fun canonical_diagonal_silhouette_round_balls_and_highlights_are_preserved() =
        withPole { pole ->
            pole.layout(0, 0, 300, 514)
            val source =
                pole.resources.getXml(R.drawable.ic_barberpole).use { xml ->
                    while (xml.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                        if (xml.name == "path") break
                    }
                    androidx.core.graphics.PathParser.createPathFromPathData(
                        xml.getAttributeValue("http://schemas.android.com/apk/res/android", "pathData"),
                    )!!
                }
            val reference = Bitmap.createBitmap(300, 514, Bitmap.Config.ARGB_8888)
            val paint =
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = pole.context.getColor(R.color.md_on_surface_variant)
                }
            val referenceCanvas = Canvas(reference)
            val scale = minOf(300f / BarberPoleLogo.WIDTH, 514f / BarberPoleLogo.HEIGHT)
            referenceCanvas.translate((300 - BarberPoleLogo.WIDTH * scale) / 2f, (514 - BarberPoleLogo.HEIGHT * scale) / 2f)
            referenceCanvas.scale(scale, scale)
            referenceCanvas.drawPath(source, paint)
            val actual = render(pole, 0f)
            // Compare canonical balls AND flared collars, including their curved cutouts.
            var compared = 0
            for (y in 0 until 514) {
                for (x in 0 until 300) {
                    if (y in 129..416) continue
                    val expectedAlpha = android.graphics.Color.alpha(reference.getPixel(x, y))
                    val actualAlpha = android.graphics.Color.alpha(actual.getPixel(x, y))
                    // Path boolean operations can rasterize a curve edge differently from
                    // a compound vector. Allow one pixel only; interiors and holes must match.
                    val neighbors =
                        (-1..1).flatMap { dy ->
                            (-1..1).map { dx ->
                                android.graphics.Color.alpha(reference.getPixel((x + dx).coerceIn(0, 299), (y + dy).coerceIn(0, 513)))
                            }
                        }
                    assertTrue(
                        "Canonical finial/collar at $x,$y expected=$expectedAlpha actual=$actualAlpha",
                        actualAlpha in (neighbors.min() - 8)..(neighbors.max() + 8),
                    )
                    if (expectedAlpha > 240) compared++
                }
            }
            assertTrue("Large ball finials, not rectangular caps", compared > 14000)
            // Canonical ball-center line leans right by 25.6 degrees; whole viewport is 0.584.
            assertEquals(0.583715f, BarberPoleLogo.WIDTH / BarberPoleLogo.HEIGHT, 0.0001f)

            fun center(rows: IntRange): Pair<Double, Double> {
                var sumX = 0.0
                var sumY = 0.0
                var count = 0
                for (y in rows) {
                    for (x in 0 until actual.width) {
                        if (android.graphics.Color.alpha(actual.getPixel(x, y)) > 240) {
                            sumX += x
                            sumY += y
                            count++
                        }
                    }
                }
                return sumX / count to sumY / count
            }
            val top = center(0..99)
            val bottom = center(448..513)
            // Lower collar overlaps the ball's upper-left quadrant, so use the lower ball's
            // canonical center row to measure its horizontal center independently.
            val bottomXs = (0 until 110).filter { android.graphics.Color.alpha(actual.getPixel(it, 464)) > 240 }
            val bottomX = (bottomXs.first() + bottomXs.last()) / 2.0
            val angle = Math.toDegrees(kotlin.math.atan2(top.first - bottomX, 464.0 - top.second))
            assertEquals("Measured ball-center axis", 26.0, angle, 1.0)
            val pixels = mutableListOf<Pair<Int, Int>>()
            for (y in 0 until 514) {
                for (x in 0 until 300) {
                    if (android.graphics.Color.alpha(actual.getPixel(x, y)) > 0) pixels.add(x to y)
                }
            }
            val boundsWidth = pixels.maxOf { it.first } - pixels.minOf { it.first } + 1
            val boundsHeight = pixels.maxOf { it.second } - pixels.minOf { it.second } + 1
            assertEquals("Measured outer aspect", 0.584, boundsWidth.toDouble() / boundsHeight, 0.003)
            assertTrue("Round lower finial diameter", bottomXs.last() - bottomXs.first() in 95..103)
            assertTrue(bottom.second > 470)
            println("Logo geometry: bounds=${boundsWidth}x$boundsHeight angle=$angle canonical-cap-pixels=$compared")

            fun opaque(
                x: Int,
                y: Int,
            ) = android.graphics.Color.alpha(actual.getPixel(x, y)) > 240
            assertTrue(opaque(249, 51) && opaque(51, 464))
            assertFalse(opaque(200, 1) || opaque(299, 1) || opaque(0, 513) || opaque(99, 513))
            assertTrue("Curved upper ball highlight", !opaque(245, 19))
            assertTrue("Curved lower ball highlight", !opaque(25, 443))
            assertTrue("Upper collar highlight", !opaque(195, 88))
            assertTrue("Lower collar highlight", !opaque(80, 420))
            actual.recycle()
            reference.recycle()
        }

    private fun render(
        pole: BarberPoleLoadingView,
        phase: Float,
    ): Bitmap = Bitmap.createBitmap(pole.width, pole.height, Bitmap.Config.ARGB_8888).also { pole.drawPole(Canvas(it), phase) }

    private fun shaftMask(pole: BarberPoleLoadingView): Bitmap =
        Bitmap.createBitmap(pole.width, pole.height, Bitmap.Config.ARGB_8888).also {
            val canvas = Canvas(it)
            val scale = minOf(pole.width / BarberPoleLogo.WIDTH, pole.height / BarberPoleLogo.HEIGHT)
            canvas.translate((pole.width - BarberPoleLogo.WIDTH * scale) / 2f, (pole.height - BarberPoleLogo.HEIGHT * scale) / 2f)
            canvas.scale(scale, scale)
            canvas.drawPath(
                BarberPoleLogo(pole.resources).shaft,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    style = android.graphics.Paint.Style.FILL_AND_STROKE
                    strokeWidth = 2f / scale // One raster-pixel tolerance at the clip edge.
                },
            )
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
        pole.layout(0, 0, 68, 116)
        try {
            block(pole)
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
