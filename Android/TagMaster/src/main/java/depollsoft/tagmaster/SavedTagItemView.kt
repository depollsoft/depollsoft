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
import com.bindroid.trackable.trackable
import com.bindroid.ui.BoundUi
import com.bindroid.ui.UiBinder
import depollsoft.tagmaster.barbershop.Tag
import kotlinx.coroutines.*

abstract class SavedTagItemView :
    FrameLayout,
    BoundUi<Int> {
    constructor(context: Context) : super(context) {
        this.init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        this.init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle,
    ) {
        this.init()
    }

    var tagId: Int? by trackable()
    var tag: Tag? by trackable {
        this.regularView!!.bind(it)
    }

    private var regularView: TagItemView? = null

    val isLoading: Boolean
        @JvmName("getIsLoading")
        get() = this.tag == null && !this.failedToLoad

    var failedToLoad: Boolean by trackable(false)

    private var loadScope: CoroutineScope? = null
    private var loadJob: Job? = null

    override fun bind(dataSource: Int?) {
        if (dataSource == tagId) return
        loadJob?.cancel()
        tagId = dataSource
        tag = null
        failedToLoad = false
        loadTag()
    }

    private fun loadTag() {
        val id = tagId ?: return
        loadJob =
            loadScope?.launch {
                try {
                    tag = Tag.loadTagById(id).await()
                    if (tag == null) failedToLoad = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("depollsoft.tagmaster", "Failed to load tag", e)
                    failedToLoad = true
                }
            }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        loadScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        if (tag == null && !failedToLoad) loadTag()
    }

    override fun onDetachedFromWindow() {
        loadScope?.cancel()
        loadScope = null
        loadJob = null
        super.onDetachedFromWindow()
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
        regularView?.setOnLongClickListener { showContextMenu() }
        findViewById<View>(R.id.savedTagMoreOptions).setOnClickListener { button ->
            if (tagId != null) {
                val rowLocation = IntArray(2)
                val buttonLocation = IntArray(2)
                getLocationOnScreen(rowLocation)
                button.getLocationOnScreen(buttonLocation)
                showContextMenu(
                    (buttonLocation[0] - rowLocation[0] + button.width / 2).toFloat(),
                    (buttonLocation[1] - rowLocation[1] + button.height / 2).toFloat(),
                )
            }
        }

        UiBinder.bind(this, R.id.loadingBar, "Visibility", "IsLoading", BoolConverter.get())
        UiBinder.bind(this, R.id.tagItemView, "Visibility", "Tag", BoolConverter.get())
        UiBinder.bind(this, R.id.failedToLoad, "Visibility", "FailedToLoad", BoolConverter.get())
        UiBinder.bind(this, R.id.tagId, "Text", "TagId", ToStringConverter())
    }
}
