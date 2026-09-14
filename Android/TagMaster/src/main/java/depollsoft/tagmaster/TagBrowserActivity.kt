package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import depollsoft.lib.ui.ThreadSwitchContext
import depollsoft.tagmaster.barbershop.TagCollection
import depollsoft.tagmaster.barbershop.TagSortOptions

class TagBrowserActivity :
    AppCompatActivity(),
    TagPaneHost {
    private val models = mutableListOf<QueryModel>()

    internal lateinit var tagPane: TagPaneController
        private set

    private val currentModel: QueryModel?
        get() = models.getOrNull(findViewById<ViewPager2>(R.id.viewPager)?.currentItem ?: 0)

    private val currentQueryFragment: TagQueryFragment?
        get() {
            val position = findViewById<ViewPager2>(R.id.viewPager)?.currentItem ?: return null
            return supportFragmentManager.findFragmentByTag("f$position") as? TagQueryFragment
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.tagmasterview)
        setUpToolbar(true)

        val latestModel = QueryModel()
        latestModel.maxResults = Integer.MAX_VALUE
        latestModel.sortBy = TagSortOptions.Posted

        val ratingModel = QueryModel()
        ratingModel.maxResults = Integer.MAX_VALUE
        ratingModel.sortBy = TagSortOptions.Rating

        val downloadsModel = QueryModel()
        downloadsModel.maxResults = Integer.MAX_VALUE
        downloadsModel.sortBy = TagSortOptions.Downloaded

        val classicModel = QueryModel()
        classicModel.sortBy = TagSortOptions.Classic
        classicModel.collection = TagCollection.ClassicTags
        classicModel.maxResults = 400

        models.clear()
        models.addAll(listOf(latestModel, ratingModel, downloadsModel, classicModel))

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = this.findViewById<ViewPager2>(R.id.viewPager)

        viewPager.adapter =
            object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
                override fun getItemCount(): Int = 4

                override fun createFragment(position: Int): Fragment {
                    val fragment = TagQueryFragment()
                    fragment.model = models.getOrElse(position) { QueryModel() }
                    return fragment
                }
            }

        val tabs = PopupMenu(this, tabLayout).apply { inflate(R.menu.browsenavigation) }.menu
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val item = tabs.getItem(position)
            tab.text = item.title
            tab.icon = item.icon
            tab.id = item.itemId
            tab.setCustomView(R.layout.bottom_tab_content)
        }.attach()

        tabLayout.applyHorizontalInsetsAsPadding()
        findViewById<View>(R.id.searchButton).applyBottomInsetsAsMargin()

        findViewById<View>(R.id.searchButton).setOnClickListener {
            val i = Intent(this, TagSearchActivity::class.java)
            startActivity(i)
        }

        supportActionBar?.title = getString(R.string.detail_brand_title).makeTitleString(this)

        tagPane =
            TagPaneController(
                activity = this,
                listedIds = { currentModel?.tags?.map { it.id } ?: emptyList() },
                reveal = { id -> currentQueryFragment?.revealTag(id) },
                hasMoreResults = { currentModel?.hasMoreResults == true },
                fetchMore = { currentModel?.fetchResults(ThreadSwitchContext(this)) },
            )
        tagPane.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        tagPane.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override val hasDetailPane: Boolean
        get() = tagPane.hasDetailPane

    override var selectedTagId: Int?
        get() = tagPane.selectedTagId
        set(value) {
            tagPane.selectedTagId = value
        }

    override fun showTag(id: Int) = tagPane.showTag(id)

    override fun listedTagIds(): List<Int> = tagPane.listedTagIds()

    override fun revealTag(id: Int) = tagPane.revealTag(id)

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = tagPane.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)

    override fun onSupportNavigateUp() = navigateUpOrHome()
}
