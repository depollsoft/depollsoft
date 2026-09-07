package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Search opened on its own. The desk shows the same page as its third
 * destination; this entry point stays for callers that jump straight here.
 */
class TagSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDeskBack()
        this.setContentView(R.layout.singlepageview)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.pageContainer, TagSearchFragment())
                .commit()
        }

        findViewById<android.view.View>(R.id.pageContainer).applyDeskInsets()
        supportActionBar?.title = "Tag Master".makeTitleString(this)
    }
}
