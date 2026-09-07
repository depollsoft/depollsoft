package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import depollsoft.tagmaster.barbershop.TagCollection
import depollsoft.tagmaster.barbershop.TagSortOptions

/** Collection is a labeled native selection, not another navigation level. */
class TagBrowseFragment : Fragment() {
    private var collectionIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val root = inflater.inflate(R.layout.tagmasterview, container, false)
        collectionIndex = savedInstanceState?.getInt("collection") ?: collectionIndex
        root.findViewById<View>(R.id.catalogSearchButton).setOnClickListener {
            startActivity(Intent(requireContext(), TagSearchActivity::class.java))
        }
        root.findViewById<Spinner>(R.id.browseCollectionSpinner).apply {
            adapter =
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf(R.string.latest, R.string.Rating, R.string.Downloads, R.string.classic).map { getString(it) },
                )
            setSelection(collectionIndex)
            onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}

                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long,
                    ) {
                        showCollection(position)
                    }
                }
        }
        return root
    }

    private fun showCollection(position: Int) {
        collectionIndex = position
        val transaction = childFragmentManager.beginTransaction()
        for (index in 0..3) {
            var fragment = childFragmentManager.findFragmentByTag("collection$index")
            if (index == position && fragment == null) {
                fragment =
                    TagQueryFragment().apply {
                        model =
                            QueryModel().apply {
                                maxResults = if (index == 3) 400 else Integer.MAX_VALUE
                                sortBy =
                                    listOf(
                                        TagSortOptions.Posted,
                                        TagSortOptions.Rating,
                                        TagSortOptions.Downloaded,
                                        TagSortOptions.Classic,
                                    )[index]
                                if (index == 3) collection = TagCollection.ClassicTags
                            }
                    }
                transaction.add(R.id.browseResultsContainer, fragment, "collection$index")
            }
            fragment?.let {
                if (index == position) transaction.show(it) else transaction.hide(it)
                transaction.setMaxLifecycle(it, if (index == position) Lifecycle.State.RESUMED else Lifecycle.State.STARTED)
            }
        }
        transaction.commitNow()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("collection", collectionIndex)
        super.onSaveInstanceState(outState)
    }
}
