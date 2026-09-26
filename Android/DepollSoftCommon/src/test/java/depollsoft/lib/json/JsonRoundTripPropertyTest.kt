package depollsoft.lib.json

import depollsoft.lib.state.StateList
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Random

/**
 * Stored data survives a round trip whatever it holds: random nested lists of strings (quotes,
 * backslashes, newlines, emoji, empty), numbers at their limits, booleans and nulls, in plain
 * lists, StateLists and beans, written and read back the way Preferences and the tag cache do.
 * Each case is seeded, and a failure names its seed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JsonRoundTripPropertyTest {
    class Record {
        private var name: String? = null
        private var count: Int? = null
        private var total: Long? = null
        private var ratio: Double? = null
        private var flag: Boolean? = null
        private var items: StateList<Any?>? = null

        fun getName() = name

        fun setName(value: String?) {
            name = value
        }

        fun getCount() = count

        fun setCount(value: Int?) {
            count = value
        }

        fun getTotal() = total

        fun setTotal(value: Long?) {
            total = value
        }

        fun getRatio() = ratio

        fun setRatio(value: Double?) {
            ratio = value
        }

        fun getFlag() = flag

        fun setFlag(value: Boolean?) {
            flag = value
        }

        fun getItems() = items

        fun setItems(value: StateList<Any?>?) {
            items = value
        }

        override fun equals(other: Any?) =
            other is Record &&
                name == other.name &&
                count == other.count &&
                total == other.total &&
                ratio == other.ratio &&
                flag == other.flag &&
                items == other.items

        override fun hashCode() = listOf(name, count, total, ratio, flag, items).hashCode()

        override fun toString() = "Record(name=$name, count=$count, total=$total, ratio=$ratio, flag=$flag, items=$items)"
    }

    private val pieces = listOf("", "a", "Heart of My Heart", "quote \" inside", "back\\slash", "line\nbreak", "tab\t", "émigré", "🎵", "{\"*type\":\"x\"}", "null", "*items")

    private fun Random.string(): String = (0 until nextInt(3)).joinToString("") { pieces[nextInt(pieces.size)] }

    private fun Random.number(): Any =
        when (nextInt(6)) {
            0 -> nextInt()
            1 -> listOf(0, -1, Int.MAX_VALUE, Int.MIN_VALUE)[nextInt(4)]
            2 -> nextLong()
            3 -> listOf(Long.MAX_VALUE, Long.MIN_VALUE, 1L shl 40)[nextInt(3)]
            4 -> (nextDouble() - 0.5) * 1e6
            else -> listOf(0.25, -3.5, 1e-9, 4.26)[nextInt(4)]
        }

    private fun Random.value(depth: Int): Any? =
        when (nextInt(if (depth > 2) 4 else 6)) {
            0 -> string()
            1 -> number()
            2 -> nextBoolean()
            3 -> null
            4 -> StateList(List(nextInt(4)) { value(depth + 1) })
            else -> ArrayList(List(nextInt(4)) { value(depth + 1) })
        }

    private fun Random.record(): Record =
        Record().apply {
            setName(if (nextInt(4) == 0) null else string())
            setCount(if (nextInt(4) == 0) null else nextInt())
            setTotal(if (nextInt(4) == 0) null else nextLong())
            setRatio(if (nextInt(4) == 0) null else (nextDouble() - 0.5) * 1000)
            setFlag(if (nextInt(4) == 0) null else nextBoolean())
            setItems(if (nextInt(4) == 0) null else StateList(List(nextInt(5)) { value(1) }))
        }

    private fun roundTrip(value: Any?): Any? = JsonSerializer.deserialize(JsonSerializer.serialize(value).toString())

    @Test
    fun randomListsComeBackEqual() {
        repeat(400) { seed ->
            val random = Random(seed.toLong())
            val list = StateList(List(random.nextInt(8)) { random.value(0) })
            val back = roundTrip(list)
            assertEquals("seed $seed", list, back)
            assertEquals("seed $seed: a StateList comes back a StateList", StateList::class.java, back!!.javaClass)
        }
    }

    @Test
    fun randomBeansComeBackEqual() {
        repeat(300) { seed ->
            val record = Random(1000L + seed).record()
            assertEquals("seed ${1000 + seed}", record, roundTrip(record))
        }
    }

    @Test
    fun writingWhatWasReadGivesTheSameText() {
        repeat(200) { seed ->
            val random = Random(2000L + seed)
            val stored = JsonSerializer.serialize(StateList(List(random.nextInt(6)) { random.value(0) })).toString()
            assertEquals("seed ${2000 + seed}", stored, JsonSerializer.serialize(JsonSerializer.deserialize(stored)).toString())
        }
    }

    @Test
    fun listsStoredUnderALegacyAliasLoadAsStateLists() {
        JsonSerializer.registerAlias(StateList::class.java, "PropertyTestLegacyList")
        repeat(100) { seed ->
            val random = Random(3000L + seed)
            val list = StateList(List(random.nextInt(6)) { random.value(0) })
            val stored = JsonSerializer.serialize(list).toString()
            assertEquals("seed ${3000 + seed}: stored under the alias", true, stored.startsWith("{\"*type\":\"PropertyTestLegacyList\""))
            assertEquals("seed ${3000 + seed}", list, JsonSerializer.deserialize(stored))
        }
    }
}
