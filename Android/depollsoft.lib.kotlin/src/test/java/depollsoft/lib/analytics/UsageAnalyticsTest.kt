package depollsoft.lib.analytics

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UsageAnalyticsTest {
    private val events = mutableListOf<Pair<String, Map<String, String>>>()
    private val properties = mutableMapOf<String, String>()

    @Before fun record() {
        UsageAnalytics.sink =
            object : UsageAnalytics.Sink {
                override fun logEvent(name: String, params: Map<String, String>) {
                    events += name to params
                }

                override fun setUserProperty(name: String, value: String) {
                    properties[name] = value
                }
            }
    }

    @After fun reset() = UsageAnalytics.resetForTesting()

    @Test fun aScreenViewNamesTheScreenAsBothNameAndClass() {
        UsageAnalytics.screen("songs")
        assertEquals(listOf("screen_view" to mapOf("screen_name" to "songs", "screen_class" to "songs")), events)
    }

    @Test fun eventsCarryTheirParameters() {
        UsageAnalytics.event("pitch_played", "source" to "keys")
        assertEquals(listOf("pitch_played" to mapOf("source" to "keys")), events)
    }

    @Test fun signInReportsTheProviderAsAMethodWord() {
        listOf("google.com", "apple.com", "facebook.com", "password", "emailLink", "phone", "github.com", null)
            .forEach(UsageAnalytics::login)
        assertEquals(
            listOf("google", "apple", "facebook", "email", "email", "phone", "other", "other"),
            events.map { it.second.getValue("method") },
        )
        assertEquals(setOf("login"), events.map { it.first }.toSet())
    }

    @Test fun signedInIsAYesOrNoProperty() {
        UsageAnalytics.signedIn(true)
        assertEquals("yes", properties["signed_in"])
        UsageAnalytics.signedIn(false)
        assertEquals("no", properties["signed_in"])
    }

    @Test fun nothingIsSentBeforeTheAppSetsASink() {
        UsageAnalytics.resetForTesting()
        UsageAnalytics.event("pitch_played")
        assertEquals(emptyList<Pair<String, Map<String, String>>>(), events)
    }
}
