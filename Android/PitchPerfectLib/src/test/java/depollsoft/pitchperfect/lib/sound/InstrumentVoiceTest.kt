package depollsoft.pitchperfect.lib.sound

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** How an instrument note starts off the UI thread, stops early, and falls back when it can't sound. */
@RunWith(RobolectricTestRunner::class) // for org.json in the tuning table
class InstrumentVoiceTest {
    /** Starts notes only when [run] is called, as the loader thread would later. */
    private class QueuedStarter : InstrumentStarter {
        val queue = ArrayDeque<() -> Unit>()
        val stopped = mutableListOf<Long>()
        val plans = mutableListOf<InstrumentNotePlan>()
        var nextId = 1L
        var fails = false

        override fun start(
            plan: () -> InstrumentNotePlan,
            onStarted: (Long) -> Unit,
            onFailed: () -> Unit,
        ) {
            queue.addLast {
                plans += plan()
                if (fails) onFailed() else onStarted(nextId++)
            }
        }

        override fun stop(note: Long) {
            stopped += note
        }

        fun run() {
            while (queue.isNotEmpty()) queue.removeFirst()()
        }
    }

    private class FakeVoice : SoundingNote {
        var plays = 0
        var stops = 0
        override var isSounding = false

        override fun play() {
            plays++
            isSounding = true
        }

        override fun stop() {
            stops++
            isSounding = false
        }
    }

    private val plan = InstrumentNotePlan(program = 0, key = 60, pitchCents = 0.0, gainDb = 0.0)
    private val starter = QueuedStarter()
    private val fallback = FakeVoice()
    private var fallbacksMade = 0
    private val voice =
        InstrumentVoice({ plan }, {
            fallbacksMade++
            fallback
        }, starter)

    @After
    fun restorePlayer() {
        InstrumentPlayer.loader = Executors.newSingleThreadExecutor()
        NoteVoices.instrumentTuning = InstrumentTuning.NONE
    }

    @Test
    fun aFallbackThatCantPlayLetsTheNoteGoAndSaysSoInsteadOfThrowingOnTheLoader() {
        // Neither the instrument nor the original voice could get an audio track.
        val noTrack =
            object : SoundingNote {
                override val isSounding = false

                override fun play(): Unit = throw IllegalStateException("play() called on uninitialized AudioTrack.")

                override fun stop() {}
            }
        var silenced = 0
        val stuck = InstrumentVoice({ plan }, { noTrack }, starter, onSilent = { silenced++ })
        starter.fails = true
        stuck.play()

        starter.run() // the loader thread; must not throw

        assertFalse("the note isn't held silent", stuck.isSounding)
        assertEquals("the owner hears so it can unlight the note", 1, silenced)
        starter.fails = false
        stuck.play()
        starter.run()
        assertTrue("the next press tries the instrument again", stuck.isSounding)
        assertEquals(2, starter.plans.size)
    }

    @Test
    fun playDoesntStartTheNoteOnTheCallingThread() {
        voice.play()
        assertTrue(voice.isSounding)
        assertTrue(starter.plans.isEmpty())
        starter.run()
        assertEquals(listOf(plan), starter.plans)
        voice.stop()
        assertEquals(listOf(1L), starter.stopped)
        assertFalse(voice.isSounding)
    }

    @Test
    fun aTapReleasedBeforeTheNoteStartsStillStartsItThenReleasesIt() {
        voice.play()
        voice.stop()
        assertTrue(starter.stopped.isEmpty())
        starter.run()
        // Started, then released at once: the engine's minimum length keeps the tap audible.
        assertEquals(listOf(1L), starter.stopped)
        assertFalse(voice.isSounding)
        assertEquals(0, fallbacksMade)
    }

    @Test
    fun anInstrumentThatCantSoundPlaysTheOriginalVoiceInstead() {
        starter.fails = true
        voice.play()
        starter.run()
        assertEquals(1, fallback.plays)
        assertTrue(voice.isSounding)
        voice.stop()
        assertEquals(1, fallback.stops)
        assertFalse(voice.isSounding)
        assertTrue(starter.stopped.isEmpty())
    }

    @Test
    fun aFailureAfterTheTapEndedSoundsNothing() {
        starter.fails = true
        voice.play()
        voice.stop()
        starter.run()
        assertEquals(0, fallbacksMade)
        assertFalse(voice.isSounding)
    }

    @Test
    fun thePlayerReportsFailureWhenThereIsNoBank() {
        val queued = mutableListOf<Runnable>()
        InstrumentPlayer.loader = Executor { queued += it }
        InstrumentPlayer.context = null
        var failed = false
        var started = false
        InstrumentPlayer.start({ plan }, { started = true }, { failed = true })
        // Nothing happens on the calling thread: the bank is read on the loader.
        assertFalse(failed || started)
        queued.forEach { it.run() }
        assertTrue(failed)
        assertFalse(started)
    }

    @Test
    fun anInstrumentNoteReadsTheTuningTableWhenItStartsNotWhenItsMade() {
        NoteVoices.instrumentTuning = InstrumentTuning.NONE
        val note = NoteVoices.create(NoteSound.CHOIR, 261.6255653, 440.0) as InstrumentVoice
        // The table arrives after the note was made but before it starts, as at launch.
        NoteVoices.instrumentTuning = InstrumentTuning(24, mapOf(52 to InstrumentTuning.Program(3.0, List(84) { 7.5 })))
        val planned = note.plan()
        assertEquals(52, planned.program)
        assertEquals(7.5, planned.pitchCents, 1e-9)
        assertEquals(3.0, planned.gainDb, 1e-9)
    }
}
