package depollsoft.tagmaster

import android.graphics.Bitmap
import android.os.Looper
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.widget.NestedScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bindroid.trackable.TrackableCollection
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import depollsoft.tagmaster.barbershop.Tag
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.isEmptyOrNullString
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.lang.ref.SoftReference

/** Synthetic saved-list fixture held only in memory. Browse uses the real read-only catalog. */
@RunWith(AndroidJUnit4::class)
class DensityNativeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var scenario: ActivityScenario<MeActivity>? = null
    private var favorites: TrackableCollection<Int>? = null
    private var teachable: TrackableCollection<Int>? = null
    private var originalNightMode = AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
    private val previousCache = mutableMapOf<Int, SoftReference<Tag>?>()
    private val names =
        listOf(
            "Evening harmony",
            "Sing me home",
            "One more chord",
            "Until we meet again",
            "Keep the melody",
            "Under the stars",
            "The last refrain",
            "A song for the road",
        )
    private val tags =
        (1..24).map { number ->
            Tag().apply {
                id = 910000 + number
                title = names[(number - 1) % names.size] + if (number > 8) " $number" else ""
                notes = "Synthetic local density fixture. Not catalog content."
                rating = 4.25
                posted = java.util.Date(1700000000000L)
                downloadCount = 1234
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun cache() =
        Tag::class.java
            .getDeclaredField("TagCache")
            .apply { isAccessible = true }
            .get(null) as SparseArray<SoftReference<Tag>?>

    private fun descendants(view: View): List<View> =
        listOf(view) +
            if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()

    @Before fun seedOnlyInMemory() {
        assertNull("Fixture must never run in a signed-in account", Firebase.auth.currentUser)
        instrumentation.runOnMainSync {
            ListModel.setTestMode(true)
            favorites = FavoritesModel.favoriteIds
            teachable = TeachableTagsModel.teachableTagIds
            originalNightMode = AppCompatDelegate.getDefaultNightMode()
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            tags.forEach {
                previousCache[it.id] = cache()[it.id]
                cache().put(it.id, SoftReference(it))
            }
            FavoritesModel.favoriteIds = TrackableCollection(tags.map { it.id }.toMutableList())
            TeachableTagsModel.teachableTagIds = TrackableCollection(tags.map { it.id }.toMutableList())
        }
        scenario = ActivityScenario.launch(MeActivity::class.java)
        onView(isRoot()).check { root, error ->
            assertNull(error)
            root.findViewById<Button>(android.R.id.button1)?.let { button ->
                assertTrue(descendants(root).filterIsInstance<TextView>().any { it.text.toString() == "Tag Master Changelog" })
                button.performClick()
            }
        }
    }

    @After fun restoreOriginalListsAndCache() {
        scenario?.close()
        instrumentation.runOnMainSync {
            favorites?.let { FavoritesModel.favoriteIds = it }
            teachable?.let { TeachableTagsModel.teachableTagIds = it }
            previousCache.forEach { (id, value) -> if (value == null) cache().remove(id) else cache().put(id, value) }
            AppCompatDelegate.setDefaultNightMode(originalNightMode)
            ListModel.setTestMode(false)
        }
    }

    private fun capture(name: String) {
        onView(isRoot()).perform(EspressoTestUtils.waitFor(350))
        instrumentation.waitForIdleSync()
        val prefix = InstrumentationRegistry.getArguments().getString("capturePrefix") ?: return
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "$prefix-$name.png")
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }

    private fun assertSavedRows(id: Int) {
        onView(withId(id)).check { view, error ->
            assertNull(error)
            val rows = descendants(view).filterIsInstance<SavedTagItemView>()
            assertEquals(24, rows.size)
            assertEquals(tags.map { it.id }, rows.map { it.tagId })
            assertTrue(rows.all { it.tag != null && !it.failedToLoad })
            assertTrue(
                "Capture needs at least five visible saved rows",
                rows.count { row ->
                    val rect = android.graphics.Rect()
                    row.getGlobalVisibleRect(rect) && rect.height() == row.height
                } >= 5,
            )
        }
    }

    @Test fun populatedHomeFavoritesTeachableBrowseAndBackPosition() {
        onView(withId(R.id.favoritesButton)).check(matches(withText("Favorites · 24")))
        onView(withId(R.id.teachableButton)).check(matches(withText("Teachable Tags · 24")))
        scenario!!.onActivity {
            assertEquals(2, it.findViewById<ViewGroup>(R.id.yourListsEntries).childCount)
            assertNull(it.findViewById<View>(R.id.favoritesItemsControl))
        }
        capture("home")
        onView(withId(R.id.favoritesButton)).perform(scrollTo(), click())
        EspressoTestUtils.waitForView(allOf(withId(R.id.titleTextView), withText(names[0])), 10000)
        onView(withText(R.string.Favorites)).check(matches(isDisplayed()))
        assertSavedRows(R.id.favoritesItemsControl)
        capture("favorites")
        // A nonzero scroll offset must survive Tag / Back, not merely return to the list top.
        val chosen = tags[16]
        onView(allOf(withId(R.id.titleTextView), withText(chosen.title))).perform(scrollTo())
        var position = 0
        onView(withId(R.id.scrollView1)).check { view, _ ->
            position = (view as NestedScrollView).scrollY
            assertTrue(position > 0)
        }
        onView(allOf(withId(R.id.titleTextView), withText(chosen.title))).perform(click())
        EspressoTestUtils.waitForView(withId(R.id.tagIdTextView), 10000)
        onView(withId(R.id.tagIdTextView)).check(matches(withText(chosen.id.toString())))
        pressBack()
        // On expanded layouts, Back first clears the automatically selected material.
        var stillDetail = false
        onView(isRoot()).check { root, _ -> stillDetail = root.findViewById<View>(R.id.detailRoot) != null }
        if (stillDetail) pressBack()
        onView(withId(R.id.scrollView1)).check { view, _ -> assertEquals(position, (view as NestedScrollView).scrollY) }
        pressBack()
        onView(withId(R.id.teachableButton)).perform(scrollTo(), click())
        EspressoTestUtils.waitForView(allOf(withId(R.id.titleTextView), withText(names[0])), 10000)
        onView(withText(R.string.TeachableTags)).check(matches(isDisplayed()))
        assertSavedRows(R.id.teachableTagsItemsControl)
        capture("teachable")
        pressBack()
        onView(withId(R.id.browseTagButton)).perform(scrollTo(), click())
        EspressoTestUtils.waitForView(
            allOf(withId(R.id.queryResultListView), hasDescendant(allOf(withId(R.id.titleTextView), withText(not(isEmptyOrNullString()))))),
            30000,
        )
        onView(withId(R.id.queryResultListView)).check { view, error ->
            assertNull(error)
            assertTrue("Browse must show five populated catalog rows", (view as ListView).childCount >= 5)
        }
        capture("browse")
        pressBack()
    }

    @Test fun contextMenusMutateOnlyTheirOwnBuiltIn() {
        onView(withId(R.id.favoritesButton)).perform(scrollTo(), click())
        EspressoTestUtils.waitForView(allOf(withId(R.id.titleTextView), withText(names[0])), 10000)
        onView(allOf(withId(R.id.titleTextView), withText(names[0]))).perform(longClick())
        onView(withText(R.string.MoveDown)).perform(click())
        instrumentation.runOnMainSync {
            assertEquals(tags[1].id, FavoritesModel.favoriteIds[0])
            assertEquals(tags[0].id, TeachableTagsModel.teachableTagIds[0])
        }
        onView(allOf(withId(R.id.titleTextView), withText(names[0]))).perform(longClick())
        onView(withText(R.string.RemoveFromFavorites)).perform(click())
        instrumentation.runOnMainSync {
            assertFalse(FavoritesModel.getIsFavorite(tags[0].id))
            assertTrue(TeachableTagsModel.getIsTeachableTag(tags[0].id))
        }
        pressBack()
        onView(withId(R.id.favoritesButton)).check(matches(withText("Favorites · 23")))
        onView(withId(R.id.teachableButton)).perform(scrollTo(), click())
        EspressoTestUtils.waitForView(allOf(withId(R.id.titleTextView), withText(names[1])), 10000)
        onView(allOf(withId(R.id.titleTextView), withText(names[1]))).perform(longClick())
        onView(withText(R.string.RemoveTeachableTagMenuText)).perform(click())
        instrumentation.runOnMainSync {
            assertTrue(FavoritesModel.getIsFavorite(tags[1].id))
            assertFalse(TeachableTagsModel.getIsTeachableTag(tags[1].id))
        }
        pressBack()
    }
}
