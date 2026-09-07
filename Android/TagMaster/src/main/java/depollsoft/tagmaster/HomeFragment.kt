package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bindroid.utils.bindTo

/** Discovery first, then two existing built-in lists. Never load saved tags on Home. */
class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val root = inflater.inflate(R.layout.homeview, container, false)
        root.bindTo(R.id.favoritesButton, "Text", {
            getString(R.string.BuiltInListCount, getString(R.string.Favorites), FavoritesModel.favoriteIds.size)
        })
        root.bindTo(R.id.teachableButton, "Text", {
            getString(R.string.BuiltInListCount, getString(R.string.TeachableTags), TeachableTagsModel.teachableTagIds.size)
        })
        root.findViewById<View>(R.id.favoritesButton).setOnClickListener {
            startActivity(Intent(requireContext(), FavoritesActivity::class.java))
        }
        root.findViewById<View>(R.id.teachableButton).setOnClickListener {
            startActivity(Intent(requireContext(), TeachableTagsActivity::class.java))
        }
        return root
    }
}
