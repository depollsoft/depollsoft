package depollsoft.pitchperfect.lib

import android.media.AudioTrack
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowAudioTrack

/**
 * An AudioTrack that takes every 16-bit write whole. Robolectric's own takes none (it shadows only
 * the byte writes), and StreamingAudioTrack counts only what a write took. Its playback head stays
 * where it is, so everything written stays queued.
 */
@Implements(AudioTrack::class)
class TakingAudioTrackShadow : ShadowAudioTrack() {
    @Suppress("FunctionName", "UNUSED_PARAMETER")
    @Implementation
    protected fun native_write_short(
        audioData: ShortArray,
        offsetInShorts: Int,
        sizeInShorts: Int,
        format: Int,
        isBlocking: Boolean,
    ): Int = sizeInShorts

    @Suppress("FunctionName")
    @Implementation
    override fun native_get_buffer_size_frames(): Int = super.native_get_buffer_size_frames() + extraFrames

    companion object {
        /** Frames every track's platform buffer has beyond the one asked for, as on an output that needs more. */
        @Volatile var extraFrames = 0
    }
}
