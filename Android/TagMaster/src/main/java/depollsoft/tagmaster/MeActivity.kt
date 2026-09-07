package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import depollsoft.lib.compat.ui.MenuItems
import depollsoft.lib.ui.ChangelogViewer

/** Home owns one page. Catalog and tags use the native activity stack. */
class MeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.meview)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.homeContainer, HomeFragment())
                .commit()
        }
        findViewById<android.view.View>(R.id.deskRoot).applyDeskInsets()
        supportActionBar?.title = "Tag Master".makeTitleString(this)
        ChangelogViewer(this, getString(R.string.Changelog)).apply {
            setTitle("Tag Master Changelog")
            setIcon(R.mipmap.ic_launcher)
            showIfAppropriate()
        }
    }

    fun showDestination(index: Int) {
        when (index) {
            BROWSE -> startActivity(Intent(this, TagBrowserActivity::class.java))
            SEARCH -> startActivity(Intent(this, TagSearchActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.memenu, menu)
        MenuItems.setShowAsAction(menu.findItem(R.id.settingsMenuItem), MenuItems.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.settingsMenuItem) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSearchRequested(): Boolean {
        showDestination(SEARCH)
        return true
    }

    companion object {
        const val HOME = 0
        const val BROWSE = 1
        const val SEARCH = 2
    }
}
