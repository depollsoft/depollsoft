package depollsoft.tagmaster

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class TagSearchResultsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.tagqueryactivity)
        enableDeskBack()

        val tagQueryFragment =
            this.supportFragmentManager.findFragmentById(R.id.tagQueryFragment) as TagQueryFragment

        findViewById<View>(R.id.resultsRoot).applyDeskInsets()

        // The search terms name the screen; a blank query is still a result set.
        val query = tagQueryFragment.model?.query
        supportActionBar?.title =
            if (query.isNullOrBlank()) getString(R.string.SearchResults) else query
    }
}
