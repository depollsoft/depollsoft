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
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.tagqueryview, container, false)
        rootView.applyContentInsets(bottom = false)
        rootView.findViewById<ListView>(R.id.queryResultListView).applyBottomInsetsAsPadding()
        rootView.findViewById<View>(R.id.loadingProgressBar).applyBottomInsetsAsMargin()

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
                        ) {}
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
            "Loading",
            { model?.isLoading },
            BoolConverter.get(),
        )

        rootView.bindTo(R.id.statusTextView, "Text", { model?.statusText })
        rootView.bindTo(
            R.id.statusTextView,
            "Visibility",
            { model?.statusText },
            BoolConverter.get(),
        )

        return rootView
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<BarberPoleLoadingView>(R.id.loadingProgressBar)?.apply {
            hostResumed = true
            // Reconcile retained-query state after the window has resumed and its insets are applied.
            post {
                if (!isResumed) return@post
                loading = model?.isLoading == true
                (parent as? View)?.requestLayout()
            }
        }
    }

    override fun onPause() {
        view?.findViewById<BarberPoleLoadingView>(R.id.loadingProgressBar)?.hostResumed = false
        super.onPause()
    }

    override fun onDestroyView() {
        view?.findViewById<BarberPoleLoadingView>(R.id.loadingProgressBar)?.apply {
            hostResumed = false
            loading = false
        }
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater,
    ) {
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

    /** Keeps the row for the tag open in the detail pane visible in this list. */
    fun revealTag(id: Int) {
        val index = model?.tags?.indexOfFirst { it.id == id } ?: -1
        if (index < 0) return
        view?.findViewById<ListView>(R.id.queryResultListView)?.smoothScrollToPosition(index)
    }

    fun onSearchRequested(): Boolean {
        val i = Intent(this.context, TagSearchActivity::class.java)
        this.startActivity(i)
        return !this.handleSearchButton
    }

    fun refresh() {
        if (this.model == null) {
            try {
                val modelString =
                    this.activity
                        ?.intent
                        ?.extras
                        ?.getString(QUERY_MODEL)
                this.model = JsonSerializer.deserialize(modelString) as QueryModel
            } catch (e: Exception) {
                this.model = QueryModel()
            }
        }
        this.model!!.refresh(ThreadSwitchContext(this.activity))
    }

    companion object {
        @JvmField
        val QUERY_MODEL = "QueryModel"
    }
}
