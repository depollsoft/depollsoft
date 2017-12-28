package depollsoft.tagmaster

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import depollsoft.lib.ui.CustomTypefaceSpan
import depollsoft.lib.ui.SpannableUtilities

fun CharSequence.makeTitleString(ctx: Context): CharSequence {
    val typeface = Typeface.createFromAsset(ctx.assets, "fonts/wickhop-handwriting.ttf")
    val span = CustomTypefaceSpan("Wickhop Handwriting", typeface)
    val title = SpannableString(this)
    SpannableUtilities.applyToAll(title, span)
    return title
}