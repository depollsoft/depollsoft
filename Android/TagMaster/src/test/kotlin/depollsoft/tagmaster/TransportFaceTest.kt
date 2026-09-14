package depollsoft.tagmaster

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TransportFaceTest {
    @Test fun native_faces_and_icon_viewports_are_square_and_centered_in_every_state() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.AppTheme)
        controller.setup()
        try {
            val root = controller.get().layoutInflater.inflate(R.layout.mediaplayerview, null)
            val density = root.resources.displayMetrics.density
            for (id in listOf(R.id.playPauseButton, R.id.stopButton)) {
                val button = root.findViewById<MaterialButton>(id)
                for (enabled in listOf(false, true)) {
                    for (pressed in listOf(false, true)) {
                        for (icon in if (id ==
                            R.id.playPauseButton
                        ) {
                            listOf(R.drawable.ic_play, R.drawable.ic_pause)
                        } else {
                            listOf(R.drawable.ic_stop)
                        }) {
                            button.isEnabled = enabled
                            button.isPressed = pressed
                            button.setIconResource(icon)
                            val size = (48 * density).toInt()
                            button.measure(
                                View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY),
                            )
                            button.layout(0, 0, size, size)
                            button.jumpDrawablesToCurrentState()
                            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                            button.background.setBounds(0, 0, size, size)
                            button.background.draw(Canvas(bitmap))
                            val face = Rect(size, size, 0, 0)
                            for (y in 0 until size) {
                                for (x in 0 until size) {
                                    if (Color.alpha(bitmap.getPixel(x, y)) > 8) {
                                        face.left = minOf(face.left, x)
                                        face.top = minOf(face.top, y)
                                        face.right = maxOf(face.right, x + 1)
                                        face.bottom = maxOf(face.bottom, y + 1)
                                    }
                                }
                            }
                            assertEquals("Actual native body $id enabled=$enabled pressed=$pressed", Rect(0, 0, size, size), face)
                            assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
                            assertTrue("Round face reaches top center", Color.alpha(bitmap.getPixel(size / 2, 0)) > 8)
                            val viewport = button.icon!!.bounds
                            assertEquals(24 * density, viewport.width().toFloat(), .01f)
                            assertEquals("Whole viewport, not triangle ink", size / 2f, button.paddingLeft + viewport.exactCenterX(), .5f)
                            assertEquals(button.paddingLeft, button.paddingRight)
                            bitmap.recycle()
                        }
                    }
                }
            }
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
