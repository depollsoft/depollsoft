package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.trackable
import com.bindroid.utils.AdapterConverter
import com.bindroid.utils.bindTo
import depollsoft.lib.json.JsonSerializer
import depollsoft.lib.ui.ThreadSwitchContext

class TagQueryFragment : Fragment() {

    var model: QueryModel? by trackable()

    var handleSearchButton: Boolean by trackable(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setHasOptionsMenu(true)
        this.retainInstance = true

        this.refresh()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.tagqueryview, container, false)

        try {
            (rootView.findViewById(R.id.queryResultListView) as ListView)
                .setOnScrollListener(object : OnScrollListener {

                    override fun onScroll(
                        view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int,
                        totalItemCount: Int
                    ) {
                        if (this@TagQueryFragment.model != null && Math.abs(totalItemCount - (firstVisibleItem + visibleItemCount)) < 2) {
                            this@TagQueryFragment.model!!.fetchResults(
                                ThreadSwitchContext(this@TagQueryFragment.activity)
                            )
                        }
                    }

                    override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }

        rootView.bindTo(
            R.id.queryResultListView,
            "Adapter",
            { model?.tags },
            AdapterConverter<TagItemView>()
        )

        rootView.bindTo(
            R.id.loadingProgressBar,
            "Visibility",
            { model?.isLoading },
            BoolConverter.get()
        )
        rootView.bindTo(
            R.id.loadingProgressBar,
            "Indeterminate",
            { model?.isLoading },
            BoolConverter.get()
        )

        rootView.bindTo(R.id.statusTextView, "Text", { model?.statusText })
        rootView.bindTo(
            R.id.statusTextView,
            "Visibility",
            { model?.statusText },
            BoolConverter.get()
        )

        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        if (menu == null || inflater == null) {
            return
        }

        inflater.inflate(R.menu.mainmenu, menu)

        menu.findItem(R.id.refreshMenuItem).setOnMenuItemClickListener {
            this@TagQueryFragment.refresh()
            true
        }

        return
    }

    fun onSearchRequested(): Boolean {
        val i = Intent(this.context, TagSearchActivity::class.java)
        this.startActivity(i)
        return !this.handleSearchButton
    }

    fun refresh() {
        if (this.model == null) {
            val modelString = this.activity?.intent?.extras?.getString(QUERY_MODEL)
            this.model = JsonSerializer.deserialize(modelString) as QueryModel
        }
        this.model!!.refresh(ThreadSwitchContext(this.activity))
    }

    companion object {
        @JvmField
        val QUERY_MODEL = "QueryModel"
    }

}
