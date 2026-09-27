package depollsoft.tagmaster.ui.detail

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.barbershop.RemoteLocation
import java.io.FileInputStream
import java.util.Locale
import kotlin.math.max

/**
 * Plays one learning track: downloads it through the content cache, prepares it off the UI
 * thread, and reports position, length and balance as snapshot state for the transport controls.
 * A new track, Stop, or leaving the screen invalidates any download still in flight.
 */
@Stable
class TrackPlayer(
    context: Context,
    private val onFailed: () -> Unit,
) {
    private val cache = ContentCache(context)
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var prepared by mutableStateOf(false)
    private var changedSinceLastPlay = true
    private var loadGeneration = 0
    private var released = false

    var location: RemoteLocation? by mutableStateOf(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var position by mutableIntStateOf(0)
        private set

    var length by mutableIntStateOf(0)
        private set

    /** 0 (all left) to 1000 (all right). */
    var balance by mutableIntStateOf(500)
        private set

    val isPrepared: Boolean get() = prepared

    val positionText: String
        get() = String.format(Locale.US, "%1.1f/%1.1fs", position / 1000.0, length / 1000.0)

    private val updatePosition =
        object : Runnable {
            override fun run() {
                val current = player ?: return
                if (released || !prepared || !isPlaying) return
                position = max(0, current.currentPosition)
                handler.postDelayed(this, 250)
            }
        }

    private fun ensurePlayer(): MediaPlayer =
        player ?: MediaPlayer().also { created ->
            player = created
            created.setOnPreparedListener { mp ->
                if (mp !== player || !isLoading || released) return@setOnPreparedListener
                prepared = true
                changedSinceLastPlay = false
                length = max(0, mp.duration)
                applyBalance()
                isLoading = false
                mp.start()
                updatePlaying(true)
            }
            created.setOnCompletionListener { mp ->
                if (mp !== player || !prepared) return@setOnCompletionListener
                position = max(0, mp.duration)
                updatePlaying(false)
            }
            created.setOnErrorListener { mp, _, _ ->
                if (mp === player) loadFailed()
                true
            }
            applyBalance()
        }

    /** Chooses the track to play (null for none); whatever was playing stops. */
    fun select(value: RemoteLocation?) {
        location = value
        stop()
    }

    fun togglePlay() = if (isPlaying) pause() else play()

    fun play() {
        if (released || isLoading) return
        val selected = location ?: return
        val current = ensurePlayer()
        if (!changedSinceLastPlay && prepared) {
            current.start()
            updatePlaying(true)
            return
        }
        val generation = ++loadGeneration
        prepared = false
        isLoading = true
        cache.loadContentPublic(selected.uri, selected.type, false).continueWith { task ->
            handler.post {
                if (generation != loadGeneration || released || player == null) return@post
                if (task.isFaulted || task.isCancelled || task.result == null) {
                    loadFailed()
                    return@post
                }
                try {
                    FileInputStream(task.result).use { stream ->
                        current.reset()
                        current.setDataSource(stream.fd)
                        // Playback begins only in the prepared listener, never on the UI thread's prepare path.
                        current.prepareAsync()
                    }
                } catch (_: Exception) {
                    loadFailed()
                }
            }
            null
        }
    }

    fun pause() {
        val current = player
        if (current != null && prepared && isPlaying) {
            current.pause()
            position = max(0, current.currentPosition)
        }
        updatePlaying(false)
    }

    fun stop() {
        ++loadGeneration
        handler.removeCallbacks(updatePosition)
        prepared = false
        changedSinceLastPlay = true
        player?.reset()
        isLoading = false
        updatePlaying(false)
        position = 0
    }

    fun seekTo(milliseconds: Int) {
        val current = player ?: return
        if (!prepared) return
        current.seekTo(milliseconds)
        position = max(0, milliseconds)
    }

    fun changeBalance(value: Int) {
        balance = value
        applyBalance()
    }

    private fun applyBalance() {
        var right = balance / 1000f
        var left = (1000 - balance) / 1000f
        val loudest = max(left, right)
        left /= loudest
        right /= loudest
        player?.setVolume(left, right)
    }

    private fun updatePlaying(value: Boolean) {
        isPlaying = value
        handler.removeCallbacks(updatePosition)
        if (value) handler.post(updatePosition)
    }

    private fun loadFailed() {
        stop()
        if (!released) onFailed()
    }

    fun release() {
        stop()
        released = true
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
    }
}
