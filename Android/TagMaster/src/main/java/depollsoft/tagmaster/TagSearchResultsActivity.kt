package depollsoft.tagmaster

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import depollsoft.lib.ui.ThreadSwitchContext

class TagSearchResultsActivity :
    AppCompatActivity(),
    TagPaneHost {
    internal lateinit var tagPane: TagPaneController
        private set

    private val queryFragment: TagQueryFragment?
        get() = supportFragmentManager.findFragmentById(R.id.tagQueryFragment) as? TagQueryFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.tagqueryactivity)
        setUpToolbar(true)

        supportActionBar?.title = queryFragment?.model?.query

        tagPane =
            TagPaneController(
                activity = this,
                listedIds = { queryFragment?.model?.tags?.map { it.id } ?: emptyList() },
                reveal = { id -> queryFragment?.revealTag(id) },
                hasMoreResults = { queryFragment?.model?.hasMoreResults == true },
                fetchMore = { queryFragment?.model?.fetchResults(ThreadSwitchContext(this)) },
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
