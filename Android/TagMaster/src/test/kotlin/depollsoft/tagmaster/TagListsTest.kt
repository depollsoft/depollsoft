package depollsoft.tagmaster

import android.app.Application
import androidx.compose.runtime.snapshots.Snapshot
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.json.JsonSerializer
import depollsoft.lib.state.SnapshotNotifications
import depollsoft.lib.state.watchState
import depollsoft.lib.util.Preferences
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The tag-list registry on its own: naming rules, key generation, ordering, the cloud shape and
 * what survives a trip through [Preferences].
 *
 * Every test starts from an empty registry. Firestore writes are off ([ListModel.setTestMode]) so
 * nothing here needs — or can reach — a FirebaseApp.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TagListsTest {
    @Before
    fun setUp() {
        RichApplication.setAppContextForTesting(RuntimeEnvironment.getApplication())
        ListModel.setTestMode(true)
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        clearStoredLists()
        TagLists.resetForTest()
    }

    @After
    fun tearDown() {
        clearStoredLists()
        TagLists.resetForTest()
        Preferences.clearTestValues()
    }

    @Test
    fun theFirstReadWorksInsideAReadOnlySnapshot() {
        val key = TagLists.create("Afterglow set")
        // A new process: the registry is read again on first use, here inside a snapshotFlow-style
        // read-only snapshot, where writing state would throw.
        TagLists.resetForTest()
        val snapshot = Snapshot.takeSnapshot()
        val keys =
            try {
                snapshot.enter { TagLists.customKeys.toList() }
            } finally {
                snapshot.dispose()
            }
        assertEquals(listOf(key), keys)
        assertEquals("Afterglow set", TagLists.name(key))
    }

    // MARK: - Names

    @Test
    fun normalizeName_trimsAndCollapsesWhitespace() {
        assertEquals("Afterglow set", TagLists.normalizeName("  Afterglow   set \n"))
        assertEquals("", TagLists.normalizeName("   \t "))
    }

    @Test
    fun validateName_acceptsAnOrdinaryName() {
        assertNull(TagLists.validateName("Afterglow set"))
        assertNull(TagLists.validateName("x".repeat(TagLists.MAX_NAME_LENGTH)))
    }

    @Test
    fun validateName_rejectsEmptyAndWhitespaceOnly() {
        assertEquals(TagLists.NameError.EMPTY, TagLists.validateName(""))
        assertEquals(TagLists.NameError.EMPTY, TagLists.validateName("   \n "))
    }

    @Test
    fun validateName_rejectsNamesPastTheLimit() {
        assertEquals(
            TagLists.NameError.TOO_LONG,
            TagLists.validateName("x".repeat(TagLists.MAX_NAME_LENGTH + 1)),
        )
    }

    @Test
    fun validateName_rejectsTheBuiltInNamesInAnyCase() {
        for (name in listOf("Favorites", "favorites", "FAVORITE", " teachable tags ", "Teachable")) {
            assertEquals("\"$name\" must be reserved", TagLists.NameError.RESERVED, TagLists.validateName(name))
        }
    }

    @Test
    fun validateName_rejectsDuplicatesIgnoringCaseAndSpacing() {
        TagLists.create("Afterglow set")
        assertEquals(TagLists.NameError.DUPLICATE, TagLists.validateName("afterglow set"))
        assertEquals(TagLists.NameError.DUPLICATE, TagLists.validateName("  AFTERGLOW   SET  "))
        assertNull(TagLists.validateName("Afterglow sets"))
    }

    @Test
    fun validateName_letsAListKeepItsOwnName() {
        val key = TagLists.create("Afterglow set")
        assertNull(TagLists.validateName("Afterglow set", excludingKey = key))
        assertNull(TagLists.validateName("AFTERGLOW SET", excludingKey = key))
        assertEquals(TagLists.NameError.RESERVED, TagLists.validateName("Favorites", excludingKey = key))
    }

    // MARK: - Create / rename / delete

    @Test
    fun create_registersTheListAndReturnsItsKey() {
        val key = TagLists.create("Afterglow set")
        assertTrue(TagLists.isCustom(key))
        assertEquals(listOf(key), TagLists.customKeys.toList())
        assertEquals("Afterglow set", TagLists.name(key))
        assertEquals(listOf(TagLists.FAVORITE, TagLists.TEACHABLE, key), TagLists.allKeys())
    }

    @Test
    fun create_normalizesTheNameItStores() {
        val key = TagLists.create("  Chorus   warmups  ")
        assertEquals("Chorus warmups", TagLists.name(key))
    }

    @Test
    fun create_rejectsAnInvalidName() {
        for (name in listOf("", "   ", "Favorites", "x".repeat(TagLists.MAX_NAME_LENGTH + 1))) {
            val thrown =
                try {
                    TagLists.create(name)
                    null
                } catch (e: IllegalArgumentException) {
                    e
                }
            assertNotEquals("\"$name\" must be rejected", null, thrown)
        }
        assertEquals(emptyList<String>(), TagLists.customKeys.toList())
    }

    @Test
    fun create_bumpsTheVersionAndNotifiesTrackers() {
        val before = TagLists.version
        var notifications = 0
        val watch = watchState(read = { TagLists.version }) { notifications++ }
        TagLists.create("Afterglow set")
        SnapshotNotifications.flush()
        watch.stop()
        assertTrue(TagLists.version > before)
        assertTrue(notifications > 0)
    }

    @Test
    fun rename_keepsTheKeyAndTheTags() {
        val key = TagLists.create("Afterglow set")
        ListModel(key).add(1809)
        TagLists.rename(key, "  Afterglow   list ")
        assertEquals("Afterglow list", TagLists.name(key))
        assertEquals(listOf(key), TagLists.customKeys.toList())
        assertEquals(listOf(1809), ListModel(key).ids.toList())
    }

    @Test
    fun rename_toTheSameNameIsANoOp() {
        val key = TagLists.create("Afterglow set")
        val version = TagLists.version
        TagLists.rename(key, "Afterglow set")
        assertEquals(version, TagLists.version)
    }

    @Test
    fun rename_rejectsInvalidNamesAndTheBuiltInLists() {
        val key = TagLists.create("Afterglow set")
        val other = TagLists.create("Chorus warmups")
        for (name in listOf("", "Chorus warmups", "Teachable Tags")) {
            try {
                TagLists.rename(key, name)
                throw AssertionError("\"$name\" must be rejected")
            } catch (expected: IllegalArgumentException) {
                // expected
            }
        }
        assertEquals("Afterglow set", TagLists.name(key))
        assertEquals("Chorus warmups", TagLists.name(other))
        for (reserved in TagLists.RESERVED) {
            try {
                TagLists.rename(reserved, "Anything")
                throw AssertionError("$reserved must not be renamable")
            } catch (expected: IllegalArgumentException) {
                // expected
            }
        }
    }

    @Test
    fun delete_removesTheListAndItsTags() {
        val key = TagLists.create("Afterglow set")
        val kept = TagLists.create("Chorus warmups")
        ListModel(key).add(1809)
        ListModel(kept).add(42)
        TagLists.delete(key)
        assertEquals(listOf(kept), TagLists.customKeys.toList())
        assertEquals(key, TagLists.name(key))
        assertEquals(emptyList<Int>(), ListModel(key).ids.toList())
        assertEquals(listOf(42), ListModel(kept).ids.toList())
        assertNull(TagLists.validateName("Afterglow set"))
    }

    @Test
    fun delete_rejectsTheBuiltInLists() {
        for (reserved in TagLists.RESERVED) {
            try {
                TagLists.delete(reserved)
                throw AssertionError("$reserved must not be deletable")
            } catch (expected: IllegalArgumentException) {
                // expected
            }
        }
    }

    // MARK: - Ordering

    @Test
    fun customKeys_followCreationOrder() {
        val keys = listOf("One", "Two", "Three").map(TagLists::create)
        assertEquals(keys, TagLists.customKeys.toList())
    }

    @Test
    fun move_reordersAndReportsWhetherAnythingMoved() {
        val (a, b, c) = listOf("One", "Two", "Three").map(TagLists::create)
        assertTrue(TagLists.move(c, 0))
        assertEquals(listOf(c, a, b), TagLists.customKeys.toList())
        assertFalse(TagLists.move(c, 0))
        assertFalse(TagLists.move(c, 3))
        assertFalse(TagLists.move("no-such-list", 0))
        assertEquals(listOf(c, a, b), TagLists.customKeys.toList())
    }

    @Test
    fun moveUpAndDown_respectTheEnds() {
        val (a, b, c) = listOf("One", "Two", "Three").map(TagLists::create)
        assertFalse(TagLists.canMoveUp(a))
        assertTrue(TagLists.canMoveDown(a))
        assertTrue(TagLists.canMoveUp(c))
        assertFalse(TagLists.canMoveDown(c))
        assertFalse(TagLists.canMoveUp("no-such-list"))
        assertFalse(TagLists.canMoveDown("no-such-list"))
        assertFalse(TagLists.moveUp(a))
        assertFalse(TagLists.moveDown(c))
        assertTrue(TagLists.moveDown(a))
        assertEquals(listOf(b, a, c), TagLists.customKeys.toList())
        assertTrue(TagLists.moveUp(a))
        assertEquals(listOf(a, b, c), TagLists.customKeys.toList())
    }

    @Test
    fun reorder_acceptsOnlyPermutations() {
        val keys = listOf("One", "Two", "Three").map(TagLists::create)
        assertFalse("an unchanged order is not a change", TagLists.reorder(keys))
        assertFalse(TagLists.reorder(keys.take(2)))
        assertFalse(TagLists.reorder(keys + "extra"))
        assertFalse(TagLists.reorder(listOf(keys[0], keys[0], keys[1])))
        assertEquals(keys, TagLists.customKeys.toList())
        assertTrue(TagLists.reorder(keys.reversed()))
        assertEquals(keys.reversed(), TagLists.customKeys.toList())
    }

    @Test
    fun aDragsOrderIsRefusedOnceTheListsHaveBeenReorderedUnderIt() {
        val keys = listOf("One", "Two", "Three").map(TagLists::create)
        // The drag began from keys and would move One to the end; a sync reverses the lists first.
        assertTrue(TagLists.reorder(keys.reversed()))
        assertFalse(TagLists.reorder(baseline = keys, order = listOf(keys[1], keys[2], keys[0])))
        assertEquals("the synced order stands", keys.reversed(), TagLists.customKeys.toList())
        assertTrue(TagLists.reorder(baseline = keys.reversed(), order = keys))
        assertEquals(keys, TagLists.customKeys.toList())
    }

    // MARK: - Keys

    @Test
    fun create_slugsTheNameIntoTheKey() {
        assertTrue(TagLists.create("Afterglow set").matches(Regex("afterglow-set-[a-z0-9]{4}")))
        assertTrue(TagLists.create("Tags to teach — Tuesday!").matches(Regex("tags-to-teach-tuesday-[a-z0-9]{4}")))
        assertTrue(TagLists.create("!!! ??? ...").matches(Regex("list-[a-z0-9]{4}")))
    }

    @Test
    fun create_truncatesLongSlugsAndNeverCollides() {
        val key = TagLists.create("A very long list name that keeps on going and going")
        val slug = key.substringBeforeLast('-')
        assertTrue("slug \"$slug\" is at most 24 characters", slug.length <= 24)
        assertFalse(slug.endsWith("-"))
        // Two different names with the same slug still get distinct keys.
        val first = TagLists.create("Warm ups")
        val second = TagLists.create("Warm-ups")
        assertEquals("warm-ups", first.substringBeforeLast('-'))
        assertEquals("warm-ups", second.substringBeforeLast('-'))
        assertNotEquals(first, second)
    }

    // MARK: - Legacy data

    @Test
    fun listsWithoutMetadataSurfaceUnderTheirKey() {
        ListModel("legacy-key").ids = listOf(1, 2)
        TagLists.resetForTest()
        assertEquals(listOf("legacy-key"), TagLists.customKeys.toList())
        assertEquals("legacy-key", TagLists.name("legacy-key"))
        assertEquals(
            "legacy-key",
            TagLists.displayName(RuntimeEnvironment.getApplication(), "legacy-key"),
        )
        assertEquals(TagLists.NameError.DUPLICATE, TagLists.validateName("Legacy-Key"))
    }

    @Test
    fun namedListsComeBeforeUnnamedOnes() {
        val named = TagLists.create("Afterglow set")
        ListModel("zzz-legacy").ids = listOf(7)
        ListModel("aaa-legacy").ids = listOf(8)
        TagLists.resetForTest()
        assertEquals(listOf(named, "aaa-legacy", "zzz-legacy"), TagLists.customKeys.toList())
    }

    @Test
    fun displayName_namesTheBuiltInLists() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals("Favorites", TagLists.displayName(context, TagLists.FAVORITE))
        assertEquals("Teachable Tags", TagLists.displayName(context, TagLists.TEACHABLE))
        val key = TagLists.create("Afterglow set")
        assertEquals("Afterglow set", TagLists.displayName(context, key))
    }

    @Test
    fun keysContaining_listsEveryListHoldingTheTagInDisplayOrder() {
        val key = TagLists.create("Afterglow set")
        ListModel(TagLists.FAVORITE).add(1809)
        ListModel(TagLists.TEACHABLE).add(1809)
        ListModel(key).add(1809)
        ListModel(key).add(42)
        assertEquals(listOf(TagLists.FAVORITE, TagLists.TEACHABLE, key), TagLists.keysContaining(1809))
        assertEquals(listOf(key), TagLists.keysContaining(42))
        assertEquals(emptyList<String>(), TagLists.keysContaining(9999))
    }

    // MARK: - Cloud shape

    @Test
    fun remoteInfo_isNameAndZeroBasedOrderPerCustomKey() {
        val first = TagLists.create("Afterglow set")
        val second = TagLists.create("Chorus warmups")
        ListModel(TagLists.FAVORITE).add(1)
        val info = TagLists.remoteInfo()
        assertEquals(setOf(first, second), info.keys)
        assertEquals(mapOf("name" to "Afterglow set", "order" to 0), info[first])
        assertEquals(mapOf("name" to "Chorus warmups", "order" to 1), info[second])
        assertTrue("built-in lists never appear in listInfo", TagLists.RESERVED.none { it in info.keys })
    }

    @Test
    fun remoteInfo_namesAnUnnamedListAfterItsKey() {
        ListModel("legacy-key").ids = listOf(1)
        TagLists.resetForTest()
        assertEquals(mapOf("name" to "legacy-key", "order" to 0), TagLists.remoteInfo()["legacy-key"])
    }

    // ==================== Sign-in snapshots ====================

    @Test
    fun prepareLocalLists_keepsListsForTheSameAccountAndDropsThemForAnotherAccount() {
        val local = TagLists.create("Local only")
        ListModel(local).add(9)
        FavoritesModel.addFavorite(1809)

        // First sign-in on this device: device-only data stays and now belongs to "a".
        assertFalse(ListModel.prepareLocalLists(forUid = "a"))
        assertEquals(listOf(local), TagLists.customKeys.toList())
        // Signing out and back in as the same account keeps everything.
        assertFalse(ListModel.prepareLocalLists(forUid = "a"))
        assertEquals(listOf(9), ListModel(local).ids.toList())

        // A different account: nothing here may leak into it.
        assertTrue(ListModel.prepareLocalLists(forUid = "b"))
        assertTrue(TagLists.customKeys.isEmpty())
        assertTrue(ListModel(local).ids.isEmpty())
        assertTrue(FavoritesModel.favoriteIds.isEmpty())
        assertFalse(ListModel.prepareLocalLists(forUid = "b"))
    }

    @Test
    fun rename_ignoresAListThatNoLongerExists() {
        val key = TagLists.create("Gone soon")
        TagLists.delete(key)
        TagLists.rename(key, "Back again")
        assertTrue(TagLists.customKeys.isEmpty())
        assertEquals(key, TagLists.name(key))
    }

    @Test
    fun applyUserSnapshot_seedsTheAccountOnlyWhenTheServerSaysThereIsNoDocument() {
        val local = TagLists.create("Local only")
        ListModel(local).add(9)
        var seeded = 0

        // A cache miss (an offline start) must not upload this device's lists over the account's.
        ListModel.applyUserSnapshot(exists = false, fromCache = true, lists = null, info = null) { seeded++ }
        assertEquals(0, seeded)
        assertEquals(listOf(local), TagLists.customKeys.toList())
        assertEquals(listOf(9), ListModel(local).ids.toList())

        // The server confirming there is no document means a brand-new account: upload, keep local.
        ListModel.applyUserSnapshot(exists = false, fromCache = false, lists = null, info = null) { seeded++ }
        assertEquals(1, seeded)
        assertEquals(listOf(local), TagLists.customKeys.toList())
        assertEquals(listOf(9), ListModel(local).ids.toList())
    }

    @Test
    fun applyUserSnapshot_existingAccountReplacesEveryLocalListIncludingEmptyBuiltIns() {
        val local = TagLists.create("Local only")
        ListModel(local).add(9)
        FavoritesModel.addFavorite(1809)
        var seeded = 0

        // The account has one custom list and no favorites field (an emptied list has none).
        ListModel.applyUserSnapshot(
            exists = true,
            fromCache = false,
            lists = mapOf("remote-key" to listOf(7L)),
            info = mapOf("remote-key" to mapOf("name" to "Remote", "order" to 0L)),
        ) { seeded++ }

        assertEquals(0, seeded)
        assertEquals(listOf("remote-key"), TagLists.customKeys.toList())
        assertEquals(listOf(7), ListModel("remote-key").ids.toList())
        assertTrue(ListModel(local).ids.isEmpty())
        assertTrue(FavoritesModel.favoriteIds.isEmpty())
    }

    @Test
    fun applyRemote_ordersByOrderThenName() {
        TagLists.create("Local only")
        TagLists.applyRemote(
            mapOf(
                "b-key" to mapOf("name" to "Bravo", "order" to 1L),
                "a-key" to mapOf("name" to "Alpha", "order" to 0L),
                "z-key" to mapOf("name" to "Zulu", "order" to 1L),
                TagLists.FAVORITE to mapOf("name" to "Nope", "order" to 0L),
                7 to mapOf("name" to "Not a key", "order" to 0L),
            ),
            listOf(TagLists.FAVORITE, "a-key", "b-key", "z-key"),
        )
        // order 0 first; the two lists sharing order 1 fall back to their names.
        assertEquals(listOf("a-key", "b-key", "z-key"), TagLists.customKeys.toList())
        assertEquals("Alpha", TagLists.name("a-key"))
        assertEquals("Bravo", TagLists.name("b-key"))
    }

    @Test
    fun applyRemote_replacesTheLocalRegistryAndKeepsRemoteOnlyKeys() {
        val localOnly = TagLists.create("Local only")
        TagLists.applyRemote(
            mapOf("remote-key" to mapOf("name" to "Remote", "order" to 0L)),
            listOf("remote-key", "remote-legacy"),
        )
        assertEquals(listOf("remote-key", "remote-legacy"), TagLists.customKeys.toList())
        assertFalse(localOnly in TagLists.customKeys)
        assertEquals("Remote", TagLists.name("remote-key"))
        assertEquals("remote-legacy", TagLists.name("remote-legacy"))
    }

    @Test
    fun applyRemote_survivesMissingAndMalformedEntries() {
        TagLists.applyRemote(null, emptyList())
        assertEquals(emptyList<String>(), TagLists.customKeys.toList())
        TagLists.applyRemote(
            mapOf(
                "good" to mapOf("name" to "Good", "order" to 0L),
                "no-name" to mapOf("order" to 1L),
                "no-order" to mapOf("name" to "No order"),
                "not-a-map" to "nonsense",
            ),
            listOf("good", "no-name", "no-order"),
        )
        // "not-a-map" never reaches the registry; "no-order" sorts last (no order = last).
        assertEquals(listOf("good", "no-name", "no-order"), TagLists.customKeys.toList())
        assertEquals("no-name", TagLists.name("no-name"))
        assertEquals("No order", TagLists.name("no-order"))
    }

    // MARK: - Persistence

    @Test
    fun preferences_holdTheNamesAndTheOrderTagListsNeedsToReload() {
        val first = TagLists.create("Afterglow set")
        val second = TagLists.create("Chorus warmups")
        TagLists.reorder(listOf(second, first))

        assertEquals(
            mapOf(first to "Afterglow set", second to "Chorus warmups"),
            Preferences.get<Map<*, *>>("tagmaster.listNames"),
        )
        assertEquals(listOf(second, first), Preferences.get<Collection<*>>("tagmaster.listOrder")?.toList())

        TagLists.resetForTest()
        assertEquals(listOf(second, first), TagLists.customKeys.toList())
        assertEquals("Afterglow set", TagLists.name(first))
    }

    @Test
    fun theStoredRegistryRoundTripsThroughJsonSerializer() {
        val first = TagLists.create("Afterglow set")
        val second = TagLists.create("Chorus warmups")
        TagLists.reorder(listOf(second, first))

        // Preferences test mode keeps values in memory, so run the two values TagLists stores
        // through the serializer by hand, exactly as `Preferences.set` would (a Map first becomes
        // a MappingList, which is what actually reaches the JSON).
        val names = Preferences.get<Map<*, *>>("tagmaster.listNames")!!
        val order = Preferences.get<Collection<*>>("tagmaster.listOrder")!!

        val restoredNames = roundTrip(names)
        val restoredOrder = roundTrip(order)

        assertEquals(mapOf(first to "Afterglow set", second to "Chorus warmups"), restoredNames)
        assertEquals(listOf(second, first), (restoredOrder as Collection<*>).toList())

        // What came back out of JSON is enough to rebuild the registry from scratch.
        Preferences.clearTestValues()
        clearStoredLists()
        Preferences.set("tagmaster.listNames", restoredNames)
        Preferences.set("tagmaster.listOrder", restoredOrder)
        TagLists.resetForTest()
        assertEquals(listOf(second, first), TagLists.customKeys.toList())
        assertEquals("Afterglow set", TagLists.name(first))
        assertEquals("Chorus warmups", TagLists.name(second))
    }

    /** Serializes a value the way `Preferences.set` does and reads it back the way `get` does. */
    private fun roundTrip(value: Any): Any {
        val payload =
            if (value is Map<*, *>) {
                Preferences.MappingList().apply {
                    value.forEach { (k, v) ->
                        add(Preferences.Mapping().apply { key = k; setValue(v) })
                    }
                }
            } else {
                value
            }
        val json = JsonSerializer.serialize(payload).toString()
        val restored = JsonSerializer.deserialize(json)
        if (restored !is Preferences.MappingList) return restored
        return restored.associate { it.key to it.value }
    }

    /** Empties the static list cache `ListModel` shares between instances. */
    @Suppress("UNCHECKED_CAST")
    private fun clearStoredLists() {
        // Both are static fields of ListModel itself: Kotlin hoists a companion object's private
        // properties onto the outer class.
        val delegate =
            ListModel::class.java
                .getDeclaredField("preferences\$delegate")
                .apply { isAccessible = true }
                .get(null) as Lazy<MutableMap<String, depollsoft.lib.state.StateList<Int>>>
        // Emptying a collection notifies its ListModel, which removes the key: snapshot first.
        val stored = delegate.value
        val collections = stored.values.toList()
        stored.clear()
        collections.forEach { it.clear() }
        (
            ListModel::class.java
                .getDeclaredField("modelInstances")
                .apply { isAccessible = true }
                .get(null) as MutableMap<String, *>
        ).clear()
    }
}
