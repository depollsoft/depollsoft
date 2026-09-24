package depollsoft.tagmaster

import depollsoft.lib.state.StateList
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.barbershop.TagCollection
import depollsoft.tagmaster.barbershop.TagSortOptions
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for QueryModel.
 * Tests the trackable properties and query state management.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class QueryModelTest {

    private lateinit var queryModel: QueryModel

    @Before
    fun setUp() {
        queryModel = QueryModel()
    }

    @Test
    fun maxResults_defaultValue_is50() {
        assertEquals(50, queryModel.maxResults)
    }

    @Test
    fun maxResults_setNewValue_returnsNewValue() {
        queryModel.maxResults = 100
        assertEquals(100, queryModel.maxResults)
    }

    @Test
    fun resultSetSize_defaultValue_is20() {
        assertEquals(20, queryModel.resultSetSize)
    }

    @Test
    fun resultSetSize_setNewValue_returnsNewValue() {
        queryModel.resultSetSize = 50
        assertEquals(50, queryModel.resultSetSize)
    }

    @Test
    fun hasMoreResults_defaultValue_isTrue() {
        assertTrue(queryModel.hasMoreResults)
    }

    @Test
    fun hasMoreResults_setToFalse_returnsFalse() {
        queryModel.hasMoreResults = false
        assertFalse(queryModel.hasMoreResults)
    }

    @Test
    fun isLoading_defaultValue_isFalse() {
        assertFalse(queryModel.isLoading)
    }

    @Test
    fun isLoading_setToTrue_returnsTrue() {
        queryModel.isLoading = true
        assertTrue(queryModel.isLoading)
    }

    @Test
    fun query_defaultValue_isNull() {
        assertNull(queryModel.query)
    }

    @Test
    fun query_setNewValue_returnsNewValue() {
        queryModel.query = "test query"
        assertEquals("test query", queryModel.query)
    }

    @Test
    fun query_setEmptyString_returnsEmptyString() {
        queryModel.query = ""
        assertEquals("", queryModel.query)
    }

    @Test
    fun statusText_defaultValue_isNull() {
        assertNull(queryModel.statusText)
    }

    @Test
    fun statusText_setNewValue_returnsNewValue() {
        queryModel.statusText = "Loading..."
        assertEquals("Loading...", queryModel.statusText)
    }

    @Test
    fun collection_defaultValue_isNull() {
        assertNull(queryModel.collection)
    }

    @Test
    fun collection_setNewValue_returnsNewValue() {
        queryModel.collection = TagCollection.ClassicTags
        assertEquals(TagCollection.ClassicTags, queryModel.collection)
    }

    @Test
    fun parts_defaultValue_isNull() {
        assertNull(queryModel.parts)
    }

    @Test
    fun parts_setNewValue_returnsNewValue() {
        queryModel.parts = 4
        assertEquals(4, queryModel.parts)
    }

    @Test
    fun parts_setZero_returnsZero() {
        queryModel.parts = 0
        assertEquals(0, queryModel.parts)
    }

    @Test
    fun hasLearningTracks_defaultValue_isNull() {
        assertNull(queryModel.hasLearningTracks)
    }

    @Test
    fun hasLearningTracks_setTrue_returnsTrue() {
        queryModel.hasLearningTracks = true
        assertTrue(queryModel.hasLearningTracks!!)
    }

    @Test
    fun hasLearningTracks_setFalse_returnsFalse() {
        queryModel.hasLearningTracks = false
        assertFalse(queryModel.hasLearningTracks!!)
    }

    @Test
    fun hasSheetMusic_defaultValue_isNull() {
        assertNull(queryModel.hasSheetMusic)
    }

    @Test
    fun hasSheetMusic_setTrue_returnsTrue() {
        queryModel.hasSheetMusic = true
        assertTrue(queryModel.hasSheetMusic!!)
    }

    @Test
    fun sortBy_defaultValue_isNull() {
        assertNull(queryModel.sortBy)
    }

    @Test
    fun sortBy_setNewValue_returnsNewValue() {
        queryModel.sortBy = TagSortOptions.Rating
        assertEquals(TagSortOptions.Rating, queryModel.sortBy)
    }

    @Test
    fun minimumRating_defaultValue_isNull() {
        assertNull(queryModel.minimumRating)
    }

    @Test
    fun minimumRating_setNewValue_returnsNewValue() {
        queryModel.minimumRating = 3.5
        assertEquals(3.5, queryModel.minimumRating!!, 0.001)
    }

    @Test
    fun minimumRating_setZero_returnsZero() {
        queryModel.minimumRating = 0.0
        assertEquals(0.0, queryModel.minimumRating!!, 0.001)
    }

    @Test
    fun minimumDownloads_defaultValue_isNull() {
        assertNull(queryModel.minimumDownloads)
    }

    @Test
    fun minimumDownloads_setNewValue_returnsNewValue() {
        queryModel.minimumDownloads = 100
        assertEquals(100, queryModel.minimumDownloads)
    }

    @Test
    fun tags_defaultValue_isEmpty() {
        assertTrue(queryModel.tags.isEmpty())
    }

    @Test
    fun tags_addTag_containsTag() {
        val tag = Tag()
        tag.id = 42
        queryModel.tags.add(tag)
        
        assertEquals(1, queryModel.tags.size)
        assertEquals(42, queryModel.tags[0].id)
    }

    @Test
    fun tags_clearTags_isEmpty() {
        val tag = Tag()
        queryModel.tags.add(tag)
        queryModel.tags.clear()
        
        assertTrue(queryModel.tags.isEmpty())
    }

    @Test
    fun tags_setNewCollection_replacesOldCollection() {
        val tag1 = Tag()
        tag1.id = 1
        queryModel.tags.add(tag1)
        
        val newCollection = StateList<Tag>()
        val tag2 = Tag()
        tag2.id = 2
        newCollection.add(tag2)
        
        queryModel.tags = newCollection
        
        assertEquals(1, queryModel.tags.size)
        assertEquals(2, queryModel.tags[0].id)
    }

    @Test
    fun maxResults_edgeCaseZero_returnsZero() {
        queryModel.maxResults = 0
        assertEquals(0, queryModel.maxResults)
    }

    @Test
    fun maxResults_edgeCaseNegative_returnsNegative() {
        queryModel.maxResults = -1
        assertEquals(-1, queryModel.maxResults)
    }

    @Test
    fun resultSetSize_edgeCaseZero_returnsZero() {
        queryModel.resultSetSize = 0
        assertEquals(0, queryModel.resultSetSize)
    }

    @Test
    fun minimumRating_negativeValue_returnsNegative() {
        queryModel.minimumRating = -1.0
        assertEquals(-1.0, queryModel.minimumRating!!, 0.001)
    }

    @Test
    fun minimumRating_maxValue_returnsMaxValue() {
        queryModel.minimumRating = 5.0
        assertEquals(5.0, queryModel.minimumRating!!, 0.001)
    }

    @Test
    fun minimumDownloads_edgeCaseZero_returnsZero() {
        queryModel.minimumDownloads = 0
        assertEquals(0, queryModel.minimumDownloads)
    }

    @Test
    fun multiplePropertiesSet_allValuesRetained() {
        queryModel.query = "barbershop"
        queryModel.parts = 4
        queryModel.hasLearningTracks = true
        queryModel.hasSheetMusic = true
        queryModel.sortBy = TagSortOptions.Rating
        queryModel.minimumRating = 3.0
        queryModel.minimumDownloads = 50
        queryModel.maxResults = 100
        queryModel.resultSetSize = 25
        queryModel.collection = TagCollection.ClassicTags
        
        assertEquals("barbershop", queryModel.query)
        assertEquals(4, queryModel.parts)
        assertTrue(queryModel.hasLearningTracks!!)
        assertTrue(queryModel.hasSheetMusic!!)
        assertEquals(TagSortOptions.Rating, queryModel.sortBy)
        assertEquals(3.0, queryModel.minimumRating!!, 0.001)
        assertEquals(50, queryModel.minimumDownloads)
        assertEquals(100, queryModel.maxResults)
        assertEquals(25, queryModel.resultSetSize)
        assertEquals(TagCollection.ClassicTags, queryModel.collection)
    }
}
