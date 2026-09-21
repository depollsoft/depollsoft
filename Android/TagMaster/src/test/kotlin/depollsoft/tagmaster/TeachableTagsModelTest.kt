package depollsoft.tagmaster

import com.bindroid.trackable.TrackableCollection
import depollsoft.lib.activity.RichApplication
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for TeachableTagsModel.
 * Tests the facade over ListModel for teachable tags functionality.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TeachableTagsModelTest {

    @After
    fun tearDown() {
        ListModel.setTestMode(false)
    }

    @Before
    fun setUp() {
        // The wrappers write through to Firestore whenever a user is signed in; there is no
        // FirebaseApp in a unit test, so cloud writes are switched off here.
        ListModel.setTestMode(true)
        // Initialize RichApplication context for Preferences
        val app = RuntimeEnvironment.getApplication()
        val contextField = RichApplication::class.java.getDeclaredField("context")
        contextField.isAccessible = true
        contextField.set(null, app)
        
        // Reset the TeachableTagsModel state
        resetTeachableTagsModelState()
    }

    @Test
    fun addTeachableTag_newId_addsTagToList() {
        setTeachableTagIds(TrackableCollection())
        
        TeachableTagsModel.addTeachableTag(42)
        
        assertTrue(TeachableTagsModel.teachableTagIds.contains(42))
    }

    @Test
    fun addTeachableTag_duplicateId_doesNotAddDuplicate() {
        setTeachableTagIds(TrackableCollection(mutableListOf(42)))
        
        TeachableTagsModel.addTeachableTag(42)
        
        assertEquals(1, TeachableTagsModel.teachableTagIds.size)
    }

    @Test
    fun addTeachableTag_multipleIds_addsAllTags() {
        setTeachableTagIds(TrackableCollection())
        
        TeachableTagsModel.addTeachableTag(1)
        TeachableTagsModel.addTeachableTag(2)
        TeachableTagsModel.addTeachableTag(3)
        
        assertEquals(3, TeachableTagsModel.teachableTagIds.size)
        assertTrue(TeachableTagsModel.teachableTagIds.contains(1))
        assertTrue(TeachableTagsModel.teachableTagIds.contains(2))
        assertTrue(TeachableTagsModel.teachableTagIds.contains(3))
    }

    @Test
    fun removeTeachableTag_existingId_removesTagFromList() {
        setTeachableTagIds(TrackableCollection(mutableListOf(42, 100)))
        
        TeachableTagsModel.removeTeachableTag(42)
        
        assertFalse(TeachableTagsModel.teachableTagIds.contains(42))
        assertTrue(TeachableTagsModel.teachableTagIds.contains(100))
    }

    @Test
    fun removeTeachableTag_nonExistingId_noEffect() {
        setTeachableTagIds(TrackableCollection(mutableListOf(42)))
        
        TeachableTagsModel.removeTeachableTag(999)
        
        assertEquals(1, TeachableTagsModel.teachableTagIds.size)
        assertTrue(TeachableTagsModel.teachableTagIds.contains(42))
    }

    @Test
    fun getIsTeachableTag_existingId_returnsTrue() {
        setTeachableTagIds(TrackableCollection(mutableListOf(42)))
        
        assertTrue(TeachableTagsModel.getIsTeachableTag(42))
    }

    @Test
    fun getIsTeachableTag_nonExistingId_returnsFalse() {
        setTeachableTagIds(TrackableCollection())
        
        assertFalse(TeachableTagsModel.getIsTeachableTag(999))
    }

    @Test
    fun getIsTeachableTag_afterAddTeachableTag_returnsTrue() {
        setTeachableTagIds(TrackableCollection())
        
        assertFalse(TeachableTagsModel.getIsTeachableTag(42))
        TeachableTagsModel.addTeachableTag(42)
        assertTrue(TeachableTagsModel.getIsTeachableTag(42))
    }

    @Test
    fun getIsTeachableTag_afterRemoveTeachableTag_returnsFalse() {
        setTeachableTagIds(TrackableCollection(mutableListOf(42)))
        
        assertTrue(TeachableTagsModel.getIsTeachableTag(42))
        TeachableTagsModel.removeTeachableTag(42)
        assertFalse(TeachableTagsModel.getIsTeachableTag(42))
    }

    @Test
    fun canMoveUp_firstElement_returnsFalse() {
        setTeachableTagIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        assertFalse(TeachableTagsModel.canMoveUp(1))
    }

    @Test
    fun canMoveUp_middleElement_returnsTrue() {
        setTeachableTagIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        assertTrue(TeachableTagsModel.canMoveUp(2))
    }

    @Test
    fun canMoveUp_lastElement_returnsTrue() {
        setTeachableTagIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        assertTrue(TeachableTagsModel.canMoveUp(3))
    }

    @Test
    fun canMoveDown_firstElement_returnsTrue() {
        setTeachableTagIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        assertTrue(TeachableTagsModel.canMoveDown(1))
    }

    @Test
    fun canMoveDown_middleElement_returnsTrue() {
        setTeachableTagIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        assertTrue(TeachableTagsModel.canMoveDown(2))
    }

    @Test
    fun canMoveDown_lastElement_returnsFalse() {
        setTeachableTagIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        assertFalse(TeachableTagsModel.canMoveDown(3))
    }

    @Test
    fun canMoveUp_singleElement_returnsFalse() {
        setTeachableTagIds(TrackableCollection(mutableListOf(1)))
        
        assertFalse(TeachableTagsModel.canMoveUp(1))
    }

    @Test
    fun canMoveDown_singleElement_returnsFalse() {
        setTeachableTagIds(TrackableCollection(mutableListOf(1)))
        
        assertFalse(TeachableTagsModel.canMoveDown(1))
    }

    @Test
    fun resetTeachableTags_clearsAllTags() {
        setTeachableTagIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        TeachableTagsModel.resetTeachableTags()
        
        assertTrue(TeachableTagsModel.teachableTagIds.isEmpty())
    }

    @Test
    fun resetTeachableTags_emptyList_noError() {
        setTeachableTagIds(TrackableCollection())
        
        TeachableTagsModel.resetTeachableTags()
        
        assertTrue(TeachableTagsModel.teachableTagIds.isEmpty())
    }

    @Test
    fun moveUp_middleElement_movesElementUp() {
        setTeachableTagIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        TeachableTagsModel.moveUp(2)
        
        assertEquals(0, TeachableTagsModel.teachableTagIds.indexOf(2))
        assertEquals(1, TeachableTagsModel.teachableTagIds.indexOf(1))
        assertEquals(2, TeachableTagsModel.teachableTagIds.indexOf(3))
    }

    @Test
    fun moveDown_middleElement_movesElementDown() {
        setTeachableTagIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        TeachableTagsModel.moveDown(2)
        
        assertEquals(0, TeachableTagsModel.teachableTagIds.indexOf(1))
        assertEquals(2, TeachableTagsModel.teachableTagIds.indexOf(2))
        assertEquals(1, TeachableTagsModel.teachableTagIds.indexOf(3))
    }

    @Test
    fun moveUp_lastElement_movesElementUp() {
        setTeachableTagIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        TeachableTagsModel.moveUp(3)
        
        assertEquals(0, TeachableTagsModel.teachableTagIds.indexOf(1))
        assertEquals(1, TeachableTagsModel.teachableTagIds.indexOf(3))
        assertEquals(2, TeachableTagsModel.teachableTagIds.indexOf(2))
    }

    @Test
    fun moveDown_firstElement_movesElementDown() {
        setTeachableTagIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        TeachableTagsModel.moveDown(1)
        
        assertEquals(0, TeachableTagsModel.teachableTagIds.indexOf(2))
        assertEquals(1, TeachableTagsModel.teachableTagIds.indexOf(1))
        assertEquals(2, TeachableTagsModel.teachableTagIds.indexOf(3))
    }

    @Test
    fun addTeachableTag_zeroId_addsTag() {
        setTeachableTagIds(TrackableCollection())
        
        TeachableTagsModel.addTeachableTag(0)
        
        assertTrue(TeachableTagsModel.teachableTagIds.contains(0))
    }

    @Test
    fun addTeachableTag_negativeId_addsTag() {
        setTeachableTagIds(TrackableCollection())
        
        TeachableTagsModel.addTeachableTag(-1)
        
        assertTrue(TeachableTagsModel.teachableTagIds.contains(-1))
    }

    @Test
    fun teachableTagIds_setNewCollection_replacesOldCollection() {
        setTeachableTagIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        val newCollection = TrackableCollection(mutableListOf(100, 200))
        TeachableTagsModel.teachableTagIds = newCollection
        
        assertEquals(2, TeachableTagsModel.teachableTagIds.size)
        assertTrue(TeachableTagsModel.teachableTagIds.contains(100))
        assertTrue(TeachableTagsModel.teachableTagIds.contains(200))
        assertFalse(TeachableTagsModel.teachableTagIds.contains(1))
    }

    @Test
    fun addTeachableTag_largeId_addsTag() {
        setTeachableTagIds(TrackableCollection())
        
        TeachableTagsModel.addTeachableTag(Int.MAX_VALUE)
        
        assertTrue(TeachableTagsModel.teachableTagIds.contains(Int.MAX_VALUE))
    }

    /**
     * Helper method to reset and set teachable tag ids.
     */
    private fun setTeachableTagIds(ids: TrackableCollection<Int>) {
        try {
            // Get the backing ListModel
            val modelField = TeachableTagsModel::class.java.getDeclaredField("model")
            modelField.isAccessible = true
            val listModel = modelField.get(TeachableTagsModel) as ListModel
            listModel.ids = ids
        } catch (e: Exception) {
            // Fallback: direct assignment
            TeachableTagsModel.teachableTagIds = ids
        }
    }

    /**
     * Resets the TeachableTagsModel state.
     */
    private fun resetTeachableTagsModelState() {
        setTeachableTagIds(TrackableCollection())
    }
}
