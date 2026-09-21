package depollsoft.pitchperfect

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.content.res.ResourcesCompat

/**
 * The set list selector: one machined part with N positions, drawn exactly like the instrument's
 * range selector.
 *
 * A single round-rect frame with a 1.5dp `plate_hairline` stroke is painted over the scrolling
 * container, so it stays put while the positions scroll inside it. The positions themselves are
 * child views — they have to be focusable, tappable and individually described to accessibility —
 * each with a 1dp hairline trailing edge, the selected one carrying a 10% ink wash and the range
 * selector's lit indicator dot.
 */
class SetListSelectorView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : HorizontalScrollView(context, attrs) {
        /** Called with the list id of a tapped position. */
        var onSelect: ((String) -> Unit)? = null

        /** Called when the trailing "+" is tapped. */
        var onCreate: (() -> Unit)? = null

        /** Called with the position view and its list id when a position is long-pressed. */
        var onLongPress: ((View, String) -> Unit)? = null

        private val model get() = SongsModel.get()
        private val row = LinearLayout(context)
        private val hairline = colorOrFallback(R.color.plate_hairline, "#2C2F33")
        private val ink = colorOrFallback(R.color.plate_ink, "#D9DBDD")
        private val inkSecondary = colorOrFallback(R.color.plate_ink_secondary, "#898D92")
        private val lit = colorOrFallback(R.color.plate_accent, "#F2EFE6")
        private val framePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = hairline
                strokeWidth = dp(FRAME_STROKE_DP)
            }
        private val displayTypeface: Typeface =
            runCatching { ResourcesCompat.getFont(context, R.font.oswald_medium) }.getOrNull()
                ?: Typeface.create("sans-serif-condensed", Typeface.NORMAL)

        /** The id the positions were last built for; a re-render only rebuilds when it changed. */
        private var renderedSignature: String? = null

        init {
            isHorizontalScrollBarEnabled = false
            isFillViewport = true
            // A position half out of the frame fades toward the plate: that is the scroll cue.
            isHorizontalFadingEdgeEnabled = true
            setFadingEdgeLength(dp(FADE_DP).toInt())
            clipToOutline = true
            outlineProvider =
                object : ViewOutlineProvider() {
                    override fun getOutline(
                        view: View,
                        outline: Outline,
                    ) {
                        outline.setRoundRect(0, 0, view.width, view.height, dp(FRAME_RADIUS_DP))
                    }
                }
            row.orientation = LinearLayout.HORIZONTAL
            addView(
                row,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT),
            )
        }

        /** The position for [listId], for tests and for scrolling a selection into view. */
        fun positionView(listId: String): View? = row.findViewWithTag(listId)

        /** The trailing "+" position. */
        val addPositionView: View?
            get() = row.findViewWithTag(ADD_TAG)

        /** Rebuilds the positions from the model. Cheap to call from a tracking block. */
        fun render() {
            val lists = model.orderedLists
            val current = model.currentListId
            val signature =
                lists.joinToString("|") { "${it.id}:${model.displayName(it)}:${it.songs.size}" } +
                    "#$current"
            if (signature == renderedSignature) return
            renderedSignature = signature

            row.removeAllViews()
            lists.forEach { list ->
                val selected = list.id == current
                val position =
                    PositionView(context).apply {
                        tag = list.id
                        isSelected = selected
                        showsTrailingHairline = true
                        text = model.displayName(list).uppercase()
                        contentDescription =
                            context.getString(
                                R.string.SetListPositionDescription,
                                model.displayName(list),
                                songCountDescription(list.songs.size),
                            )
                        setOnClickListener {
                            if (model.currentListId != list.id) {
                                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onSelect?.invoke(list.id)
                            }
                        }
                        // The list's own actions, without entering edit mode first.
                        setOnLongClickListener { view ->
                            val handler = onLongPress ?: return@setOnLongClickListener false
                            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            handler(view, list.id)
                            true
                        }
                        // The long press, as a named action for screen readers.
                        ViewCompat.addAccessibilityAction(
                            this,
                            context.getString(R.string.SetListRowActions),
                        ) { view, _ ->
                            onLongPress?.invoke(view, list.id)
                            true
                        }
                    }
                // Positions share the whole part between them, so a single list is never a label
                // floating in an empty frame; once they overflow, the weights have nothing to give.
                row.addView(
                    position,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1f,
                    ),
                )
            }

            val add =
                PositionView(context).apply {
                    tag = ADD_TAG
                    isAddPosition = true
                    showsTrailingHairline = false
                    text = context.getString(R.string.SetListAddGlyph)
                    contentDescription = context.getString(R.string.SetListNew)
                    setOnClickListener {
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onCreate?.invoke()
                    }
                }
            row.addView(
                add,
                LinearLayout.LayoutParams(
                    dp(ADD_WIDTH_DP).toInt(),
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )

            scrollSelectionIntoView()
            invalidate()
        }

        private fun scrollSelectionIntoView() {
            val selected = positionView(model.currentListId) ?: return
            post {
                if (selected.parent !== row) return@post
                val target = selected.left - (width - selected.width) / 2
                scrollTo(target.coerceAtLeast(0), 0)
            }
        }

        private fun songCountDescription(count: Int): String =
            if (count == 0) {
                context.getString(R.string.NoSongsAccessibility)
            } else {
                resources.getQuantityString(R.plurals.SongCount, count, count)
            }

        override fun draw(canvas: Canvas) {
            super.draw(canvas)
            // Painted last, in viewport coordinates: a scrolling view draws its own content
            // scrolled along with its children, so the frame is translated back to stay put on
            // the plate while the positions slide through it.
            val inset = dp(FRAME_STROKE_DP) / 2f
            canvas.save()
            canvas.translate(scrollX.toFloat(), 0f)
            canvas.drawRoundRect(
                inset,
                inset,
                width - inset,
                height - inset,
                dp(FRAME_RADIUS_DP),
                dp(FRAME_RADIUS_DP),
                framePaint,
            )
            canvas.restore()
        }

        private fun dp(value: Float): Float =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

        private fun colorOrFallback(
            resourceId: Int,
            fallback: String,
        ): Int =
            runCatching { ContextCompat.getColor(context, resourceId) }
                .getOrElse { Color.parseColor(fallback) }

        /** One position on the part: an engraved label, its wash, its dot and its trailing edge. */
        private inner class PositionView(
            context: Context,
        ) : AppCompatTextView(context) {
            var showsTrailingHairline: Boolean = true
            var isAddPosition: Boolean = false
                set(value) {
                    field = value
                    applyMetrics()
                }

            private val washPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            private val edgePaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = dp(1f)
                    color = hairline
                }

            init {
                typeface = displayTypeface
                setTextSize(TypedValue.COMPLEX_UNIT_SP, LABEL_SIZE_SP)
                letterSpacing = LABEL_TRACKING
                isSingleLine = true
                ellipsize = android.text.TextUtils.TruncateAt.END
                maxWidth = dp(MAX_POSITION_WIDTH_DP).toInt()
                isClickable = true
                isLongClickable = true
                isFocusable = true
                background = null
                applyMetrics()
            }

            private fun applyMetrics() {
                if (isAddPosition) {
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, 0)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, ADD_GLYPH_SIZE_SP)
                    letterSpacing = 0f
                    setTextColor(inkSecondary)
                } else {
                    gravity = Gravity.CENTER_VERTICAL or Gravity.START
                    // Room for the dot before the label on every position, so the label never
                    // shifts when the selection moves.
                    setPadding(dp(LEADING_PADDING_DP).toInt(), 0, dp(SIDE_PADDING_DP).toInt(), 0)
                }
            }

            override fun setSelected(selected: Boolean) {
                super.setSelected(selected)
                if (isAddPosition) return
                // 75% alpha on the unselected label; full ink when this position is the current
                // list, exactly as the range selector reads.
                setTextColor(if (selected) ink else withAlpha(inkSecondary, UNSELECTED_ALPHA))
                invalidate()
            }

            override fun onDraw(canvas: Canvas) {
                if (isSelected && !isAddPosition) {
                    washPaint.color = withAlpha(ink, WASH_ALPHA)
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), washPaint)
                    dotPaint.color = lit
                    canvas.drawCircle(
                        dp(DOT_INSET_DP) + dp(DOT_SIZE_DP) / 2f,
                        height / 2f,
                        dp(DOT_SIZE_DP) / 2f,
                        dotPaint,
                    )
                }
                super.onDraw(canvas)
                if (showsTrailingHairline) {
                    val x = width - edgePaint.strokeWidth / 2f
                    canvas.drawLine(x, 0f, x, height.toFloat(), edgePaint)
                }
            }

            override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(info)
                info.className = android.widget.Button::class.java.name
            }

            private fun withAlpha(
                color: Int,
                alpha: Int,
            ): Int =
                Color.argb(
                    alpha.coerceIn(0, 255),
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color),
                )
        }

        companion object {
            const val ADD_TAG = "depollsoft.pitchperfect.setListSelector.add"
            private const val FRAME_RADIUS_DP = 5f
            private const val FRAME_STROKE_DP = 1.5f
            private const val SIDE_PADDING_DP = 14f
            private const val LEADING_PADDING_DP = 20f
            private const val FADE_DP = 24f
            private const val ADD_GLYPH_SIZE_SP = 20f
            private const val DOT_INSET_DP = 8f
            private const val DOT_SIZE_DP = 6f
            private const val ADD_WIDTH_DP = 44f
            private const val MAX_POSITION_WIDTH_DP = 180f
            private const val LABEL_SIZE_SP = 13f
            private const val LABEL_TRACKING = 0.16f
            private const val WASH_ALPHA = 26
            private const val UNSELECTED_ALPHA = 191
        }
    }
