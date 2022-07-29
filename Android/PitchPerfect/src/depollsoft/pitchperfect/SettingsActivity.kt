package depollsoft.pitchperfect

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.bindroid.BindingMode
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.Trackable
import com.bindroid.trackable.track
import com.bindroid.ui.CompoundButtonCheckedProperty
import com.bindroid.ui.UiBinder
import com.bindroid.utils.uibind
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.parse.ParseUser
import depollsoft.lib.ui.ChangelogViewer

class SettingsActivity : AppCompatActivity() {
    private val loggingIn = false
    private val loginTrackable: Trackable = Trackable()
    private lateinit var logInDialog: Dialog
    val licensed: Boolean
        get() = SettingsModel.licensed

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK && loggingIn) {
            true
        } else super.onKeyDown(keyCode, event)
    }

    val loggedIn: Boolean
        get() {
            loginTrackable.track()
            return ParseUser.getCurrentUser() != null
        }
    var toggleNotes: Boolean
        get() = SettingsModel.toggleNotes
        set(value) {
            SettingsModel.toggleNotes = value
        }
    var wakeLock: Boolean
        get() = SettingsModel.wakeLock
        set(value) {
            SettingsModel.wakeLock = value
        }

    var areAdsRemoved: Boolean
        get() = SettingsModel.areAdsRemoved
        set(value) {
            SettingsModel.areAdsRemoved = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logInDialog = LoginPrompt.buildDialog(this, false)
        logInDialog.setOnDismissListener { loginTrackable.updateTrackers() }
        this.title = "Pitch Perfect Settings"
        this.setContentView(R.layout.settingsview)
        UiBinder.bind(
            this,
            CompoundButtonCheckedProperty(findViewById<View>(R.id.toggleNoteCheckBox) as CheckBox),
            "ToggleNotes", BindingMode.TWO_WAY
        )
        UiBinder.bind(
            this,
            CompoundButtonCheckedProperty(findViewById<View>(R.id.wakeLockCheckBox) as CheckBox),
            "WakeLock", BindingMode.TWO_WAY
        )
        UiBinder.bind(
            this,
            R.id.rateReviewHyperlink,
            "Visibility",
            "ShowBuyLink",
            BoolConverter.get()
        )
        UiBinder.bind(this, R.id.aboutPurchased, "Visibility", "Licensed", BoolConverter.get())
        UiBinder.bind(this, R.id.loginButton, "Visibility", "LoggedIn", BoolConverter.get(true))
        UiBinder.bind(this, R.id.logoutButton, "Visibility", "LoggedIn", BoolConverter.get())
        uibind(
            R.id.manageSubscriptionButton,
            "Visibility",
            { (this::areAdsRemoved) },
            converter = BoolConverter.get()
        )
        findViewById<View>(R.id.manageSubscriptionButton).setOnClickListener {
            val manageIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/account/subscriptions?sku=${PurchaseService.REMOVE_ADS_SKU}&package=${applicationContext.packageName}")
            )
            startActivity(manageIntent)
        }
        findViewById<View>(R.id.loginButton).setOnClickListener {
            logInDialog.show()
        }
        findViewById<View>(R.id.logoutButton).setOnClickListener {
            AuthUI.getInstance().signOut(it.context)
        }
        val clearSongListButton = findViewById<View>(R.id.clearSongListButton)
        clearSongListButton.setOnClickListener {
            val builder = AlertDialog.Builder(this@SettingsActivity)
            builder.setMessage("Are you sure you want to clear your song list?")
                .setPositiveButton("Yes") { dialog, which ->
                    SongsModel.get().defaultSongList.resetSongs()
                    Toast.makeText(this@SettingsActivity, "Song list cleared.", Toast.LENGTH_SHORT)
                        .show()
                }
                .setNegativeButton("No") { dialog, which -> }.show()
        }
        findViewById<View>(R.id.changelogButton).setOnClickListener {
            val viewer = ChangelogViewer(
                this@SettingsActivity, this@SettingsActivity
                    .getString(R.string.Changelog)
            )
            viewer.setTitle("Pitch Perfect Changelog")
            viewer.setIcon(R.mipmap.ic_launcher)
            viewer.show()
        }

        this.findViewById<RadioButton>(R.id.radio_system).setOnClickListener {
            PitchPerfectApplication.themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        this.findViewById<RadioButton>(R.id.radio_dark).setOnClickListener {
            PitchPerfectApplication.themeMode = AppCompatDelegate.MODE_NIGHT_YES
        }
        this.findViewById<RadioButton>(R.id.radio_light).setOnClickListener {
            PitchPerfectApplication.themeMode = AppCompatDelegate.MODE_NIGHT_NO
        }
        track({ PitchPerfectApplication.themeMode }) {
            when (it()) {
                AppCompatDelegate.MODE_NIGHT_YES ->
                    findViewById<RadioButton>(R.id.radio_dark).isChecked = true
                AppCompatDelegate.MODE_NIGHT_NO ->
                    findViewById<RadioButton>(R.id.radio_light).isChecked = true
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM ->
                    findViewById<RadioButton>(R.id.radio_system).isChecked = true
            }
            if (!this@SettingsActivity.isDestroyed) {
                keepTracking
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }
}