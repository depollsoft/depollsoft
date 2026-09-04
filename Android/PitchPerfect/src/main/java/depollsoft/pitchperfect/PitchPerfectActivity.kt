package depollsoft.pitchperfect

import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import bolts.Task
import com.bindroid.converters.BoolConverter
import com.bindroid.ui.UiBinder
import com.bindroid.utils.uibind
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.wearable.Wearable
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import depollsoft.lib.ui.ChangelogViewer
import depollsoft.lib.util.RunUtils

class PitchPerfectActivity : AppCompatActivity() {
    private lateinit var bottomNavigation: NavigationBarView
    private lateinit var logInDialog: Dialog
    private var optionsMenu: Menu? = null
    private var menuSyncPending = false
    private var selectedPage: Int = 0
    private var songListFragment: SongListFragment? = null
    private var frameMonitor: FramePerformanceMonitor? = null
    private var adRequested = false
    private var adReady = false
    private val adLoadRunnable =
        Runnable {
            if (isDestroyed || !adsShouldShow || adRequested) return@Runnable
            adRequested = true
            val startedAt = SystemClock.elapsedRealtime()
            MobileAds.initialize(applicationContext) {
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    adReady = true
                    loadBanner()
                    PerformanceDiagnostics.logDuration("Ads initialized and requested", startedAt)
                }
            }
        }

    private fun resolveSongListFragment(): SongListFragment? =
        songListFragment
            ?: supportFragmentManager.fragments.filterIsInstance<SongListFragment>().firstOrNull()

    val adsShouldShow: Boolean
        get() = !SettingsModel.areAdsRemoved && !SettingsModel.licensed

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        val startedAt = SystemClock.elapsedRealtime()
        super.onCreate(savedInstanceState)

        logInDialog = LoginPrompt.buildDialog(this, false)

        this.volumeControlStream = AudioManager.STREAM_MUSIC

        this.setContentView(R.layout.pitchperfectview)

        uibind(
            R.id.adContainer,
            "Visibility",
            { (this::adsShouldShow) },
            converter = BoolConverter.get(),
        )
        uibind(
            R.id.removeAds,
            "Visibility",
            { (this::adsShouldShow) },
            converter = BoolConverter.get(),
        )

        findViewById<View>(R.id.removeAds).setOnClickListener {
            PurchaseService.beginRemoveAds(this@PitchPerfectActivity, 666)
        }

        bottomNavigation = findViewById(R.id.bottomNavigation)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        viewPager.offscreenPageLimit = 3
        viewPager.adapter =
            object : FragmentStateAdapter(this) {
                override fun createFragment(position: Int): Fragment =
                    when (position) {
                        0 -> PitchPipeFragment()
                        1 -> NoteListFragment()
                        2 -> KeySignatureFragment()
                        3 -> SongListFragment().also { songListFragment = it }
                        else -> PitchPipeFragment()
                    }

                override fun getItemCount(): Int = 4
            }

        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    if (selectedPage != position) {
                        selectedPage = position
                        // Toolbar actions change only once the page settles, so the
                        // menu update never lands on the first animation frame.
                        if (viewPager.scrollState == ViewPager2.SCROLL_STATE_IDLE) {
                            syncSongMenuItems()
                        } else {
                            menuSyncPending = true
                        }
                    }
                    val itemId =
                        when (position) {
                            0 -> R.id.pitchpipe_item
                            1 -> R.id.notes_item
                            2 -> R.id.keys_item
                            3 -> R.id.songs_item
                            else -> R.id.pitchpipe_item
                        }
                    if (bottomNavigation.selectedItemId != itemId) {
                        bottomNavigation.selectedItemId = itemId
                    }
                }

                override fun onPageScrollStateChanged(state: Int) {
                    if (state == ViewPager2.SCROLL_STATE_IDLE && menuSyncPending) {
                        menuSyncPending = false
                        syncSongMenuItems()
                    }
                }
            },
        )

        bottomNavigation.setOnItemSelectedListener {
            val position =
                when (it.itemId) {
                    R.id.pitchpipe_item -> 0
                    R.id.notes_item -> 1
                    R.id.keys_item -> 2
                    R.id.songs_item -> 3
                    else -> 0
                }
            if (viewPager.currentItem != position) {
                // Pages stay resident and static artwork is cached, so the
                // standard transition no longer inflates or decodes mid-swipe.
                viewPager.setCurrentItem(position, true)
            }
            scheduleAdLoadAfterIdle()
            true
        }

        // First launch belongs to the first pitch: the login prompt waits for the next session.
        val isFirstLaunchEver = RunUtils.runOnce("firstLaunch")
        if (!isFirstLaunchEver && Firebase.auth.currentUser == null && RunUtils.runOnce("loginDialog")) {
            logInDialog.show()
        } else if (!isFirstLaunchEver) {
            val viewer = ChangelogViewer(this, this.getString(R.string.Changelog))
            viewer.setTitle("Pitch Perfect Changelog")
            viewer.setIcon(R.mipmap.ic_launcher)
            viewer.showIfAppropriate()
        }

        PurchaseService.bind(this) { SettingsModel.areAdsRemoved = PurchaseService.areAdsRemoved }

        reserveBannerSpace()
        scheduleAdLoadAfterIdle()
        PerformanceDiagnostics.logDuration("Main activity created", startedAt)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        reserveBannerSpace()
        if (adReady && adsShouldShow) loadBanner()
    }

    override fun onRestoreInstanceState(state: Bundle) {
        super.onRestoreInstanceState(state)
        // tabHost.restoreInstanceState("tabs", state);
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // tabHost.saveInstanceState("tabs", outState);
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate every action once; page changes only toggle visibility. The
        // gear stays a visible action on every tab, as on iOS. AppCompat's
        // inflater honors app:showAsAction; the framework one ignores it.
        menuInflater.inflate(R.menu.mainmenu, menu)
        menuInflater.inflate(R.menu.songsmenu, menu)
        menu.findItem(R.id.settingsMenuItem).setOnMenuItemClickListener {
            startActivity(Intent(this@PitchPerfectActivity, SettingsActivity::class.java))
            true
        }
        menu.findItem(R.id.editSongsMenuItem).setOnMenuItemClickListener {
            // Resolve at click time: the menu can build before the page-3
            // fragment transaction commits.
            resolveSongListFragment()?.toggleEditingSongs()
            true
        }
        menu.findItem(R.id.sortMenuItem).setOnMenuItemClickListener {
            resolveSongListFragment()?.sortSongs()
            true
        }
        optionsMenu = menu
        syncSongMenuItems(menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        syncSongMenuItems(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    /** Shows the song actions only on the Songs page without rebuilding the menu. */
    internal fun syncSongMenuItems(menu: Menu? = optionsMenu) {
        val editItem = menu?.findItem(R.id.editSongsMenuItem) ?: return
        val sortItem = menu.findItem(R.id.sortMenuItem) ?: return
        val onSongs = selectedPage == 3
        val editing = onSongs && resolveSongListFragment()?.isEditingSongs() == true
        val title = getString(if (editing) R.string.StopEditing else R.string.EditSongList)
        if (editItem.title?.toString() != title) {
            editItem.title = title
            editItem.setIcon(if (editing) R.drawable.ic_check else R.drawable.ic_edit_button)
        }
        if (editItem.isVisible != onSongs) editItem.isVisible = onSongs
        if (sortItem.isVisible != editing) sortItem.isVisible = editing
    }

    override fun onStart() {
        super.onStart()
        if (PerformanceDiagnostics.enabled) {
            frameMonitor = FramePerformanceMonitor("Main").also { it.start(window) }
            window.decorView.postDelayed(
                { PerformanceDiagnostics.logCompilationStatusOnce() },
                COMPILATION_STATUS_DELAY_MS,
            )
        }
        scheduleAdLoadAfterIdle()
    }

    override fun onStop() {
        window.decorView.removeCallbacks(adLoadRunnable)
        frameMonitor?.stop(window)
        frameMonitor = null
        super.onStop()
    }

    override fun onDestroy() {
        window.decorView.removeCallbacks(adLoadRunnable)
        super.onDestroy()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) scheduleAdLoadAfterIdle()
        return super.dispatchTouchEvent(event)
    }

    override fun onResume() {
        super.onResume()

        runOnUiThread(
            Runnable {
                if (handlingResult) {
                    handlingResult = false
                    return@Runnable
                }
            },
        )

        if (SettingsModel.wakeLock) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun scheduleAdLoadAfterIdle() {
        if (adRequested || !adsShouldShow) return
        window.decorView.removeCallbacks(adLoadRunnable)
        window.decorView.postDelayed(adLoadRunnable, AD_INITIALIZATION_DELAY_MS)
    }

    private fun reserveBannerSpace() {
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)
        val reservedSize = getAdSize()
        adContainer.layoutParams =
            adContainer.layoutParams.apply {
                height = reservedSize.getHeightInPixels(this@PitchPerfectActivity)
            }
    }

    private fun loadBanner() {
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)
        adContainer.removeAllViews()
        reserveBannerSpace()
        val adView = AdView(this)
        adContainer.addView(adView)
        // Create an ad request. Check your logcat output for the hashed device ID
        // to get test ads on a physical device, e.g.,
        // "Use AdRequest.Builder.addTestDevice("ABCDE0123") to get test ads on this
        // device."
        val adRequest: AdRequest =
            AdRequest
                .Builder()
                .build()
        val adSize = getAdSize()
        // Step 4 - Set the adaptive ad size on the ad view.
        adView.setAdSize(adSize)
        adView.adUnitId = resources.getString(R.string.ad_unit_id)

        // Step 5 - Start loading the ad in the background.
        adView.loadAd(adRequest)
    }

    private fun getAdSize(): AdSize {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        val density: Float = resources.displayMetrics.density

        @Suppress("DEPRECATION")
        val widthPixels: Float = resources.displayMetrics.widthPixels.toFloat()
        val adWidth = (widthPixels / density).toInt()
        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

    companion object {
        private const val AD_INITIALIZATION_DELAY_MS = 5_000L
        private const val COMPILATION_STATUS_DELAY_MS = 10_000L

        @JvmField
        internal var handlingResult: Boolean = false
    }
}
