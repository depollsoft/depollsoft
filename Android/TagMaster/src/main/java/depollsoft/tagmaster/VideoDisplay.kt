package depollsoft.tagmaster

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.trackable
import com.bindroid.ui.BoundUi
import com.bindroid.ui.UiBinder
import depollsoft.tagmaster.barbershop.Video

class VideoDisplay :
    LinearLayout,
    BoundUi<Video?> {
    var video: Video? by trackable()
    val thumbnailUri: String
        get() = String.format("https://img.youtube.com/vi/%s/2.jpg", video?.youTubeCode)
    val watchUri: String
        get() = String.format("https://www.youtube.com/watch?v=%s", video?.youTubeCode)

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    override fun bind(dataSource: Video?) {
        video = dataSource
    }

    private fun init() {
        val inflater =
            this.context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE,
            ) as LayoutInflater
        inflater.inflate(R.layout.videodisplay, this, true)
        val background = android.util.TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
        setBackgroundResource(background.resourceId)
        this.isFocusable = true
        this.isClickable = true
        setOnClickListener {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(watchUri)
            this@VideoDisplay.context.startActivity(i)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        UiBinder.bind(this, R.id.sungByTextView, "Text", "Video.SungBy")
        UiBinder.bind(this, R.id.keyTextView, "Text", "Video.SungKey")
        UiBinder.bind(this, R.id.keyRow, "Visibility", "Video.SungKey", BoolConverter.get())
        UiBinder.bind(
            this,
            R.id.postedTextView,
            "Text",
            "Video.Posted",
            NullableDateConverter(
                "%1\$tA, %1\$tB %1\$te, %1\$tY",
            ),
        )
        UiBinder.bind(this, R.id.postedRow, "Visibility", "Video.Posted", BoolConverter.get())
        UiBinder.bind(this, R.id.multitrackCheckBox, "IsAvailable", "Video.IsMultitrack")
        UiBinder.bind(this, R.id.videoPreview, "Source", "ThumbnailUri")
    }
}
