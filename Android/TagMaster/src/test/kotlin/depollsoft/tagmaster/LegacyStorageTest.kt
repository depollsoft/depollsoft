package depollsoft.tagmaster

import android.app.Application
import android.content.Context
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.json.JsonSerializer
import depollsoft.lib.state.StateList
import depollsoft.lib.util.Preferences
import depollsoft.tagmaster.barbershop.Tag
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.TimeZone

/**
 * What the View-era app wrote to disk still loads after the port.
 *
 * The fixtures in `src/test/resources/legacy` were written by the View-era app (commit
 * 082383d0): its saved lists (stored as `depollsoft.lib.binding.ObservableCollection`), the
 * pre-lists favorites format, a cached tag, the rated-tag set and a saved search.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LegacyStorageTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val defaultZone = TimeZone.getDefault()

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader!!.getResourceAsStream("legacy/$name.json")) { "missing fixture $name" }
            .bufferedReader()
            .use { it.readText() }

    @Before
    fun setUp() {
        TagMasterApplication.registerStorageAliases()
        RichApplication.setAppContextForTesting(app)
        // These read what the View-era app wrote to SharedPreferences, so they need the real store
        // even when an earlier class in this JVM left the in-memory test store on.
        Preferences.setTestMode(false)
        rebindPreferences()
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(defaultZone)
        rebindPreferences()
    }

    private fun rebindPreferences() {
        Preferences::class.java
            .getDeclaredField("initialized")
            .apply { isAccessible = true }
            .setBoolean(null, false)
    }

    @Test
    fun savedListsLoadThroughPreferencesAsStateLists() {
        app
            .getSharedPreferences("depollsoft.lib.Preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("tagmaster.lists", fixture("lists"))
            .commit()
        val stored: Map<*, *> = requireNotNull(Preferences.get("tagmaster.lists"))
        assertTrue(stored.values.all { it is StateList<*> })

        val lists = ListModel.decodeLists(stored)
        assertEquals(listOf(1809, 42, 7), lists[TagLists.FAVORITE])
        assertEquals(listOf(42), lists[TagLists.TEACHABLE])
        assertEquals(listOf(7, 1809), lists["afterglow-set-k3x9"])
    }

    @Test
    fun savedListsAreStillWrittenUnderTheLegacyName() {
        val json = JsonSerializer.serialize(StateList(listOf(1, 2))).toString()
        assertTrue(json, json.contains("\"depollsoft.lib.binding.ObservableCollection\""))
        assertEquals(listOf(1, 2), JsonSerializer.deserialize(json))
    }

    @Test
    fun preListsFavoritesStillMigrate() {
        val favorites = JsonSerializer.deserialize(fixture("favorites-v1")) as Collection<*>
        assertEquals(listOf(5, 6), favorites.filterIsInstance<Int>())
    }

    /**
     * The app's own migration. It runs once, from ListModel's companion, which earlier test classes
     * have already initialized in this sandbox, so it is called directly.
     */
    @Test
    fun preListsFavoritesMoveIntoTheFavoritesList() {
        app
            .getSharedPreferences("depollsoft.lib.Preferences", Context.MODE_PRIVATE)
            .edit()
            .remove("tagmaster.lists")
            .putString("tagmaster.Favorites", fixture("favorites-v1"))
            .commit()
        rebindPreferences()
        ListModel.setTestMode(true)
        try {
            ListModel.Companion::class.java
                .getDeclaredMethod("migrateOldFavorites")
                .apply { isAccessible = true }
                .invoke(ListModel.Companion)

            assertEquals(listOf(5, 6), FavoritesModel.favoriteIds)
            assertEquals(null, Preferences.get<Any?>("tagmaster.Favorites"))
        } finally {
            FavoritesModel.favoriteIds = emptyList()
            ListModel.setTestMode(false)
        }
    }

    @Test
    fun cachedTagLoads() {
        // A stored Date carries its local-time fields beside Time, and loading applies them in
        // the device's zone. The fixture was written in Pacific time.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        val tag = JsonSerializer.deserialize(fixture("tag")) as Tag
        assertEquals(2147483101, tag.id)
        assertEquals("Heart of My Heart", tag.title)
        assertEquals("The Story of the Rose", tag.alternativeTitle)
        assertEquals("Bb", tag.writtenKey)
        assertEquals("Val Hicks", tag.arranger)
        assertEquals(4, tag.parts)
        assertEquals(4.26, tag.rating!!, 0.0)
        assertEquals(48213, tag.downloadCount)
        assertEquals(12, tag.classicTagNumber)
        assertEquals("https://example.invalid/screenshot-all.mp3", tag.allPartsTrackUri!!.uri)
        assertEquals("mp3", tag.allPartsTrackUri!!.type)
        assertEquals("pdf", tag.sheetMusicUri!!.type)
        assertEquals(1228651200000, tag.posted!!.time)
        assertEquals("video-one", tag.videos!!.first().youTubeCode)
    }

    @Test
    fun ratedTagsLoad() {
        assertEquals(listOf(3, 4), JsonSerializer.deserialize(fixture("rated")))
    }

    @Test
    fun savedSearchLoads() {
        val query = JsonSerializer.deserialize(fixture("query")) as QueryModel
        assertEquals("heart", query.query)
        assertEquals(20, query.resultSetSize)
        assertEquals(50, query.maxResults)
        assertEquals(4, query.parts)
        assertEquals(true, query.hasSheetMusic)
        assertEquals(null, query.hasLearningTracks)
        assertEquals(3.0, query.minimumRating!!, 0.0)
        assertEquals(depollsoft.tagmaster.barbershop.TagCollection.ClassicTags, query.collection)
    }
}
