package depollsoft.lib.kotlin.ui

import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.viewpager2.widget.ViewPager2
import android.view.Menu

fun BottomNavigationView.attachToViewPager(viewPager: ViewPager2, onChange: () -> Unit = {}) {
    this.setOnNavigationItemSelectedListener {
        viewPager.currentItem = getItemIndex(it.itemId, this.menu)
        onChange()
        true
    }

    viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            this@attachToViewPager.selectedIndex = position
            onChange()
        }

        override fun onPageScrollStateChanged(state: Int) {
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        }
    })
}

private fun getItemIndex(itemId: Int, menu: Menu): Int {
    return (0 until menu.size()).firstOrNull { menu.getItem(it).itemId == itemId }
            ?: -1
}

private var BottomNavigationView.selectedIndex: Int
    get() {
        return getItemIndex(this.selectedItemId, this.menu)
    }
    set(value) {
        this.selectedItemId = this.menu.getItem(value).itemId
    }