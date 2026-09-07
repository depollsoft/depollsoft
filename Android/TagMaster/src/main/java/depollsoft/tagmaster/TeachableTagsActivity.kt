package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bindroid.BindingMode
import com.bindroid.converters.AdapterConverter
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.TrackableCollection
import com.bindroid.ui.UiBinder
import com.bindroid.utils.Function
import com.bindroid.utils.Property
import com.bindroid.utils.ReflectedProperty

class TeachableTagsActivity : AppCompatActivity() {
    val teachableTags: TrackableCollection<Int>
        get() = TeachableTagsModel.teachableTagIds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDeskBack()

        this.setContentView(R.layout.teachabletagsview)

        UiBinder.bind(
            this,
            R.id.teachableTagsItemsControl,
            "Adapter",
            "TeachableTags",
            AdapterConverter(TeachableTagItemView::class.java, false, true),
        )
        UiBinder.bind(
            ReflectedProperty(
                this.findViewById(R.id.noTeachableTagsTextView),
                "Visibility",
            ),
            Property(
                Function {
                    this@TeachableTagsActivity.teachableTags.size == 0
                },
                null,
                Boolean::class.java,
            ),
            BindingMode.ONE_WAY,
            BoolConverter.get(),
        )

        findViewById<android.view.View>(R.id.teachableRoot).applyDeskInsets()

        supportActionBar?.title = getString(R.string.TeachableTags).makeTitleString(this)
    }

    override fun onSearchRequested(): Boolean {
        val i = Intent(this, TagSearchActivity::class.java)
        this.startActivity(i)
        return true
    }
}
