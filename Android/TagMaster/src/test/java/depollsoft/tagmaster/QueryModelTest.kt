package depollsoft.tagmaster

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.*

import depollsoft.tagmaster.barbershop.TagCollection
import depollsoft.tagmaster.barbershop.TagSortOptions
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.lib.ui.ThreadSwitchContext

@RunWith(RobolectricTestRunner::class)
@Config(sdk = 28)
class QueryModelTest {

    private lateinit var queryModel: QueryModel
    
    @Mock
    private lateinit var mockContext: ThreadSwitchContext

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        queryModel = QueryModel()
    }

    @Test
    fun testQueryModel_InitialState() {
        assertEquals("Default max results should be 50", 50, queryModel.maxResults)
        assertNull("Status text should be null initially", queryModel.statusText)
        assertNull("Collection should be null initially", queryModel.collection)
        assertTrue("Should have more results initially", queryModel.hasMoreResults)
        assertFalse("Should not be loading initially", queryModel.isLoading)
        assertNull("Query should be null initially", queryModel.query)
        assertEquals("Default result set size should be 20", 20, queryModel.resultSetSize)
        assertNull("Parts should be null initially", queryModel.parts)
        assertNull("Has learning tracks should be null initially", queryModel.hasLearningTracks)
        assertNull("Has sheet music should be null initially", queryModel.hasSheetMusic)
        assertNull("Sort by should be null initially", queryModel.sortBy)
        assertNotNull("Tags collection should not be null", queryModel.tags)
        assertTrue("Tags collection should be empty initially", queryModel.tags.isEmpty())
        assertNull("Minimum rating should be null initially", queryModel.minimumRating)
        assertNull("Minimum downloads should be null initially", queryModel.minimumDownloads)
    }

    @Test
    fun testSetMaxResults_UpdatesValue() {
        queryModel.maxResults = 100
        assertEquals("Max results should be updated", 100, queryModel.maxResults)
    }

    @Test
    fun testSetStatusText_UpdatesValue() {
        queryModel.statusText = "Loading..."
        assertEquals("Status text should be updated", "Loading...", queryModel.statusText)
    }

    @Test
    fun testSetCollection_UpdatesValue() {
        val collection = TagCollection.AllSongs
        queryModel.collection = collection
        assertEquals("Collection should be updated", collection, queryModel.collection)
    }

    @Test
    fun testSetHasMoreResults_UpdatesValue() {
        queryModel.hasMoreResults = false
        assertFalse("Has more results should be updated", queryModel.hasMoreResults)
    }

    @Test
    fun testSetIsLoading_UpdatesValue() {
        queryModel.isLoading = true
        assertTrue("Is loading should be updated", queryModel.isLoading)
    }

    @Test
    fun testSetQuery_UpdatesValue() {
        queryModel.query = "test query"
        assertEquals("Query should be updated", "test query", queryModel.query)
    }

    @Test
    fun testSetResultSetSize_UpdatesValue() {
        queryModel.resultSetSize = 50
        assertEquals("Result set size should be updated", 50, queryModel.resultSetSize)
    }

    @Test
    fun testSetParts_UpdatesValue() {
        queryModel.parts = 4
        assertEquals("Parts should be updated", Integer.valueOf(4), queryModel.parts)
    }

    @Test
    fun testSetHasLearningTracks_UpdatesValue() {
        queryModel.hasLearningTracks = true
        assertEquals("Has learning tracks should be updated", true, queryModel.hasLearningTracks)
    }

    @Test
    fun testSetHasSheetMusic_UpdatesValue() {
        queryModel.hasSheetMusic = true
        assertEquals("Has sheet music should be updated", true, queryModel.hasSheetMusic)
    }

    @Test
    fun testSetSortBy_UpdatesValue() {
        queryModel.sortBy = TagSortOptions.Rating
        assertEquals("Sort by should be updated", TagSortOptions.Rating, queryModel.sortBy)
    }

    @Test
    fun testSetMinimumRating_UpdatesValue() {
        queryModel.minimumRating = 4.5
        assertEquals("Minimum rating should be updated", 4.5, queryModel.minimumRating!!, 0.01)
    }

    @Test
    fun testSetMinimumDownloads_UpdatesValue() {
        queryModel.minimumDownloads = 1000
        assertEquals("Minimum downloads should be updated", Integer.valueOf(1000), queryModel.minimumDownloads)
    }

    @Test
    fun testRefresh_ClearsTagsAndResetsState() {
        // Add some tags first
        val mockTag = mock(Tag::class.java)
        queryModel.tags.add(mockTag)
        
        queryModel.refresh(mockContext)
        
        assertTrue("Tags should be cleared after refresh", queryModel.tags.isEmpty())
    }

    @Test
    fun testFetchResults_WhenAlreadyLoading_DoesNotFetch() {
        queryModel.isLoading = true
        
        queryModel.fetchResults(mockContext)
        
        // Should still be loading and no additional fetch should occur
        assertTrue("Should remain in loading state", queryModel.isLoading)
    }

    @Test
    fun testFetchResults_WhenNoMoreResults_DoesNotFetch() {
        queryModel.hasMoreResults = false
        
        queryModel.fetchResults(mockContext)
        
        assertFalse("Should remain not loading", queryModel.isLoading)
    }

    @Test
    fun testFetchResults_WhenCanFetch_SetsLoadingTrue() {
        queryModel.isLoading = false
        queryModel.hasMoreResults = true
        
        queryModel.fetchResults(mockContext)
        
        assertTrue("Should set loading to true when fetching", queryModel.isLoading)
    }

    @Test
    fun testTrackableProperties_AreTrackable() {
        // Test that properties are properly trackable by setting values
        queryModel.maxResults = 75
        queryModel.statusText = "Test Status"
        queryModel.collection = TagCollection.AllSongs
        queryModel.hasMoreResults = false
        queryModel.isLoading = true
        queryModel.query = "test"
        queryModel.resultSetSize = 30
        queryModel.parts = 3
        queryModel.hasLearningTracks = true
        queryModel.hasSheetMusic = false
        queryModel.sortBy = TagSortOptions.Title
        queryModel.minimumRating = 3.0
        queryModel.minimumDownloads = 500
        
        // Verify all values are set correctly
        assertEquals(75, queryModel.maxResults)
        assertEquals("Test Status", queryModel.statusText)
        assertEquals(TagCollection.AllSongs, queryModel.collection)
        assertFalse(queryModel.hasMoreResults)
        assertTrue(queryModel.isLoading)
        assertEquals("test", queryModel.query)
        assertEquals(30, queryModel.resultSetSize)
        assertEquals(Integer.valueOf(3), queryModel.parts)
        assertEquals(true, queryModel.hasLearningTracks)
        assertEquals(false, queryModel.hasSheetMusic)
        assertEquals(TagSortOptions.Title, queryModel.sortBy)
        assertEquals(3.0, queryModel.minimumRating!!, 0.01)
        assertEquals(Integer.valueOf(500), queryModel.minimumDownloads)
    }
}