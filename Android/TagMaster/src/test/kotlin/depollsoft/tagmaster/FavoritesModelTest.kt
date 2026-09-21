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
import java.lang.reflect.Field

/**
 * Unit tests for FavoritesModel.
 * Tests the facade over ListModel for favorites functionality.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FavoritesModelTest {

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
        
        // Reset the ListModel backing the FavoritesModel
        resetFavoritesModelState()
    }

    @Test
    fun addFavorite_newId_addsFavoriteToList() {
        setFavoriteIds(TrackableCollection())
        
        FavoritesModel.addFavorite(42)
        
        assertTrue(FavoritesModel.favoriteIds.contains(42))
    }

    @Test
    fun addFavorite_duplicateId_doesNotAddDuplicate() {
        setFavoriteIds(TrackableCollection(mutableListOf(42)))
        
        FavoritesModel.addFavorite(42)
        
        assertEquals(1, FavoritesModel.favoriteIds.size)
    }

    @Test
    fun addFavorite_multipleIds_addsAllFavorites() {
        setFavoriteIds(TrackableCollection())
        
        FavoritesModel.addFavorite(1)
        FavoritesModel.addFavorite(2)
        FavoritesModel.addFavorite(3)
        
        assertEquals(3, FavoritesModel.favoriteIds.size)
        assertTrue(FavoritesModel.favoriteIds.contains(1))
        assertTrue(FavoritesModel.favoriteIds.contains(2))
        assertTrue(FavoritesModel.favoriteIds.contains(3))
    }

    @Test
    fun removeFavorite_existingId_removesFavoriteFromList() {
        setFavoriteIds(TrackableCollection(mutableListOf(42, 100)))
        
        FavoritesModel.removeFavorite(42)
        
        assertFalse(FavoritesModel.favoriteIds.contains(42))
        assertTrue(FavoritesModel.favoriteIds.contains(100))
    }

    @Test
    fun removeFavorite_nonExistingId_noEffect() {
        setFavoriteIds(TrackableCollection(mutableListOf(42)))
        
        FavoritesModel.removeFavorite(999)
        
        assertEquals(1, FavoritesModel.favoriteIds.size)
        assertTrue(FavoritesModel.favoriteIds.contains(42))
    }

    @Test
    fun getIsFavorite_existingId_returnsTrue() {
        setFavoriteIds(TrackableCollection(mutableListOf(42)))
        
        assertTrue(FavoritesModel.getIsFavorite(42))
    }

    @Test
    fun getIsFavorite_nonExistingId_returnsFalse() {
        setFavoriteIds(TrackableCollection())
        
        assertFalse(FavoritesModel.getIsFavorite(999))
    }

    @Test
    fun getIsFavorite_afterAddFavorite_returnsTrue() {
        setFavoriteIds(TrackableCollection())
        
        assertFalse(FavoritesModel.getIsFavorite(42))
        FavoritesModel.addFavorite(42)
        assertTrue(FavoritesModel.getIsFavorite(42))
    }

    @Test
    fun getIsFavorite_afterRemoveFavorite_returnsFalse() {
        setFavoriteIds(TrackableCollection(mutableListOf(42)))
        
        assertTrue(FavoritesModel.getIsFavorite(42))
        FavoritesModel.removeFavorite(42)
        assertFalse(FavoritesModel.getIsFavorite(42))
    }

    @Test
    fun canMoveUp_firstElement_returnsFalse() {
        setFavoriteIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        assertFalse(FavoritesModel.canMoveUp(1))
    }

    @Test
    fun canMoveUp_middleElement_returnsTrue() {
        setFavoriteIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        assertTrue(FavoritesModel.canMoveUp(2))
    }

    @Test
    fun canMoveUp_lastElement_returnsTrue() {
        setFavoriteIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        assertTrue(FavoritesModel.canMoveUp(3))
    }

    @Test
    fun canMoveDown_firstElement_returnsTrue() {
        setFavoriteIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        assertTrue(FavoritesModel.canMoveDown(1))
    }

    @Test
    fun canMoveDown_middleElement_returnsTrue() {
        setFavoriteIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        assertTrue(FavoritesModel.canMoveDown(2))
    }

    @Test
    fun canMoveDown_lastElement_returnsFalse() {
        setFavoriteIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        assertFalse(FavoritesModel.canMoveDown(3))
    }

    @Test
    fun canMoveUp_singleElement_returnsFalse() {
        setFavoriteIds(TrackableCollection(mutableListOf(1)))
        
        assertFalse(FavoritesModel.canMoveUp(1))
    }

    @Test
    fun canMoveDown_singleElement_returnsFalse() {
        setFavoriteIds(TrackableCollection(mutableListOf(1)))
        
        assertFalse(FavoritesModel.canMoveDown(1))
    }

    @Test
    fun resetFavorites_clearsAllFavorites() {
        setFavoriteIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        FavoritesModel.resetFavorites()
        
        assertTrue(FavoritesModel.favoriteIds.isEmpty())
    }

    @Test
    fun resetFavorites_emptyList_noError() {
        setFavoriteIds(TrackableCollection())
        
        FavoritesModel.resetFavorites()
        
        assertTrue(FavoritesModel.favoriteIds.isEmpty())
    }

    @Test
    fun moveUp_middleElement_movesElementUp() {
        setFavoriteIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        FavoritesModel.moveUp(2)
        
        assertEquals(0, FavoritesModel.favoriteIds.indexOf(2))
        assertEquals(1, FavoritesModel.favoriteIds.indexOf(1))
        assertEquals(2, FavoritesModel.favoriteIds.indexOf(3))
    }

    @Test
    fun moveDown_middleElement_movesElementDown() {
        setFavoriteIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        FavoritesModel.moveDown(2)
        
        assertEquals(0, FavoritesModel.favoriteIds.indexOf(1))
        assertEquals(2, FavoritesModel.favoriteIds.indexOf(2))
        assertEquals(1, FavoritesModel.favoriteIds.indexOf(3))
    }

    @Test
    fun addFavorite_zeroId_addsFavorite() {
        setFavoriteIds(TrackableCollection())
        
        FavoritesModel.addFavorite(0)
        
        assertTrue(FavoritesModel.favoriteIds.contains(0))
    }

    @Test
    fun addFavorite_negativeId_addsFavorite() {
        setFavoriteIds(TrackableCollection())
        
        FavoritesModel.addFavorite(-1)
        
        assertTrue(FavoritesModel.favoriteIds.contains(-1))
    }

    @Test
    fun favoriteIds_setNewCollection_replacesOldCollection() {
        setFavoriteIds(TrackableCollection(mutableListOf(1, 2, 3)))
        
        val newCollection = TrackableCollection(mutableListOf(100, 200))
        FavoritesModel.favoriteIds = newCollection
        
        assertEquals(2, FavoritesModel.favoriteIds.size)
        assertTrue(FavoritesModel.favoriteIds.contains(100))
        assertTrue(FavoritesModel.favoriteIds.contains(200))
        assertFalse(FavoritesModel.favoriteIds.contains(1))
    }

    /**
     * Helper method to reset and set favorite ids.
     */
    private fun setFavoriteIds(ids: TrackableCollection<Int>) {
        try {
            // Get the backing ListModel
            val modelField = FavoritesModel::class.java.getDeclaredField("model")
            modelField.isAccessible = true
            val listModel = modelField.get(FavoritesModel) as ListModel
            listModel.ids = ids
        } catch (e: Exception) {
            // Fallback: direct assignment
            FavoritesModel.favoriteIds = ids
        }
    }

    /**
     * Resets the FavoritesModel state.
     */
    private fun resetFavoritesModelState() {
        setFavoriteIds(TrackableCollection())
    }
}
