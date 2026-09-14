package depollsoft.tagmaster

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import com.bindroid.BindingMode
import com.bindroid.converters.BoolConverter
import com.bindroid.converters.ToStringConverter
import com.bindroid.trackable.trackable
import com.bindroid.ui.BoundUi
import com.bindroid.ui.UiBinder
import com.bindroid.utils.Property
import com.bindroid.utils.bindTo
import depollsoft.tagmaster.barbershop.Tag

class TagItemView :
    LinearLayout,
    BoundUi<Tag?> {
    var tag: Tag? by trackable()

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    override fun bind(dataSource: Tag?) {
        this.tag = dataSource
    }

    private fun init() {
        val inflater =
            this.context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE,
            ) as LayoutInflater
        inflater.inflate(R.layout.tagitemview, this, true)
        // Carries the lit state of the tag open in the detail pane as well as the touch ripple.
        setBackgroundResource(R.drawable.tag_row_background)
        this.isFocusable = true
        this.isClickable = true
        this.isLongClickable = false
        setOnClickListener {
            val id = this@TagItemView.tag?.id ?: return@setOnClickListener
            val host = this@TagItemView.context as? TagPaneHost
            if (host != null && host.hasDetailPane) {
                host.showTag(id)
            } else {
                val i = Intent(this@TagItemView.context, TagDetailActivity::class.java)
                val b = Bundle()
                b.putInt(TagDetailActivity.TAG_ID_EXTRA, id)
                i.putExtras(b)
                this@TagItemView.context.startActivity(i)
            }
        }
        setOnLongClickListener { false }
    }

    /** True while this row's tag is the one open in the detail pane. */
    private val isShowing: Boolean
        get() {
            val host = context as? TagPaneHost ?: return false
            val selected = host.selectedTagId
            return selected != null && selected == tag?.id
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Both the fill and the spoken state come from the same trackable read, so the row lights
        // and unlights as the pane's selection moves.
        UiBinder.bind(
            Property<Boolean>(null, { showing ->
                isActivated = showing == true
                ViewCompat.setStateDescription(this, if (showing == true) context.getString(R.string.tag_row_showing) else null)
            }, Boolean::class.java),
            Property<Boolean>({ isShowing }, null, Boolean::class.java),
            BindingMode.ONE_WAY,
        )
        bindTo(R.id.titleTextView, "Text", { tag?.title })
        bindTo(
            R.id.akaTextView,
            "Text",
            { tag?.alternativeTitle },
            ToStringConverter(context.getString(R.string.alternative_title_format)),
        )
        bindTo(
            R.id.akaTextView,
            "Visibility",
            { tag?.alternativeTitle },
            BoolConverter.get(),
        )
        bindTo(R.id.idTextView, "Text", { tag?.id }, ToStringConverter())
        bindTo(
            R.id.ratingTextView,
            "Text",
            { tag?.rating },
            ToStringConverter("%3.2f"),
        )
        bindTo(
            R.id.ratingContainer,
            "Visibility",
            { tag?.rating },
            BoolConverter.get(),
        )
        bindTo(R.id.postedTextView, "Text", { tag?.posted }, NullableDateConverter(" %tD"))
        bindTo(R.id.postedContainer, "Visibility", { tag?.posted }, BoolConverter.get())
        bindTo(R.id.downloadsTextView, "Text", { tag?.downloadCount }, ToStringConverter("%d"))
        bindTo(R.id.downloadsContainer, "Visibility", { tag?.downloadCount }, BoolConverter.get())
        bindTo(R.id.sheetMusicCheckBox, "IsAvailable", { tag?.sheetMusicUri }, BoolConverter.get())
        bindTo(
            R.id.learningTracksCheckBox,
            "IsAvailable",
            { tag?.tracks },
            BoolConverter.get(false, true),
        )
    }
}
