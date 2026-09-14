package depollsoft.tagmaster

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

/**
 * One fixed view inside a RecyclerView (home header and footer joined to the favorites list with
 * a ConcatAdapter). A pre-built [view] is reused as-is so its state survives scrolling off screen;
 * otherwise [layout] is inflated.
 */
class StaticViewAdapter(
    @LayoutRes private val layout: Int = 0,
    private val view: View? = null,
) : RecyclerView.Adapter<StaticViewAdapter.Holder>() {
    class Holder(
        view: View,
    ) : RecyclerView.ViewHolder(view)

    override fun getItemCount(): Int = 1

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): Holder {
        val content =
            view?.also { (it.parent as? ViewGroup)?.removeView(it) }
                ?: LayoutInflater.from(parent.context).inflate(layout, parent, false)
        if (content.layoutParams == null) {
            content.layoutParams =
                RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return Holder(content).also { it.setIsRecyclable(false) }
    }

    override fun onBindViewHolder(
        holder: Holder,
        position: Int,
    ) = Unit
}
