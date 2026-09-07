/*
 * Tag Master — the singing desk.
 *
 * Thesis: Tag Master is the desk a barbershop singer stands at when other people
 * are waiting to sing. Everything on it is one reach from the tag itself.
 *
 * World: the app's own charcoal and one blue, the handwriting wordmark, and the
 * barber pole reduced from a full-screen watermark to a mark that sits in a
 * navigation slot. Material 3 carries structure and components; the brand is
 * expressed through its colour roles, type scale and shape, not around them.
 *
 * Home: Find a tag, Random tag, Open tag ID, then Favorites and Teachable tags —
 * the repertoire the singer brought with them, available without an account.
 * Home, Browse and Search are the three destinations; Settings stays a utility
 * in the app bar.
 *
 * Tablets: the same three destinations become a persistent navigation rail at
 * expanded width (layout-w600dp), text is capped at a readable measure, and the
 * tag detail sections likewise move to a rail so the page under them keeps its
 * width.
 *
 * Seed a858fb14, candidate 6, established identity.
 * Finish: listed native review fix resolved; app-scoped DESIGN.md records the
 * shipped UI. Hardware and account verification remain separate.
 */
package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationBarView
import depollsoft.lib.compat.ui.MenuItems
import depollsoft.lib.ui.ChangelogViewer

/**
 * The shell. Holds the three desk destinations and nothing else, so the same
 * pages can also be opened directly by [TagBrowserActivity] and
 * [TagSearchActivity] from a link or a shortcut.
 */
class MeActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var navigation: NavigationBarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.meview)

        viewPager = findViewById(R.id.viewPager)
        navigation = findViewById(R.id.bottomNavigation)

        viewPager.adapter =
            object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
                override fun getItemCount(): Int = DESTINATIONS

                override fun createFragment(position: Int): Fragment =
                    when (position) {
                        HOME -> HomeFragment()
                        BROWSE -> TagBrowseFragment()
                        else -> TagSearchFragment()
                    }
            }
        navigation.attachToPager(viewPager)

        findViewById<android.view.View>(R.id.deskRoot).applyDeskInsets()

        this.supportActionBar?.title = "Tag Master".makeTitleString(this)

        val viewer = ChangelogViewer(this, this.getString(R.string.Changelog))
        viewer.setTitle("Tag Master Changelog")
        viewer.setIcon(R.mipmap.ic_launcher)
        viewer.showIfAppropriate()
    }

    /** Moves the desk to a destination; used by Home's "Find a tag" action. */
    fun showDestination(index: Int) {
        if (index in 0 until DESTINATIONS) {
            viewPager.currentItem = index
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.memenu, menu)

        MenuItems
            .setShowAsAction(menu.findItem(R.id.settingsMenuItem), MenuItems.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.settingsMenuItem) {
            val i = Intent(this, SettingsActivity::class.java)
            this.startActivity(i)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /** The hardware or gesture search request lands on the Search destination. */
    override fun onSearchRequested(): Boolean {
        showDestination(SEARCH)
        return true
    }

    companion object {
        const val HOME = 0
        const val BROWSE = 1
        const val SEARCH = 2
        const val DESTINATIONS = 3
    }
}
