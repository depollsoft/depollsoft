package depollsoft.tagmaster

import android.content.Intent
import android.content.pm.ActivityInfo
import android.view.View
import android.widget.Spinner
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CharacterStateTest {
    @Test fun materialSurvivesRecreationAndRotation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ActivityScenario
            .launch<TagDetailActivity>(
                Intent(context, TagDetailActivity::class.java)
                    .putExtra(TagDetailActivity.TAG_ID_EXTRA, 31),
            ).use { scenario ->
                EspressoTestUtils.waitForView(withId(R.id.titleTextView), 30000)
                scenario.onActivity { it.openMaterial("tracks") }
                onView(withId(R.id.mediaPlayer)).check(matches(isDisplayed()))
                scenario.recreate()
                onView(withId(R.id.mediaPlayer)).check(matches(isDisplayed()))
                scenario.onActivity {
                    assertEquals(31, it.tagId)
                    assertTrue(it.hasOpenMaterial)
                    assertFalse(it.supportFragmentManager.findFragmentByTag("tracks")!!.isHidden)
                    it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                EspressoTestUtils.shortWait(500)
                onView(withId(R.id.mediaPlayer)).check(matches(isDisplayed()))
                scenario.onActivity {
                    assertEquals(31, it.tagId)
                    assertFalse(it.supportFragmentManager.findFragmentByTag("tracks")!!.isHidden)
                    it.onBackPressedDispatcher.onBackPressed()
                    assertFalse(it.hasOpenMaterial)
                    assertEquals(View.VISIBLE, it.findViewById<View>(R.id.summaryContainer).visibility)
                }
            }
    }

    @Test fun querySurvivesRecreation() {
        ActivityScenario.launch(TagSearchActivity::class.java).use { scenario ->
            onView(withId(R.id.searchTextBox)).perform(replaceText("ring a chord"), closeSoftKeyboard())
            scenario.recreate()
            onView(withId(R.id.searchTextBox)).check(matches(withText("ring a chord")))
        }
    }

    @Test fun collectionSurvivesRecreation() {
        ActivityScenario.launch(TagBrowserActivity::class.java).use { scenario ->
            scenario.onActivity { it.findViewById<Spinner>(R.id.browseCollectionSpinner).setSelection(1) }
            EspressoTestUtils.shortWait(100)
            scenario.recreate()
            scenario.onActivity { assertEquals(1, it.findViewById<Spinner>(R.id.browseCollectionSpinner).selectedItemPosition) }
        }
    }
}
