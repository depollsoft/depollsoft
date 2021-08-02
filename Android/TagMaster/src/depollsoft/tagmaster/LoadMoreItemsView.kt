package depollsoft.tagmaster

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import depollsoft.tagmaster.QueryModel
import com.bindroid.utils.ReflectedProperty
import depollsoft.lib.ui.ThreadSwitchContext
import com.bindroid.ui.UiBinder
import com.bindroid.converters.BoolConverter
import com.bindroid.utils.bindTo

class LoadMoreItemsView : LinearLayout {
    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        val inflater = this.context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.loadmoreitemview, this, true)
        val b = findViewById<View>(R.id.loadMoreItemsButton) as Button
        b.setOnClickListener {
            val qm = ReflectedProperty(
                this@LoadMoreItemsView, "Context.Model"
            ).value as QueryModel
            qm.fetchResults(ThreadSwitchContext(this@LoadMoreItemsView))
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        UiBinder.bind(
            this, R.id.loadMoreItemsLayout, "Visibility",
            "Context.Model.HasMoreResults", BoolConverter.get()
        )
        UiBinder.bind(
            this, R.id.loadMoreItemsButton, "Enabled",
            "Context.Model.IsLoading", BoolConverter.get(true)
        )
    }
}