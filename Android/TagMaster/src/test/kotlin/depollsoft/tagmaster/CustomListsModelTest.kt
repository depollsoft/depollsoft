package depollsoft.tagmaster

import com.bindroid.trackable.TrackableCollection
import depollsoft.lib.activity.RichApplication
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for CustomListsModel and ListMetadata.
 * Tests custom list CRUD without Firestore dependencies.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CustomListsModelTest {

    @Before
    fun setUp() {
        ListModel.setTestMode(true)
        CustomListsModel.setTestMode(true)

        val app = RuntimeEnvironment.getApplication()
        val contextField = RichApplication::class.java.getDeclaredField("context")
        contextField.isAccessible = true
        contextField.set(null, app)

        CustomListsModel.resetForTesting()
        resetModelInstances()
    }

    // --- ListMetadata tests ---

    @Test
    fun builtInMetadata_favorite_hasCorrectProperties() {
        assertEquals("favorite", ListMetadata.favorite.key)
        assertEquals("Favorites", ListMetadata.favorite.name)
        assertTrue(ListMetadata.favorite.isBuiltIn)
    }

    @Test
    fun builtInMetadata_teachable_hasCorrectProperties() {
        assertEquals("teachable", ListMetadata.teachable.key)
        assertEquals("Teachable Tags", ListMetadata.teachable.name)
        assertTrue(ListMetadata.teachable.isBuiltIn)
    }

    // --- allLists / customLists ---

    @Test
    fun allLists_noCustomLists_returnsOnlyBuiltIn() {
        val lists = CustomListsModel.allLists()
        assertEquals(2, lists.size)
        assertEquals("favorite", lists[0].key)
        assertEquals("teachable", lists[1].key)
    }

    @Test
    fun customLists_noCustomLists_returnsEmpty() {
        assertTrue(CustomListsModel.customLists().isEmpty())
    }

    // --- createList ---

    @Test
    fun createList_addsCustomList() {
        val key = CustomListsModel.createList("Contest Tags")

        val custom = CustomListsModel.customLists()
        assertEquals(1, custom.size)
        assertEquals(key, custom[0].key)
        assertEquals("Contest Tags", custom[0].name)
        assertFalse(custom[0].isBuiltIn)
    }

    @Test
    fun createList_generatesUniqueKeys() {
        val key1 = CustomListsModel.createList("List 1")
        val key2 = CustomListsModel.createList("List 2")

        assertNotEquals(key1, key2)
    }

    @Test
    fun createList_appearsInAllLists() {
        CustomListsModel.createList("My List")

        val all = CustomListsModel.allLists()
        assertEquals(3, all.size)
        assertTrue(all[0].isBuiltIn)
        assertTrue(all[1].isBuiltIn)
        assertEquals("My List", all[2].name)
    }

    @Test
    fun createList_multipleListsHaveIncreasingOrder() {
        CustomListsModel.createList("First")
        CustomListsModel.createList("Second")
        CustomListsModel.createList("Third")

        val custom = CustomListsModel.customLists()
        assertEquals(3, custom.size)
        assertTrue(custom[0].order < custom[1].order)
        assertTrue(custom[1].order < custom[2].order)
    }

    @Test
    fun createList_doesNotAffectFavorites() {
        val favModel = ListModel("favorite")
        favModel.ids = TrackableCollection(mutableListOf(42, 100))

        CustomListsModel.createList("New List")

        assertTrue(favModel.ids.contains(42))
        assertTrue(favModel.ids.contains(100))
        assertEquals(2, favModel.ids.size)
    }

    @Test
    fun createList_doesNotAffectTeachable() {
        val teachModel = ListModel("teachable")
        teachModel.ids = TrackableCollection(mutableListOf(55))

        CustomListsModel.createList("New List")

        assertTrue(teachModel.ids.contains(55))
        assertEquals(1, teachModel.ids.size)
    }

    // --- renameList ---

    @Test
    fun renameList_updatesName() {
        val key = CustomListsModel.createList("Old Name")

        CustomListsModel.renameList(key, "New Name")

        assertEquals("New Name", CustomListsModel.getMetadata(key)?.name)
    }

    @Test
    fun renameList_builtInKey_noOp() {
        CustomListsModel.renameList("favorite", "Not Favorites")

        assertEquals("Favorites", CustomListsModel.getMetadata("favorite")?.name)
    }

    @Test
    fun renameList_nonExistentKey_noOp() {
        CustomListsModel.renameList("nonexistent", "Name")
        assertNull(CustomListsModel.getMetadata("nonexistent"))
    }

    @Test
    fun renameList_doesNotAffectFavoriteIds() {
        val favModel = ListModel("favorite")
        favModel.ids = TrackableCollection(mutableListOf(42))

        val key = CustomListsModel.createList("My List")
        CustomListsModel.renameList(key, "Renamed")

        assertTrue(favModel.ids.contains(42))
    }

    // --- deleteList ---

    @Test
    fun deleteList_removesFromCustomLists() {
        val key = CustomListsModel.createList("To Delete")
        assertEquals(1, CustomListsModel.customLists().size)

        CustomListsModel.deleteList(key)

        assertEquals(0, CustomListsModel.customLists().size)
        assertNull(CustomListsModel.getMetadata(key))
    }

    @Test
    fun deleteList_builtInFavorite_noOp() {
        CustomListsModel.deleteList("favorite")

        assertNotNull(CustomListsModel.getMetadata("favorite"))
        assertEquals(2, CustomListsModel.allLists().size)
    }

    @Test
    fun deleteList_builtInTeachable_noOp() {
        CustomListsModel.deleteList("teachable")

        assertNotNull(CustomListsModel.getMetadata("teachable"))
    }

    @Test
    fun deleteList_doesNotAffectOtherCustomLists() {
        val key1 = CustomListsModel.createList("Keep")
        val key2 = CustomListsModel.createList("Delete")

        CustomListsModel.deleteList(key2)

        assertEquals(1, CustomListsModel.customLists().size)
        assertEquals("Keep", CustomListsModel.getMetadata(key1)?.name)
    }

    @Test
    fun deleteList_doesNotAffectFavoriteIds() {
        val favModel = ListModel("favorite")
        favModel.ids = TrackableCollection(mutableListOf(42, 100))

        val key = CustomListsModel.createList("Temp")
        val tempModel = ListModel(key)
        tempModel.ids = TrackableCollection(mutableListOf(999))

        CustomListsModel.deleteList(key)

        assertTrue(favModel.ids.contains(42))
        assertTrue(favModel.ids.contains(100))
        assertEquals(2, favModel.ids.size)
    }

    @Test
    fun deleteList_doesNotAffectTeachableIds() {
        val teachModel = ListModel("teachable")
        teachModel.ids = TrackableCollection(mutableListOf(55))

        val key = CustomListsModel.createList("Temp")
        CustomListsModel.deleteList(key)

        assertTrue(teachModel.ids.contains(55))
    }

    // --- getMetadata ---

    @Test
    fun getMetadata_favorite_returnsBuiltIn() {
        val meta = CustomListsModel.getMetadata("favorite")
        assertNotNull(meta)
        assertEquals("Favorites", meta!!.name)
        assertTrue(meta.isBuiltIn)
    }

    @Test
    fun getMetadata_teachable_returnsBuiltIn() {
        val meta = CustomListsModel.getMetadata("teachable")
        assertNotNull(meta)
        assertEquals("Teachable Tags", meta!!.name)
        assertTrue(meta.isBuiltIn)
    }

    @Test
    fun getMetadata_customKey_returnsCustom() {
        val key = CustomListsModel.createList("Custom")
        val meta = CustomListsModel.getMetadata(key)
        assertNotNull(meta)
        assertEquals("Custom", meta!!.name)
        assertFalse(meta.isBuiltIn)
    }

    @Test
    fun getMetadata_nonExistentKey_returnsNull() {
        assertNull(CustomListsModel.getMetadata("does-not-exist"))
    }

    // --- fromFirestore ---

    @Test
    fun fromFirestore_withListMeta_populatesMetadata() {
        val data = mapOf(
            "list-1" to mapOf("name" to "Contest Tags", "order" to 0L),
            "list-2" to mapOf("name" to "My Quartet", "order" to 1L)
        )

        CustomListsModel.fromFirestore(data)

        val custom = CustomListsModel.customLists()
        assertEquals(2, custom.size)
        assertEquals("Contest Tags", custom[0].name)
        assertEquals("My Quartet", custom[1].name)
    }

    @Test
    fun fromFirestore_null_noOp() {
        CustomListsModel.createList("Existing")
        CustomListsModel.fromFirestore(null)

        // null means old user doc — don't clear existing custom lists
        // Actually fromFirestore(null) returns early, so existing state preserved
        assertEquals(1, CustomListsModel.customLists().size)
    }

    @Test
    fun fromFirestore_emptyMap_clearsCustomLists() {
        CustomListsModel.createList("To Clear")
        assertEquals(1, CustomListsModel.customLists().size)

        CustomListsModel.fromFirestore(emptyMap<String, Any>())

        assertEquals(0, CustomListsModel.customLists().size)
    }

    @Test
    fun fromFirestore_doesNotAffectBuiltInLists() {
        val favModel = ListModel("favorite")
        favModel.ids = TrackableCollection(mutableListOf(42))

        CustomListsModel.fromFirestore(mapOf(
            "custom-1" to mapOf("name" to "New", "order" to 0L)
        ))

        assertEquals("Favorites", CustomListsModel.getMetadata("favorite")?.name)
        assertTrue(favModel.ids.contains(42))
    }

    // --- toFirestoreMap ---

    @Test
    fun toFirestoreMap_emptyWhenNoCustomLists() {
        assertTrue(CustomListsModel.toFirestoreMap().isEmpty())
    }

    @Test
    fun toFirestoreMap_containsCustomListMetadata() {
        val key = CustomListsModel.createList("Test List")

        val map = CustomListsModel.toFirestoreMap()
        assertEquals(1, map.size)
        assertEquals("Test List", map[key]?.get("name"))
    }

    @Test
    fun toFirestoreMap_doesNotContainBuiltInLists() {
        CustomListsModel.createList("Custom")

        val map = CustomListsModel.toFirestoreMap()
        assertNull(map["favorite"])
        assertNull(map["teachable"])
    }

    // --- Helpers ---

    private fun resetModelInstances() {
        try {
            val companion = ListModel::class.java.getDeclaredField("Companion")
            companion.isAccessible = true
            val companionInstance = companion.get(null)
            val modelInstancesField = companionInstance.javaClass.getDeclaredField("modelInstances")
            modelInstancesField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val map = modelInstancesField.get(companionInstance) as MutableMap<String, *>
            map.clear()
        } catch (e: Exception) {
            // May fail in some test configurations
        }
    }
}
