package depollsoft.pitchperfect

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import depollsoft.pitchperfect.ui.PlateText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.facebook.login.LoginManager
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.google.firebase.Firebase
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.functions.functions
import depollsoft.lib.util.AppLog
import depollsoft.pitchperfect.ui.AppCompatAlertDialog
import depollsoft.pitchperfect.ui.DialogButton
import depollsoft.pitchperfect.ui.PlateTheme
import depollsoft.pitchperfect.ui.PlateTopBar
import depollsoft.pitchperfect.ui.plateColors
import depollsoft.pitchperfect.ui.plateText
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsActivity(
    private val watchNodeSource: WatchNodeSource? = null,
) : AppCompatActivity() {
    private val watchSource by lazy { watchNodeSource ?: WearableWatchNodeSource(this) }
    private var watchRefresh: Job? = null
    private var watchResumed = false
    private var watchCapabilityClient: CapabilityClient? = null
    private var watchCapabilityListener: CapabilityClient.OnCapabilityChangedListener? = null

    internal val state = SettingsState()

    /** Which dialog is up, if any. */
    internal var dialog by mutableStateOf<SettingsDialog?>(null)

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.Settings)
        // An open sign-in prompt must come back to receive FirebaseUI's result.
        keepDialogOpen("depollsoft.pitchperfect.SettingsDialog", { dialog }) { dialog = it }
        setupPrivateBuildDiagnostics()
        val actions =
            SettingsActions(
                clearSongs = { dialog = SettingsDialog.CLEAR_SONGS },
                installOnWatch = ::installOnWatch,
                logIn = { dialog = SettingsDialog.LOG_IN },
                logOut = ::signOutImmediately,
                deleteAccount = { dialog = SettingsDialog.DELETE_ACCOUNT },
                manageSubscription = {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                "https://play.google.com/store/account/subscriptions?sku=${PurchaseService.REMOVE_ADS_SKU}&package=${applicationContext.packageName}",
                            ),
                        ),
                    )
                },
                showChangelog = { dialog = SettingsDialog.CHANGELOG },
                openLink = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) },
                privacyChoices = { depollsoft.lib.privacy.TelemetryConsent.show(this) },
                copyLogs = ::copyLogs,
            )
        setContent {
            PlateTheme {
                Column(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                    PlateTopBar(stringResource(R.string.Settings))
                    SettingsScreen(state, actions)
                }
                SettingsDialogs()
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun SettingsDialogs() {
        val close = { dialog = null }
        when (dialog) {
            SettingsDialog.LOG_IN -> LoginPromptDialog(onDismiss = close, onAuthenticated = state::changed)
            SettingsDialog.CHANGELOG -> ChangelogDialog(onDismiss = close)
            SettingsDialog.CLEAR_SONGS ->
                ConfirmDialog(
                    title = stringResource(R.string.ClearAllSongs),
                    message = stringResource(R.string.ClearAllSongsConfirmation),
                    onDismiss = close,
                ) {
                    close()
                    SongsModel.get().clearAll()
                    Toast.makeText(this, R.string.ClearAllSongsDone, Toast.LENGTH_SHORT).show()
                }
            SettingsDialog.DELETE_ACCOUNT ->
                ConfirmDialog(
                    title = "Delete account ($userString)",
                    message = stringResource(R.string.DeleteAccountConfirmation),
                    onDismiss = close,
                ) {
                    close()
                    lifecycleScope.launch {
                        SongsModel.get().detachFromFirestore()
                        SettingsModel.detachFromFirestore()
                        Firebase.functions
                            .getHttpsCallable("deleteUser")
                            .call()
                            .await()
                        signOutImmediately()
                    }
                }
            null -> Unit
        }
    }

    private fun signOutImmediately() {
        val startedAt = SystemClock.elapsedRealtime()
        SongsModel.get().detachFromFirestore()
        SettingsModel.detachFromFirestore()
        LoginManager.getInstance().logOut()
        Firebase.auth.signOut()
        state.changed()
        PerformanceDiagnostics.logDuration("Logout completed", startedAt)
    }

    override fun onResume() {
        super.onResume()
        state.changed()
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
        watchRefresh =
            lifecycleScope.launch {
                val nodes = watchSource.connectedNodes()
                // The source returns an empty list even for cancellation; discard stale results.
                coroutineContext.ensureActive()
                state.watches = nodes
            }
    }

    private fun registerWatchCapabilityListener() {
        try {
            val client = Wearable.getCapabilityClient(applicationContext)
            val listener = CapabilityClient.OnCapabilityChangedListener { runOnUiThread { refreshWatches() } }
            watchCapabilityClient = client
            watchCapabilityListener = listener
            client
                .addListener(listener, WatchCompanion.CAPABILITY)
                .addOnSuccessListener {
                    // Registration may complete after onPause or a subsequent onResume.
                    if (watchCapabilityListener !== listener) {
                        removeWatchCapabilityListener(client, listener)
                    }
                }.addOnFailureListener { error ->
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
            client
                .removeListener(listener, WatchCompanion.CAPABILITY)
                .addOnFailureListener { error ->
                    AppLog.info("Settings", "Watch capability listener removal failed: ${error.javaClass.simpleName}")
                }
        } catch (error: Exception) {
            AppLog.info("Settings", "Watch capability listener removal failed: ${error.javaClass.simpleName}")
        }
    }

    private fun installOnWatch(node: WatchNode) {
        try {
            val result =
                RemoteActivityHelper(this).startRemoteActivity(
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

    private fun showWatchInstallFailure(
        node: WatchNode,
        error: Exception,
    ) {
        AppLog.info("Settings", "Watch Play Store launch failed: ${error.javaClass.simpleName}")
        Toast.makeText(this, getString(R.string.WatchOpenStoreFailed, node.name), Toast.LENGTH_SHORT).show()
    }

    private fun setupPrivateBuildDiagnostics() {
        val build = BuildConfig.PRIVATE_BUILD_NUMBER
        if (build.isBlank()) return
        val pr = BuildConfig.PRIVATE_PR_NUMBER.ifBlank { "?" }
        state.privateBuild = "Build $build · PR #$pr"
        AppLog.info("Settings", "Private build diagnostics opened")
    }

    private fun copyLogs() {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("App logs", "${state.privateBuild}\n\n${AppLog.contents()}"))
        Toast.makeText(this, "Logs copied", Toast.LENGTH_SHORT).show()
    }

    val userString: String
        get() {
            val curUser = Firebase.auth.currentUser ?: return "Logged out"
            val providerData = curUser.providerData.firstOrNull() ?: return "Current User: (${curUser.uid})"
            return when (providerData.providerId) {
                FacebookAuthProvider.PROVIDER_ID -> providerData.email?.let { "Facebook: $it" } ?: "Facebook account"
                GoogleAuthProvider.PROVIDER_ID -> providerData.email?.let { "Google: $it" } ?: "Google account"
                PhoneAuthProvider.PROVIDER_ID -> providerData.phoneNumber!!
                else -> providerData.email ?: "Current User: (${curUser.uid})"
            }
        }
}

/** The settings screen's dialogs. */
enum class SettingsDialog { LOG_IN, CHANGELOG, CLEAR_SONGS, DELETE_ACCOUNT }

/** A yes/no question in AppCompat's alert dialog. */
@androidx.compose.runtime.Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AppCompatAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        buttons =
            listOf(
                DialogButton(stringResource(R.string.No), onDismiss),
                DialogButton(stringResource(R.string.Yes), onConfirm),
            ),
    ) {
        PlateText(
            message,
            style = plateText(16.sp, plateColors.inkSecondary),
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp),
        )
    }
}
