package depollsoft.lib.kotlin.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView

class TestActivity : AppCompatActivity() {
    lateinit var bottomNav: BottomNavigationView
    lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Programmatically create views to avoid XML
        bottomNav = BottomNavigationView(this)
        viewPager = ViewPager2(this)
        
        // Set up a simple adapter with 3 pages
        viewPager.adapter = SimpleAdapter(3)

        // Simple container
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.addView(viewPager, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            0, 1f))
        layout.addView(bottomNav, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT))
        setContentView(layout)
    }
    
    // Simple adapter for ViewPager2 tests
    private class SimpleAdapter(private val pageCount: Int) : RecyclerView.Adapter<SimpleAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = View(parent.context)
            view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
        override fun getItemCount(): Int = pageCount
    }
}
