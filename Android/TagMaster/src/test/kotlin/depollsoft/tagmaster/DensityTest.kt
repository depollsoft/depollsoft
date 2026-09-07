package depollsoft.tagmaster

import android.content.ComponentName
import android.content.Intent
import android.os.Looper
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bindroid.trackable.TrackableCollection
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.barbershop.Tag
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.lang.ref.SoftReference

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(application = TestDeskApplication::class, sdk = [35])
class DensityTest {
    private lateinit var originalFavorites: TrackableCollection<Int>
    private lateinit var originalTeachable: TrackableCollection<Int>
    private val tags =
        (1..3).map { number ->
            Tag().apply {
                id = 910000 + number
                title = "Evening harmony $number"
            }
        }
    private val previousCache = mutableMapOf<Int, SoftReference<Tag>?>()

    @Suppress("UNCHECKED_CAST")
    private fun cache() =
        Tag::class.java
            .getDeclaredField("TagCache")
            .apply { isAccessible = true }
            .get(null) as SparseArray<SoftReference<Tag>?>

    @Before fun isolate() {
        ListModel.setTestMode(true)
        originalFavorites = FavoritesModel.favoriteIds
        originalTeachable = TeachableTagsModel.teachableTagIds
        FavoritesModel.favoriteIds = TrackableCollection()
        TeachableTagsModel.teachableTagIds = TrackableCollection()
        tags.forEach {
            previousCache[it.id] = cache()[it.id]
            cache().put(it.id, SoftReference(it))
        }
    }

    @After fun restore() {
        FavoritesModel.favoriteIds = originalFavorites
        TeachableTagsModel.teachableTagIds = originalTeachable
        previousCache.forEach { (id, value) -> if (value == null) cache().remove(id) else cache().put(id, value) }
        ListModel.setTestMode(false)
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun descendants(view: View): List<View> =
        listOf(view) +
            if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()

    private fun measure(
        view: View,
        width: Int = 411,
    ): Float {
        val density = view.resources.displayMetrics.density
        view.measure(
            View.MeasureSpec.makeMeasureSpec((width * density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        println(
            "DENSITY height=${view.measuredHeight / density}dp width=${view.measuredWidth / density}dp fontScale=${view.resources.configuration.fontScale}",
        )
        descendants(view).filterIsInstance<TextView>().filter { it.visibility == View.VISIBLE }.forEach {
            println("TEXT '${it.text}' size=${it.textSize / density}dp lines=${it.lineCount} height=${it.height / density}dp")
        }
        return view.measuredHeight / density
    }

    @Test fun homeHasSameTwoRolesForZeroOneAndOneHundredOverlappingIds() {
        Robolectric.buildActivity(MeActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val root = activity.findViewById<View>(R.id.scrollView1)
            val heights = mutableListOf<Float>()
            for (count in listOf(0, 1, 100)) {
                FavoritesModel.favoriteIds = TrackableCollection((1..count).toMutableList())
                TeachableTagsModel.teachableTagIds = TrackableCollection((1..count).toMutableList())
                idle()
                val entries = activity.findViewById<ViewGroup>(R.id.yourListsEntries)
                assertEquals(2, entries.childCount)
                assertEquals("Favorites · $count", activity.findViewById<TextView>(R.id.favoritesButton).text.toString())
                assertEquals("Teachable Tags · $count", activity.findViewById<TextView>(R.id.teachableButton).text.toString())
                assertTrue(descendants(root).none { it is SavedTagItemView || it is TagItemView || it is depollsoft.lib.ui.ItemsControl })
                heights.add(measure(root))
                listOf(R.id.favoritesButton, R.id.teachableButton).forEach { id ->
                    val entry = activity.findViewById<View>(id)
                    assertEquals(View.VISIBLE, entry.visibility)
                    assertTrue(entry.isClickable)
                    assertTrue(entry.height / entry.resources.displayMetrics.density >= 48)
                }
            }
            assertEquals(1, heights.distinct().size)
        }
    }

    @Test fun bothEmptyDestinationsAreRegisteredTitledAndKeepPole() {
        Robolectric.buildActivity(FavoritesActivity::class.java).setup().use {
            val activity = it.get()
            assertEquals("Favorites", activity.supportActionBar!!.title.toString())
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.noFavoritesTextView).visibility)
            assertTrue(activity.findViewById<View>(R.id.favoritesRoot).background is PoleBackground)
            assertNotNull(activity.packageManager.getActivityInfo(ComponentName(activity, FavoritesActivity::class.java), 0))
        }
        Robolectric.buildActivity(TeachableTagsActivity::class.java).setup().use {
            assertEquals(
                "Teachable Tags",
                it
                    .get()
                    .supportActionBar!!
                    .title
                    .toString(),
            )
            assertEquals(View.VISIBLE, it.get().findViewById<View>(R.id.noTeachableTagsTextView).visibility)
            assertTrue(it.get().findViewById<View>(R.id.teachableRoot).background is PoleBackground)
        }
    }

    @Test fun peerEntriesLaunchTheCorrectActivities() {
        Robolectric.buildActivity(MeActivity::class.java).setup().use {
            val activity = it.get()
            activity.findViewById<View>(R.id.favoritesButton).performClick()
            assertEquals(FavoritesActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)
            activity.findViewById<View>(R.id.teachableButton).performClick()
            assertEquals(TeachableTagsActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)
        }
    }

    @Test fun separatePagesKeepOverlapOrderAndIndependentMutations() {
        val ids = tags.map { it.id }
        FavoritesModel.favoriteIds = TrackableCollection(ids.toMutableList())
        TeachableTagsModel.teachableTagIds = TrackableCollection(ids.reversed().toMutableList())
        Robolectric.buildActivity(FavoritesActivity::class.java).setup().use { favorites ->
            Robolectric.buildActivity(TeachableTagsActivity::class.java).setup().use { teachable ->
                fun favoriteRows() =
                    descendants(favorites.get().findViewById(R.id.favoritesItemsControl)).filterIsInstance<FavoriteTagItemView>().map {
                        it.tagId
                    }

                fun teachableRows() =
                    descendants(teachable.get().findViewById(R.id.teachableTagsItemsControl)).filterIsInstance<TeachableTagItemView>().map {
                        it.tagId
                    }
                idle()
                assertEquals(ids, favoriteRows())
                assertEquals(ids.reversed(), teachableRows())
                FavoritesModel.moveDown(ids[0])
                idle()
                assertEquals(listOf(ids[1], ids[0], ids[2]), favoriteRows())
                assertEquals(ids.reversed(), teachableRows())
                FavoritesModel.removeFavorite(ids[0])
                idle()
                assertEquals(listOf(ids[1], ids[2]), favoriteRows())
                assertTrue(TeachableTagsModel.getIsTeachableTag(ids[0]))
                TeachableTagsModel.moveUp(ids[0])
                idle()
                assertEquals(listOf(ids[2], ids[0], ids[1]), teachableRows())
                TeachableTagsModel.removeTeachableTag(ids[1])
                idle()
                assertTrue(FavoritesModel.getIsFavorite(ids[1]))
                assertEquals(listOf(ids[2], ids[0]), teachableRows())
            }
        }
    }

    private fun withRow(block: (TagItemView, AppCompatActivity) -> Unit) {
        Robolectric.buildActivity(FavoritesActivity::class.java).setup().use {
            val row = TagItemView(it.get())
            row.bind(tags.first())
            it.get().setContentView(row)
            idle()
            block(row, it.get())
        }
    }

    @Test fun defaultRowIsTwoLinesSixtyToSeventyTwoDpWithReadOnlyStatus() =
        withRow { row, _ ->
            val height = measure(row)
            assertTrue("Default row is ${height}dp", height in 60f..72f)
            assertEquals("ID 910001 · No materials", row.findViewById<TextView>(R.id.materialStatusTextView).text.toString())
            assertEquals(View.GONE, row.findViewById<View>(R.id.akaTextView).visibility)
            assertTrue(descendants(row).none { it is CompoundButton })
            assertFalse(row.findViewById<View>(R.id.materialStatusTextView).isClickable)
            assertNull(row.findViewById<View>(R.id.ratingTextView))
            assertNull(row.findViewById<View>(R.id.postedTextView))
            assertNull(row.findViewById<View>(R.id.downloadsTextView))
        }

    @Test fun alternateOnlyWhenNonemptyAndDifferentAndMaterialsAreReadable() =
        withRow { row, _ ->
            val tag = tags.first()
            for (alternate in listOf(null, "", "   ", tag.title, " ${tag.title} ")) {
                tag.alternativeTitle = alternate
                idle()
                assertEquals(View.GONE, row.findViewById<View>(R.id.akaTextView).visibility)
            }
            tag.alternativeTitle = "Another complete title"
            tag.sheetMusicUri =
                RemoteLocation().apply {
                    uri = "https://example.invalid/fixture.pdf"
                    type = "pdf"
                }
            tag.allPartsTrackUri =
                RemoteLocation().apply {
                    uri = "https://example.invalid/fixture.mp3"
                    type = "mp3"
                }
            idle()
            assertEquals(View.VISIBLE, row.findViewById<View>(R.id.akaTextView).visibility)
            assertEquals("a.k.a. Another complete title", row.findViewById<TextView>(R.id.akaTextView).text.toString())
            assertEquals(
                "ID 910001 · Sheet music · Learning tracks",
                row.findViewById<TextView>(R.id.materialStatusTextView).text.toString(),
            )
        }

    @Test
    @Config(fontScale = 2f, qualifiers = "w411dp-h891dp")
    fun longTitleAndLargeTypeGrowWithoutClipping() =
        withRow { row, _ ->
            val title = "A long complete title that must remain readable when the singer uses the largest system font size"
            tags.first().title = title
            tags.first().alternativeTitle = "An equally complete alternate title for this synthetic fixture"
            idle()
            assertTrue(measure(row) > 100f)
            val titleView = row.findViewById<TextView>(R.id.titleTextView)
            assertEquals(title, titleView.text.toString())
            assertTrue(titleView.lineCount > 1)
            descendants(row).filterIsInstance<TextView>().filter { it.visibility == View.VISIBLE }.forEach {
                assertNull(it.ellipsize)
                assertTrue("Clipped ${it.text}", it.layout.height <= it.height - it.paddingTop - it.paddingBottom)
            }
        }

    @Test fun savedRowsHaveNoInvisibleHeightPlaceholderAndKeepLoadingErrorStates() {
        Robolectric.buildActivity(FavoritesActivity::class.java).setup().use {
            val row = FavoriteTagItemView(it.get())
            it.get().setContentView(row)
            idle()
            assertEquals(1, descendants(row).filterIsInstance<TagItemView>().size)
            assertEquals(View.VISIBLE, row.findViewById<View>(R.id.loadingBar).visibility)
            row.tagId = 910001
            row.failedToLoad = true
            idle()
            assertEquals(View.VISIBLE, row.findViewById<View>(R.id.failedToLoad).visibility)
            assertEquals(View.GONE, row.findViewById<View>(R.id.loadingBar).visibility)
            row.bind(910002)
            idle()
            assertFalse(row.failedToLoad)
            assertEquals(910002, row.tag!!.id)
            assertTrue(measure(row) in 60f..72f)
        }
    }

    @Test fun compactRowStillOpensFullDetailAndMetadataLayoutsRemain() =
        withRow { row, activity ->
            row.performClick()
            val intent = shadowOf(activity).nextStartedActivity
            assertEquals(TagDetailActivity::class.java.name, intent.component!!.className)
            assertEquals(tags.first().id, intent.getIntExtra(TagDetailActivity.TAG_ID_EXTRA, -1))
            val inflater = LayoutInflater.from(activity)
            val summary = inflater.inflate(R.layout.tagsummaryview, null)
            val detail = inflater.inflate(R.layout.tagmiscview, null)
            assertNotNull(summary.findViewById<View>(R.id.ratingTextView))
            assertNotNull(detail.findViewById<View>(R.id.postedTextView))
            assertNotNull(detail.findViewById<View>(R.id.downloadsTextView))
        }
}
