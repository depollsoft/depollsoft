package depollsoft.pitchperfect

import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.bindroid.BindingMode
import com.bindroid.ValueConverter
import com.bindroid.converters.AdapterConverter
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.TrackableField
import com.bindroid.ui.CompoundButtonCheckedProperty
import com.bindroid.ui.UiBinder
import com.bindroid.utils.ReflectedProperty
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class KeySignatureFragment : Fragment() {
    val model: KeySignatureModel by TrackableField(KeySignatureModel())
    private lateinit var majorView: ListView
    private lateinit var minorView: ListView
    private var fab: ExtendedFloatingActionButton? = null

    private fun centerList(list: ListView) {
        val priorVisibility = list.visibility
        list.post {
            list.visibility = View.INVISIBLE

            val totalVisible = list.lastVisiblePosition - list.firstVisiblePosition
            list.setSelection(list.count / 2 - totalVisible / 2)
            list.visibility = priorVisibility
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.activity?.volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.keysignatureview, container, false)

        UiBinder.bind(
            rootView,
            R.id.majorMinorFab,
            "IconResource",
            this,
            "Model.IsMajor",
            BindingMode.ONE_WAY,
            object : ValueConverter() {
                override fun convertToTarget(
                    sourceValue: Any?,
                    targetType: Class<*>?,
                ): Any {
                    if (sourceValue == true) {
                        return R.drawable.ic_major
                    }
                    return R.drawable.ic_minor
                }
            },
        )

        UiBinder.bind(
            rootView,
            R.id.majorKeySignatureListView,
            "Adapter",
            this,
            "Model.MajorKeys",
            AdapterConverter(KeySignatureListItemView::class.java),
        )
        UiBinder.bind(
            rootView,
            R.id.majorKeySignatureListView,
            "Visibility",
            this,
            "Model.IsMajor",
            BoolConverter(),
        )
        UiBinder.bind(
            rootView,
            R.id.minorKeySignatureListView,
            "Adapter",
            this,
            "Model.MinorKeys",
            AdapterConverter(KeySignatureListItemView::class.java),
        )
        UiBinder.bind(
            rootView,
            R.id.minorKeySignatureListView,
            "Visibility",
            this,
            "Model.IsMajor",
            BoolConverter(true),
        )

        this.majorView = rootView.findViewById(R.id.majorKeySignatureListView) as ListView
        this.minorView = rootView.findViewById(R.id.minorKeySignatureListView) as ListView

        this.centerList(this.majorView)
        this.centerList(this.minorView)

        fab = rootView.findViewById(R.id.majorMinorFab)
        updateFabState()
        fab?.setOnClickListener {
            this.model.isMajor = !this.model.isMajor
            updateFabState()
            if (this@KeySignatureFragment.model.isMajor) {
                this@KeySignatureFragment.majorView.setSelection(
                    this@KeySignatureFragment
                        .minorView
                        .firstVisiblePosition,
                )
            } else {
                this@KeySignatureFragment.minorView.setSelection(
                    this@KeySignatureFragment
                        .majorView
                        .firstVisiblePosition,
                )
            }
            for (k in this@KeySignatureFragment.model.majorKeys) {
                k.note.stop()
            }
            for (k in this@KeySignatureFragment.model.minorKeys) {
                k.note.stop()
            }
        }
        fab?.show()

        return rootView
    }

    private fun updateFabState() {
        // The shared models no longer notify Bindroid bindings, so show the chosen list here.
        if (::majorView.isInitialized) {
            majorView.visibility = if (model.isMajor) View.VISIBLE else View.GONE
            minorView.visibility = if (model.isMajor) View.GONE else View.VISIBLE
        }
        fab?.setIconResource(if (model.isMajor) R.drawable.ic_major else R.drawable.ic_minor)
        fab?.text = getString(if (model.isMajor) R.string.major else R.string.minor)
        fab?.contentDescription =
            getString(if (model.isMajor) R.string.SwitchToMinorKeys else R.string.SwitchToMajorKeys)
    }

    private fun stopPlaying() {
        for (k in this.model.majorKeys) {
            k.note.stop()
        }
        for (k in this.model.minorKeys) {
            k.note.stop()
        }
    }

    override fun onPause() {
        super.onPause()
        stopPlaying()
        fab?.hide()
    }

    override fun onResume() {
        super.onResume()
        stopPlaying()
        fab?.show()
    }
}
