package depollsoft.pitchperfect

import androidx.compose.runtime.CompositionLocalProvider
import depollsoft.compose.LocalMenuKey
import depollsoft.compose.MenuKey

import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import depollsoft.lib.util.RunUtils
import depollsoft.pitchperfect.ui.PlateTheme

class PitchPerfectActivity : AppCompatActivity(), depollsoft.lib.privacy.TelemetryConsent.Host {
    private var consentRevision = -1
    private var startupDialogsShown = false
    private var frameMonitor: FramePerformanceMonitor? = null
    private var adRequested = false
    private var adReady = false

    /** The banner once it has been requested; the screen hosts it in the reserved slot. */
    private var banner by mutableStateOf<AdView?>(null)

    /** The adaptive banner's height, reserved before any ad arrives so nothing jumps. */
    private var bannerHeight by mutableStateOf(0)

    /** Which startup prompt is up, if any. */
    internal var startupPrompt by mutableStateOf<StartupPrompt?>(null)

    internal lateinit var songs: SongListState
        private set

    internal lateinit var pager: PagerState
        private set

    private val adLoadRunnable =
        Runnable {
            if (isDestroyed || !adsShouldShow || adRequested || !AdConsent.canRequestAds(this)) return@Runnable
            adRequested = true
            val startedAt = SystemClock.elapsedRealtime()
            // Initialize on the main thread: it costs one idle-time hitch, but a
            // background-thread initialization never produced a banner on a
            // Pixel 11 Pro even though it worked on the emulator. The ads SDK
            // loads its implementation from Play Services, so keep the path
            // the phone has proven.
            MobileAds.initialize(applicationContext) {
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    adReady = true
                    loadBanner()
                    PerformanceDiagnostics.logDuration("Ads initialized and requested", startedAt)
                }
            }
        }

    val adsShouldShow: Boolean
        get() = !SettingsModel.areAdsRemoved && !SettingsModel.licensed

    /** The Menu key opens the Songs tab's overflow menu, as the window action bar did. */
    private val menuKey = MenuKey()

    @OptIn(ExperimentalComposeUiApi::class)
    public override fun onCreate(savedInstanceState: Bundle?) {
        val startedAt = SystemClock.elapsedRealtime()
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        songs = SongListState(SongsModel.get())
        keepSetListPromptsOpen(songs)
        keepAcrossRecreation("depollsoft.pitchperfect.SongsScroll", songs::saveScrollPositions, songs::restoreScrollPositions)
        // An open sign-in prompt must come back to receive FirebaseUI's result.
        keepDialogOpen("depollsoft.pitchperfect.StartupPrompt", { startupPrompt }) { startupPrompt = it }

        setContent {
            PlateTheme {
                CompositionLocalProvider(LocalMenuKey provides menuKey) {
                    val pagerState = rememberPagerState { MainTab.entries.size }
                    pager = pagerState
                    val pitchPipe = remember { PitchPipeModel() }
                    MainScreen(
                        pagerState,
                        AdSlot(adsShouldShow, bannerHeight, banner) {
                            PurchaseService.beginRemoveAds(this@PitchPerfectActivity, 666)
                        },
                        actions = {
                            MainActions(
                                onSongs = pagerState.settledPage == MainTab.SONGS.ordinal,
                                songs = songs,
                                openSettings = { startActivity(Intent(this@PitchPerfectActivity, SettingsActivity::class.java)) },
                            )
                        },
                        modifier = androidx.compose.ui.Modifier.semantics { testTagsAsResourceId = true },
                        overlay = { SongAnnouncements(songs, androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.BottomCenter)) },
                    ) { tab ->
                        val current = pagerState.settledPage == tab.ordinal
                        when (tab) {
                            MainTab.PITCH_PIPE -> PitchPipeScreen(pitchPipe, current)
                            MainTab.NOTES -> NoteListScreen(current)
                            MainTab.KEYS -> KeySignatureScreen(current)
                            MainTab.SONGS -> SongListScreen(songs, current)
                        }
                    }
                    StartupPrompts(this)
                }
            }
        }
        menuKey.install(window)

        PurchaseService.bind(this)

        reserveBannerSpace()
        scheduleAdLoadAfterIdle()
        PerformanceDiagnostics.logDuration("Main activity created", startedAt)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        reserveBannerSpace()
        if (adReady && adsShouldShow) loadBanner()
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
        banner?.destroy()
        super.onDestroy()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) scheduleAdLoadAfterIdle()
        return super.dispatchTouchEvent(event)
    }

    override fun onResume() {
        super.onResume()
        if (consentRevision != AdConsent.revision) {
            banner?.destroy()
            banner = null
            adRequested = false
            adReady = false
            consentRevision = AdConsent.revision
        }
        if (depollsoft.lib.privacy.PrivacyChoices(this).hasChosen) {
            onPrivacyChoicesClosed()
        } else {
            depollsoft.lib.privacy.TelemetryConsent.showIfNeeded(this)
        }

        if (SettingsModel.wakeLock) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onPrivacyChoicesClosed() {
        if (adsShouldShow) {
            AdConsent.gather(this) {
                scheduleAdLoadAfterIdle()
                showStartupDialogs()
            }
        } else {
            showStartupDialogs()
        }
    }

    private fun showStartupDialogs() {
        if (startupDialogsShown || isDestroyed || isFinishing || !depollsoft.lib.privacy.PrivacyChoices(this).hasChosen) return
        startupDialogsShown = true
        // First launch belongs to the first pitch: the login prompt waits for the next session.
        val isFirstLaunchEver = RunUtils.runOnce("firstLaunch")
        startupPrompt =
            startupPromptFor(
                startupPrompt,
                loginDue = { !isFirstLaunchEver && Firebase.auth.currentUser == null && RunUtils.runOnce("loginDialog") },
                changelogDue = { !isFirstLaunchEver && Changelog.shouldShow() },
            )
    }

    /** Switches to [tab], as a tap on its navigation item does. */
    internal fun showTab(tab: MainTab) {
        if (::pager.isInitialized) pager.requestScrollToPage(tab.ordinal)
    }

    private fun scheduleAdLoadAfterIdle() {
        if (adRequested || !adsShouldShow || !AdConsent.canRequestAds(this)) return
        window.decorView.removeCallbacks(adLoadRunnable)
        window.decorView.postDelayed(adLoadRunnable, AD_INITIALIZATION_DELAY_MS)
    }

    private fun reserveBannerSpace() {
        bannerHeight = getAdSize().getHeightInPixels(this)
    }

    private fun loadBanner() {
        if (!AdConsent.canRequestAds(this)) return
        banner?.destroy()
        reserveBannerSpace()
        val adView = AdView(this)
        val adSize = getAdSize()
        adView.setAdSize(adSize)
        adView.adUnitId = resources.getString(R.string.ad_unit_id)
        if (PerformanceDiagnostics.enabled) {
            val requestedAt = SystemClock.elapsedRealtime()
            adView.adListener =
                object : AdListener() {
                    override fun onAdLoaded() {
                        PerformanceDiagnostics.logDuration("Banner ad loaded", requestedAt)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        PerformanceDiagnostics.logDuration(
                            "Banner ad failed",
                            requestedAt,
                            "code=${error.code}; reason=${error.message}",
                        )
                    }
                }
        }
        banner = adView
        adView.loadAd(AdRequest.Builder().build())
    }

    private fun getAdSize(): AdSize {
        val density: Float = resources.displayMetrics.density
        val adWidth = (resources.displayMetrics.widthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

    companion object {
        private const val AD_INITIALIZATION_DELAY_MS = 5_000L
        private const val COMPILATION_STATUS_DELAY_MS = 10_000L
    }
}
