package depollsoft.tagmaster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import depollsoft.tagmaster.barbershop.TagCollection
import depollsoft.tagmaster.barbershop.TagSortOptions

/**
 * Browse, as a page the shell can host and [TagBrowserActivity] can open on its
 * own. The four query models and their ordering are unchanged from the previous
 * bottom-navigation version; only the control that switches between them moved.
 */
class TagBrowseFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val rootView = inflater.inflate(R.layout.tagmasterview, container, false)

        val models = listOf(
            QueryModel().apply {
                maxResults = Integer.MAX_VALUE
                sortBy = TagSortOptions.Posted
            },
            QueryModel().apply {
                maxResults = Integer.MAX_VALUE
                sortBy = TagSortOptions.Rating
            },
            QueryModel().apply {
                maxResults = Integer.MAX_VALUE
                sortBy = TagSortOptions.Downloaded
            },
            QueryModel().apply {
                sortBy = TagSortOptions.Classic
                collection = TagCollection.ClassicTags
                maxResults = 400
            },
        )

        val viewPager = rootView.findViewById<ViewPager2>(R.id.browseViewPager)
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = models.size

            override fun createFragment(position: Int): Fragment =
                TagQueryFragment().apply { model = models[position] }
        }

        val tabs = rootView.findViewById<TabLayout>(R.id.browseTabs)
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.setText(TAB_LABELS[position])
        }.attach()

        return rootView
    }

    companion object {
        private val TAB_LABELS = intArrayOf(
            R.string.latest,
            R.string.Rating,
            R.string.Downloads,
            R.string.classic,
        )
    }
}
