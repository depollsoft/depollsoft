package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnKeyListener
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.EditText
import android.widget.Spinner
import com.bindroid.BindingMode
import com.bindroid.trackable.TrackableField
import com.bindroid.ui.EditTextTextProperty
import com.bindroid.ui.UiBinder
import depollsoft.lib.compat.ui.ActionBars
import depollsoft.lib.json.JsonSerializer
import depollsoft.tagmaster.barbershop.TagCollection
import depollsoft.tagmaster.barbershop.TagSortOptions

class TagSearchActivity : AppCompatActivity() {

    val model: QueryModel by TrackableField(QueryModel())

    init {
        this.model.maxResults = Integer.MAX_VALUE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.tagsearchview)

        UiBinder.bind(this, EditTextTextProperty(this.findViewById(R.id.searchTextBox) as EditText),
                "Model.Query", BindingMode.TWO_WAY)

        this.findViewById<View>(R.id.searchTextBox).setOnKeyListener(OnKeyListener { _, _, event ->
            if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                this@TagSearchActivity.search()
                return@OnKeyListener true
            }
            false
        })

        this.findViewById<EditText>(R.id.searchTextBox).setOnEditorActionListener { _, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                this@TagSearchActivity.search()
                handled = true
            }
            handled
        }

        this.findViewById<View>(R.id.searchButton).setOnClickListener(OnClickListener { this@TagSearchActivity.search() })

        val sheetMusicSpinner = this.findViewById(R.id.sheetMusicSpinner) as Spinner
        sheetMusicSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>?, arg1: View?, arg2: Int, arg3: Long) {
                val selectedValue = arg0?.selectedItem as String
                if (selectedValue == "Not important")
                    this@TagSearchActivity.model.hasSheetMusic = null
                else this@TagSearchActivity.model.hasSheetMusic = selectedValue == "Yes"
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        val learningTracksSpinner = this.findViewById(R.id.learningTracksSpinner) as Spinner
        learningTracksSpinner.onItemSelectedListener = object : OnItemSelectedListener {

            override fun onItemSelected(arg0: AdapterView<*>?, arg1: View?, arg2: Int, arg3: Long) {
                val selectedValue = arg0?.selectedItem as String
                if (selectedValue == "Not important")
                    this@TagSearchActivity.model.hasLearningTracks = null
                else this@TagSearchActivity.model.hasLearningTracks = selectedValue == "Yes"
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }

        val partsSpinner = this.findViewById(R.id.partsSpinner) as Spinner
        partsSpinner.onItemSelectedListener = object : OnItemSelectedListener {

            override fun onItemSelected(arg0: AdapterView<*>?, arg1: View?, arg2: Int, arg3: Long) {
                val selectedValue = arg0?.selectedItem as String
                if (selectedValue == "Any")
                    this@TagSearchActivity.model.parts = null
                else
                    this@TagSearchActivity.model.parts = Integer.parseInt(selectedValue)
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }

        val tagCollectionSpinner = this.findViewById(R.id.tagCollectionSpinner) as Spinner
        tagCollectionSpinner.onItemSelectedListener = object : OnItemSelectedListener {

            override fun onItemSelected(arg0: AdapterView<*>?, arg1: View?, arg2: Int, arg3: Long) {
                val selectedValue = arg0?.selectedItem as String
                if (selectedValue == "Any")
                    this@TagSearchActivity.model.collection = null
                else if (selectedValue == "Classic Tags")
                    this@TagSearchActivity.model.collection = TagCollection.ClassicTags
                else
                    this@TagSearchActivity.model.collection = TagCollection.EasyTags
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }

        val sortyBySpinner = this.findViewById(R.id.sortBySpinner) as Spinner
        sortyBySpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>?, arg1: View?, arg2: Int, arg3: Long) {
                val selectedValue = arg0?.selectedItem as String
                if (selectedValue == "Title")
                    this@TagSearchActivity.model.sortBy = TagSortOptions.Title
                else if (selectedValue == "Downloads")
                    this@TagSearchActivity.model.sortBy = TagSortOptions.Downloaded
                else if (selectedValue == "Most recent")
                    this@TagSearchActivity.model.sortBy = TagSortOptions.Posted
                else if (selectedValue == "Rating")
                    this@TagSearchActivity.model.sortBy = TagSortOptions.Rating
                else
                    this@TagSearchActivity.model.sortBy = TagSortOptions.Classic
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }

        supportActionBar?.title = "Tag Master".makeTitleString(this)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null && item.itemId == ActionBars.HOME_MENU_ITEM_ID) {
            val intent = Intent(this, MeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            this.startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun search() {
        val searchResultsIntent = Intent(this@TagSearchActivity, TagSearchResultsActivity::class.java)
        searchResultsIntent.putExtra(TagQueryFragment.QUERY_MODEL,
                JsonSerializer.serialize(this@TagSearchActivity.model).toString())
        this@TagSearchActivity.startActivity(searchResultsIntent)
    }
}
