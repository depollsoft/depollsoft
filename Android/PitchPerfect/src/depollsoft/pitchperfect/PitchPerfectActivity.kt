package depollsoft.pitchperfect

import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.media.AudioManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import bolts.Task
import com.bindroid.converters.BoolConverter
import com.bindroid.ui.UiBinder
import com.bindroid.utils.uibind
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.wearable.Wearable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.parse.ParseUser
import depollsoft.lib.compat.ui.Activities
import depollsoft.lib.ui.ChangelogViewer
import depollsoft.lib.util.RunUtils

class PitchPerfectActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var logInDialog: Dialog
    private var preparingMenu: Boolean = false

    val adsShouldShow: Boolean
        get() = !SettingsModel.areAdsRemoved && !SettingsModel.licensed

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logInDialog = LoginPrompt.buildDialog(this, false)

        this.volumeControlStream = AudioManager.STREAM_MUSIC

        this.setContentView(R.layout.pitchperfectview)

        uibind(
            R.id.adContainer,
            "Visibility",
            { (this::adsShouldShow) },
            converter = BoolConverter.get()
        )
        uibind(
            R.id.removeAds,
            "Visibility",
            { (this::adsShouldShow) },
            converter = BoolConverter.get()
        )

        findViewById<View>(R.id.removeAds).setOnClickListener {
            PurchaseService.beginRemoveAds(this@PitchPerfectActivity, 666)
        }

        bottomNavigation = findViewById(R.id.bottomNavigation)
        val viewPager = findViewById<ViewPager>(R.id.viewPager)

        viewPager.adapter = object : FragmentPagerAdapter(this.supportFragmentManager) {
            override fun getItem(position: Int): Fragment {
                when (position) {
                    0 -> return PitchPipeFragment()
                    1 -> return NoteListFragment()
                    2 -> return KeySignatureFragment()
                    3 -> return SongListFragment()
                }
                return PitchPipeFragment()
            }

            override fun getCount(): Int {
                return 4
            }
        }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(position: Int) {
                Activities.invalidateOptionsMenu(this@PitchPerfectActivity)
                bottomNavigation.selectedItemId = when (position) {
                    0 -> R.id.pitchpipe_item
                    1 -> R.id.notes_item
                    2 -> R.id.keys_item
                    3 -> R.id.songs_item
                    else -> R.id.pitchpipe_item
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }
        })

        bottomNavigation.setOnNavigationItemSelectedListener {
            viewPager.setCurrentItem(
                when (it.itemId) {
                    R.id.pitchpipe_item -> 0
                    R.id.notes_item -> 1
                    R.id.keys_item -> 2
                    R.id.songs_item -> 3
                    else -> 0
                }, true
            )
            true
        }

        if (RunUtils.runOnce("loginDialog") && ParseUser.getCurrentUser() == null) {
            logInDialog.show()
        } else {
            val viewer = ChangelogViewer(this, this.getString(R.string.Changelog))
            viewer.setTitle("Pitch Perfect Changelog")
            viewer.setIcon(R.mipmap.ic_launcher)
            viewer.showIfAppropriate()
        }

        PurchaseService.bind(this) { SettingsModel.areAdsRemoved = PurchaseService.areAdsRemoved }

        this.onConfigurationChanged(Resources.getSystem().configuration)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        loadBanner()
    }

    override fun onRestoreInstanceState(state: Bundle) {
        super.onRestoreInstanceState(state)
        //tabHost.restoreInstanceState("tabs", state);
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //tabHost.saveInstanceState("tabs", outState);
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (preparingMenu)
            return false
        preparingMenu = true
        try {
            menu.clear()
            val mi = MenuInflater(this)
            mi.inflate(R.menu.mainmenu, menu)

            menu.findItem(R.id.settingsMenuItem).setOnMenuItemClickListener {
                val i = Intent(this@PitchPerfectActivity, SettingsActivity::class.java)
                this@PitchPerfectActivity.startActivity(i)
                true
            }
            //getLocalActivityManager().getCurrentActivity().onPrepareOptionsMenu(menu);
            return super.onPrepareOptionsMenu(menu)
        } finally {
            preparingMenu = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        runOnUiThread(Runnable {
            if (handlingResult) {
                handlingResult = false
                return@Runnable
            }
        })

        if (SettingsModel.wakeLock) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun loadBanner() {
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)
        adContainer.removeAllViews()
        val adView = AdView(this)
        adContainer.addView(adView)
        // Create an ad request. Check your logcat output for the hashed device ID
        // to get test ads on a physical device, e.g.,
        // "Use AdRequest.Builder.addTestDevice("ABCDE0123") to get test ads on this
        // device."
        val adRequest: AdRequest = AdRequest.Builder()
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
        val display: Display = windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels: Float = outMetrics.widthPixels.toFloat()
        val density: Float = outMetrics.density
        val adWidth = (widthPixels / density).toInt()
        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

    companion object {
        @JvmField
        internal var handlingResult: Boolean = false
    }
}