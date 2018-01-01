package depollsoft.tagmaster

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.bindroid.converters.BoolConverter
import com.bindroid.converters.ToStringConverter
import com.bindroid.trackable.TrackableBoolean
import com.bindroid.trackable.TrackableField
import com.bindroid.ui.BoundUi
import com.bindroid.ui.UiBinder
import depollsoft.tagmaster.barbershop.Tag

abstract class SavedTagItemView : FrameLayout, BoundUi<Int> {
    constructor(context: Context) : super(context) {
        this.init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        this.init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        this.init()
    }

    var tagId: Int? by TrackableField()
    private val tag = TrackableField<Tag>()
    private var regularView: TagItemView? = null
    val isLoading: Boolean
        @JvmName("getIsLoading")
        get () = this.tag.get() == null && !this.failedToLoad
    var failedToLoad: Boolean by TrackableBoolean(false)
    override fun bind(dataSource: Int?) {
        if (dataSource == tagId)
            return
        tagId = dataSource
        this.setTag(null)
        Tag.loadTagById(dataSource!!).continueWith { task ->
            if (task.isFaulted) {
                Log.e("depollsoft.tagmaster", "Failed to load tag", task.error)
                this.failedToLoad = true
            } else {
                setTag(task.result)
            }
            null
        }
    }

    override fun getTag(): Tag {
        return this.tag.get()
    }

    private fun init() {
        View.inflate(this.context, R.layout.savedtagitemview, this)
        this.isLongClickable = true
        this.setOnLongClickListener {
            showContextMenu()
            true
        }

        this.setOnClickListener {
            val tagId = this.tagId
            if (!failedToLoad || tagId == null) {
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Tag.getTagUri(tagId)))
            context.startActivity(intent)
        }

        this.regularView = this.findViewById<View>(R.id.tagItemView) as TagItemView

        UiBinder.bind(this, R.id.loadingBar, "Visibility", "IsLoading", BoolConverter.get())
        UiBinder.bind(this, R.id.tagItemView, "Visibility", "Tag", BoolConverter.get())
        UiBinder.bind(this, R.id.failedToLoad, "Visibility", "FailedToLoad", BoolConverter.get())
        UiBinder.bind(this, R.id.tagId, "Text", "TagId", ToStringConverter())
    }

    fun setTag(value: Tag?) {
        this.tag.set(value)
        this.regularView!!.bind(value)
    }
}