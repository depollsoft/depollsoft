package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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

class CustomListActivity : AppCompatActivity() {

    companion object {
        const val CUSTOM_LIST_KEY_EXTRA = "CUSTOM_LIST_KEY_EXTRA"
    }

    lateinit var listKey: String

    val listTags: TrackableCollection<Int>
        get() = ListModel(listKey).ids

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listKey = intent.getStringExtra(CUSTOM_LIST_KEY_EXTRA) ?: return

        this.setContentView(R.layout.customlistview)

        UiBinder.bind(this, R.id.customListItemsControl, "Adapter", "ListTags",
                AdapterConverter(CustomListTagItemView::class.java, false, true))
        UiBinder.bind(ReflectedProperty(this.findViewById(R.id.noCustomListTagsTextView),
                "Visibility"), Property(Function { this@CustomListActivity.listTags.size == 0 }, null, Boolean::class.java), BindingMode.ONE_WAY, BoolConverter.get())

        val displayName = CustomListsModel.getMetadata(listKey)?.name ?: listKey
        supportActionBar?.title = displayName.makeTitleString(this)
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
