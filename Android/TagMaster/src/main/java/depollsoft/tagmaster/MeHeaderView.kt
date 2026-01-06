package depollsoft.tagmaster

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import bolts.Continuation
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.TeachableTagsModel.teachableTagIds
import depollsoft.tagmaster.barbershop.TagQueryResult
import com.bindroid.ui.UiBinder
import com.bindroid.utils.ReflectedProperty
import com.bindroid.BindingMode
import com.bindroid.converters.BoolConverter
import com.bindroid.utils.Property
import depollsoft.lib.kotlin.ui.safeDismiss
import java.util.*

class MeHeaderView : LinearLayout {
    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        inflate(this.context, R.layout.meviewheader, this)
        if (!this.isInEditMode) {
            val randomTagButton = findViewById<View>(R.id.randomTagButton)
            randomTagButton.setOnClickListener {
                val progress = ProgressDialog(this@MeHeaderView.context)
                progress.setMessage("Loading...")
                progress.isIndeterminate = true
                progress.show()
                Tag.query(
                    null,
                    0,
                    0,
                    null,
                    SettingsModel.randomLearningTracksFilter,
                    SettingsModel.randomSheetMusicFilter,
                    null,
                    null,
                    SettingsModel.minimumRandomTagRating,
                    SettingsModel.minimumRandomDownloads,
                    false,
                    "id"
                ).continueWith(Continuation<TagQueryResult, Void?> { task ->
                    if (task.isFaulted) {
                        post {
                            progress.safeDismiss()
                            Toast.makeText(
                                this@MeHeaderView.context, "Could not load a random tag.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        if (task.result.available == 0) {
                            post {
                                progress.safeDismiss()
                                Toast
                                    .makeText(
                                        this@MeHeaderView.context,
                                        "No tags that match your filters could be found.  Please adjust your filters using the Settings menu.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                            }
                            return@Continuation null
                        }
                        val r = Random()
                        val chosenNumber = r.nextInt(task.result.available)
                        Tag.query(
                            null,
                            1,
                            chosenNumber,
                            null,
                            SettingsModel.randomLearningTracksFilter,
                            SettingsModel.randomSheetMusicFilter,
                            null,
                            null,
                            SettingsModel.minimumRandomTagRating,
                            SettingsModel.minimumRandomDownloads,
                            false,
                            "id"
                        ).continueWith<Void> { task ->
                            if (task.isFaulted) {
                                post {
                                    progress.safeDismiss()
                                    Toast.makeText(
                                        this@MeHeaderView.context,
                                        "Could not load a random tag.", Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                post {
                                    val i = Intent(
                                        this@MeHeaderView.context,
                                        TagDetailActivity::class.java
                                    )
                                    i.putExtra(
                                        TagDetailActivity.TAG_ID_EXTRA, task.result.tags[0]
                                            .id
                                    )
                                    this@MeHeaderView.context.startActivity(i)
                                    progress.safeDismiss()
                                }
                            }
                            null
                        }
                    }
                    null
                })
            }
            val browseButton = findViewById<View>(R.id.browseButton)
            browseButton.setOnClickListener {
                val i = Intent(this@MeHeaderView.context, TagBrowserActivity::class.java)
                this@MeHeaderView.context.startActivity(i)
            }
            val teachableButton = findViewById<View>(R.id.teachableButton)
            teachableButton.setOnClickListener {
                val i = Intent(this@MeHeaderView.context, TeachableTagsActivity::class.java)
                this@MeHeaderView.context.startActivity(i)
            }
            val openTagByIdButton = findViewById<View>(R.id.openByIdButton)
            openTagByIdButton.setOnClickListener { v ->
                val editText = EditText(v.context)
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                editText.setImeActionLabel("Open", KeyEvent.KEYCODE_ENTER)
                val dlg = AlertDialog.Builder(context)
                    .setCancelable(true)
                    .setNegativeButton("Cancel", null)
                    .setTitle("Enter Tag ID")
                    .setPositiveButton("Open") { dialog: DialogInterface?, which: Int ->
                        if (editText.text.toString().isEmpty()) {
                            return@setPositiveButton
                        }
                        openTag(editText.text.toString().toInt())
                    }
                    .setView(editText)
                    .create()
                editText.setOnEditorActionListener { v1: TextView?, actionId: Int, event: KeyEvent? ->
                    if (event == null || event?.action == KeyEvent.ACTION_UP) {
                        if (editText.text.toString().isEmpty()) {
                            return@setOnEditorActionListener false
                        }
                        openTag(editText.text.toString().toInt())
                        dlg.safeDismiss()
                    }
                    true
                }
                dlg.show()
                editText.requestFocus()
            }
        }
    }

    private fun openTag(id: Int) {
        val i = Intent(
            this@MeHeaderView.context,
            TagDetailActivity::class.java
        )
        i.putExtra(
            TagDetailActivity.TAG_ID_EXTRA,
            id
        )
        this.context.startActivity(i)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!this.isInEditMode) {
            UiBinder.bind(ReflectedProperty(findViewById(R.id.teachableButton), "Visibility"),
                Property({ teachableTagIds.size > 0 }, null, Boolean::class.java),
                BindingMode.ONE_WAY,
                BoolConverter.get()
            )
        }
    }
}