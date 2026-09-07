package depollsoft.tagmaster

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.bindroid.converters.BoolConverter
import com.bindroid.converters.ToStringConverter
import com.bindroid.trackable.trackable
import com.bindroid.ui.BoundUi
import com.bindroid.ui.UiBinder
import com.bindroid.utils.bindTo
import depollsoft.tagmaster.TagDetailActivity
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
        this.isClickable = true
        this.isLongClickable = false
        setOnClickListener {
            if (this@TagItemView.tag != null) {
                val i = Intent(this@TagItemView.context, TagDetailActivity::class.java)
                val b = Bundle()
                b.putInt(TagDetailActivity.TAG_ID_EXTRA, this@TagItemView.tag!!.id)
                i.putExtras(b)
                this@TagItemView.context.startActivity(i)
            }
        }
        setOnLongClickListener { false }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bindTo(R.id.titleTextView, "Text", { tag?.title })
        bindTo(R.id.akaTextView, "Text", { tag?.alternativeTitle }, ToStringConverter("a.k.a. %s"))
        bindTo(R.id.akaTextView, "Visibility", {
            val alternate = tag?.alternativeTitle?.trim()
            !alternate.isNullOrEmpty() && alternate != tag?.title?.trim()
        }, BoolConverter.get())
        bindTo(R.id.materialStatusTextView, "Text", {
            tag?.let { current ->
                val materials = mutableListOf<String>()
                if (current.sheetMusicUri != null) materials.add(context.getString(R.string.SheetMusicCheck))
                if (!current.tracks.isNullOrEmpty()) materials.add(context.getString(R.string.LearningTracks))
                if (materials.isEmpty()) materials.add(context.getString(R.string.NoMaterials))
                context.getString(R.string.TagScanStatus, current.id, materials.joinToString(" · "))
            }
        })
    }
}
