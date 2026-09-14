package depollsoft.tagmaster

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.bindroid.BindingMode
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.Trackable
import com.bindroid.trackable.track
import com.bindroid.trackable.trackable
import com.bindroid.ui.CompoundButtonCheckedProperty
import com.bindroid.ui.UiBinder
import com.bindroid.utils.bind
import com.bindroid.utils.compiledProp
import com.firebase.ui.auth.AuthUI
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import depollsoft.lib.ui.ChangelogViewer
import depollsoft.lib.util.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {
    private val RC_SIGN_IN = 123
    private lateinit var minDownloadSpinner: MaterialAutoCompleteTextView
    private lateinit var minRatingSpinner: MaterialAutoCompleteTextView
    private lateinit var sheetMusicSpinner: MaterialAutoCompleteTextView
    private lateinit var learningTrackSpinner: MaterialAutoCompleteTextView
    private lateinit var minDownloadChoices: List<String>
    private lateinit var minRatingChoices: List<String>
    private var loggingIn: Boolean = false
    private var cacheSizeJob: Job? = null
    var clearingCache: Boolean by trackable(false)
        private set

    private val loginTrackable = Trackable()

    val loggedIn: Boolean
        get() {
            loginTrackable.track()
            return Firebase.auth.currentUser != null
        }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        loginTrackable.updateTrackers()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settingsview)
        setUpToolbar(true)
        findViewById<View>(R.id.scrollView1).applyContentInsets()

        minDownloadSpinner = findViewById(R.id.minimumDownloadSpinner)
        minRatingSpinner = findViewById(R.id.minimumRatingSpinner)
        sheetMusicSpinner = findViewById(R.id.sheetMusicSpinner)
        learningTrackSpinner = findViewById(R.id.learningTracksSpinner)
        minDownloadChoices = resources.getStringArray(R.array.MinDownloadChoices).toList()
        minRatingChoices = resources.getStringArray(R.array.MinRatingChoices).toList()
        refreshMinDownload()
        refreshMinRating()
        refreshSheetMusicChoice()
        refreshLearningTracksChoice()
        refreshCacheSize()

        UiBinder.bind(this, R.id.clearCacheButton, "Enabled", "ClearingCache", BoolConverter.get(true))
        UiBinder.bind(this, R.id.cacheProgress, "Visibility", "ClearingCache", BoolConverter.get())
        findViewById<View>(R.id.clearCacheButton).setOnClickListener {
            confirm(R.string.forms_clear_cache_confirmation) { clearCache() }
        }

        minDownloadSpinner.setOnItemClickListener { _, _, position, _ ->
            SettingsModel.minimumRandomDownloads =
                if (position == 0) 0 else minDownloadChoices[position].toInt()
        }
        minRatingSpinner.setOnItemClickListener { _, _, position, _ ->
            SettingsModel.minimumRandomTagRating =
                if (position == 0) 0.0 else minRatingChoices[position].toDouble()
        }
        val booleanChoices = listOf(null, true, false)
        learningTrackSpinner.setOnItemClickListener { _, _, position, _ ->
            SettingsModel.randomLearningTracksFilter = booleanChoices[position]
        }
        sheetMusicSpinner.setOnItemClickListener { _, _, position, _ ->
            SettingsModel.randomSheetMusicFilter = booleanChoices[position]
        }
        findViewById<View>(R.id.clearFavoritesButton).setOnClickListener {
            confirm(R.string.forms_clear_favorites_confirmation) {
                FavoritesModel.resetFavorites()
                showMessage(R.string.forms_favorites_cleared)
            }
        }
        findViewById<View>(R.id.clearTeachableTags).setOnClickListener {
            confirm(R.string.forms_clear_teachable_confirmation) {
                TeachableTagsModel.resetTeachableTags()
                showMessage(R.string.forms_teachable_cleared)
            }
        }

        UiBinder.bind(this, R.id.loginButton, "Visibility", "LoggedIn", BoolConverter.get(true))
        UiBinder.bind(this, R.id.logoutButton, "Visibility", "LoggedIn", BoolConverter.get())
        findViewById<View>(R.id.loginButton).setOnClickListener {
            startActivityForResult(
                AuthUI
                    .getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(
                        listOf(
                            AuthUI.IdpConfig
                                .EmailBuilder()
                                .setRequireName(false)
                                .setAllowNewAccounts(true)
                                .build(),
                            AuthUI.IdpConfig.GoogleBuilder().build(),
                            AuthUI.IdpConfig.FacebookBuilder().build(),
                        ),
                    ).setTheme(R.style.AppTheme_ActionBar)
                    .build(),
                RC_SIGN_IN,
            )
        }
        findViewById<View>(R.id.logoutButton).setOnClickListener {
            AuthUI.getInstance().signOut(this@SettingsActivity).continueWith {
                loginTrackable.updateTrackers()
            }
        }
        findViewById<View>(R.id.changelogButton).setOnClickListener {
            val viewer = ChangelogViewer(this, getString(R.string.Changelog))
            viewer.setTitle(getString(R.string.forms_changelog_title))
            viewer.setIcon(R.mipmap.ic_launcher)
            viewer.show()
        }

        val themeGroup = findViewById<MaterialButtonToggleGroup>(R.id.themeToggleGroup)
        themeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val mode =
                    when (checkedId) {
                        R.id.radio_dark -> AppCompatDelegate.MODE_NIGHT_YES
                        R.id.radio_light -> AppCompatDelegate.MODE_NIGHT_NO
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                if (TagMasterApplication.themeMode != mode) {
                    TagMasterApplication.themeMode = mode
                }
            }
        }
        track({ TagMasterApplication.themeMode }) {
            if (!isFinishing && !isDestroyed) {
                themeGroup.check(
                    when (it()) {
                        AppCompatDelegate.MODE_NIGHT_YES -> R.id.radio_dark
                        AppCompatDelegate.MODE_NIGHT_NO -> R.id.radio_light
                        else -> R.id.radio_system
                    },
                )
                keepTracking
            }
        }

        bind(
            CompoundButtonCheckedProperty(findViewById(R.id.sheetMusicWakeLockCheckBox)),
            compiledProp { SettingsModel::wakeLockOnSheetMusic },
            BindingMode.TWO_WAY,
        )
        supportActionBar?.title = getString(R.string.app_name).makeTitleString(this)
        setupPrivateBuildDiagnostics()
    }

    private fun confirm(
        message: Int,
        action: () -> Unit,
    ) {
        if (isFinishing || isDestroyed) return
        MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setPositiveButton(R.string.forms_yes) { _, _ ->
                if (!isFinishing && !isDestroyed) action()
            }.setNegativeButton(R.string.forms_no, null)
            .show()
    }

    private fun showMessage(message: Int) {
        if (!isFinishing && !isDestroyed) {
            Snackbar.make(findViewById(R.id.scrollView1), message, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun clearCache() {
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

    private fun refreshCacheSize() {
        cacheSizeJob?.cancel()
        val display = findViewById<TextView>(R.id.cacheSizeDisplay)
        display.setText(R.string.forms_cache_size_pending)
        cacheSizeJob =
            lifecycleScope.launch {
                try {
                    val size = withContext(Dispatchers.IO) { SettingsModel.cacheSizeInMegabytes }
                    if (!isFinishing && !isDestroyed) {
                        display.text = getString(R.string.forms_cache_size, size)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (!isFinishing && !isDestroyed) {
                        Snackbar
                            .make(
                                findViewById(R.id.scrollView1),
                                R.string.forms_cache_size_failed,
                                Snackbar.LENGTH_LONG,
                            ).setAction(R.string.Refresh) { refreshCacheSize() }
                            .show()
                    }
                }
            }
    }

    private fun setupPrivateBuildDiagnostics() {
        val build = BuildConfig.PRIVATE_BUILD_NUMBER
        if (build.isBlank()) return
        val pr = BuildConfig.PRIVATE_PR_NUMBER.ifBlank { "?" }
        val metadata = getString(R.string.forms_private_build_metadata, build, pr)
        findViewById<View>(R.id.privateBuildDiagnostics).visibility = View.VISIBLE
        findViewById<TextView>(R.id.privateBuildMetadata).text = metadata
        AppLog.info("Settings", "Private build diagnostics opened")
        findViewById<View>(R.id.copyLogsButton).setOnClickListener {
            val clipboard = getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    getString(R.string.forms_app_logs),
                    "$metadata\n\n${AppLog.contents()}",
                ),
            )
            showMessage(R.string.forms_logs_copied)
        }
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = keyCode == KeyEvent.KEYCODE_BACK && loggingIn || super.onKeyDown(keyCode, event)

    override fun onSupportNavigateUp() = navigateUpOrHome()

    private fun refreshLearningTracksChoice() {
        val index = listOf(null, true, false).indexOf(SettingsModel.randomLearningTracksFilter)
        learningTrackSpinner.setText(resources.getStringArray(R.array.LearningTracksChoices)[index], false)
    }

    private fun refreshMinDownload() {
        val index = minDownloadChoices.indexOf(SettingsModel.minimumRandomDownloads.toString()).coerceAtLeast(0)
        minDownloadSpinner.setText(minDownloadChoices[index], false)
    }

    private fun refreshMinRating() {
        val index = minRatingChoices.indexOf(SettingsModel.minimumRandomTagRating.toInt().toString()).coerceAtLeast(0)
        minRatingSpinner.setText(minRatingChoices[index], false)
    }

    private fun refreshSheetMusicChoice() {
        val index = listOf(null, true, false).indexOf(SettingsModel.randomSheetMusicFilter)
        sheetMusicSpinner.setText(resources.getStringArray(R.array.SheetMusicChoices)[index], false)
    }
}
