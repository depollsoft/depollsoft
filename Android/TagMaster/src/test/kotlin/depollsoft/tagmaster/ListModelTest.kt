package depollsoft.tagmaster

import depollsoft.lib.activity.RichApplication
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Unit tests for ListModel.
 * Tests the list management operations without Firebase dependencies.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ListModelTest {

    @Before
    fun setUp() {
        // Enable test mode to disable Firebase storage
        ListModel.setTestMode(true)
        
        // Initialize RichApplication context for Preferences
        val app = RuntimeEnvironment.getApplication()
        val contextField = RichApplication::class.java.getDeclaredField("context")
        contextField.isAccessible = true
        contextField.set(null, app)
        
        // Reset the singleton instances before each test
        resetModelInstances()
    }

    @Test
    fun add_newId_addsIdToCollection() {
        val model = createTestListModel("test_list")
        model.ids = listOf()
        
        model.add(42)
        
        assertTrue(model.ids.contains(42))
        assertEquals(1, model.ids.size)
    }

    @Test
    fun add_duplicateId_doesNotAddAgain() {
        val model = createTestListModel("test_list_dup")
        model.ids = listOf()
        
        model.add(42)
        model.add(42)
        
        assertEquals(1, model.ids.size)
    }

    @Test
    fun add_multipleIds_addsAllIds() {
        val model = createTestListModel("test_list_multi")
        model.ids = listOf()
        
        model.add(1)
        model.add(2)
        model.add(3)
        
        assertEquals(3, model.ids.size)
        assertTrue(model.ids.contains(1))
        assertTrue(model.ids.contains(2))
        assertTrue(model.ids.contains(3))
    }

    @Test
    fun remove_existingId_removesIdFromCollection() {
        val model = createTestListModel("test_list_remove")
        model.ids = listOf(42, 100)
        
        model.remove(42)
        
        assertFalse(model.ids.contains(42))
        assertTrue(model.ids.contains(100))
        assertEquals(1, model.ids.size)
    }

    @Test
    fun remove_nonExistingId_noEffect() {
        val model = createTestListModel("test_list_remove_none")
        model.ids = listOf(42)
        
        model.remove(999)
        
        assertEquals(1, model.ids.size)
        assertTrue(model.ids.contains(42))
    }

    @Test
    fun contains_existingId_returnsTrue() {
        val model = createTestListModel("test_list_contains")
        model.ids = listOf(42)
        
        assertTrue(model.contains(42))
    }

    @Test
    fun contains_nonExistingId_returnsFalse() {
        val model = createTestListModel("test_list_contains_none")
        model.ids = listOf()
        
        assertFalse(model.contains(999))
    }

    @Test
    fun contains_afterAdd_returnsTrue() {
        val model = createTestListModel("test_list_contains_after")
        model.ids = listOf()
        
        assertFalse(model.contains(42))
        model.add(42)
        assertTrue(model.contains(42))
    }

    @Test
    fun contains_afterRemove_returnsFalse() {
        val model = createTestListModel("test_list_contains_after_remove")
        model.ids = listOf(42)
        
        assertTrue(model.contains(42))
        model.remove(42)
        assertFalse(model.contains(42))
    }

    @Test
    fun canMoveUp_firstElement_returnsFalse() {
        val model = createTestListModel("test_list_moveup_first")
        model.ids = listOf(1, 2, 3)
        
        assertFalse(model.canMoveUp(1))
    }

    @Test
    fun canMoveUp_middleElement_returnsTrue() {
        val model = createTestListModel("test_list_moveup_middle")
        model.ids = listOf(1, 2, 3)
        
        assertTrue(model.canMoveUp(2))
    }

    @Test
    fun canMoveUp_lastElement_returnsTrue() {
        val model = createTestListModel("test_list_moveup_last")
        model.ids = listOf(1, 2, 3)
        
        assertTrue(model.canMoveUp(3))
    }

    @Test
    fun canMoveDown_firstElement_returnsTrue() {
        val model = createTestListModel("test_list_movedown_first")
        model.ids = listOf(1, 2, 3)
        
        assertTrue(model.canMoveDown(1))
    }

    @Test
    fun canMoveDown_middleElement_returnsTrue() {
        val model = createTestListModel("test_list_movedown_middle")
        model.ids = listOf(1, 2, 3)
        
        assertTrue(model.canMoveDown(2))
    }

    @Test
    fun canMoveDown_lastElement_returnsFalse() {
        val model = createTestListModel("test_list_movedown_last")
        model.ids = listOf(1, 2, 3)
        
        assertFalse(model.canMoveDown(3))
    }

    @Test
    fun canMoveUp_singleElement_returnsFalse() {
        val model = createTestListModel("test_list_moveup_single")
        model.ids = listOf(1)
        
        assertFalse(model.canMoveUp(1))
    }

    @Test
    fun canMoveDown_singleElement_returnsFalse() {
        val model = createTestListModel("test_list_movedown_single")
        model.ids = listOf(1)
        
        assertFalse(model.canMoveDown(1))
    }

    @Test
    fun reset_clearsAllIds() {
        val model = createTestListModel("test_list_reset")
        model.ids = listOf(1, 2, 3)
        
        model.reset()
        
        assertTrue(model.ids.isEmpty())
    }

    @Test
    fun reset_emptyList_noError() {
        val model = createTestListModel("test_list_reset_empty")
        model.ids = listOf()
        
        model.reset()
        
        assertTrue(model.ids.isEmpty())
    }

    @Test
    fun moveUp_middleElement_movesElementUp() {
        val model = createTestListModel("test_list_moveup_action")
        model.ids = listOf(1, 2, 3)
        
        model.moveUp(2)
        
        assertEquals(0, model.ids.indexOf(2))
        assertEquals(1, model.ids.indexOf(1))
        assertEquals(2, model.ids.indexOf(3))
    }

    @Test
    fun moveDown_middleElement_movesElementDown() {
        val model = createTestListModel("test_list_movedown_action")
        model.ids = listOf(1, 2, 3)
        
        model.moveDown(2)
        
        assertEquals(0, model.ids.indexOf(1))
        assertEquals(2, model.ids.indexOf(2))
        assertEquals(1, model.ids.indexOf(3))
    }

    @Test
    fun moveUp_lastElement_movesElementUp() {
        val model = createTestListModel("test_list_moveup_last_action")
        model.ids = listOf(1, 2, 3)
        
        model.moveUp(3)
        
        assertEquals(0, model.ids.indexOf(1))
        assertEquals(1, model.ids.indexOf(3))
        assertEquals(2, model.ids.indexOf(2))
    }

    @Test
    fun moveDown_firstElement_movesElementDown() {
        val model = createTestListModel("test_list_movedown_first_action")
        model.ids = listOf(1, 2, 3)
        
        model.moveDown(1)
        
        assertEquals(0, model.ids.indexOf(2))
        assertEquals(1, model.ids.indexOf(1))
        assertEquals(2, model.ids.indexOf(3))
    }

    @Test
    fun listName_returnsCorrectName() {
        val model = createTestListModel("my_custom_list")
        
        assertEquals("my_custom_list", model.listName)
    }

    @Test
    fun add_zeroId_addsZeroToCollection() {
        val model = createTestListModel("test_list_zero")
        model.ids = listOf()
        
        model.add(0)
        
        assertTrue(model.ids.contains(0))
    }

    @Test
    fun add_negativeId_addsNegativeIdToCollection() {
        val model = createTestListModel("test_list_negative")
        model.ids = listOf()
        
        model.add(-1)
        
        assertTrue(model.ids.contains(-1))
    }

    @Test
    fun add_largeId_addsLargeIdToCollection() {
        val model = createTestListModel("test_list_large")
        model.ids = listOf()
        
        model.add(Int.MAX_VALUE)
        
        assertTrue(model.ids.contains(Int.MAX_VALUE))
    }

    /** The model for [listName], from the same factory the app uses. */
    private fun createTestListModel(listName: String): ListModel = ListModel(listName)

    /** Forgets every cached model, so each test starts from a fresh one. */
    private fun resetModelInstances() {
        @Suppress("UNCHECKED_CAST")
        (
            ListModel::class.java
                .getDeclaredField("modelInstances")
                .apply { isAccessible = true }
                .get(null) as MutableMap<String, *>
        ).clear()
    }
}
