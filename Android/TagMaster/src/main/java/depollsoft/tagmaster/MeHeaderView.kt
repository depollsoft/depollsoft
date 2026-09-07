package depollsoft.tagmaster

import android.content.Context
import android.content.Intent
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import bolts.Continuation
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.barbershop.TagQueryResult
import java.util.Random

/**
 * The three ways into a tag.
 *
 * "Find a tag" moves the desk to its Search destination when the view is hosted
 * by the shell, and opens the search screen on its own when it is not — the same
 * action either way.
 */
class MeHeaderView : LinearLayout {
    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private val progress: LinearProgressIndicator?
        get() = findViewById(R.id.homeProgress)

    private fun init() {
        inflate(this.context, R.layout.meviewheader, this)
        if (this.isInEditMode) return

        findViewById<View>(R.id.findTagButton).setOnClickListener {
            val host = deskHost()
            if (host != null) {
                host.showDestination(MeActivity.SEARCH)
            } else {
                context.startActivity(Intent(context, TagSearchActivity::class.java))
            }
        }

        findViewById<View>(R.id.randomTagButton).setOnClickListener { loadRandomTag() }

        findViewById<View>(R.id.openByIdButton).setOnClickListener { promptForTagId() }
    }

    private fun deskHost(): MeActivity? {
        var ctx: Context? = this.context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is MeActivity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private fun setBusy(busy: Boolean) {
        progress?.visibility = if (busy) View.VISIBLE else View.GONE
        findViewById<View>(R.id.randomTagButton).isEnabled = !busy
    }

    private fun report(
        message: String,
        retry: (() -> Unit)? = null,
    ) {
        val bar = Snackbar.make(this, message, Snackbar.LENGTH_LONG)
        if (retry != null) {
            bar.setAction(R.string.Retry) { retry() }
        }
        bar.show()
    }

    /**
     * Picks a tag uniformly from everything that passes the singer's Random Tag
     * filters: one query for the count, one for the tag at that offset.
     */
    private fun loadRandomTag() {
        setBusy(true)
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
            "id",
        ).continueWith(
            Continuation<TagQueryResult, Void?> { task ->
                if (task.isFaulted) {
                    post {
                        setBusy(false)
                        report(context.getString(R.string.RandomTagFailed)) { loadRandomTag() }
                    }
                    return@Continuation null
                }
                if (task.result.available == 0) {
                    post {
                        setBusy(false)
                        report(context.getString(R.string.RandomTagNoMatches))
                    }
                    return@Continuation null
                }
                val chosenNumber = Random().nextInt(task.result.available)
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
                    "id",
                ).continueWith<Void> { inner ->
                    post {
                        setBusy(false)
                        if (inner.isFaulted || inner.result.tags.isEmpty()) {
                            report(context.getString(R.string.RandomTagFailed)) { loadRandomTag() }
                        } else {
                            openTag(inner.result.tags[0].id)
                        }
                    }
                    null
                }
                null
            },
        )
    }

    private fun promptForTagId() {
        val field = TextInputLayout(context, null, com.google.android.material.R.attr.textInputOutlinedStyle)
        val editText = TextInputEditText(field.context)
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        editText.imeOptions = EditorInfo.IME_ACTION_GO
        field.hint = context.getString(R.string.TagId)
        field.addView(editText)
        val padding = resources.getDimensionPixelSize(R.dimen.tm_space_xl)
        field.setPadding(padding, padding / 2, padding, 0)

        val dlg =
            AlertDialog
                .Builder(context)
                .setCancelable(true)
                .setNegativeButton(R.string.Cancel, null)
                .setTitle(R.string.EnterTagId)
                .setPositiveButton(R.string.Open, null)
                .setView(field)
                .create()
        fun submit() {
            val id = editText.text?.toString()?.trim()?.toIntOrNull()
            if (id == null || id <= 0) {
                field.error = context.getString(R.string.InvalidTagId)
                return
            }
            openTag(id)
            dlg.dismiss()
        }
        editText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            ) {
                submit()
                true
            } else {
                false
            }
        }
        dlg.show()
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { submit() }
        editText.requestFocus()
    }

    private fun openTag(id: Int) {
        val i = Intent(this.context, TagDetailActivity::class.java)
        i.putExtra(TagDetailActivity.TAG_ID_EXTRA, id)
        this.context.startActivity(i)
    }
}
