package depollsoft.pitchperfect

import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.lib.PitchedSong
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The set list rules from `docs/pitchperfect-set-lists.md`, exercised against the model alone.
 *
 * [SongsModel] is a process-wide singleton, so each test rebuilds the map down to a single
 * `default` list rather than trying to make a second instance.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class)
class SongsModelSetListsTest {
    private val model get() = SongsModel.get()

    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        val default = SongList(SongsModel.DEFAULT_ID)
        default.name = "Default"
        model.songLists = mapOf(SongsModel.DEFAULT_ID to default)
        model.currentListId = SongsModel.DEFAULT_ID
    }

    @After
    fun tearDown() {
        val default = SongList(SongsModel.DEFAULT_ID)
        default.name = "Default"
        model.songLists = mapOf(SongsModel.DEFAULT_ID to default)
        Preferences.setTestMode(false)
    }

    private fun song(
        title: String,
        keyIndex: Int = 0,
    ): PitchedSong =
        PitchedSong().apply {
            name = title
            key = Key.getMajorKeys()[keyIndex]
        }

    // ==================== Display names ====================

    @Test
    fun theLegacySeedNameReadsAsMySongs() {
        assertEquals("My Songs", model.displayName(model.defaultSongList))
    }

    @Test
    fun aBlankNameReadsAsMySongs() {
        model.defaultSongList.name = "   "
        assertEquals("My Songs", model.displayName(model.defaultSongList))
    }

    @Test
    fun aRenamedDefaultListShowsItsOwnName() {
        model.renameList(SongsModel.DEFAULT_ID, "Chorus book")
        assertEquals("Chorus book", model.displayName(model.defaultSongList))
    }

    // ==================== Name validation ====================

    @Test
    fun anEmptyNameIsRejected() {
        assertEquals(SongsModel.NameError.EMPTY, model.validateName("   "))
    }

    @Test
    fun aNameLongerThanSixtyCharactersIsRejected() {
        assertNull(model.validateName("a".repeat(60)))
        assertEquals(SongsModel.NameError.TOO_LONG, model.validateName("a".repeat(61)))
    }

    @Test
    fun theReservedNameIsRejectedWhateverItsCase() {
        assertEquals(SongsModel.NameError.RESERVED, model.validateName("Default"))
        assertEquals(SongsModel.NameError.RESERVED, model.validateName("  default "))
    }

    @Test
    fun aNameCollidingWithAnotherListsDisplayNameIsRejected() {
        model.createList("Saturday show")
        assertEquals(SongsModel.NameError.DUPLICATE, model.validateName("saturday  SHOW"))
        // "my songs" collides with the default list, whose stored name is the legacy seed.
        assertEquals(SongsModel.NameError.DUPLICATE, model.validateName("my songs"))
    }

    @Test
    fun renamingAListToItsOwnNameIsAllowed() {
        val id = model.createList("Saturday show")
        assertNull(model.validateName("Saturday show", excludingId = id))
        assertEquals(SongsModel.NameError.DUPLICATE, model.validateName("Saturday show"))
    }

    @Test
    fun namesAreNormalizedBeforeUse() {
        val id = model.createList("  Saturday   show  ")
        assertEquals("Saturday show", model.displayName(id))
    }

    // ==================== Ids ====================

    @Test
    fun anIdIsASlugPlusFourBase36Characters() {
        val id = model.createList("Saturday Show!")
        assertTrue(id, Regex("^saturday-show-[a-z0-9]{4}$").matches(id))
    }

    @Test
    fun aNameWithNoSlugSafeCharactersFallsBackToList() {
        val id = model.createList("!!! ???")
        assertTrue(id, Regex("^list-[a-z0-9]{4}$").matches(id))
    }

    @Test
    fun renamingNeverChangesTheId() {
        val id = model.createList("Saturday show")
        model.renameList(id, "Sunday show")
        assertTrue(model.songLists.containsKey(id))
        assertEquals("Sunday show", model.displayName(id))
    }

    // ==================== Ordering ====================

    @Test
    fun createWritesTheNextOrderAndDefaultStaysPinnedFirst() {
        val first = model.createList("Alpha")
        val second = model.createList("Beta")
        assertEquals(0L, model.songLists[first]!!.order)
        assertEquals(1L, model.songLists[second]!!.order)
        assertEquals(
            listOf(SongsModel.DEFAULT_ID, first, second),
            model.orderedLists.map { it.id },
        )
    }

    @Test
    fun unorderedListsSortAfterOrderedOnesByDisplayName() {
        val ordered = model.createList("Zulu")
        val legacyB = model.createList("Bravo")
        val legacyA = model.createList("Alpha")
        model.songLists[legacyA]!!.order = null
        model.songLists[legacyB]!!.order = null
        model.songLists[ordered]!!.order = 0L
        assertEquals(
            listOf(SongsModel.DEFAULT_ID, ordered, legacyA, legacyB),
            model.orderedLists.map { it.id },
        )
    }

    @Test
    fun reorderRewritesOrderDenselyOverEveryCustomList() {
        val a = model.createList("Alpha")
        val b = model.createList("Bravo")
        val c = model.createList("Charlie")
        model.reorderLists(listOf(c, a, b))
        assertEquals(0L, model.songLists[c]!!.order)
        assertEquals(1L, model.songLists[a]!!.order)
        assertEquals(2L, model.songLists[b]!!.order)
        assertEquals(
            listOf(SongsModel.DEFAULT_ID, c, a, b),
            model.orderedLists.map { it.id },
        )
    }

    // ==================== Duplicate ====================

    @Test
    fun duplicateNamesCountUp() {
        val source = model.createList("Saturday show")
        val first = model.duplicateList(source)!!
        assertEquals("Saturday show copy", model.displayName(first))
        val second = model.duplicateList(source)!!
        assertEquals("Saturday show copy 2", model.displayName(second))
        val third = model.duplicateList(source)!!
        assertEquals("Saturday show copy 3", model.displayName(third))
    }

    @Test
    fun duplicateDeepCopiesEverySongWithAFreshId() {
        val source = model.createList("Saturday show")
        model.songLists[source]!!.addSong(song("Blue Skies"))
        model.songLists[source]!!.addSong(song("Shenandoah", 3))

        val copyId = model.duplicateList(source)!!
        val original = model.songLists[source]!!.songs
        val copy = model.songLists[copyId]!!.songs

        assertEquals(listOf("Blue Skies", "Shenandoah"), copy.map { it.name })
        assertEquals(original.map { it.key }, copy.map { it.key })
        original.zip(copy).forEach { (left, right) ->
            assertNotEquals("copies are independent songs", left.id, right.id)
        }
        // Editing the copy must leave the source alone.
        copy[0].name = "Renamed"
        assertEquals("Blue Skies", original[0].name)
    }

    @Test
    fun duplicateOrdersAfterEveryExistingList() {
        val source = model.createList("Saturday show")
        model.createList("Afterglow")
        val copyId = model.duplicateList(source)!!
        assertEquals(2L, model.songLists[copyId]!!.order)
    }

    // ==================== Delete ====================

    @Test
    fun deletingTheCurrentListFallsBackToDefault() {
        val id = model.createList("Saturday show")
        model.currentListId = id
        assertEquals(id, model.currentListId)

        model.deleteList(id)
        assertEquals(SongsModel.DEFAULT_ID, model.currentListId)
        assertFalse(model.songLists.containsKey(id))
    }

    @Test
    fun theDefaultListCannotBeDeleted() {
        model.deleteList(SongsModel.DEFAULT_ID)
        assertTrue(model.songLists.containsKey(SongsModel.DEFAULT_ID))
    }

    @Test
    fun aCurrentListThatVanishesReadsAsDefault() {
        val id = model.createList("Saturday show")
        model.currentListId = id
        // As a remote REMOVED change would leave it.
        model.songLists = model.songLists - id
        assertEquals(SongsModel.DEFAULT_ID, model.currentListId)
        assertEquals(SongsModel.DEFAULT_ID, model.currentList.id)
    }

    // ==================== Clear all ====================

    @Test
    fun clearAllEmptiesMySongsAndDeletesEveryOtherList() {
        model.defaultSongList.addSong(song("Blue Skies"))
        val id = model.createList("Saturday show")
        model.songLists[id]!!.addSong(song("Shenandoah"))
        model.currentListId = id

        model.clearAll()

        assertEquals(setOf(SongsModel.DEFAULT_ID), model.songLists.keys)
        assertEquals(0, model.defaultSongList.songs.size)
        assertEquals(SongsModel.DEFAULT_ID, model.currentListId)
    }

    @Test
    fun aCopyOfAMaximumLengthNameStillFitsTheLimit() {
        val longest = "S".repeat(60)
        val id = model.createList(longest)
        val copy = model.duplicateList(id)!!
        val copyName = model.displayName(copy)
        assertEquals(60, copyName.length)
        assertTrue(copyName.endsWith(" copy"))
        assertNull("a generated name passes the same validation as a typed one", model.validateName(copyName, copy))

        val second = model.duplicateList(id)!!
        val secondName = model.displayName(second)
        assertEquals(60, secondName.length)
        assertTrue(secondName.endsWith(" copy 2"))
    }

    // ==================== Signing in ====================

    @Test
    fun remoteWinsKeepsMySongsAndTheAccountsListsOnly() {
        val kept = model.createList("Saturday show")
        val dropped = model.createList("Made offline")
        val droppedList = model.songLists[dropped]!!
        model.currentListId = dropped

        val survivors = model.localListsAfterRemoteWins(model.songLists, setOf(kept, "unknown-elsewhere"))

        assertEquals(setOf(SongsModel.DEFAULT_ID, kept), survivors.keys)
        assertTrue("a discarded list can never write itself back", droppedList.isDeleted)
        assertFalse(model.songLists[kept]!!.isDeleted)
    }

    @Test
    fun anAccountCreatedByThisSignInIsNew() {
        assertTrue(model.isNewAccount(createdAt = 1_000L, lastSignInAt = 1_800L))
        assertFalse(model.isNewAccount(createdAt = 1_000L, lastSignInAt = 1_000L + 120_000L))
        assertFalse("unknown stamps never count as new", model.isNewAccount(null, 5L))
    }

    // ==================== Addable songs ====================

    @Test
    fun addableSongsOmitsTitleAndKeyMatchesInTheTarget() {
        val target = model.createList("Saturday show")
        model.defaultSongList.addSong(song("Blue Skies"))
        model.defaultSongList.addSong(song("Shenandoah", 3))
        // Same title in a different case, same key: already there.
        model.songLists[target]!!.addSong(song("blue skies"))

        val addable = model.addableSongs(target)
        assertEquals(1, addable.size)
        assertEquals(SongsModel.DEFAULT_ID, addable[0].first.id)
        assertEquals(listOf("Shenandoah"), addable[0].second.map { it.name })
    }

    @Test
    fun addableSongsMatchKeysByValueNotByInstance() {
        // Every song restored from Firestore or preferences carries its own Key instance, and
        // Key defines equals without hashCode, so a match must not depend on identity.
        val target = model.createList("Saturday show")
        val shared = Key.getMajorKeys()[2]
        model.defaultSongList.addSong(song("Blue Skies", 2))
        model.songLists[target]!!.addSong(
            PitchedSong().apply {
                name = "Blue Skies"
                key = Key(shared.note, shared.keyType, shared.numAccidentals)
            },
        )
        assertEquals(emptyList<Any>(), model.addableSongs(target))
    }

    @Test
    fun aDeletedListNeverWritesItselfBack() {
        val id = model.createList("Saturday show")
        val stale = model.songLists[id]!!
        stale.addSong(song("Blue Skies"))
        model.currentListId = id

        model.deleteList(id)
        // An editor that was open on one of its songs saves after the delete landed.
        stale.addSong(song("Shenandoah"))
        stale.name = "Renamed too late"
        stale.storeValue()

        assertTrue(stale.isDeleted)
        assertNull(model.songLists[id])
        assertEquals(SongsModel.DEFAULT_ID, model.currentListId)
    }

    @Test
    fun aSameTitledSongInAnotherKeyIsStillAddable() {
        val target = model.createList("Saturday show")
        model.defaultSongList.addSong(song("Blue Skies", 0))
        model.songLists[target]!!.addSong(song("Blue Skies", 4))
        assertEquals(1, model.addableSongs(target).size)
        assertTrue(model.hasAddableSongs(target))
    }

    @Test
    fun aListWithNothingToOfferIsNotASection() {
        val target = model.createList("Saturday show")
        assertEquals(emptyList<Any>(), model.addableSongs(target))
        assertFalse(model.hasAddableSongs(target))
    }

    @Test
    fun copySongsAppendsDeepCopiesWithFreshIds() {
        val target = model.createList("Saturday show")
        model.defaultSongList.addSong(song("Blue Skies"))
        model.defaultSongList.addSong(song("Shenandoah", 3))

        val sources = model.defaultSongList.songs.toList()
        model.copySongs(sources, target)

        val copied = model.songLists[target]!!.songs
        assertEquals(listOf("Blue Skies", "Shenandoah"), copied.map { it.name })
        sources.zip(copied).forEach { (left, right) ->
            assertNotEquals(left.id, right.id)
            assertEquals(left.key, right.key)
        }
    }

    // ==================== Tracking ====================

    @Test
    fun creatingAListNotifiesListTrackers() {
        var updates = 0
        com.bindroid.trackable.track({ model.trackLists() }) {
            updates++
            keepTracking
        }
        val before = updates
        model.createList("Saturday show")
        assertTrue("creating a list re-renders the list UI", updates > before)

        val after = updates
        model.renameList(model.orderedLists.last().id, "Sunday show")
        assertTrue("renaming a list re-renders the list UI", updates > after)
    }
}
