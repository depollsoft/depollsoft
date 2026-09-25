package depollsoft.tagmaster

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.facebook.login.LoginManager
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import depollsoft.lib.auth.SignInOutcome
import depollsoft.lib.ui.ChangelogViewer
import depollsoft.lib.util.AppLog
import depollsoft.tagmaster.ui.BarberPoleWatermark
import depollsoft.tagmaster.ui.ButtonStyle
import depollsoft.tagmaster.ui.DialogButton
import depollsoft.tagmaster.ui.DropdownField
import depollsoft.tagmaster.ui.LocalSnackbars
import depollsoft.tagmaster.ui.ReadingWidth
import depollsoft.tagmaster.ui.Snackbars
import depollsoft.tagmaster.ui.TagMasterButton
import depollsoft.tagmaster.ui.TagMasterDialog
import depollsoft.tagmaster.ui.TagMasterTheme
import depollsoft.tagmaster.ui.TagMasterTopBar
import depollsoft.tagmaster.ui.TagMasterType
import depollsoft.tagmaster.ui.TagMasterType.withoutLineHeight
import depollsoft.tagmaster.ui.ViewAlign
import depollsoft.tagmaster.ui.navigateUpOrHome
import depollsoft.tagmaster.ui.scrollViewScrollbar
import depollsoft.tagmaster.ui.setTagMasterContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings: the cache, signing in to sync lists, clearing the built-in lists, the Random Tag
 * filters, keeping the screen on for sheet music, the theme, the changelog and privacy choices.
 */
class SettingsActivity : AppCompatActivity() {
    private var loggingIn = false

    /** Mirrors Firebase's auth state through [AuthState], so it repaints on any auth change. */
    val loggedIn: Boolean
        get() = AuthState.isSignedIn

    /** Set while the cache is being cleared. */
    var clearingCache by mutableStateOf(false)
        private set

    /** The cache size text, or null while it is being measured. */
    internal var cacheSize by mutableStateOf<String?>(null)
        private set

    private var cacheSizeJob: Job? = null
    /**
     * A message waiting for the screen to show it. A sign-in result can arrive before a recreated
     * screen has composed its snackbar host, so messages queue here instead of being dropped.
     */
    internal var pendingMessage by mutableStateOf<Message?>(null)

    internal class Message(
        val text: String,
        val action: String? = null,
        val onAction: () -> Unit = {},
    )

    private val signInLauncher =
        registerForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
            loggingIn = false
            val response = result.idpResponse
            // Firebase's auth state, not FirebaseUI's result code, decides whether the person is
            // signed in: FirebaseUI can report an error or cancellation after Firebase has already
            // accepted a Facebook account that has no email address.
            val outcome =
                SignInOutcome.resolve(
                    isSignedIn = Firebase.auth.currentUser != null,
                    resultOk = result.resultCode == RESULT_OK && response != null,
                    hasError = response?.error != null,
                )
            AuthState.notifyChanged()
            showMessage(
                when (outcome) {
                    SignInOutcome.SIGNED_IN -> R.string.forms_signed_in
                    SignInOutcome.FAILED -> R.string.forms_sign_in_failed
                    SignInOutcome.CANCELED -> R.string.forms_sign_in_canceled
                },
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTagMasterContent { SettingsScreen(this) }
        refreshCacheSize()
    }

    override fun onResume() {
        super.onResume()
        // The sign-in result, when one arrives, is delivered before onResume; anything that
        // reaches here without one (process death mid-flow, a lost result) still repaints.
        loggingIn = false
        AuthState.notifyChanged()
    }

    internal fun logIn() {
        if (loggingIn) return
        loggingIn = true
        signInLauncher.launch(createSignInIntent())
    }

    internal fun logOut() {
        LoginManager.getInstance().logOut()
        AuthUI.getInstance().signOut(this).continueWith { AuthState.notifyChanged() }
    }

    internal fun showMessage(message: Int) {
        if (!isFinishing && !isDestroyed) pendingMessage = Message(getString(message))
    }

    internal fun clearCache() {
        if (clearingCache) return
        clearingCache = true
        cacheSizeJob?.cancel()
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { SettingsModel.clearCache() }
                refreshCacheSize()
                showMessage(R.string.forms_cache_cleared)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage(R.string.forms_clear_cache_failed)
            } finally {
                clearingCache = false
            }
        }
    }

    internal fun refreshCacheSize() {
        cacheSizeJob?.cancel()
        cacheSize = null
        cacheSizeJob =
            lifecycleScope.launch {
                try {
                    val size = withContext(Dispatchers.IO) { SettingsModel.cacheSizeInMegabytes }
                    if (!isFinishing && !isDestroyed) cacheSize = getString(R.string.forms_cache_size, size)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (!isFinishing && !isDestroyed) {
                        pendingMessage = Message(getString(R.string.forms_cache_size_failed), getString(R.string.Refresh)) { refreshCacheSize() }
                    }
                }
            }
    }

    internal fun showChangelog() {
        val viewer = ChangelogViewer(this, getString(R.string.Changelog))
        viewer.setTitle(getString(R.string.forms_changelog_title))
        viewer.setIcon(R.mipmap.ic_launcher)
        viewer.show()
    }

    internal fun copyLogs(metadata: String) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.forms_app_logs), "$metadata\n\n${AppLog.contents()}"))
        showMessage(R.string.forms_logs_copied)
    }

    private fun createSignInIntent() =
        AuthUI
            .getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(SIGN_IN_PROVIDERS)
            // Credential saving is what crashes FirebaseUI for Facebook accounts without an
            // email address; the build-time patch in buildSrc guards the rest of that path.
            .setCredentialManagerEnabled(false)
            .setTheme(R.style.AppTheme_ActionBar)
            .build()

    internal companion object {
        val SIGN_IN_PROVIDERS: List<AuthUI.IdpConfig> by lazy {
            listOf(
                AuthUI.IdpConfig
                    .EmailBuilder()
                    .setRequireName(false)
                    .setAllowNewAccounts(true)
                    .build(),
                AuthUI.IdpConfig.GoogleBuilder().build(),
                AuthUI.IdpConfig
                    .FacebookBuilder()
                    .setPermissions(listOf("email", "public_profile"))
                    .build(),
            )
        }
    }
}

@Composable
private fun SettingsScreen(activity: SettingsActivity) {
    val colors = TagMasterTheme.colors
    val snackbars = LocalSnackbars.current
    activity.pendingMessage?.let { message ->
        LaunchedEffect(message) {
            activity.pendingMessage = null
            // A message with an action (Refresh) stays up long; a plain result is brief.
            snackbars.show(message.text, message.action, long = message.action != null, onAction = message.onAction)
        }
    }
    var confirming by remember { mutableStateOf<Pair<Int, () -> Unit>?>(null) }
    Column(Modifier.fillMaxSize()) {
        TagMasterTopBar(title = stringResource(R.string.app_name), brandTitle = true, onNavigateUp = { activity.navigateUpOrHome() })
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            BarberPoleWatermark()
            ReadingWidth(Modifier.fillMaxSize()) { extra ->
                val scroll = rememberScrollState()
                Column(
                    Modifier
                        .fillMaxSize()
                        .scrollViewScrollbar(scroll, bottom = 16.dp, end = 16.dp)
                        .verticalScroll(scroll)
                        .padding(start = 16.dp + extra, end = 16.dp + extra, bottom = 16.dp)
                        .testTag("settingsForm"),
                ) {
                    Heading(R.string.cache)
                    Text(stringResource(R.string.CurrentCacheSize), style = TagMasterType.labelLarge, color = colors.text)
                    Text(
                        activity.cacheSize ?: stringResource(R.string.forms_cache_size_pending),
                        Modifier
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag("cacheSizeDisplay"),
                        style = TagMasterType.bodyMedium,
                        color = colors.text,
                    )
                    if (activity.clearingCache) {
                        LinearProgressIndicator(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("cacheProgress"),
                            color = colors.primary,
                        )
                    }
                    WideButton(R.string.ClearCache, ButtonStyle.Outlined, "clearCacheButton", enabled = !activity.clearingCache) {
                        confirming = R.string.forms_clear_cache_confirmation to { activity.clearCache() }
                    }
                    Heading(R.string.LogInTitle)
                    Text(stringResource(R.string.LogInInfo), Modifier.fillMaxWidth(), style = TagMasterType.bodyMedium, color = colors.text)
                    if (activity.loggedIn) {
                        WideButton(R.string.LogOut, ButtonStyle.Filled, "logoutButton") { activity.logOut() }
                    } else {
                        WideButton(R.string.LogIn, ButtonStyle.Filled, "loginButton") { activity.logIn() }
                    }
                    Heading(R.string.Favorites)
                    WideButton(R.string.ClearFavorites, ButtonStyle.Outlined, "clearFavoritesButton") {
                        confirming =
                            R.string.forms_clear_favorites_confirmation to {
                                FavoritesModel.resetFavorites()
                                activity.showMessage(R.string.forms_favorites_cleared)
                            }
                    }
                    Heading(R.string.TeachableTags)
                    WideButton(R.string.ClearTeachableTags, ButtonStyle.Outlined, "clearTeachableTags") {
                        confirming =
                            R.string.forms_clear_teachable_confirmation to {
                                TeachableTagsModel.resetTeachableTags()
                                activity.showMessage(R.string.forms_teachable_cleared)
                            }
                    }
                    Heading(R.string.RandomTagSettings)
                    RandomTagFilters()
                    Heading(R.string.keep_screen_on)
                    WakeLockSwitch()
                    Heading(R.string.themeTitle)
                    ThemeChoice()
                    WideButton(R.string.ViewChangelog, ButtonStyle.Outlined, "changelogButton", Modifier.padding(top = 24.dp)) {
                        activity.showChangelog()
                    }
                    WideButton(depollsoft.lib.kotlin.R.string.privacy_title, ButtonStyle.Filled, "privacyChoicesButton", Modifier.padding(top = 16.dp)) {
                        depollsoft.lib.privacy.TelemetryConsent.show(activity)
                    }
                    PrivateBuildDiagnostics(activity)
                }
            }
        }
    }
    confirming?.let { (message, action) ->
        TagMasterDialog(
            onDismissRequest = { confirming = null },
            title = null,
            message = stringResource(message),
            dismiss = DialogButton(stringResource(R.string.forms_no)) { confirming = null },
            confirm =
                DialogButton(stringResource(R.string.forms_yes)) {
                    confirming = null
                    if (!activity.isFinishing && !activity.isDestroyed) action()
                },
        )
    }
}

@Composable
private fun Heading(text: Int) {
    Text(
        stringResource(text),
        Modifier
            .padding(top = 24.dp, bottom = 8.dp)
            .semantics { heading() },
        style = TagMasterType.titleLarge,
        color = TagMasterTheme.colors.text,
    )
}

@Composable
private fun WideButton(
    text: Int,
    style: ButtonStyle,
    tag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    TagMasterButton(
        stringResource(text),
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(tag),
        style = style,
        enabled = enabled,
    )
}

@Composable
private fun RandomTagFilters() {
    val downloads = stringArrayResource(R.array.MinDownloadChoices).toList()
    val ratings = stringArrayResource(R.array.MinRatingChoices).toList()
    val booleans = listOf(null, true, false)
    Setting(R.string.MinimumRating, ratings, ratings.indexOf(SettingsModel.minimumRandomTagRating.toInt().toString()).coerceAtLeast(0), "minimumRatingSpinner") {
        SettingsModel.minimumRandomTagRating = if (it == 0) 0.0 else ratings[it].toDouble()
    }
    Setting(
        R.string.MinimumDownloads,
        downloads,
        downloads.indexOf(SettingsModel.minimumRandomDownloads.toString()).coerceAtLeast(0),
        "minimumDownloadSpinner",
    ) { SettingsModel.minimumRandomDownloads = if (it == 0) 0 else downloads[it].toInt() }
    val sheetChoices = stringArrayResource(R.array.SheetMusicChoices).toList()
    Setting(R.string.SheetMusicSentence, sheetChoices, booleans.indexOf(SettingsModel.randomSheetMusicFilter), "sheetMusicSpinner") {
        SettingsModel.randomSheetMusicFilter = booleans[it]
    }
    val trackChoices = stringArrayResource(R.array.LearningTracksChoices).toList()
    Setting(R.string.LearningTracks, trackChoices, booleans.indexOf(SettingsModel.randomLearningTracksFilter), "learningTracksSpinner") {
        SettingsModel.randomLearningTracksFilter = booleans[it]
    }
}

@Composable
private fun Setting(
    label: Int,
    choices: List<String>,
    selected: Int,
    tag: String,
    onSelect: (Int) -> Unit,
) {
    DropdownField(stringResource(label), choices, selected, onSelect, Modifier.padding(bottom = 8.dp), tag)
}

@Composable
private fun WakeLockSwitch() {
    val colors = TagMasterTheme.colors
    val checked = SettingsModel.wakeLockOnSheetMusic
    // A switch reads "On"/"Off" to a screen reader, and the row's press moves the thumb, as
    // MaterialSwitch did.
    val interactions = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            // Only the thumb ripples, as MaterialSwitch bounded its ripple there; the Switch draws it
            // from the shared interactions.
            .toggleable(checked, interactions, indication = null, role = Role.Switch) { SettingsModel.wakeLockOnSheetMusic = it }
            .testTag("sheetMusicWakeLockCheckBox"),
        verticalAlignment = ViewAlign.CenterVertically,
    ) {
        Text(
            stringResource(R.string.while_viewing_sheet_music),
            Modifier.weight(1f),
            style = TagMasterType.bodyMedium.withoutLineHeight(),
            color = colors.text,
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            interactionSource = interactions,
            colors =
                SwitchDefaults.colors(
                    checkedTrackColor = colors.primary,
                    checkedThumbColor = colors.onPrimary,
                    uncheckedTrackColor = colors.surfaceContainerHighest,
                    uncheckedBorderColor = colors.outline,
                    uncheckedThumbColor = colors.outline,
                ),
        )
    }
}

/** Default / Light / Dark as one connected, single-selection button group. */
@Composable
private fun ThemeChoice() {
    val colors = TagMasterTheme.colors
    val modes =
        listOf(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM to R.string.theme_system,
            AppCompatDelegate.MODE_NIGHT_NO to R.string.theme_light,
            AppCompatDelegate.MODE_NIGHT_YES to R.string.theme_dark,
        )
    val current = TagMasterApplication.themeMode
    val selected =
        when (current) {
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            else -> 0
        }
    Layout(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectableGroup()
                .testTag("themeToggleGroup"),
        content = {
            modes.forEachIndexed { index, (mode, label) ->
                val shape =
                    when (index) {
                        0 -> RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
                        modes.lastIndex -> RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
                        else -> RoundedCornerShape(0.dp)
                    }
                val checked = index == selected
                Box(
                    Modifier
                        .heightIn(min = 48.dp)
                        .then(if (checked) Modifier.background(colors.secondaryContainer, shape) else Modifier)
                        .border(BorderStroke(1.dp, colors.outlineVariant), shape)
                        .clip(shape)
                        .selectable(checked, role = Role.RadioButton) {
                            if (TagMasterApplication.themeMode != mode) TagMasterApplication.themeMode = mode
                        }.padding(horizontal = 8.dp, vertical = 10.dp)
                        .testTag("theme:$index"),
                    contentAlignment = ViewAlign.Center,
                ) {
                    Text(
                        stringResource(label),
                        style = TagMasterType.labelLarge.withoutLineHeight(),
                        color = if (checked) colors.onSecondaryContainer else colors.onSurface,
                        maxLines = 1,
                    )
                }
            }
        },
    ) { measurables, constraints ->
        // MaterialButtonToggleGroup overlaps neighbouring strokes by 1dp.
        val overlap = 1.dp.roundToPx()
        val count = measurables.size
        val width = constraints.maxWidth
        val each = (width + overlap * (count - 1)) / count
        val placeables = measurables.map { it.measure(Constraints.fixedWidth(each)) }
        val height = placeables.maxOf { it.height }
        layout(width, height) {
            placeables.forEachIndexed { index, placeable -> placeable.placeRelative(index * (each - overlap), 0) }
        }
    }
}

@Composable
private fun PrivateBuildDiagnostics(activity: SettingsActivity) {
    val build = BuildConfig.PRIVATE_BUILD_NUMBER
    if (build.isBlank()) return
    val pr = BuildConfig.PRIVATE_PR_NUMBER.ifBlank { "?" }
    val metadata = stringResource(R.string.forms_private_build_metadata, build, pr)
    LaunchedEffect(Unit) { AppLog.info("Settings", "Private build diagnostics opened") }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 12.dp)
            .testTag("privateBuildDiagnostics"),
    ) {
        Text(
            stringResource(R.string.forms_private_build),
            Modifier
                .padding(bottom = 8.dp)
                .semantics { heading() },
            style = TagMasterType.titleLarge,
            color = TagMasterTheme.colors.text,
        )
        Text(metadata, style = TagMasterType.bodyMedium, color = TagMasterTheme.colors.text)
        WideButton(R.string.forms_copy_logs, ButtonStyle.Filled, "copyLogsButton", Modifier.padding(top = 8.dp)) { activity.copyLogs(metadata) }
    }
}
