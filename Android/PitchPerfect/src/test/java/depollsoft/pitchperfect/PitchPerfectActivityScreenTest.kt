package depollsoft.pitchperfect

import android.os.Looper
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationBarView
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.ScreenTestSupport.assertDisplayed
import depollsoft.pitchperfect.ScreenTestSupport.idle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * The main screen's layout and tab navigation, migrated from the instrumented
 * `PitchPerfectActivityTest`.
 *
 * The instrumented original only re-checked "the pager is still displayed" after each tap, which no
 * navigation bug could fail. These assert the page the pager actually landed on and the nav item
 * that ended up selected.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class)
class PitchPerfectActivityScreenTest {
    private var controller: ActivityController<PitchPerfectActivity>? = null

    @Before
    fun setUp() {
        // A first-ever launch shows neither the login prompt nor the changelog, which is what the
        // instrumented test's dismissStartupDialogs() was reaching for. Ads stay off so the
        // deferred MobileAds initialization never runs.
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
        PurchaseService.areAdsRemoved = true
    }

    @After
    fun tearDown() {
        PurchaseService.areAdsRemoved = false
        ScreenTestSupport.finishScreenTest(controller)
    }

    private fun launch(): PitchPerfectActivity {
        val created = Robolectric.buildActivity(PitchPerfectActivity::class.java)
        controller = created
        created.setup()
        idle()
        return created.get()
    }

    private fun PitchPerfectActivity.pager() = findViewById<ViewPager2>(R.id.viewPager)

    private fun PitchPerfectActivity.nav() = findViewById<NavigationBarView>(R.id.bottomNavigation)

    /** A bottom-navigation tap, settled through the pager's animated page change. */
    private fun PitchPerfectActivity.tap(itemId: Int) {
        findViewById<View>(itemId).performClick()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
    }

    @Test
    fun activityLaunches() {
        val activity = launch()
        assertNotNull(activity)
        assertEquals(androidx.lifecycle.Lifecycle.State.RESUMED, activity.lifecycle.currentState)
    }

    @Test
    fun viewPagerIsDisplayed() {
        assertDisplayed("viewPager", launch().pager())
    }

    @Test
    fun bottomNavigationIsDisplayed() {
        assertDisplayed("bottomNavigation", launch().nav())
    }

    @Test
    fun navigateToPitchPipeTab() {
        val activity = launch()
        activity.tap(R.id.keys_item)
        activity.tap(R.id.pitchpipe_item)
        assertDisplayed("viewPager", activity.pager())
        assertEquals("Pitch Pipe is page 0", 0, activity.pager().currentItem)
        assertEquals(R.id.pitchpipe_item, activity.nav().selectedItemId)
    }

    @Test
    fun navigateToKeysTab() {
        val activity = launch()
        activity.tap(R.id.keys_item)
        assertDisplayed("viewPager", activity.pager())
        assertEquals("Keys is page 2", 2, activity.pager().currentItem)
        assertEquals(R.id.keys_item, activity.nav().selectedItemId)
    }

    @Test
    fun navigateToSongsTab() {
        val activity = launch()
        activity.tap(R.id.songs_item)
        assertDisplayed("viewPager", activity.pager())
        assertEquals("Songs is page 3", 3, activity.pager().currentItem)
        assertEquals(R.id.songs_item, activity.nav().selectedItemId)
    }

    @Test
    fun navigationBetweenAllTabs() {
        val activity = launch()
        for ((item, page) in listOf(
            R.id.pitchpipe_item to 0,
            R.id.notes_item to 1,
            R.id.keys_item to 2,
            R.id.songs_item to 3,
            R.id.pitchpipe_item to 0,
        )) {
            activity.tap(item)
            assertDisplayed("viewPager on page $page", activity.pager())
            assertEquals(page, activity.pager().currentItem)
            assertEquals(item, activity.nav().selectedItemId)
        }
    }

    @Test
    fun canNavigateToAllTabsFromAnyTab() {
        val activity = launch()
        activity.tap(R.id.songs_item)
        assertEquals(3, activity.pager().currentItem)
        activity.tap(R.id.keys_item)
        assertEquals("Keys is reachable directly from Songs", 2, activity.pager().currentItem)
        assertDisplayed("viewPager", activity.pager())
    }

    @Test
    fun activitySurvivesRecreation() {
        val created = Robolectric.buildActivity(PitchPerfectActivity::class.java)
        controller = created
        created.setup()
        idle()
        created.get().tap(R.id.keys_item)
        assertEquals(2, created.get().pager().currentItem)

        created.recreate()
        idle()

        val restored = created.get()
        assertDisplayed("viewPager after recreation", restored.pager())
        assertDisplayed("bottomNavigation after recreation", restored.nav())
    }
}
