package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import android.view.View
import android.view.ViewGroup
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
        savedInstanceState?.getString(QUERY_MODEL)?.let { serialized ->
            model = JsonSerializer.deserialize(serialized) as? QueryModel
        }
        this.refresh()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.tagqueryview, container, false)

        try {
            (rootView.findViewById(R.id.queryResultListView) as ListView)
                .setOnScrollListener(
                    object : OnScrollListener {
                        override fun onScroll(
                            view: AbsListView,
                            firstVisibleItem: Int,
                            visibleItemCount: Int,
                            totalItemCount: Int,
                        ) {
                            if (this@TagQueryFragment.model != null &&
                                Math.abs(totalItemCount - (firstVisibleItem + visibleItemCount)) < 2
                            ) {
                                this@TagQueryFragment.model!!.fetchResults(
                                    ThreadSwitchContext(this@TagQueryFragment.activity),
                                )
                            }
                        }

                        override fun onScrollStateChanged(
                            view: AbsListView,
                            scrollState: Int,
                        ) {
                        }
                    },
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        rootView.bindTo(
            R.id.queryResultListView,
            "Adapter",
            { model?.tags },
            AdapterConverter<TagItemView>(),
        )

        rootView.bindTo(
            R.id.loadingProgressBar,
            "Visibility",
            { model?.isLoading },
            BoolConverter.get(),
        )
        rootView.bindTo(
            R.id.loadingProgressBar,
            "Indeterminate",
            { model?.isLoading },
            BoolConverter.get(),
        )

        rootView.bindTo(R.id.statusTextView, "Text", { model?.statusText })
        // The empty and error states are the same block: a sentence and a way back.
        rootView.bindTo(
            R.id.statusContainer,
            "Visibility",
            { model?.statusText },
            BoolConverter.get(),
        )

        rootView.findViewById<View>(R.id.retryButton).setOnClickListener {
            this@TagQueryFragment.refresh()
        }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Only the visible query owns Refresh, including nested Browse pages.
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.mainmenu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId != R.id.refreshMenuItem) return false
                refresh()
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        model?.let { outState.putString(QUERY_MODEL, JsonSerializer.serialize(it).toString()) }
    }

    fun onSearchRequested(): Boolean {
        val i = Intent(this.context, TagSearchActivity::class.java)
        this.startActivity(i)
        return !this.handleSearchButton
    }

    fun refresh() {
        if (this.model == null) {
            try {
                val modelString = this.activity?.intent?.extras?.getString(QUERY_MODEL)
                this.model = JsonSerializer.deserialize(modelString) as QueryModel
            } catch (e: Exception) {
                this.model = QueryModel()
            }
        }
        // A retry after a failed fetch has to be allowed to start again.
        this.model!!.hasMoreResults = true
        this.model!!.refresh(ThreadSwitchContext(this.activity))
    }

    companion object {
        @JvmField
        val QUERY_MODEL = "QueryModel"
    }
}
