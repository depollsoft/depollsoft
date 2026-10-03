package depollsoft.lib.review

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReviewPolicyTest {
    private val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("review_policy_test", Context.MODE_PRIVATE)
    private val zone = TimeZone.getTimeZone("America/Los_Angeles")

    // Noon on 2026-10-01 in Los Angeles.
    private var now = 1_790_881_200_000L
    private val policy = ReviewPolicy(prefs, { now }, { zone })

    @Before fun clear() {
        prefs.edit().clear().commit()
    }

    private fun advanceDays(days: Int) {
        now += days * DAY
    }

    /** Uses the app on [days] consecutive days, starting today, and ends on the last of them. */
    private fun useOnDays(days: Int) {
        repeat(days) { day ->
            if (day > 0) advanceDays(1)
            policy.recordUse()
        }
    }

    private fun eligible(): Boolean {
        useOnDays(ReviewPolicy.MIN_ACTIVE_DAYS)
        advanceDays(ReviewPolicy.MIN_DAYS_SINCE_FIRST_USE)
        return policy.shouldAsk("1.0")
    }

    @Test fun aNewInstallationIsNeverAsked() {
        assertFalse(policy.shouldAsk("1.0"))
        policy.recordUse()
        assertFalse(policy.shouldAsk("1.0"))
    }

    @Test fun asksAfterFiveDaysOfUseSpreadOverTwoWeeks() {
        assertTrue(eligible())
    }

    @Test fun usingTheAppManyTimesInOneDayCountsAsOneDay() {
        repeat(50) { policy.recordUse() }
        assertEquals(1, policy.activeDays)
        advanceDays(30)
        assertFalse(policy.shouldAsk("1.0"))
    }

    @Test fun fourDaysOfUseAreNotEnough() {
        useOnDays(ReviewPolicy.MIN_ACTIVE_DAYS - 1)
        advanceDays(60)
        assertFalse(policy.shouldAsk("1.0"))
    }

    @Test fun fiveDaysOfUseInsideTwoWeeksWaitForTheFortnight() {
        useOnDays(ReviewPolicy.MIN_ACTIVE_DAYS)
        // Day 5 of use is four days after the first; ten more make thirteen.
        advanceDays(ReviewPolicy.MIN_DAYS_SINCE_FIRST_USE - ReviewPolicy.MIN_ACTIVE_DAYS)
        assertFalse(policy.shouldAsk("1.0"))
        advanceDays(1)
        assertTrue(policy.shouldAsk("1.0"))
    }

    @Test fun aNewDayStartsAtLocalMidnight() {
        // 23:30 and 00:30 local are two days of use.
        now = 1_790_922_600_000L // 2026-10-01 23:30 in Los Angeles
        policy.recordUse()
        now += 60 * 60 * 1000L
        policy.recordUse()
        assertEquals(2, policy.activeDays)
    }

    @Test fun neverAsksTwiceInOneVersion() {
        assertTrue(eligible())
        policy.recordAsk("1.0")
        useOnDays(ReviewPolicy.MIN_ACTIVE_DAYS)
        advanceDays(400)
        assertFalse(policy.shouldAsk("1.0"))
        assertTrue(policy.shouldAsk("1.1"))
    }

    @Test fun waitsHalfAYearBetweenAsksAndNeedsFreshDaysOfUse() {
        assertTrue(eligible())
        policy.recordAsk("1.0")
        assertEquals(0, policy.activeDays)
        advanceDays(ReviewPolicy.MIN_DAYS_BETWEEN_ASKS)
        // The wait is over, but the days of use started over at the ask.
        assertFalse(policy.shouldAsk("2.0"))
        useOnDays(ReviewPolicy.MIN_ACTIVE_DAYS)
        assertTrue(policy.shouldAsk("2.0"))
    }

    @Test fun aNewVersionSoonAfterAnAskStillWaits() {
        assertTrue(eligible())
        policy.recordAsk("1.0")
        useOnDays(ReviewPolicy.MIN_ACTIVE_DAYS)
        advanceDays(30)
        assertFalse(policy.shouldAsk("1.1"))
    }

    @Test fun staysQuietForFiveMinutesAfterASound() {
        assertTrue(eligible())
        policy.recordSound()
        assertFalse(policy.shouldAsk("1.0"))
        now += ReviewPolicy.QUIET_MILLIS_AFTER_SOUND - 1
        assertFalse(policy.shouldAsk("1.0"))
        now += 1
        assertTrue(policy.shouldAsk("1.0"))
    }

    @Test fun aClockSetBackAfterASoundKeepsItRecentUntilTheNextSound() {
        assertTrue(eligible())
        policy.recordSound()
        now -= 60 * 60 * 1000L
        assertFalse("a sound ahead of the clock is recent", policy.shouldAsk("1.0"))
        policy.recordSound()
        now += ReviewPolicy.QUIET_MILLIS_AFTER_SOUND
        assertTrue("the next sound records the time afresh", policy.shouldAsk("1.0"))
    }

    @Test fun theCountsSurviveANewPolicyInstance() {
        useOnDays(ReviewPolicy.MIN_ACTIVE_DAYS)
        advanceDays(ReviewPolicy.MIN_DAYS_SINCE_FIRST_USE)
        assertTrue(ReviewPolicy(prefs, { now }, { zone }).shouldAsk("1.0"))
    }

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
    }
}
