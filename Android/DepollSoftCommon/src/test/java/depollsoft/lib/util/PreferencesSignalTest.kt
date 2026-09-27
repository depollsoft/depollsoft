package depollsoft.lib.util

import androidx.compose.runtime.snapshots.Snapshot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Anything that read a preference (a composable, a snapshot flow) hears when it changes, in the
 * in-memory test store as on a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PreferencesSignalTest {
    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
    }

    @After
    fun tearDown() {
        Preferences.setTestMode(false)
    }

    /** How many applied snapshot changes touched state [read] used. */
    private fun changesSeenBy(
        read: () -> Unit,
        write: () -> Unit,
    ): Int {
        val used = mutableSetOf<Any>()
        Snapshot.observe(readObserver = { used += it }) { read() }
        var seen = 0
        val handle = Snapshot.registerApplyObserver { changed, _ -> if (changed.any { it in used }) seen++ }
        try {
            write()
            Snapshot.sendApplyNotifications()
        } finally {
            handle.dispose()
        }
        return seen
    }

    @Test
    fun aReaderHearsTheKeyItReadChange() {
        Preferences.set("signal.a", "one")
        assertEquals(1, changesSeenBy({ Preferences.get<String>("signal.a") }) { Preferences.set("signal.a", "two") })
    }

    @Test
    fun anotherKeyOrAnEqualValueIsNoChange() {
        Preferences.set("signal.a", "one")
        assertEquals(0, changesSeenBy({ Preferences.get<String>("signal.a") }) { Preferences.set("signal.b", "two") })
        assertEquals(0, changesSeenBy({ Preferences.get<String>("signal.a") }) { Preferences.set("signal.a", "one") })
    }

    @Test
    fun removingAValueIsAChange() {
        Preferences.set("signal.a", "one")
        assertEquals(1, changesSeenBy({ Preferences.get<String>("signal.a") }) { Preferences.set("signal.a", null) })
    }

    @Test
    fun theSameCollectionSetAgainIsAChangeBecauseItMayHaveBeenEdited() {
        val list = arrayListOf(1)
        Preferences.set("signal.list", list)
        list += 2
        assertEquals(1, changesSeenBy({ Preferences.get<List<Int>>("signal.list") }) { Preferences.set("signal.list", list) })
    }

    @Test
    fun aReaderOfAMissingKeyHearsItInitialized() {
        assertEquals(1, changesSeenBy({ Preferences.get<String>("signal.init") }) { Preferences.initialize("signal.init", "default") })
        // Initializing a key that has a value changes nothing.
        assertEquals(0, changesSeenBy({ Preferences.get<String>("signal.init") }) { Preferences.initialize("signal.init", "other") })
    }
}
