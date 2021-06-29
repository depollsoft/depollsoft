package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import depollsoft.lib.kotlin.ui.attachToViewPager
import depollsoft.tagmaster.barbershop.TagCollection
import depollsoft.tagmaster.barbershop.TagSortOptions

class TagBrowserActivity : AppCompatActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.tagmasterview)

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

        val bottomNavigation = this.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val viewPager = this.findViewById<ViewPager>(R.id.viewPager)

        viewPager.adapter = object : FragmentPagerAdapter(supportFragmentManager) {
            override fun getCount(): Int {
                return 4
            }

            override fun getItem(position: Int): Fragment {
                val fragment = TagQueryFragment()
                fragment.model = when (position) {
                    0 -> latestModel
                    1 -> ratingModel
                    2 -> downloadsModel
                    3 -> classicModel
                    else -> QueryModel()
                }
                return fragment
            }
        }

        bottomNavigation.attachToViewPager(viewPager)

        findViewById<View>(R.id.searchButton).setOnClickListener {
            val i = Intent(this, TagSearchActivity::class.java)
            startActivity(i)
        }

        supportActionBar?.title = "Tag Master".makeTitleString(this)
    }
}