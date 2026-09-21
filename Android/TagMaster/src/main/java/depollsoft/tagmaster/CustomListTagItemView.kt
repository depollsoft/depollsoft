package depollsoft.tagmaster

import android.content.Context
import android.util.AttributeSet

/** A tag row inside one of the user's own lists ([TagListActivity]). */
class CustomListTagItemView : SavedTagItemView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)
}
