package depollsoft.pitchperfect

import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.PitchedSong
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class)
class SongListFragmentLiveUpdateTest {
    private val songs
        get() = SongsModel.get().defaultSongList.songs

    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        songs.clear()
    }

    @After
    fun tearDown() {
        songs.clear()
        Preferences.setTestMode(false)
    }

    @Test
    fun songAddedAfterViewCreation_notifiesVisibleList() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)
        val fragment = SongListFragment()
        activity.supportFragmentManager
            .beginTransaction()
            .add(container.id, fragment)
            .commitNow()

        val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.songListView)
        var changeNotifications = 0
        recycler.adapter!!.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    changeNotifications++
                }
            },
        )

        val remoteSong = PitchedSong().apply { name = "Synced Song" }
        songs.add(remoteSong)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, recycler.adapter!!.itemCount)
        assertTrue("the visible list is invalidated by a live model update", changeNotifications > 0)
    }
}
