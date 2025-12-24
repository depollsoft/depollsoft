package depollsoft.tagmaster.barbershop

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE)
class TagQueryNetworkStubTest {

    companion object {
        private fun xmlForTwoTags(): String = """
            <tags count="2" available="2">
              <tag>
                <id>1</id>
                <Title>One</Title>
                <Downloaded>1,000</Downloaded>
              </tag>
              <tag>
                <id>2</id>
                <Title>Two</Title>
                <Downloaded>2,000</Downloaded>
              </tag>
            </tags>
        """.trimIndent()
    }

    @Test
    fun buildQueryUrl_builds_expected_url_parameters() {
        val url = Tag.buildQueryUrl(
            query = "hello world",
            numberOfResults = 1,
            start = 0,
            parts = 4,
            learning = true,
            sheetMusic = false,
            collection = TagCollection.EasyTags,
            sortBy = TagSortOptions.Title,
            minimumRating = 4.0,
            minimumDownloads = 10,
            fieldList = "id,Title,Downloaded"
        )

        val s = url.toString()
        assertTrue(s.startsWith("https://www.barbershoptags.com/api.php?client=TagMaster&"))
        assertTrue(s.contains("n=1"))
        // start is 0-based in wrapper but +1 in request
        assertTrue(s.contains("start=1"))
        assertTrue(s.contains("q=hello%20world"))
        assertTrue(s.contains("Parts=4"))
        assertTrue(s.contains("Learning=Yes"))
        assertTrue(s.contains("SheetMusic=No"))
        assertTrue(s.contains("Collection=easy"))
        assertTrue(s.contains("Sortby=Title"))
        assertTrue(s.contains("MinRating=4.0"))
        assertTrue(s.contains("MinDownloaded=10"))
        assertTrue(s.contains("fldlist=id,Title,Downloaded"))
    }

    @Test
    fun parseTagQueryResult_parses_download_counts_and_fields() {
        val result = Tag.parseTagQueryResult(
            ByteArrayInputStream(xmlForTwoTags().toByteArray(Charsets.UTF_8)),
            start = 0,
            cache = false
        )

        assertEquals(0, result.start)
        assertEquals(2, result.count)
        assertEquals(2, result.available)
        assertEquals(listOf(1, 2), result.tags.map { it.id })
        assertEquals(listOf("One", "Two"), result.tags.map { it.title })
        assertEquals(listOf(1000, 2000), result.tags.map { it.downloadCount })
    }

    @Test
    fun buildQueryByIdsUrl_encodes_pipe_separator() {
        val url = Tag.buildQueryByIdsUrl(listOf(1, 2))
        val s = url.toString()
        assertTrue(s.startsWith("https://www.barbershoptags.com/api.php?client=TagMaster&"))
        assertTrue(s.contains("id=1%7C2"))
        assertTrue(s.contains("n=2"))
    }

    @Test
    fun parseTagsByIdResponse_parses_tags_into_id_map() {
        val tagsById = Tag.parseTagsByIdResponse(
            ByteArrayInputStream(xmlForTwoTags().toByteArray(Charsets.UTF_8))
        )

        assertEquals(setOf(1, 2), tagsById.keys)
        assertEquals("One", tagsById[1]?.title)
        assertEquals(1000, tagsById[1]?.downloadCount)
        assertEquals("Two", tagsById[2]?.title)
        assertEquals(2000, tagsById[2]?.downloadCount)
    }
}
