package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.bindroid.BindingMode
import com.bindroid.converters.AdapterConverter
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.TrackableCollection
import com.bindroid.ui.UiBinder
import com.bindroid.utils.Function
import com.bindroid.utils.Property
import com.bindroid.utils.ReflectedProperty
import depollsoft.lib.compat.ui.ActionBars

class TeachableTagsActivity : AppCompatActivity() {

    val teachableTags: TrackableCollection<Int>
        get() = TeachableTagsModel.getTeachableTagIds()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setContentView(R.layout.teachabletagsview)

        UiBinder.bind(this, R.id.teachableTagsItemsControl, "Adapter", "TeachableTags",
                AdapterConverter(TeachableTagItemView::class.java, false, true))
        UiBinder.bind(ReflectedProperty(this.findViewById(R.id.noTeachableTagsTextView),
                "Visibility"), Property(Function { this@TeachableTagsActivity.teachableTags.size == 0 }, null, Boolean::class.java), BindingMode.ONE_WAY, BoolConverter.get())

        supportActionBar?.title = "Tag Master".makeTitleString(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == ActionBars.HOME_MENU_ITEM_ID) {
            val intent = Intent(this, MeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            this.startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSearchRequested(): Boolean {
        val i = Intent(this, TagSearchActivity::class.java)
        this.startActivity(i)
        return true
    }
}
