package depollsoft.lib.state

import android.os.Looper
import depollsoft.lib.json.JsonSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ObservableStateTest {
    private fun settle() {
        SnapshotNotifications.flush()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun watchStateRunsAgainWhenAFieldItReadChanges() {
        val field = StateField("a")
        val seen = mutableListOf<String>()
        val watch = watchState(read = { field.get() }) { seen += it }
        settle()
        assertEquals("the initial value is not a change", emptyList<String>(), seen)

        field.set("b")
        settle()
        field.set("b")
        settle()
        field.set("c")
        settle()
        assertEquals("an equal value is not a change", listOf("b", "c"), seen)

        watch.stop()
        field.set("d")
        settle()
        assertEquals("a stopped watch stays quiet", listOf("b", "c"), seen)
    }

    @Test
    fun emitInitialDeliversTheCurrentValueFirst() {
        val field = StateField(1)
        val seen = mutableListOf<Int>()
        watchState(emitInitial = true, read = { field.get() }) { seen += it }
        settle()
        assertEquals(listOf(1), seen)
    }

    @Test
    fun severalWritesBeforeTheLooperRunsAreOneChange() {
        val list = StateList<Int>()
        val sizes = mutableListOf<Int>()
        watchState(read = { list.size }) { sizes += it }
        list.add(1)
        list.add(2)
        list.add(3)
        settle()
        assertEquals(listOf(3), sizes)
    }

    @Test
    fun aTransactionIsOneChangeAndReplaceWithSwapsContents() {
        val list = StateList(listOf(1, 2))
        val seen = mutableListOf<List<Int>>()
        watchState(read = { list.snapshot() }) { seen += it }
        list.transaction {
            clear()
            addAll(listOf(3, 4, 5))
        }
        settle()
        list.replaceWith(listOf(9))
        settle()
        assertEquals(listOf(listOf(3, 4, 5), listOf(9)), seen)
    }

    @Test
    fun replacingAListWithItselfOrAViewOfItKeepsTheItems() {
        val list = StateList(listOf(1, 2, 3))
        list.replaceWith(list)
        assertEquals(listOf(1, 2, 3), list.snapshot())
        list.replaceWith(list.subList(1, 3))
        assertEquals(listOf(2, 3), list.snapshot())
    }

    @Test
    fun changeSignalWakesReaders() {
        val signal = ChangeSignal()
        var external = 0
        val seen = mutableListOf<Int>()
        watchState(read = {
            signal.read()
            external
        }) { seen += it }
        external = 5
        signal.changed()
        settle()
        assertEquals(listOf(5), seen)
    }

    @Test
    fun aStoredStateListRoundTripsThroughTheSerializerUnderALegacyAlias() {
        // The apps register StateList under the name the Bindroid collection was stored as, so
        // lists saved by earlier versions keep loading.
        JsonSerializer.registerAlias(StateList::class.java, "LegacyListAliasForTest")
        val legacy = """{"*type":"LegacyListAliasForTest","*items":["a","b"]}"""
        val loaded = JsonSerializer.deserialize(legacy)
        assertTrue(loaded is StateList<*>)
        assertEquals(listOf("a", "b"), loaded)
        assertEquals(
            legacy.replace(" ", ""),
            JsonSerializer.serialize(StateList(listOf("a", "b"))).toString(),
        )
    }
}
