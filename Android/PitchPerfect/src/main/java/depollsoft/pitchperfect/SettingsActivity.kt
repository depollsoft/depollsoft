package depollsoft.pitchperfect

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.bindroid.BindingMode
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.Trackable
import com.bindroid.trackable.track
import com.bindroid.ui.CompoundButtonCheckedProperty
import com.bindroid.ui.UiBinder
import com.bindroid.utils.uibind
import com.facebook.login.LoginManager
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.google.android.material.button.MaterialButton
import com.google.firebase.Firebase
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.functions.functions
import depollsoft.lib.ui.ChangelogViewer
import depollsoft.lib.util.AppLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsActivity(private val watchNodeSource: WatchNodeSource? = null) : AppCompatActivity() {
    private val watchSource by lazy { watchNodeSource ?: WearableWatchNodeSource(this) }
    private var watchRefresh: Job? = null
    private var watchResumed = false
    private var watchCapabilityClient: CapabilityClient? = null
    private var watchCapabilityListener: CapabilityClient.OnCapabilityChangedListener? = null
    private val loggingIn = false
    private val loginTrackable: Trackable = Trackable()
    private lateinit var logInDialog: Dialog
    val licensed: Boolean
        get() = SettingsModel.licensed

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean =
        if (keyCode == KeyEvent.KEYCODE_BACK && loggingIn) {
            true
        } else {
            super.onKeyDown(keyCode, event)
        }

    val loggedIn: Boolean
        get() {
            loginTrackable.track()
            return Firebase.auth.currentUser != null
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

    private fun signOutImmediately() {
        val startedAt = SystemClock.elapsedRealtime()
        SongsModel.get().detachFromFirestore()
        SettingsModel.detachFromFirestore()
        LoginManager.getInstance().logOut()
        Firebase.auth.signOut()
        loginTrackable.updateTrackers()
        PerformanceDiagnostics.logDuration("Logout completed", startedAt)
    }

    var areAdsRemoved: Boolean
        get() = SettingsModel.areAdsRemoved
        set(value) {
            SettingsModel.areAdsRemoved = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logInDialog =
            LoginPrompt.buildDialog(this, false) {
                loginTrackable.updateTrackers()
            }
        this.title = getString(R.string.Settings)
        this.setContentView(R.layout.settingsview)
        findViewById<View>(R.id.privacyChoicesButton).setOnClickListener {
            depollsoft.lib.privacy.TelemetryConsent.show(this)
        }
        UiBinder.bind(
            this,
            CompoundButtonCheckedProperty(findViewById<View>(R.id.toggleNoteCheckBox) as CompoundButton),
            "ToggleNotes",
            BindingMode.TWO_WAY,
        )
        UiBinder.bind(
            this,
            CompoundButtonCheckedProperty(findViewById<View>(R.id.wakeLockCheckBox) as CompoundButton),
            "WakeLock",
            BindingMode.TWO_WAY,
        )
        UiBinder.bind(
            this,
            R.id.rateReviewHyperlink,
            "Visibility",
            "ShowBuyLink",
            BoolConverter.get(),
        )
        uibind(
            R.id.aboutPurchased,
            "Visibility",
            { (this::licensed) },
            converter = BoolConverter.get(),
        )
        uibind(
            R.id.loginButton,
            "Visibility",
            { (this::loggedIn) },
            converter = BoolConverter.get(true),
        )
        uibind(
            R.id.logoutButton,
            "Visibility",
            { (this::loggedIn) },
            converter = BoolConverter.get(),
        )
        uibind(
            R.id.deleteAccountButton,
            "Visibility",
            { (this::loggedIn) },
            converter = BoolConverter.get(),
        )
        uibind(
            R.id.manageSubscriptionButton,
            "Visibility",
            { (this::areAdsRemoved) },
            converter = BoolConverter.get(),
        )
        findViewById<View>(R.id.manageSubscriptionButton).setOnClickListener {
            val manageIntent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://play.google.com/store/account/subscriptions?sku=${PurchaseService.REMOVE_ADS_SKU}&package=${applicationContext.packageName}",
                    ),
                )
            startActivity(manageIntent)
        }
        findViewById<View>(R.id.loginButton).setOnClickListener {
            logInDialog.show()
        }
        findViewById<View>(R.id.logoutButton).setOnClickListener {
            signOutImmediately()
        }
        findViewById<View>(R.id.deleteAccountButton).setOnClickListener {
            AlertDialog
                .Builder(this)
                .setMessage(R.string.DeleteAccountConfirmation)
                .setTitle("Delete account ($userString)")
                .setPositiveButton(R.string.Yes) { dlg, which ->
                    lifecycleScope.launch {
                        SongsModel.get().detachFromFirestore()
                        SettingsModel.detachFromFirestore()
                        Firebase.functions
                            .getHttpsCallable("deleteUser")
                            .call()
                            .await()
                        signOutImmediately()
                    }
                }.setNegativeButton(R.string.No) { dlg, which ->
                    // Do nothing
                }.create()
                .show()
        }
        val clearSongListButton = findViewById<View>(R.id.clearSongListButton)
        clearSongListButton.setOnClickListener {
            val builder = AlertDialog.Builder(this@SettingsActivity)
            builder
                .setTitle(R.string.ClearAllSongs)
                .setMessage(R.string.ClearAllSongsConfirmation)
                .setPositiveButton(R.string.Yes) { dialog, which ->
                    SongsModel.get().clearAll()
                    Toast
                        .makeText(this@SettingsActivity, R.string.ClearAllSongsDone, Toast.LENGTH_SHORT)
                        .show()
                }.setNegativeButton(R.string.No) { dialog, which -> }
                .show()
        }
        findViewById<View>(R.id.changelogButton).setOnClickListener {
            val viewer =
                ChangelogViewer(
                    this@SettingsActivity,
                    this@SettingsActivity
                        .getString(R.string.Changelog),
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
                AppCompatDelegate.MODE_NIGHT_YES -> {
                    findViewById<RadioButton>(R.id.radio_dark).isChecked = true
                }

                AppCompatDelegate.MODE_NIGHT_NO -> {
                    findViewById<RadioButton>(R.id.radio_light).isChecked = true
                }

                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> {
                    findViewById<RadioButton>(R.id.radio_system).isChecked = true
                }
            }
            if (!this@SettingsActivity.isDestroyed) {
                keepTracking
            }
        }
        setupPrivateBuildDiagnostics()
    }

    override fun onResume() {
        super.onResume()
        loginTrackable.updateTrackers()
        watchResumed = true
        registerWatchCapabilityListener()
        refreshWatches()
    }

    override fun onPause() {
        watchResumed = false
        watchRefresh?.cancel()
        val client = watchCapabilityClient
        val listener = watchCapabilityListener
        watchCapabilityClient = null
        watchCapabilityListener = null
        if (client != null && listener != null) {
            removeWatchCapabilityListener(client, listener)
        }
        super.onPause()
    }

    private fun refreshWatches() {
        if (!watchResumed) return
        watchRefresh?.cancel()
        watchRefresh = lifecycleScope.launch {
            val nodes = watchSource.connectedNodes()
            // The source returns an empty list even for cancellation; discard stale results.
            coroutineContext.ensureActive()
            findViewById<View>(R.id.watchSection).visibility =
                if (nodes.isEmpty()) View.GONE else View.VISIBLE
            findViewById<TextView>(R.id.watchStatus).text = nodes.joinToString("\n") { node ->
                getString(
                    if (node.installed) R.string.WatchInstalledOn else R.string.WatchNotInstalledOn,
                    node.name,
                )
            }
            val buttons = findViewById<LinearLayout>(R.id.watchButtons)
            buttons.removeAllViews()
            nodes.filterNot { it.installed }.forEach { node ->
                val button = layoutInflater.inflate(R.layout.settings_watch_button, buttons, false) as MaterialButton
                button.text = getString(R.string.WatchInstallOn, node.name)
                button.setOnClickListener { installOnWatch(node) }
                buttons.addView(button)
            }
        }
    }

    private fun registerWatchCapabilityListener() {
        try {
            val client = Wearable.getCapabilityClient(applicationContext)
            val listener = CapabilityClient.OnCapabilityChangedListener {
                runOnUiThread { refreshWatches() }
            }
            watchCapabilityClient = client
            watchCapabilityListener = listener
            client.addListener(listener, WatchCompanion.CAPABILITY)
                .addOnSuccessListener {
                    // Registration may complete after onPause or a subsequent onResume.
                    if (watchCapabilityListener !== listener) {
                        removeWatchCapabilityListener(client, listener)
                    }
                }
                .addOnFailureListener { error ->
                    AppLog.info("Settings", "Watch capability listener unavailable: ${error.javaClass.simpleName}")
                }
        } catch (error: Exception) {
            AppLog.info("Settings", "Watch capability listener unavailable: ${error.javaClass.simpleName}")
        }
    }

    private fun removeWatchCapabilityListener(
        client: CapabilityClient,
        listener: CapabilityClient.OnCapabilityChangedListener,
    ) {
        try {
            client.removeListener(listener, WatchCompanion.CAPABILITY)
                .addOnFailureListener { error ->
                    AppLog.info("Settings", "Watch capability listener removal failed: ${error.javaClass.simpleName}")
                }
        } catch (error: Exception) {
            AppLog.info("Settings", "Watch capability listener removal failed: ${error.javaClass.simpleName}")
        }
    }

    private fun installOnWatch(node: WatchNode) {
        try {
            val result = RemoteActivityHelper(this).startRemoteActivity(
                WatchCompanion.installIntent(applicationContext.packageName),
                node.id,
            )
            result.addListener(
                {
                    try {
                        result.get()
                        AppLog.info("Settings", "Watch Play Store launch succeeded")
                        Toast.makeText(this, getString(R.string.WatchOpeningStore, node.name), Toast.LENGTH_SHORT).show()
                    } catch (error: Exception) {
                        showWatchInstallFailure(node, error)
                    }
                },
                ContextCompat.getMainExecutor(this),
            )
        } catch (error: Exception) {
            showWatchInstallFailure(node, error)
        }
    }

    private fun showWatchInstallFailure(node: WatchNode, error: Exception) {
        AppLog.info("Settings", "Watch Play Store launch failed: ${error.javaClass.simpleName}")
        Toast.makeText(this, getString(R.string.WatchOpenStoreFailed, node.name), Toast.LENGTH_SHORT).show()
    }

    private fun setupPrivateBuildDiagnostics() {
        val build = BuildConfig.PRIVATE_BUILD_NUMBER
        if (build.isBlank()) return
        val pr = BuildConfig.PRIVATE_PR_NUMBER.ifBlank { "?" }
        val metadata = "Build $build · PR #$pr"
        findViewById<View>(R.id.privateBuildDiagnostics).visibility = View.VISIBLE
        findViewById<android.widget.TextView>(R.id.privateBuildMetadata).text = metadata
        AppLog.info("Settings", "Private build diagnostics opened")
        findViewById<View>(R.id.copyLogsButton).setOnClickListener {
            val clipboard = getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(
                ClipData.newPlainText("App logs", "$metadata\n\n${AppLog.contents()}"),
            )
            Toast.makeText(this, "Logs copied", Toast.LENGTH_SHORT).show()
        }
    }

    val userString: String
        get() {
            val curUser = Firebase.auth.currentUser
            if (curUser == null) {
                return "Logged out"
            }
            if (curUser.providerData.size > 0) {
                val providerData = curUser.providerData.first()
                return when (providerData.providerId) {
                    FacebookAuthProvider.PROVIDER_ID -> {
                        providerData.email?.let { "Facebook: $it" } ?: "Facebook account"
                    }

                    GoogleAuthProvider.PROVIDER_ID -> {
                        providerData.email?.let { "Google: $it" } ?: "Google account"
                    }

                    PhoneAuthProvider.PROVIDER_ID -> {
                        providerData.phoneNumber!!
                    }

                    else -> {
                        providerData.email ?: "Current User: (${curUser.uid})"
                    }
                }
            }
            return "Current User: (${curUser.uid})"
        }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }
}
