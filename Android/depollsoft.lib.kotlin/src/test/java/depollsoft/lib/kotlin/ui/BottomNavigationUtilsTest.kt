package depollsoft.lib.kotlin.ui

import android.view.Menu
import android.view.MenuItem
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [28], manifest = org.robolectric.annotation.Config.NONE)
class BottomNavigationUtilsTest {

    private lateinit var bottomNav: BottomNavigationView

    @Before
    fun setUp() {
        // Mock BottomNavigationView to avoid resource issues
        bottomNav = mock(BottomNavigationView::class.java)
        
        // Mock menu
        val menu = mock(Menu::class.java)
        `when`(bottomNav.menu).thenReturn(menu)
        `when`(menu.size()).thenReturn(3)
        
        val item1 = mock(MenuItem::class.java); `when`(item1.itemId).thenReturn(100)
        val item2 = mock(MenuItem::class.java); `when`(item2.itemId).thenReturn(101)
        val item3 = mock(MenuItem::class.java); `when`(item3.itemId).thenReturn(102)
        
        `when`(menu.getItem(0)).thenReturn(item1)
        `when`(menu.getItem(1)).thenReturn(item2)
        `when`(menu.getItem(2)).thenReturn(item3)
    }

    @Test
    fun attachToViewPager_syncs_viewPager_to_bottomNav() {
        val mockViewPager = mock(ViewPager2::class.java)
        
        bottomNav.attachToViewPager(mockViewPager)
        
        // Capture the listener
        val captor = org.mockito.ArgumentCaptor.forClass(com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener::class.java)
        org.mockito.Mockito.verify(bottomNav).setOnNavigationItemSelectedListener(captor.capture())
        val listener = captor.value
        
        // Simulate item selection
        val item2 = mock(MenuItem::class.java)
        `when`(item2.itemId).thenReturn(101)
        listener.onNavigationItemSelected(item2)
        
        // Verify ViewPager moved to index 1
        org.mockito.Mockito.verify(mockViewPager).currentItem = 1
        
        // Simulate item selection
        val item3 = mock(MenuItem::class.java)
        `when`(item3.itemId).thenReturn(102)
        listener.onNavigationItemSelected(item3)
        
        org.mockito.Mockito.verify(mockViewPager).currentItem = 2
    }

    @Test
    fun attachToViewPager_syncs_bottomNav_to_viewPager() {
        val mockViewPager = mock(ViewPager2::class.java)
        bottomNav.attachToViewPager(mockViewPager)
        
        org.mockito.Mockito.verify(mockViewPager).registerOnPageChangeCallback(org.mockito.ArgumentMatchers.any())
    }
    
    @Test
    fun attachToViewPager_callback_invoked() {
        val mockViewPager = mock(ViewPager2::class.java)
        var callbackCount = 0
        
        bottomNav.attachToViewPager(mockViewPager) {
            callbackCount++
        }
        
        val captor = org.mockito.ArgumentCaptor.forClass(com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener::class.java)
        org.mockito.Mockito.verify(bottomNav).setOnNavigationItemSelectedListener(captor.capture())
        val listener = captor.value
        
        val item2 = mock(MenuItem::class.java)
        `when`(item2.itemId).thenReturn(101)
        listener.onNavigationItemSelected(item2)
        
        assertEquals(1, callbackCount)
    }
}