package depollsoft.tagmaster.barbershop

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Tag entity class.
 * Tests entity behavior and equality logic.
 */
class TagTest {

    @Test
    fun setId_validId_setsIdCorrectly() {
        val tag = Tag()
        tag.id = 42
        assertEquals(42, tag.id)
    }

    @Test
    fun setId_zeroId_setsIdCorrectly() {
        val tag = Tag()
        tag.id = 0
        assertEquals(0, tag.id)
    }

    @Test
    fun setId_negativeId_setsIdCorrectly() {
        val tag = Tag()
        tag.id = -1
        assertEquals(-1, tag.id)
    }

    @Test
    fun setTitle_validTitle_setsTitleCorrectly() {
        val tag = Tag()
        tag.title = "Test Title"
        assertEquals("Test Title", tag.title)
    }

    @Test
    fun setTitle_emptyTitle_setsTitleCorrectly() {
        val tag = Tag()
        tag.title = ""
        assertEquals("", tag.title)
    }

    @Test
    fun setTitle_nullTitle_setsTitleCorrectly() {
        val tag = Tag()
        tag.title = null
        assertNull(tag.title)
    }

    @Test
    fun setParts_validParts_setsPartsCorrectly() {
        val tag = Tag()
        tag.parts = 4
        assertEquals(4, tag.parts)
    }

    @Test
    fun setParts_zeroParts_setsPartsCorrectly() {
        val tag = Tag()
        tag.parts = 0
        assertEquals(0, tag.parts)
    }

    @Test
    fun setRating_validRating_setsRatingCorrectly() {
        val tag = Tag()
        tag.rating = 4.5
        assertEquals(4.5, tag.rating ?: 0.0, 0.001)
    }

    @Test
    fun setRating_zeroRating_setsRatingCorrectly() {
        val tag = Tag()
        tag.rating = 0.0
        assertEquals(0.0, tag.rating ?: -1.0, 0.001)
    }

    @Test
    fun setRating_maxRating_setsRatingCorrectly() {
        val tag = Tag()
        tag.rating = 5.0
        assertEquals(5.0, tag.rating ?: 0.0, 0.001)
    }

    @Test
    fun setDownloadCount_validCount_setsCountCorrectly() {
        val tag = Tag()
        tag.downloadCount = 12345
        assertEquals(12345, tag.downloadCount)
    }

    @Test
    fun setDownloadCount_zeroCount_setsCountCorrectly() {
        val tag = Tag()
        tag.downloadCount = 0
        assertEquals(0, tag.downloadCount)
    }

    @Test
    fun equals_sameId_returnsTrue() {
        val tag1 = Tag()
        tag1.id = 42
        val tag2 = Tag()
        tag2.id = 42
        
        assertTrue(tag1 == tag2)
    }

    @Test
    fun equals_differentId_returnsFalse() {
        val tag1 = Tag()
        tag1.id = 42
        val tag2 = Tag()
        tag2.id = 100
        
        assertFalse(tag1 == tag2)
    }

    @Test
    fun equals_sameIdDifferentTitle_returnsTrue() {
        val tag1 = Tag()
        tag1.id = 42
        tag1.title = "Title 1"
        val tag2 = Tag()
        tag2.id = 42
        tag2.title = "Title 2"
        
        // Equality is based on ID only
        assertTrue(tag1 == tag2)
    }

    @Test
    fun setAlternativeTitle_validTitle_setsTitleCorrectly() {
        val tag = Tag()
        tag.alternativeTitle = "Alt Title"
        assertEquals("Alt Title", tag.alternativeTitle)
    }

    @Test
    fun setVersion_validVersion_setsVersionCorrectly() {
        val tag = Tag()
        tag.version = "1.0"
        assertEquals("1.0", tag.version)
    }

    @Test
    fun setArranger_validArranger_setsArrangerCorrectly() {
        val tag = Tag()
        tag.arranger = "John Doe"
        assertEquals("John Doe", tag.arranger)
    }

    @Test
    fun setLyrics_validLyrics_setsLyricsCorrectly() {
        val tag = Tag()
        tag.lyrics = "La la la"
        assertEquals("La la la", tag.lyrics)
    }

    @Test
    fun setNotes_validNotes_setsNotesCorrectly() {
        val tag = Tag()
        tag.notes = "Some notes"
        assertEquals("Some notes", tag.notes)
    }

    @Test
    fun setClassicTagNumber_validNumber_setsNumberCorrectly() {
        val tag = Tag()
        tag.classicTagNumber = 9
        assertEquals(9, tag.classicTagNumber)
    }

    @Test
    fun setClassicTagNumber_null_setsNullCorrectly() {
        val tag = Tag()
        tag.classicTagNumber = null
        assertNull(tag.classicTagNumber)
    }

    @Test
    fun setTagType_validType_setsTypeCorrectly() {
        val tag = Tag()
        tag.tagType = "Tag"
        assertEquals("Tag", tag.tagType)
    }

    @Test
    fun setRecordingMethod_validMethod_setsMethodCorrectly() {
        val tag = Tag()
        tag.recordingMethod = "Studio"
        assertEquals("Studio", tag.recordingMethod)
    }

    @Test
    fun setSungBy_validSungBy_setsSungByCorrectly() {
        val tag = Tag()
        tag.sungBy = "Group Name"
        assertEquals("Group Name", tag.sungBy)
    }

    @Test
    fun setTeacher_validTeacher_setsTeacherCorrectly() {
        val tag = Tag()
        tag.teacher = "Teacher Name"
        assertEquals("Teacher Name", tag.teacher)
    }

    @Test
    fun setProvider_validProvider_setsProviderCorrectly() {
        val tag = Tag()
        tag.provider = "Provider Name"
        assertEquals("Provider Name", tag.provider)
    }

    @Test
    fun setLearningTrackQuartet_validQuartet_setsQuartetCorrectly() {
        val tag = Tag()
        tag.learningTrackQuartet = "Quartet Name"
        assertEquals("Quartet Name", tag.learningTrackQuartet)
    }

    @Test
    fun setYearArranged_validYear_setsYearCorrectly() {
        val tag = Tag()
        tag.yearArranged = "1999"
        assertEquals("1999", tag.yearArranged)
    }

    @Test
    fun setSungYear_validYear_setsYearCorrectly() {
        val tag = Tag()
        tag.sungYear = "2020"
        assertEquals("2020", tag.sungYear)
    }

    @Test
    fun setTeachingVideo_validVideo_setsVideoCorrectly() {
        val tag = Tag()
        tag.teachingVideo = "video_url"
        assertEquals("video_url", tag.teachingVideo)
    }

    @Test
    fun setArrangerWebsite_validWebsite_setsWebsiteCorrectly() {
        val tag = Tag()
        tag.arrangerWebsite = "http://example.com"
        assertEquals("http://example.com", tag.arrangerWebsite)
    }

    @Test
    fun setSungByWebsite_validWebsite_setsWebsiteCorrectly() {
        val tag = Tag()
        tag.sungByWebsite = "http://example.com"
        assertEquals("http://example.com", tag.sungByWebsite)
    }

    @Test
    fun setTeacherWebsite_validWebsite_setsWebsiteCorrectly() {
        val tag = Tag()
        tag.teacherWebsite = "http://example.com"
        assertEquals("http://example.com", tag.teacherWebsite)
    }

    @Test
    fun setProviderWebsite_validWebsite_setsWebsiteCorrectly() {
        val tag = Tag()
        tag.providerWebsite = "http://example.com"
        assertEquals("http://example.com", tag.providerWebsite)
    }

    @Test
    fun setLearningTrackQuartetWebsite_validWebsite_setsWebsiteCorrectly() {
        val tag = Tag()
        tag.learningTrackQuartetWebsite = "http://example.com"
        assertEquals("http://example.com", tag.learningTrackQuartetWebsite)
    }

    @Test
    fun tracks_derivedFromTrackUris_containsTracks() {
        val tag = Tag()
        val allPartsLocation = RemoteLocation().apply { uri = "http://example.com/allparts.mp3" }
        val tenorLocation = RemoteLocation().apply { uri = "http://example.com/tenor.mp3" }
        tag.allPartsTrackUri = allPartsLocation
        tag.tenorTrackUri = tenorLocation
        // tracks property is derived from track URIs
        val tracks = tag.tracks
        assertNotNull(tracks)
        assertEquals(2, tracks?.size)
    }
}
