package depollsoft.tagmaster

import android.app.Activity
import android.app.Application
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.widget.ScrollView
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuItemCompat
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class PolishLayoutRegressionTest {
    @Test fun short_viewport_can_scroll_to_and_select_a_voice_part() {
        val controller = Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.AppTheme)
        controller.setup()
        try {
            val activity = controller.get()
            val root = LayoutInflater.from(activity).inflate(R.layout.tagtracksview, null) as ScrollView
            activity.setContentView(root)
            root.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(350, View.MeasureSpec.EXACTLY))
            root.layout(0, 0, 1080, 350)
            val bass = root.findViewById<View>(R.id.bassButton)
            val rect = Rect(0, 0, bass.width, bass.height)
            root.offsetDescendantRectToMyCoords(bass, rect)
            assertTrue("Parts must remain in scrollable content", rect.bottom > root.height)
            root.scrollTo(0, rect.top)
            assertTrue(root.scrollY > 0)
            bass.performClick()
            assertTrue((bass as android.widget.RadioButton).isChecked)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun all_toolbar_icons_have_a_theme_resolved_tint() {
        val controller = Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.AppTheme)
        controller.setup()
        try {
            val activity = controller.get()
            for (resource in listOf(R.menu.memenu, R.menu.tagdetailmenu, R.menu.mainmenu, R.menu.sheetmusicmenu)) {
                val popup = PopupMenu(activity, View(activity))
                popup.inflate(resource)
                for (index in 0 until popup.menu.size()) {
                    val item = popup.menu.getItem(index)
                    assertNotNull(item.title.toString(), MenuItemCompat.getIconTintList(item))
                    assertEquals(item.title.toString(), activity.getColor(R.color.brand_on_chrome),
                        MenuItemCompat.getIconTintList(item)!!.defaultColor)
                }
            }
        } finally { controller.pause().stop().destroy() }
    }
}
