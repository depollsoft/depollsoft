package depollsoft.lib.json

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Deserialization is safe from several threads at once, with the reflection caches still cold.
 * Tag Master loads a list's tags on background threads while the main thread reads preferences;
 * with plain HashMaps the racing first lookups lost property accessors for the rest of the process,
 * and every object read after that silently lacked those fields (tags with no title or rating, a
 * "Posted" date of today, a missing Double preference that crashed Settings).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JsonSerializerConcurrencyTest {
    class Record {
        var title: String? = null
        var alternativeTitle: String? = null
        var rating: Double? = null
        var posted: Date? = null
        var downloads: Int = 0
        var notes: String? = null
        var arranger: String? = null
        var sungBy: String? = null
        var teacher: String? = null
        var provider: String? = null
        var version: String? = null
        var writtenKey: String? = null
    }

    private fun sample() =
        Record().apply {
            title = "Lone Prairie"
            alternativeTitle = "Out on the Lone Prairie"
            rating = 3.25
            posted = Date(1_229_400_000_000)
            downloads = 111_014
            notes = "notes"
            arranger = "arranger"
            sungBy = "sung by"
            teacher = "teacher"
            provider = "provider"
            version = "version"
            writtenKey = "Bb"
        }

    /** Empties the reflection caches, as a freshly started process has them. */
    private fun coldCaches() {
        for (name in listOf("properties", "revProperties")) {
            val field = JsonSerializer::class.java.getDeclaredField(name).apply { isAccessible = true }
            // A new, small map of the same kind: clear() would keep the grown table, and the lost
            // entries come from threads growing it at once.
            field.set(null, field.get(null).javaClass.getDeclaredConstructor().newInstance())
        }
    }

    @Test
    fun manyThreadsReadingAtOnceOnColdCachesEachGetEveryField() {
        val json = JsonSerializer.serialize(sample()).toString()
        val expected = sample()
        val pool = Executors.newFixedThreadPool(32)
        try {
            repeat(400) { round ->
                coldCaches()
                val start = CountDownLatch(1)
                val results =
                    (0 until 32).map {
                        pool.submit<Record> {
                            start.await()
                            JsonSerializer.deserialize(json) as Record
                        }
                    }
                start.countDown()
                results.forEach { future ->
                    val record = future.get(10, TimeUnit.SECONDS)
                    assertEquals("round $round title", expected.title, record.title)
                    assertEquals("round $round rating", expected.rating, record.rating)
                    assertEquals("round $round posted", expected.posted, record.posted)
                    assertEquals("round $round downloads", expected.downloads, record.downloads)
                    assertEquals("round $round key", expected.writtenKey, record.writtenKey)
                }
            }
        } finally {
            pool.shutdownNow()
        }
    }
}
