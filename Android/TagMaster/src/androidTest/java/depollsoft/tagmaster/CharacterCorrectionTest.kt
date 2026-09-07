package depollsoft.tagmaster

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ListView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.textfield.TextInputEditText
import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Live catalog route proof. Network failure is a hard failure, never an empty-state pass. */
@RunWith(AndroidJUnit4::class)
class CharacterCorrectionTest : SingingDeskTest() {
    private fun capture(name: String) {
        onView(isRoot()).perform(EspressoTestUtils.waitFor(350))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val prefix = InstrumentationRegistry.getArguments().getString("capturePrefix") ?: "android-proof"
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "$prefix-$name.png")
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }

    @Test fun homeCatalogTagTracksBackAndId() {
        capture("home")
        onView(withId(R.id.browseTagButton)).perform(scrollTo(), click())
        EspressoTestUtils.waitForView(
            allOf(withId(R.id.queryResultListView), hasDescendant(allOf(withId(R.id.titleTextView), withText(not(isEmptyOrNullString()))))),
            30000,
        )
        capture("browse")
        onData(anything()).inAdapterView(withId(R.id.queryResultListView)).atPosition(0).perform(click())
        EspressoTestUtils.waitForView(allOf(withId(R.id.titleTextView), isDisplayed()), 30000)
        onView(withId(R.id.openTracksButton)).perform(scrollTo(), click())
        onView(withId(R.id.mediaPlayer)).check(matches(isDisplayed()))
        pressBack()
        onView(withId(R.id.openTracksButton)).check(matches(isDisplayed()))
        pressBack()
        onView(withId(R.id.browseCollectionSpinner)).check(matches(isDisplayed()))
        pressBack()
        onView(withId(R.id.findTagButton)).perform(scrollTo(), click())
        onView(withId(R.id.searchTextBox)).perform(replaceText("ring a chord"), closeSoftKeyboard())
        onView(withId(R.id.searchButton)).perform(click())
        onView(withId(R.id.resultsRoot)).check(matches(isDisplayed()))
        pressBack()
        onView(withId(R.id.searchTextBox)).check(matches(withText("ring a chord")))
        capture("search")
        pressBack()
        onView(withId(R.id.openByIdButton)).perform(scrollTo(), click())
        onView(isAssignableFrom(TextInputEditText::class.java)).perform(replaceText("31"), closeSoftKeyboard())
        // Let the tablet dialog finish moving after the IME closes before tapping Open.
        EspressoTestUtils.shortWait(400)
        onView(withId(android.R.id.button1))
            .inRoot(
                androidx.test.espresso.matcher.RootMatchers
                    .isDialog(),
            ).perform(click())
        EspressoTestUtils.waitForView(allOf(withId(R.id.titleTextView), isDisplayed()), 30000)
        onView(withId(R.id.tagIdTextView)).check(matches(withText("31")))
        capture("tag31")
        onView(withId(R.id.openTracksButton)).perform(scrollTo(), click())
        EspressoTestUtils.waitForView(withId(R.id.allPartsButton), 30000)
        onView(withId(R.id.allPartsButton)).perform(scrollTo(), click())
        capture("tracks")
        pressBack()
        onView(withId(R.id.openTracksButton)).check(matches(isDisplayed()))
        pressBack()
        assertDestination(MeActivity.HOME, R.id.home)
        scenario.onActivity {
            it.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://www.barbershoptags.com/dbpage.php?pg=view&dbase=tags&id=31"),
                    it,
                    UrlHandlerActivity::class.java,
                ),
            )
        }
        EspressoTestUtils.waitForView(allOf(withId(R.id.titleTextView), isDisplayed()), 30000)
        onView(withId(R.id.tagIdTextView)).check(matches(withText("31")))
    }
}
