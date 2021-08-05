package depollsoft.pitchperfect

import android.content.Intent
import android.os.AsyncTask
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
import com.facebook.login.LoginManager
import com.parse.ParseUser
import depollsoft.lib.ui.ChangelogViewer

class SettingsActivity : AppCompatActivity() {
    private val loggingIn = false
    private val loginTrackable: Trackable = Trackable()
    val licensed: Boolean
        get() = SettingsModel.getLicensed()

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
        get() = SettingsModel.getToggleNotes()
        set(value) {
            SettingsModel.setToggleNotes(value)
        }
    var wakeLock: Boolean
        get() = SettingsModel.getWakeLock()
        set(value) {
            SettingsModel.setWakeLock(value)
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        LoginPrompt.FACEBOOK_CALLBACK_MANAGER.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        findViewById<View>(R.id.loginButton).setOnClickListener {
            val dlg = LoginPrompt.buildDialog(this@SettingsActivity, false)
            dlg.setOnDismissListener { loginTrackable.updateTrackers() }
            dlg.show()
        }
        findViewById<View>(R.id.logoutButton).setOnClickListener {
            val logOutTask: AsyncTask<Void?, Void?, Void?> =
                object : AsyncTask<Void?, Void?, Void?>() {
                    protected override fun doInBackground(vararg params: Void?): Void? {
                        ParseUser.logOut()
                        SongsModel.get().handleLogOut()
                        LoginManager.getInstance().logOut()
                        return null
                    }

                    override fun onPostExecute(result: Void?) {
                        super.onPostExecute(result)
                        loginTrackable.updateTrackers()
                    }
                }
            logOutTask.execute()
        }
        val clearSongListButton = findViewById<View>(R.id.clearSongListButton)
        clearSongListButton.setOnClickListener {
            val builder = AlertDialog.Builder(this@SettingsActivity)
            builder.setMessage("Are you sure you want to clear your song list?")
                .setPositiveButton("Yes") { dialog, which ->
                    SongsModel.get().resetSongs()
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
        track({PitchPerfectApplication.themeMode}) {
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

    override fun onPause() {
        super.onPause()
        SettingsModel.refreshUser()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }
}