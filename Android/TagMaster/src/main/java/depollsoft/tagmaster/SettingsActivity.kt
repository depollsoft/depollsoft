package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.bindroid.BindingMode
import com.bindroid.ValueConverter
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.Trackable
import com.bindroid.trackable.track
import com.bindroid.ui.CompoundButtonCheckedProperty
import com.bindroid.ui.UiBinder
import com.bindroid.utils.bind
import com.bindroid.utils.compiledProp
import com.bindroid.utils.uibind
import com.firebase.ui.auth.AuthUI
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import depollsoft.lib.compat.ui.ActionBars
import depollsoft.lib.ui.ChangelogViewer
import java.util.*

class SettingsActivity : AppCompatActivity() {
    private val RC_SIGN_IN = 123
    private var minDownloadSpinner: Spinner? = null
    private var minRatingSpinner: Spinner? = null
    private var sheetMusicSpinner: Spinner? = null
    private var learningTrackSpinner: Spinner? = null

    private var minDownloadChoices: List<String>? = null
    private var minRatingChoices: List<String>? = null

    private var loggingIn: Boolean = false

    private val loginTrackable = Trackable()

    val loggedIn: Boolean
        get() {
            this.loginTrackable.track()
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
        this.setContentView(R.layout.settingsview)

        this.minDownloadSpinner = this.findViewById(R.id.minimumDownloadSpinner) as Spinner
        this.minRatingSpinner = this.findViewById(R.id.minimumRatingSpinner) as Spinner
        this.sheetMusicSpinner = this.findViewById(R.id.sheetMusicSpinner) as Spinner
        this.learningTrackSpinner = this.findViewById(R.id.learningTracksSpinner) as Spinner

        this.minDownloadChoices =
            Arrays.asList(
                *this.resources.getStringArray(
                    R.array.MinDownloadChoices,
                ),
            )
        this.minRatingChoices =
            Arrays.asList(
                *this.resources.getStringArray(
                    R.array.MinRatingChoices,
                ),
            )
        Arrays.asList(*this.resources.getStringArray(R.array.SheetMusicChoices))
        Arrays.asList(*this.resources.getStringArray(R.array.LearningTracksChoices))

        this.refreshMinDownload()
        this.refreshMinRating()
        this.refreshSheetMusicChoice()
        this.refreshLearningTracksChoice()

        this.refreshCacheSize()

        this.findViewById<View>(R.id.clearCacheButton).setOnClickListener(
            OnClickListener {
                val builder = AlertDialog.Builder(this@SettingsActivity)
                builder
                    .setMessage(
                        "Are you sure you want to clear your cache?  Cached sheet music and tracks will not be accessible until you are connected to the internet again.",
                    ).setPositiveButton("Yes") { _, _ ->
                        SettingsModel.clearCache()
                        this@SettingsActivity.refreshCacheSize()
                        Toast
                            .makeText(this@SettingsActivity, "Cache cleared.", Toast.LENGTH_SHORT)
                            .show()
                    }.setNegativeButton("No") { _, _ -> }
                    .show()
            },
        )

        this.minDownloadSpinner!!.onItemSelectedListener =
            object : OnItemSelectedListener {
                override fun onItemSelected(
                    arg0: AdapterView<*>?,
                    arg1: View?,
                    arg2: Int,
                    arg3: Long,
                ) {
                    val selected = arg0?.selectedItem as String
                    var amount = 0
                    if (selected != "Any") {
                        amount = Integer.parseInt(selected)
                    }
                    SettingsModel.minimumRandomDownloads = amount
                }

                override fun onNothingSelected(arg0: AdapterView<*>?) {}
            }
        this.minRatingSpinner!!.onItemSelectedListener =
            object : OnItemSelectedListener {
                override fun onItemSelected(
                    arg0: AdapterView<*>?,
                    arg1: View?,
                    arg2: Int,
                    arg3: Long,
                ) {
                    val selected = arg0?.selectedItem as String
                    var amount = 0.0
                    if (selected != "Any") {
                        amount = java.lang.Double.parseDouble(selected)
                    }
                    SettingsModel.minimumRandomTagRating = amount
                }

                override fun onNothingSelected(arg0: AdapterView<*>?) {}
            }

        this.learningTrackSpinner!!.onItemSelectedListener =
            object : OnItemSelectedListener {
                override fun onItemSelected(
                    arg0: AdapterView<*>?,
                    arg1: View?,
                    arg2: Int,
                    arg3: Long,
                ) {
                    val selected = arg0?.selectedItem as String
                    var result: Boolean? = null
                    if (selected == "Yes") {
                        result = true
                    } else if (selected == "No") {
                        result = false
                    }
                    SettingsModel.randomLearningTracksFilter = result
                }

                override fun onNothingSelected(arg0: AdapterView<*>?) {}
            }
        this.sheetMusicSpinner!!.onItemSelectedListener =
            object : OnItemSelectedListener {
                override fun onItemSelected(
                    arg0: AdapterView<*>?,
                    arg1: View?,
                    arg2: Int,
                    arg3: Long,
                ) {
                    val selected = arg0?.selectedItem as String
                    var result: Boolean? = null
                    if (selected == "Yes") {
                        result = true
                    } else if (selected == "No") {
                        result = false
                    }
                    SettingsModel.randomSheetMusicFilter = result
                }

                override fun onNothingSelected(arg0: AdapterView<*>?) {}
            }
        this.findViewById<View>(R.id.clearFavoritesButton).setOnClickListener(
            OnClickListener {
                val builder = AlertDialog.Builder(this@SettingsActivity)
                builder
                    .setMessage("Are you sure you want to clear your favorite tags list?")
                    .setPositiveButton("Yes") { _, _ ->
                        FavoritesModel.resetFavorites()
                        Toast
                            .makeText(
                                this@SettingsActivity,
                                "Favorite tags cleared.",
                                Toast.LENGTH_SHORT,
                            ).show()
                    }.setNegativeButton("No") { _, _ -> }
                    .show()
            },
        )
        this.findViewById<View>(R.id.clearTeachableTags).setOnClickListener(
            OnClickListener {
                val builder = AlertDialog.Builder(this@SettingsActivity)
                builder
                    .setMessage("Are you sure you want to clear your teachable tags list?")
                    .setPositiveButton("Yes") { _, _ ->
                        TeachableTagsModel.resetTeachableTags()
                        Toast
                            .makeText(
                                this@SettingsActivity,
                                "Teachable tags cleared.",
                                Toast.LENGTH_SHORT,
                            ).show()
                    }.setNegativeButton("No") { _, _ -> }
                    .show()
            },
        )

        UiBinder.bind(this, R.id.loginButton, "Visibility", "LoggedIn", BoolConverter.get(true))
        UiBinder.bind(this, R.id.logoutButton, "Visibility", "LoggedIn", BoolConverter.get())

        this.findViewById<View>(R.id.loginButton).setOnClickListener {
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
                    ).setTheme(R.style.AppTheme)
                    .build(),
                RC_SIGN_IN,
            )
        }
        this.findViewById<View>(R.id.logoutButton).setOnClickListener(
            OnClickListener {
                AuthUI.getInstance().signOut(this@SettingsActivity).continueWith {
                    loginTrackable.updateTrackers()
                }
            },
        )

        this.findViewById<View>(R.id.changelogButton).setOnClickListener(
            OnClickListener {
                val viewer =
                    ChangelogViewer(
                        this@SettingsActivity,
                        this@SettingsActivity
                            .getString(R.string.Changelog),
                    )
                viewer.setTitle("Tag Master Changelog")
                viewer.setIcon(R.mipmap.ic_launcher)
                viewer.show()
            },
        )

        uibind(
            R.id.radio_system,
            "IsChecked",
            compiledProp { TagMasterApplication.Companion::themeMode },
            converter =
                object : ValueConverter() {
                    override fun convertToSource(
                        targetValue: Any?,
                        sourceType: Class<*>?,
                    ): Any = super.convertToSource(targetValue, sourceType)

                    override fun convertToTarget(
                        sourceValue: Any?,
                        targetType: Class<*>?,
                    ): Any = super.convertToTarget(sourceValue, targetType)
                },
        )

        this.findViewById<RadioButton>(R.id.radio_system).setOnClickListener {
            TagMasterApplication.themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        this.findViewById<RadioButton>(R.id.radio_dark).setOnClickListener {
            TagMasterApplication.themeMode = AppCompatDelegate.MODE_NIGHT_YES
        }
        this.findViewById<RadioButton>(R.id.radio_light).setOnClickListener {
            TagMasterApplication.themeMode = AppCompatDelegate.MODE_NIGHT_NO
        }
        track({ TagMasterApplication.themeMode }) {
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

        bind(
            CompoundButtonCheckedProperty(findViewById(R.id.sheetMusicWakeLockCheckBox)),
            compiledProp { SettingsModel::wakeLockOnSheetMusic },
            BindingMode.TWO_WAY,
        )

        supportActionBar?.title = "Tag Master".makeTitleString(this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = keyCode == KeyEvent.KEYCODE_BACK && this.loggingIn || super.onKeyDown(keyCode, event)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == ActionBars.HOME_MENU_ITEM_ID) {
            val intent = Intent(this, MeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            this.startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
    }

    private fun refreshCacheSize() {
        (this.findViewById(R.id.cacheSizeDisplay) as TextView).text =
            String.format(
                "%1.2f MB",
                SettingsModel.cacheSizeInMegabytes,
            )
    }

    private fun refreshLearningTracksChoice() {
        val index: Int
        if (SettingsModel.randomLearningTracksFilter == null) {
            index = 0
        } else if (SettingsModel.randomLearningTracksFilter == true) {
            index = 1
        } else {
            index = 2
        }
        this.learningTrackSpinner!!.setSelection(index)
    }

    private fun refreshMinDownload() {
        val index =
            Math.max(
                0,
                this.minDownloadChoices!!.indexOf("" + SettingsModel.minimumRandomDownloads),
            )
        this.minDownloadSpinner!!.setSelection(index)
    }

    private fun refreshMinRating() {
        val index =
            Math.max(
                0,
                this.minRatingChoices!!.indexOf("" + SettingsModel.minimumRandomTagRating.toInt()),
            )
        this.minRatingSpinner!!.setSelection(index)
    }

    private fun refreshSheetMusicChoice() {
        val index: Int
        if (SettingsModel.randomSheetMusicFilter == null) {
            index = 0
        } else if (SettingsModel.randomSheetMusicFilter == true) {
            index = 1
        } else {
            index = 2
        }
        this.sheetMusicSpinner!!.setSelection(index)
    }
}
