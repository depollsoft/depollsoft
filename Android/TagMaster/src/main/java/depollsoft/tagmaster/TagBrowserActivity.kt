package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import depollsoft.tagmaster.barbershop.TagCollection
import depollsoft.tagmaster.barbershop.TagSortOptions

class TagBrowserActivity : AppCompatActivity() {
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

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = this.findViewById<ViewPager2>(R.id.viewPager)

        viewPager.adapter =
            object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
                override fun getItemCount(): Int = 4

                override fun createFragment(position: Int): Fragment {
                    val fragment = TagQueryFragment()
                    fragment.model =
                        when (position) {
                            0 -> latestModel
                            1 -> ratingModel
                            2 -> downloadsModel
                            3 -> classicModel
                            else -> QueryModel()
                        }
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
    }

    override fun onSupportNavigateUp() = navigateUpOrHome()
}
