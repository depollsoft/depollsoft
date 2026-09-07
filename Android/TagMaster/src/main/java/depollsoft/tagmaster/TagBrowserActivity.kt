package depollsoft.tagmaster

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Browse opened on its own. Same page as the desk's second destination.
 */
class TagBrowserActivity : AppCompatActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.singlepageview)
        enableDeskBack()

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.pageContainer, TagBrowseFragment())
                .commit()
        }

        findViewById<android.view.View>(R.id.pageContainer).applyDeskInsets()
        supportActionBar?.title = "Tag Master".makeTitleString(this)
    }
}
