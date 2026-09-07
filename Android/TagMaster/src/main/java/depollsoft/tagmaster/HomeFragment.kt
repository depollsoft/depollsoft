package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bindroid.converters.AdapterConverter
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.TrackableCollection
import com.bindroid.ui.UiBinder
import com.bindroid.utils.bindTo

/**
 * The desk surface: the three ways into a tag, then the singer's own lists.
 *
 * Both lists read straight from the models that own the stored ids, so nothing
 * about favourites or teachable storage changes by being shown here.
 */
class HomeFragment : Fragment() {
    val favoriteIds: TrackableCollection<Int>
        get() = FavoritesModel.favoriteIds

    val teachableTagIds: TrackableCollection<Int>
        get() = TeachableTagsModel.teachableTagIds

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val rootView = inflater.inflate(R.layout.homeview, container, false)

        UiBinder.bind(
            rootView,
            R.id.favoritesItemsControl,
            "Adapter",
            this,
            "FavoriteIds",
            AdapterConverter(FavoriteTagItemView::class.java, true, true),
        )
        UiBinder.bind(
            rootView,
            R.id.homeTeachableItemsControl,
            "Adapter",
            this,
            "TeachableTagIds",
            AdapterConverter(TeachableTagItemView::class.java, false, true),
        )

        // Empty state instead of a bare heading over nothing.
        rootView.bindTo(
            R.id.favoritesEmptyTextView,
            "Visibility",
            { favoriteIds.size == 0 },
            BoolConverter.get(),
        )
        // Keep the list entry point available before the singer saves a first tag.
        rootView.bindTo(
            R.id.teachableEmptyTextView,
            "Visibility",
            { teachableTagIds.size == 0 },
            BoolConverter.get(),
        )

        rootView.findViewById<View>(R.id.teachableButton).setOnClickListener {
            startActivity(Intent(requireContext(), TeachableTagsActivity::class.java))
        }

        return rootView
    }
}
