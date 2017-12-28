package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener
import android.widget.ListView
import com.bindroid.converters.AdapterConverter
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.TrackableField
import com.bindroid.ui.UiBinder
import depollsoft.lib.compat.ui.MenuItems
import depollsoft.lib.json.JsonSerializer
import depollsoft.lib.ui.ThreadSwitchContext

class TagQueryFragment : Fragment() {

    var model: QueryModel? by TrackableField()

    var handleSearchButton: Boolean by TrackableField(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setHasOptionsMenu(true)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.tagqueryview, container, false)

        try {
            (rootView.findViewById(R.id.queryResultListView) as ListView)
                    .setOnScrollListener(object : OnScrollListener {

                        override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int,
                                              totalItemCount: Int) {
                            if (this@TagQueryFragment.model != null && Math.abs(totalItemCount - (firstVisibleItem + visibleItemCount)) < 2) {
                                this@TagQueryFragment.model!!.fetchResults(
                                        ThreadSwitchContext(this@TagQueryFragment.activity))
                            }
                        }

                        override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}
                    })
        } catch (e: Exception) {
            e.printStackTrace()
        }

        UiBinder.bind(rootView, R.id.queryResultListView, "Adapter", this, "Model.Tags", AdapterConverter(
                TagItemView::class.java))

        UiBinder.bind(rootView, R.id.loadingProgressBar, "Visibility", this, "Model.IsLoading",
                BoolConverter.get())
        UiBinder.bind(rootView, R.id.loadingProgressBar, "Indeterminate", this, "Model.IsLoading",
                BoolConverter.get())

        UiBinder.bind(rootView, R.id.statusTextView, "Text", this, "Model.StatusText")
        UiBinder.bind(rootView, R.id.statusTextView, "Visibility", this, "Model.StatusText", BoolConverter.get())

        this.refresh()

        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
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
