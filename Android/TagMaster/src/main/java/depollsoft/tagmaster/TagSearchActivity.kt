package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.bindroid.BindingMode
import com.bindroid.trackable.trackable
import com.bindroid.ui.EditTextTextProperty
import com.bindroid.ui.UiBinder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import depollsoft.lib.json.JsonSerializer
import depollsoft.tagmaster.barbershop.TagCollection
import depollsoft.tagmaster.barbershop.TagSortOptions

class TagSearchActivity : AppCompatActivity() {
    var model: QueryModel by trackable(QueryModel())
        private set

    init {
        model.maxResults = Integer.MAX_VALUE
        model.sortBy = TagSortOptions.Title
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(TagQueryFragment.QUERY_MODEL)?.let { saved ->
            model = JsonSerializer.deserialize(org.json.JSONObject(saved)) as QueryModel
        }
        setContentView(R.layout.tagsearchview)
        setUpToolbar(true)
        // Both the scrolling form and the FAB are children of this inset-aware container.
        findViewById<View>(R.id.linearLayout2).applyImeAndBarInsetsAsPadding()
        findViewById<View>(R.id.searchButton).applyBottomInsetsAsMargin()
        findViewById<View>(R.id.scrollView1).applyContentInsets(bottom = false)
        // The leading search glyph is decoration; without this it is exposed as an unnamed button.
        findViewById<TextInputLayout>(R.id.searchInputLayout).setStartIconOnClickListener(null)

        UiBinder.bind(
            this,
            EditTextTextProperty(findViewById<EditText>(R.id.searchTextBox)),
            "Model.Query",
            BindingMode.TWO_WAY,
        )

        findViewById<View>(R.id.searchTextBox).setOnKeyListener { _, _, event ->
            if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                search()
                true
            } else {
                false
            }
        }
        findViewById<EditText>(R.id.searchTextBox).setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search()
                true
            } else {
                false
            }
        }
        findViewById<View>(R.id.searchButton).setOnClickListener { search() }

        val booleanChoices = listOf(null, true, false)
        bindChoice(
            R.id.sheetMusicSpinner,
            R.array.SheetMusicChoices,
            booleanChoices.indexOf(model.hasSheetMusic),
        ) { model.hasSheetMusic = booleanChoices[it] }
        bindChoice(
            R.id.learningTracksSpinner,
            R.array.LearningTracksChoices,
            booleanChoices.indexOf(model.hasLearningTracks),
        ) { model.hasLearningTracks = booleanChoices[it] }
        val parts = listOf(null, 3, 4, 5, 6, 7, 8)
        bindChoice(R.id.partsSpinner, R.array.PartsChoices, parts.indexOf(model.parts)) {
            model.parts = parts[it]
        }
        val collections = listOf(null, TagCollection.ClassicTags, TagCollection.EasyTags)
        bindChoice(
            R.id.tagCollectionSpinner,
            R.array.TagCollectionChoices,
            collections.indexOf(model.collection),
        ) { model.collection = collections[it] }
        val sorts =
            listOf(
                TagSortOptions.Title,
                TagSortOptions.Downloaded,
                TagSortOptions.Posted,
                TagSortOptions.Rating,
                TagSortOptions.Classic,
            )
        bindChoice(R.id.sortBySpinner, R.array.SortByChoices, sorts.indexOf(model.sortBy)) {
            model.sortBy = sorts[it]
        }

        supportActionBar?.title = getString(R.string.app_name).makeTitleString(this)
    }

    private fun bindChoice(
        viewId: Int,
        choicesId: Int,
        index: Int,
        onSelected: (Int) -> Unit,
    ) {
        val dropdown = findViewById<MaterialAutoCompleteTextView>(viewId)
        val choices = resources.getStringArray(choicesId)
        dropdown.setText(choices[index.coerceAtLeast(0)], false)
        dropdown.setOnItemClickListener { _, _, position, _ -> onSelected(position) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(TagQueryFragment.QUERY_MODEL, JsonSerializer.serialize(model).toString())
        super.onSaveInstanceState(outState)
    }

    override fun onSupportNavigateUp() = navigateUpOrHome()

    private fun search() {
        val searchResultsIntent = Intent(this@TagSearchActivity, TagSearchResultsActivity::class.java)
        searchResultsIntent.putExtra(
            TagQueryFragment.QUERY_MODEL,
            JsonSerializer.serialize(this@TagSearchActivity.model).toString(),
        )
        this@TagSearchActivity.startActivity(searchResultsIntent)
    }
}
