package depollsoft.tagmaster

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
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
        updateEditingLabels()
    }

    private var regularView: TagItemView? = null

    val isLoading: Boolean
        @JvmName("getIsLoading")
        get() = this.tagId != null && this.tag == null && !this.failedToLoad

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

    var isEditing = false
        private set
    private var editingPosition = 0
    private var editingCount = 0
    private var editingList = ""
    private val editingActions = mutableListOf<Int>()
    val dragHandle: View get() = findViewById(R.id.savedTagDragHandle)
    val removeControl: View get() = findViewById(R.id.savedTagRemove)
    val displayName: String get() =
        tag?.title?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.saved_list_tag_id, tagId)

    internal fun setEditing(
        editing: Boolean,
        position: Int,
        count: Int,
        list: String,
        remove: () -> Unit,
        move: (Int) -> Boolean,
        startDrag: () -> Unit,
    ) {
        isEditing = editing
        isClickable = !editing
        editingPosition = position
        editingCount = count
        editingList = list
        removeControl.visibility = if (editing) VISIBLE else GONE
        dragHandle.visibility = if (editing) VISIBLE else GONE
        dragHandle.isEnabled = count > 1
        dragHandle.alpha = if (count > 1) 1f else 0.38f
        regularView?.isClickable = !editing
        regularView?.isFocusable = !editing
        regularView?.importantForAccessibility =
            if (editing) IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else IMPORTANT_FOR_ACCESSIBILITY_AUTO
        findViewById<View>(R.id.failedToLoad).importantForAccessibility =
            if (editing) IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else IMPORTANT_FOR_ACCESSIBILITY_AUTO
        isFocusable = editing || failedToLoad
        ViewCompat.setScreenReaderFocusable(this, editing)
        removeControl.setOnClickListener { if (isEditing) remove() }
        dragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN && isEditing && dragHandle.isEnabled) startDrag()
            // Interception sends CANCEL to this child. It is NOT cancellation of the drag.
            false
        }
        dragHandle.setOnKeyListener { _, key, event ->
            if (isEditing && event.action == KeyEvent.ACTION_DOWN && event.isAltPressed) {
                when (key) {
                    KeyEvent.KEYCODE_DPAD_UP -> move(-1)
                    KeyEvent.KEYCODE_DPAD_DOWN -> move(1)
                    else -> false
                }
            } else {
                false
            }
        }
        editingActions.forEach { ViewCompat.removeAccessibilityAction(this, it) }
        editingActions.clear()
        if (editing) {
            editingActions +=
                ViewCompat.addAccessibilityAction(this, context.getString(R.string.saved_list_remove)) { _, _ ->
                    remove()
                    true
                }
            if (position > 0) {
                editingActions += ViewCompat.addAccessibilityAction(this, context.getString(R.string.MoveUp)) { _, _ -> move(-1) }
            }
            if (position < count - 1) {
                editingActions +=
                    ViewCompat.addAccessibilityAction(this, context.getString(R.string.MoveDown)) { _, _ -> move(1) }
            }
        }
        updateEditingLabels()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val metadata = regularView?.findViewById<LinearLayout>(R.id.linearLayout2)
        val lines = mutableListOf(findViewById<LinearLayout>(R.id.failedToLoad))
        if (metadata != null) {
            for (index in 0 until metadata.childCount) {
                (metadata.getChildAt(index) as? LinearLayout)?.let { lines.add(it) }
            }
        }
        var changed = false
        for (line in lines) {
            val orientation =
                if (isEditing && line.measuredWidth > 0 && unwrappedWidth(line) > line.measuredWidth) {
                    LinearLayout.VERTICAL
                } else {
                    LinearLayout.HORIZONTAL
                }
            if (line.orientation != orientation) {
                line.orientation = orientation
                changed = true
            }
        }
        if (changed) super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    // Saved rows alone lose width to editing controls. Keep metadata inline when it fits and
    // wrap whole label/value groups when it does not, without changing TagItemView elsewhere.
    private fun unwrappedWidth(view: View): Float {
        if (view.visibility == GONE) return 0f
        val content =
            when (view) {
                is TextView -> {
                    view.paint.measureText(view.text.toString())
                }

                is ViewGroup -> {
                    (0 until view.childCount)
                        .sumOf { index ->
                            val child = view.getChildAt(index)
                            val margins = child.layoutParams as? ViewGroup.MarginLayoutParams
                            (
                                unwrappedWidth(child) +
                                    if (child.visibility == GONE) {
                                        0
                                    } else {
                                        (margins?.leftMargin ?: 0) + (margins?.rightMargin ?: 0)
                                    }
                            ).toDouble()
                        }.toFloat()
                }

                else -> {
                    view.measuredWidth.toFloat()
                }
            }
        return content + view.paddingLeft + view.paddingRight
    }

    private fun updateEditingLabels() {
        if (regularView == null) return
        removeControl.contentDescription = context.getString(R.string.saved_list_remove_named, displayName, editingList)
        dragHandle.contentDescription =
            context.getString(R.string.saved_list_drag_named, displayName, editingPosition + 1, editingCount, editingList)
        contentDescription =
            if (isEditing) {
                context.getString(
                    R.string.saved_list_position,
                    displayName,
                    editingPosition + 1,
                    editingCount,
                    editingList,
                )
            } else {
                null
            }
    }

    private fun init() {
        View.inflate(this.context, R.layout.savedtagitemview, this)
        this.isLongClickable = false

        this.setOnClickListener {
            val tagId = this.tagId
            if (isEditing || !failedToLoad || tagId == null) {
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Tag.getTagUri(tagId)))
            context.startActivity(intent)
        }

        this.regularView = this.findViewById<View>(R.id.tagItemView) as TagItemView
        regularView?.isLongClickable = false

        UiBinder.bind(this, R.id.loadingBar, "Loading", "IsLoading")
        UiBinder.bind(this, R.id.tagItemView, "Visibility", "Tag", BoolConverter.get())
        UiBinder.bind(this, R.id.failedToLoad, "Visibility", "FailedToLoad", BoolConverter.get())
        UiBinder.bind(this, R.id.tagId, "Text", "TagId", ToStringConverter())
    }
}
