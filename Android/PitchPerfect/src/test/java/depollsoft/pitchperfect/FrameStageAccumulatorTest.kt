package depollsoft.pitchperfect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameStageAccumulatorTest {
    private fun stages(vararg pairs: Pair<String, Long>): LongArray {
        val result = LongArray(FrameStageAccumulator.STAGE_NAMES.size)
        for ((name, ms) in pairs) result[FrameStageAccumulator.STAGE_NAMES.indexOf(name)] = ms
        return result
    }

    @Test
    fun emptyAccumulatorDescribesNothing() {
        assertEquals("", FrameStageAccumulator().describe())
    }

    @Test
    fun sumsStagesAndOrdersWorstFramesFirst() {
        val accumulator = FrameStageAccumulator()
        accumulator.add(40L, stages("anim" to 30L, "draw" to 5L))
        accumulator.add(120L, stages("layout" to 90L, "anim" to 20L))

        val description = accumulator.describe()
        assertEquals(2, accumulator.slowFrames)
        assertTrue(description, description.contains("slowStagesMs[layout=90 anim=50 draw=5]"))
        assertTrue(description, description.contains("worst[120ms{layout=90,anim=20}; 40ms{anim=30,draw=5}]"))
    }

    @Test
    fun keepsOnlyTheWorstFrames() {
        val accumulator = FrameStageAccumulator()
        for (ms in listOf(33L, 200L, 50L, 90L, 70L)) accumulator.add(ms, stages("gpu" to ms))

        val description = accumulator.describe()
        assertEquals(5, accumulator.slowFrames)
        assertTrue(description, description.contains("worst[200ms{gpu=200}; 90ms{gpu=90}; 70ms{gpu=70}]"))
    }
}
