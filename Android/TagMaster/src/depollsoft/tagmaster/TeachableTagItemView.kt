package depollsoft.tagmaster

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.ContextMenu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnMenuItemClickListener
import android.view.View
import android.widget.FrameLayout

import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.TrackableField
import com.bindroid.ui.BoundUi
import com.bindroid.ui.UiBinder
import com.bindroid.utils.Action
import com.bindroid.utils.ObjectUtilities

import bolts.Continuation
import bolts.Task
import com.bindroid.converters.ToStringConverter
import com.bindroid.trackable.TrackableBoolean
import depollsoft.tagmaster.barbershop.Tag

class TeachableTagItemView : SavedTagItemView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun onCreateContextMenu(menu: ContextMenu) {
        val mi = MenuInflater(this.context)
        mi.inflate(R.menu.teachabletagcontextmenu, menu)

        menu.findItem(R.id.removeTeachableTagMenuItem).setOnMenuItemClickListener {
            TeachableTagsModel.removeTeachableTag(tagId!!)
            true
        }

        menu.findItem(R.id.moveDownMenuItem).setOnMenuItemClickListener {
            TeachableTagsModel.moveDown(tagId!!)
            true
        }

        menu.findItem(R.id.moveDownMenuItem).isEnabled = TeachableTagsModel.canMoveDown(tagId!!)

        menu.findItem(R.id.moveUpMenuItem).isEnabled = TeachableTagsModel.canMoveUp(tagId!!)

        menu.findItem(R.id.moveUpMenuItem).setOnMenuItemClickListener {
            TeachableTagsModel.moveUp(tagId!!)
            true
        }
        super.onCreateContextMenu(menu)
    }

}
