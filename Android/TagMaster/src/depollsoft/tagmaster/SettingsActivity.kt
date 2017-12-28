package depollsoft.tagmaster

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import bolts.Continuation
import bolts.Task
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.Trackable
import com.bindroid.ui.UiBinder
import com.parse.ParseFacebookUtils
import com.parse.ParseUser
import depollsoft.lib.compat.ui.ActionBars
import depollsoft.lib.ui.ChangelogViewer
import java.util.*

class SettingsActivity : AppCompatActivity() {
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
            return ParseUser.getCurrentUser() != null
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.settingsview)

        this.minDownloadSpinner = this.findViewById(R.id.minimumDownloadSpinner) as Spinner
        this.minRatingSpinner = this.findViewById(R.id.minimumRatingSpinner) as Spinner
        this.sheetMusicSpinner = this.findViewById(R.id.sheetMusicSpinner) as Spinner
        this.learningTrackSpinner = this.findViewById(R.id.learningTracksSpinner) as Spinner

        this.minDownloadChoices = Arrays.asList(*this.resources.getStringArray(
                R.array.MinDownloadChoices))
        this.minRatingChoices = Arrays.asList(*this.resources.getStringArray(
                R.array.MinRatingChoices))
        Arrays.asList(*this.resources.getStringArray(R.array.SheetMusicChoices))
        Arrays.asList(*this.resources.getStringArray(R.array.LearningTracksChoices))

        this.refreshMinDownload()
        this.refreshMinRating()
        this.refreshSheetMusicChoice()
        this.refreshLearningTracksChoice()

        this.refreshCacheSize()

        this.findViewById<View>(R.id.clearCacheButton).setOnClickListener(OnClickListener {
            val builder = AlertDialog.Builder(this@SettingsActivity)
            builder
                    .setMessage(
                            "Are you sure you want to clear your cache?  Cached sheet music and tracks will not be accessible until you are connected to the internet again.")
                    .setPositiveButton("Yes") { _, _ ->
                        SettingsModel.clearCache()
                        this@SettingsActivity.refreshCacheSize()
                        Toast.makeText(this@SettingsActivity, "Cache cleared.", Toast.LENGTH_SHORT).show()
                    }.setNegativeButton("No") { _, _ -> }.show()
        })

        this.minDownloadSpinner!!.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>, arg1: View, arg2: Int, arg3: Long) {
                val selected = arg0.selectedItem as String
                var amount = 0
                if (selected != "Any")
                    amount = Integer.parseInt(selected)
                SettingsModel.setMinimumRandomDownloads(amount)
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }
        this.minRatingSpinner!!.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>, arg1: View, arg2: Int, arg3: Long) {
                val selected = arg0.selectedItem as String
                var amount = 0.0
                if (selected != "Any")
                    amount = java.lang.Double.parseDouble(selected)
                SettingsModel.setMinimumRandomTagRating(amount)
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }

        this.learningTrackSpinner!!.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>, arg1: View, arg2: Int, arg3: Long) {
                val selected = arg0.selectedItem as String
                var result: Boolean? = null
                if (selected == "Yes")
                    result = true
                else if (selected == "No")
                    result = false
                SettingsModel.setRandomLearningTracksFilter(result)
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }
        this.sheetMusicSpinner!!.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>, arg1: View, arg2: Int, arg3: Long) {
                val selected = arg0.selectedItem as String
                var result: Boolean? = null
                if (selected == "Yes")
                    result = true
                else if (selected == "No")
                    result = false
                SettingsModel.setRandomSheetMusicFilter(result)
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }
        this.findViewById<View>(R.id.clearFavoritesButton).setOnClickListener(OnClickListener {
            val builder = AlertDialog.Builder(this@SettingsActivity)
            builder.setMessage("Are you sure you want to clear your favorite tags list?")
                    .setPositiveButton("Yes") { _, _ ->
                        FavoritesModel.resetFavorites()
                        Toast.makeText(this@SettingsActivity, "Favorite tags cleared.", Toast.LENGTH_SHORT)
                                .show()
                    }.setNegativeButton("No") { _, _ -> }.show()
        })
        this.findViewById<View>(R.id.clearTeachableTags).setOnClickListener(OnClickListener {
            val builder = AlertDialog.Builder(this@SettingsActivity)
            builder.setMessage("Are you sure you want to clear your teachable tags list?")
                    .setPositiveButton("Yes") { _, _ ->
                        TeachableTagsModel.resetTeachableTags()
                        Toast
                                .makeText(this@SettingsActivity, "Teachable tags cleared.", Toast.LENGTH_SHORT)
                                .show()
                    }.setNegativeButton("No") { _, _ -> }.show()
        })

        UiBinder.bind(this, R.id.loginButton, "Visibility", "LoggedIn", BoolConverter.get(true))
        UiBinder.bind(this, R.id.logoutButton, "Visibility", "LoggedIn", BoolConverter.get())

        this.findViewById<View>(R.id.loginButton).setOnClickListener(OnClickListener { v ->
            v.isEnabled = false
            val progress = ProgressDialog(this@SettingsActivity)
            progress.setMessage("Logging in...")
            this@SettingsActivity.loggingIn = true
            progress.show()
            ParseFacebookUtils.logInWithReadPermissionsInBackground(
                    this@SettingsActivity, Arrays.asList("public_profile")).continueWith(
                    Continuation<ParseUser, Void> { task ->
                        this@SettingsActivity.loggingIn = false
                        progress.dismiss()
                        v.isEnabled = true
                        if (task.error != null) {
                            Toast.makeText(this@SettingsActivity, "Facebook login failed.", Toast.LENGTH_SHORT)
                                    .show()
                            Log.d("Tag Master", "Failed to log in.", task.error)
                            return@Continuation null
                        }

                        if (task.result == null) {
                            Log.d("Tag Master", "User cancelled login.")
                            return@Continuation null
                        }
                        this@SettingsActivity.loginTrackable.updateTrackers()
                        if (task.result.isNew) {
                            FavoritesModel.storeToUser()
                            TeachableTagsModel.storeToUser()
                            task.result.saveEventually()
                        } else {
                            FavoritesModel.restoreFromUser()
                            TeachableTagsModel.restoreFromUser()
                        }
                        null
                    }, Task.UI_THREAD_EXECUTOR)
        })

        this.findViewById<View>(R.id.logoutButton).setOnClickListener(OnClickListener {
            val logOutTask = object : AsyncTask<Void, Void, Void>() {

                override fun doInBackground(vararg params: Void): Void? {
                    ParseUser.logOut()
                    return null
                }

                override fun onPostExecute(result: Void) {
                    super.onPostExecute(result)
                    this@SettingsActivity.loginTrackable.updateTrackers()
                }
            }
            logOutTask.execute()
        })

        this.findViewById<View>(R.id.changelogButton).setOnClickListener(OnClickListener {
            val viewer = ChangelogViewer(this@SettingsActivity, this@SettingsActivity
                    .getString(R.string.Changelog))
            viewer.setTitle("Tag Master Changelog")
            viewer.setIcon(R.mipmap.ic_launcher)
            viewer.show()
        })

        supportActionBar?.title = "Tag Master".makeTitleString(this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return keyCode == KeyEvent.KEYCODE_BACK && this.loggingIn || super.onKeyDown(keyCode, event)
    }

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
        (this.findViewById(R.id.cacheSizeDisplay) as TextView).text = String.format("%1.2f MB",
                SettingsModel.getCacheSizeInMegabytes())
    }

    private fun refreshLearningTracksChoice() {
        val index: Int
        if (SettingsModel.getRandomLearningTracksFilter() == null)
            index = 0
        else if (SettingsModel.getRandomLearningTracksFilter() == true)
            index = 1
        else
            index = 2
        this.learningTrackSpinner!!.setSelection(index)
    }

    private fun refreshMinDownload() {
        val index = Math.max(0,
                this.minDownloadChoices!!.indexOf("" + SettingsModel.getMinimumRandomDownloads()))
        this.minDownloadSpinner!!.setSelection(index)
    }

    private fun refreshMinRating() {
        val index = Math.max(0,
                this.minRatingChoices!!.indexOf("" + SettingsModel.getMinimumRandomTagRating().toInt()))
        this.minRatingSpinner!!.setSelection(index)
    }

    private fun refreshSheetMusicChoice() {
        val index: Int
        if (SettingsModel.getRandomSheetMusicFilter() == null)
            index = 0
        else if (SettingsModel.getRandomSheetMusicFilter() == true)
            index = 1
        else
            index = 2
        this.sheetMusicSpinner!!.setSelection(index)
    }
}
