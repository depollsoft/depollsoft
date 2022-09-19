package depollsoft.tagmaster

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TagSearchResultsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.tagqueryactivity)

        val tagQueryFragment = this.supportFragmentManager.findFragmentById(R.id.tagQueryFragment) as TagQueryFragment

        supportActionBar?.title = tagQueryFragment.model?.query
    }
}
