package depollsoft.pitchperfect

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import depollsoft.lib.util.Preferences
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class PitchInstrumentAnimationTest {
    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
    }

    @After
    fun tearDown() {
        Preferences.setTestMode(false)
    }

    @Test
    fun stopAll_cancelsInfiniteBreathingAnimatorWhileViewRemainsAttached() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val view = PitchInstrumentView(context)
        val model = PitchPipeModel()
        view.model = model
        view.layout(0, 0, 600, 900)
        model.notes[0].isPlaying = true
        view.draw(Canvas(Bitmap.createBitmap(600, 900, Bitmap.Config.ARGB_8888)))

        val animatorField = PitchInstrumentView::class.java.getDeclaredField("breatheAnimator")
        animatorField.isAccessible = true
        assertTrue(animatorField.get(view) != null)

        view.stopAll()

        assertNull(animatorField.get(view))
    }
}
