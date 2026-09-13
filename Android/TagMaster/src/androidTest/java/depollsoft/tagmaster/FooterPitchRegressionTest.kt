package depollsoft.tagmaster

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.lib.ui.PitchPipeButton
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Production views and note binding, with local catalog data and no network/audio dependency. */
@RunWith(AndroidJUnit4::class)
class FooterPitchRegressionTest {
    @get:Rule val fixture = NavigationTestFixture()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val args get() = InstrumentationRegistry.getArguments()
    private val baseline get() = args.getString("baseline") == "true"
    private val captureLabel get() = args.getString("captureLabel")

    @Test fun home_with_favorite_and_responsive_footer() {
        // Never sync synthetic data into a signed-in account.
        assertNull(
            com.google.firebase.auth.FirebaseAuth
                .getInstance()
                .currentUser,
        )
        val existed = FavoritesModel.getIsFavorite(fixture.tag.id)
        instrumentation.runOnMainSync { FavoritesModel.addFavorite(fixture.tag.id) }
        try {
            ActivityScenario.launch(MeActivity::class.java).use { scenario ->
                try {
                    onView(withId(android.R.id.button1)).perform(click())
                } catch (_: NoMatchingViewException) {
                }
                waitUntil {
                    var ready = false
                    scenario.onActivity { ready = it.favoritesAdapter.itemCount > 0 }
                    ready
                }
                scenario.onActivity { activity ->
                    val list = activity.findViewById<RecyclerView>(R.id.homeList)
                    list.scrollToPosition(list.adapter!!.itemCount - 1)
                    for (scale in listOf(1f, 1.3f, 2f)) {
                        val config = Configuration(activity.resources.configuration).apply { fontScale = scale }
                        val context = android.view.ContextThemeWrapper(activity.createConfigurationContext(config), R.style.AppTheme)
                        val footer = LayoutInflater.from(context).inflate(R.layout.meviewfooter, null) as ViewGroup
                        val density = context.resources.displayMetrics.density
                        val width = (360 * density).toInt()
                        footer.measure(
                            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        )
                        footer.layout(0, 0, width, footer.measuredHeight)
                        val height = footer.height / density
                        android.util.Log.i("FooterPitch", "footer 360dp font=$scale height=$height baseline=$baseline")
                        if (!baseline) {
                            if (scale == 1f) assertTrue("Footer height $height", height <= 144f)
                            verifyFooter(footer, density)
                        }
                    }
                }
                settle()
                capture("home")
            }
        } finally {
            if (!existed) instrumentation.runOnMainSync { FavoritesModel.removeFavorite(fixture.tag.id) }
        }
    }

    private fun verifyFooter(
        footer: ViewGroup,
        density: Float,
    ) {
        val destinations =
            mapOf(
                R.id.TextView01 to "http://apps.depoll.com",
                R.id.textView4 to "http://www.barbershoptags.com",
                R.id.termsHyperlink to "http://apps.depoll.com/terms-of-use",
                R.id.donateHyperlink to "http://www.davidpoll.com/applications/tag-master/donate",
            )
        val bounds =
            destinations.map { (id, url) ->
                val link = footer.findViewById<TextView>(id)
                assertEquals(url, link.tag)
                assertTrue(link.isClickable)
                assertTrue(link.height >= (48 * density).toInt())
                assertTrue(link.width >= (48 * density).toInt())
                assertTrue("No ellipsis: ${link.text}", (0 until link.lineCount).all { link.layout.getEllipsisCount(it) == 0 })
                assertTrue(
                    "Text height: ${link.text}",
                    link.layout.height <= link.height - link.compoundPaddingTop - link.compoundPaddingBottom,
                )
                Rect(0, 0, link.width, link.height).also { footer.offsetDescendantRectToMyCoords(link, it) }
            }
        bounds.forEachIndexed { index, rect ->
            assertTrue(
                "Within footer: $rect",
                rect.left >= 0 && rect.right <= footer.width && rect.top >= 0 && rect.bottom <= footer.height,
            )
            bounds.drop(index + 1).forEach { assertFalse("Nonoverlapping targets", Rect.intersects(rect, it)) }
        }
        assertEquals(footer.context.getString(R.string.app_version), footer.findViewById<TextView>(R.id.appVersionTextView).text.toString())
        assertEquals(footer.context.getString(R.string.Copyright), footer.findViewById<TextView>(R.id.copyrightTextView).text.toString())
        assertEquals(View.GONE, footer.findViewById<View>(R.id.marketHyperlink).visibility)
    }

    @Test fun summary_note_bound_rendering_and_lifecycle() {
        withSilentPlayer {
            ActivityScenario.launch<TagDetailActivity>(fixture.detailIntent()).use { scenario ->
                var button: PitchPipeButton? = null
                waitUntil {
                    scenario.onActivity { button = it.findViewById(R.id.playKeyNoteButton) }
                    button?.note != null
                }
                val key = button!!
                val note = key.note
                settle()
                capture("key-before")
                checkRendered(key, false)
                touch(key, MotionEvent.ACTION_DOWN)
                settle()
                assertTrue(note.isPlaying)
                checkRendered(key, true)
                capture("key-held")
                touch(key, MotionEvent.ACTION_UP)
                settle()
                assertFalse(note.isPlaying)
                checkRendered(key, false)
                capture("key-released")
                touch(key, MotionEvent.ACTION_DOWN)
                touch(key, MotionEvent.ACTION_CANCEL)
                settle()
                assertFalse(note.isPlaying)
                checkRendered(key, false)
                instrumentation.runOnMainSync { key.isEnabled = false }
                touch(key, MotionEvent.ACTION_DOWN)
                touch(key, MotionEvent.ACTION_UP)
                settle()
                assertFalse(note.isPlaying)
                checkRendered(key, false, disabled = true)
                instrumentation.runOnMainSync {
                    key.isEnabled = true
                    key.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK, null)
                }
                Thread.sleep(200)
                assertTrue(note.isPlaying)
                checkRendered(key, true)
                Thread.sleep(1600)
                assertFalse(note.isPlaying)
                checkRendered(key, false)
                touch(key, MotionEvent.ACTION_DOWN)
                instrumentation.runOnMainSync { key.note = Note.findNote("D", depollsoft.pitchperfect.lib.Accidental.Natural, 4) }
                settle()
                assertFalse(note.isPlaying)
                checkRendered(key, false)
                touch(key, MotionEvent.ACTION_CANCEL)
                settle()
                instrumentation.runOnMainSync {
                    key.note = note
                    key.setIsToggle(true)
                }
                touch(key, MotionEvent.ACTION_DOWN)
                touch(key, MotionEvent.ACTION_UP)
                Thread.sleep(1700)
                assertTrue("Toggle remains sounding after release and the click timeout", note.isPlaying)
                checkRendered(key, true)
                touch(key, MotionEvent.ACTION_DOWN)
                touch(key, MotionEvent.ACTION_UP)
                settle()
                assertFalse(note.isPlaying)
                checkRendered(key, false)
                instrumentation.runOnMainSync { key.setIsToggle(false) }
                touch(key, MotionEvent.ACTION_DOWN)
                assertTrue("Leave while actively held", note.isPlaying)
                scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
                settle() // Native window detachment follows the activity DESTROYED callback.
                assertFalse(note.isPlaying)
            }
        }
    }

    @Test fun sheet_note_bound_rendering_and_lifecycle() {
        withSilentPlayer {
            val file = File(fixture.context.cacheDir, "footer-pitch-sheet.png")
            val bitmap = Bitmap.createBitmap(80, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            try {
                val intent =
                    Intent(fixture.context, SheetMusicActivity::class.java)
                        .setDataAndType(Uri.fromFile(file), "image/png")
                        .putExtra("tagId", fixture.tag.id)
                ActivityScenario.launch<SheetMusicActivity>(intent).use { scenario ->
                    var key: ExtendedFloatingActionButton? = null
                    var note: Note? = null
                    waitUntil {
                        scenario.onActivity {
                            key = it.keyButton
                            note = it.tag?.keyNote
                        }
                        note != null
                    }
                    settle()
                    checkRendered(key!!, false)
                    capture("sheet-key-before")
                    touch(key!!, MotionEvent.ACTION_DOWN)
                    settle()
                    assertTrue(note!!.isPlaying)
                    checkRendered(key!!, true)
                    capture("sheet-key-held")
                    touch(key!!, MotionEvent.ACTION_UP)
                    settle()
                    assertFalse(note!!.isPlaying)
                    checkRendered(key!!, false)
                    capture("sheet-key-released")
                    touch(key!!, MotionEvent.ACTION_DOWN)
                    touch(key!!, MotionEvent.ACTION_CANCEL)
                    settle()
                    assertFalse(note!!.isPlaying)
                    checkRendered(key!!, false)
                    instrumentation.runOnMainSync { key!!.isEnabled = false }
                    touch(key!!, MotionEvent.ACTION_DOWN)
                    touch(key!!, MotionEvent.ACTION_UP)
                    settle()
                    assertFalse(note!!.isPlaying)
                    checkRendered(key!!, false, disabled = true)
                    instrumentation.runOnMainSync {
                        key!!.isEnabled = true
                        key!!.performAccessibilityAction(
                            android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK,
                            null,
                        )
                    }
                    Thread.sleep(200)
                    checkRendered(key!!, true)
                    Thread.sleep(1600)
                    checkRendered(key!!, false)
                    touch(key!!, MotionEvent.ACTION_DOWN)
                    scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
                    assertFalse(note!!.isPlaying)
                    checkRendered(key!!, false)
                }
            } finally {
                file.delete()
            }
        }
    }

    private fun checkRendered(
        button: TextView,
        playing: Boolean,
        disabled: Boolean = false,
    ) {
        if (baseline) return
        instrumentation.runOnMainSync {
            val expectedFill = button.context.getColor(if (playing) R.color.md_primary else R.color.md_surface)
            val expectedInk =
                button.context.getColor(
                    if (disabled) {
                        R.color.md_on_surface
                    } else if (playing) {
                        R.color.md_on_primary
                    } else {
                        R.color.md_primary
                    },
                )
            val bitmap = Bitmap.createBitmap(button.width, button.height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(button.context.getColor(R.color.md_surface))
            button.background.jumpToCurrentState()
            button.draw(Canvas(bitmap))
            // Interior far from text and rounded corners, not the boolean state or ripple alone.
            assertEquals("Rendered fill", expectedFill, bitmap.getPixel(button.width / 2, button.height / 4))
            if (!disabled) {
                assertEquals("Text contrast role", expectedInk, button.currentTextColor)
                val textPixels =
                    (0 until button.height).any { y ->
                        (button.compoundPaddingLeft until button.width - button.compoundPaddingRight).any { x ->
                            bitmap.getPixel(x, y) == expectedInk
                        }
                    }
                assertTrue("Rendered text uses contrasting ink", textPixels)
                assertTrue(ColorUtils.calculateContrast(expectedInk, expectedFill) >= 4.5)
            }
            val icon = if (button is ExtendedFloatingActionButton) button.icon else button.compoundDrawablesRelative[0]
            val iconBitmap = Bitmap.createBitmap(icon.intrinsicWidth, icon.intrinsicHeight, Bitmap.Config.ARGB_8888)
            icon.setBounds(0, 0, iconBitmap.width, iconBitmap.height)
            icon.draw(Canvas(iconBitmap))
            if (!disabled) {
                val pixels = IntArray(iconBitmap.width * iconBitmap.height)
                iconBitmap.getPixels(pixels, 0, iconBitmap.width, 0, 0, iconBitmap.width, iconBitmap.height)
                assertTrue("Rendered icon contrast role", pixels.any { it == expectedInk })
            }
            bitmap.recycle()
            iconBitmap.recycle()
        }
    }

    private fun touch(
        view: View,
        action: Int,
    ) = instrumentation.runOnMainSync {
        val now = SystemClock.uptimeMillis()
        MotionEvent.obtain(now, now, action, view.width / 2f, view.height / 2f, 0).also {
            view.dispatchTouchEvent(it)
            it.recycle()
        }
    }

    private fun settle() {
        instrumentation.waitForIdleSync()
        Thread.sleep(250)
        instrumentation.waitForIdleSync()
    }

    private fun waitUntil(check: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 8000
        while (!check() && SystemClock.uptimeMillis() < deadline) Thread.sleep(50)
        assertTrue("Fixture ready", check())
    }

    private fun capture(name: String) {
        val label = captureLabel ?: return
        // SurfaceFlinger capture does not wait for an intentionally endless pending animation.
        val bitmap =
            android.os.ParcelFileDescriptor
                .AutoCloseInputStream(
                    instrumentation.uiAutomation.executeShellCommand("screencap -p"),
                ).use { android.graphics.BitmapFactory.decodeStream(it) }
        val scale = 800f / maxOf(bitmap.width, bitmap.height)
        val small = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        File(fixture.context.getExternalFilesDir(null), "tagmaster-android-footer-pitch-$label-$name.jpg").outputStream().use {
            small.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
        small.recycle()
        bitmap.recycle()
    }

    private fun withSilentPlayer(block: () -> Unit) {
        Note.setPlayer(
            object : Note.NotePlayer {
                override fun play(n: Note) = Unit

                override fun stop(n: Note) = Unit
            },
        )
        try {
            block()
        } finally {
            Note.setPlayer(Note.DEFAULT_PLAYER)
        }
    }
}
