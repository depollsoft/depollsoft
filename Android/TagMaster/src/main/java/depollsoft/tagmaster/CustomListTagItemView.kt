package depollsoft.tagmaster

import android.content.Context
import android.util.AttributeSet
import android.view.ContextMenu
import android.view.MenuInflater

class CustomListTagItemView : SavedTagItemView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    private val listKey: String?
        get() {
            var ctx = context
            if (ctx is CustomListActivity) {
                return ctx.listKey
            }
            return null
        }

    override fun onCreateContextMenu(menu: ContextMenu) {
        val mi = MenuInflater(this.context)
        mi.inflate(R.menu.customlistcontextmenu, menu)

        val key = listKey ?: return
        val model = ListModel(key)

        menu.findItem(R.id.removeFromListMenuItem).setOnMenuItemClickListener {
            model.remove(tagId!!)
            true
        }

        menu.findItem(R.id.moveDownMenuItem).setOnMenuItemClickListener {
            model.moveDown(tagId!!)
            true
        }

        menu.findItem(R.id.moveDownMenuItem).isEnabled = model.canMoveDown(tagId!!)

        menu.findItem(R.id.moveUpMenuItem).isEnabled = model.canMoveUp(tagId!!)

        menu.findItem(R.id.moveUpMenuItem).setOnMenuItemClickListener {
            model.moveUp(tagId!!)
            true
        }
        super.onCreateContextMenu(menu)
    }
}
