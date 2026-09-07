package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.bindroid.BindingMode
import com.bindroid.trackable.trackable
import com.bindroid.ui.EditTextTextProperty
import com.bindroid.ui.UiBinder
import depollsoft.lib.json.JsonSerializer
import depollsoft.tagmaster.barbershop.TagCollection
import depollsoft.tagmaster.barbershop.TagSortOptions

/**
 * The search form, as a page the shell hosts and [TagSearchActivity] can also
 * open on its own.
 *
 * The query model, its serialization into the results screen and every filter
 * value are unchanged; this is the same search, reachable in one fewer step.
 */
class TagSearchFragment : Fragment() {

    val model: QueryModel by trackable(QueryModel())

    init {
        this.model.maxResults = Integer.MAX_VALUE
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val rootView = inflater.inflate(R.layout.tagsearchview, container, false)

        val searchBox = rootView.findViewById<EditText>(R.id.searchTextBox)
        UiBinder.bind(EditTextTextProperty(searchBox), this, "Model.Query", BindingMode.TWO_WAY)

        searchBox.setOnKeyListener { _, _, event ->
            if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                search()
                true
            } else {
                false
            }
        }

        searchBox.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search()
                true
            } else {
                false
            }
        }

        rootView.findViewById<View>(R.id.searchButton).setOnClickListener { search() }

        onSelection(rootView, R.id.sheetMusicSpinner) {
            model.hasSheetMusic = if (it == "Not important") null else it == "Yes"
        }
        onSelection(rootView, R.id.learningTracksSpinner) {
            model.hasLearningTracks = if (it == "Not important") null else it == "Yes"
        }
        onSelection(rootView, R.id.partsSpinner) {
            model.parts = if (it == "Any") null else Integer.parseInt(it)
        }
        onSelection(rootView, R.id.tagCollectionSpinner) {
            model.collection = when (it) {
                "Any" -> null
                "Classic Tags" -> TagCollection.ClassicTags
                else -> TagCollection.EasyTags
            }
        }
        onSelection(rootView, R.id.sortBySpinner) {
            model.sortBy = when (it) {
                "Title" -> TagSortOptions.Title
                "Downloads" -> TagSortOptions.Downloaded
                "Most recent" -> TagSortOptions.Posted
                "Rating" -> TagSortOptions.Rating
                else -> TagSortOptions.Classic
            }
        }

        return rootView
    }

    private fun onSelection(
        rootView: View,
        spinnerId: Int,
        apply: (String) -> Unit,
    ) {
        val spinner = rootView.findViewById<Spinner>(spinnerId)
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                val selected = parent?.selectedItem as? String ?: return
                apply(selected)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun search() {
        val intent = Intent(requireContext(), TagSearchResultsActivity::class.java)
        intent.putExtra(
            TagQueryFragment.QUERY_MODEL,
            JsonSerializer.serialize(this.model).toString(),
        )
        startActivity(intent)
    }
}
