package depollsoft.lib.kotlin.ui

import android.view.Menu
import android.view.MenuItem
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

/**
 * Additional comprehensive tests for BottomNavigationUtils.kt
 */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [28], manifest = org.robolectric.annotation.Config.NONE)
class BottomNavigationUtilsComprehensiveTest {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var mockMenu: Menu
    private lateinit var mockViewPager: ViewPager2

    @Before
    fun setUp() {
        bottomNav = mock(BottomNavigationView::class.java)
        mockMenu = mock(Menu::class.java)
        mockViewPager = mock(ViewPager2::class.java)
        
        `when`(bottomNav.menu).thenReturn(mockMenu)
    }

    private fun setupMenuItems(vararg itemIds: Int) {
        `when`(mockMenu.size()).thenReturn(itemIds.size)
        itemIds.forEachIndexed { index, id ->
            val item = mock(MenuItem::class.java)
            `when`(item.itemId).thenReturn(id)
            `when`(mockMenu.getItem(index)).thenReturn(item)
        }
    }

    // =====================
    // Item selection tests
    // =====================

    @Test
    fun attachToViewPager_selects_first_item() {
        setupMenuItems(100, 101, 102)
        
        bottomNav.attachToViewPager(mockViewPager)
        
        val captor = ArgumentCaptor.forClass(
            BottomNavigationView.OnNavigationItemSelectedListener::class.java
        )
        verify(bottomNav).setOnNavigationItemSelectedListener(captor.capture())
        val listener = captor.value
        
        val item = mock(MenuItem::class.java)
        `when`(item.itemId).thenReturn(100)
        listener.onNavigationItemSelected(item)
        
        verify(mockViewPager).currentItem = 0
    }

    @Test
    fun attachToViewPager_selects_last_item() {
        setupMenuItems(100, 101, 102, 103, 104)
        
        bottomNav.attachToViewPager(mockViewPager)
        
        val captor = ArgumentCaptor.forClass(
            BottomNavigationView.OnNavigationItemSelectedListener::class.java
        )
        verify(bottomNav).setOnNavigationItemSelectedListener(captor.capture())
        val listener = captor.value
        
        val item = mock(MenuItem::class.java)
        `when`(item.itemId).thenReturn(104)
        listener.onNavigationItemSelected(item)
        
        verify(mockViewPager).currentItem = 4
    }

    @Test
    fun attachToViewPager_returns_negative_one_for_unknown_item() {
        setupMenuItems(100, 101, 102)
        
        bottomNav.attachToViewPager(mockViewPager)
        
        val captor = ArgumentCaptor.forClass(
            BottomNavigationView.OnNavigationItemSelectedListener::class.java
        )
        verify(bottomNav).setOnNavigationItemSelectedListener(captor.capture())
        val listener = captor.value
        
        val unknownItem = mock(MenuItem::class.java)
        `when`(unknownItem.itemId).thenReturn(999) // Unknown ID
        listener.onNavigationItemSelected(unknownItem)
        
        verify(mockViewPager).currentItem = -1
    }

    // =====================
    // Page change callback tests
    // =====================

    @Test
    fun attachToViewPager_registers_page_change_callback() {
        setupMenuItems(100, 101)
        
        bottomNav.attachToViewPager(mockViewPager)
        
        verify(mockViewPager).registerOnPageChangeCallback(any())
    }

    @Test
    fun page_change_callback_onChange_called() {
        setupMenuItems(100, 101, 102)
        var onChangeCount = 0
        
        bottomNav.attachToViewPager(mockViewPager) {
            onChangeCount++
        }
        
        val listenerCaptor = ArgumentCaptor.forClass(
            BottomNavigationView.OnNavigationItemSelectedListener::class.java
        )
        verify(bottomNav).setOnNavigationItemSelectedListener(listenerCaptor.capture())
        
        val item = mock(MenuItem::class.java)
        `when`(item.itemId).thenReturn(101)
        listenerCaptor.value.onNavigationItemSelected(item)
        
        assertEquals(1, onChangeCount)
    }

    @Test
    fun item_selection_always_returns_true() {
        setupMenuItems(100, 101)
        
        bottomNav.attachToViewPager(mockViewPager)
        
        val captor = ArgumentCaptor.forClass(
            BottomNavigationView.OnNavigationItemSelectedListener::class.java
        )
        verify(bottomNav).setOnNavigationItemSelectedListener(captor.capture())
        val listener = captor.value
        
        val item = mock(MenuItem::class.java)
        `when`(item.itemId).thenReturn(100)
        val result = listener.onNavigationItemSelected(item)
        
        assertTrue(result)
    }

    // =====================
    // Multiple items tests
    // =====================

    @Test
    fun attachToViewPager_with_single_item() {
        setupMenuItems(100)
        
        bottomNav.attachToViewPager(mockViewPager)
        
        val captor = ArgumentCaptor.forClass(
            BottomNavigationView.OnNavigationItemSelectedListener::class.java
        )
        verify(bottomNav).setOnNavigationItemSelectedListener(captor.capture())
        val listener = captor.value
        
        val item = mock(MenuItem::class.java)
        `when`(item.itemId).thenReturn(100)
        listener.onNavigationItemSelected(item)
        
        verify(mockViewPager).currentItem = 0
    }

    @Test
    fun attachToViewPager_with_many_items() {
        val itemIds = (0..9).map { 100 + it }.toIntArray()
        setupMenuItems(*itemIds)
        
        bottomNav.attachToViewPager(mockViewPager)
        
        val captor = ArgumentCaptor.forClass(
            BottomNavigationView.OnNavigationItemSelectedListener::class.java
        )
        verify(bottomNav).setOnNavigationItemSelectedListener(captor.capture())
        val listener = captor.value
        
        // Test selecting item at index 5
        val item = mock(MenuItem::class.java)
        `when`(item.itemId).thenReturn(105)
        listener.onNavigationItemSelected(item)
        
        verify(mockViewPager).currentItem = 5
    }

    // =====================
    // Callback timing tests
    // =====================

    @Test
    fun onChange_called_after_viewPager_update() {
        setupMenuItems(100, 101)
        val callOrder = mutableListOf<String>()
        
        doAnswer {
            callOrder.add("viewpager")
            null
        }.`when`(mockViewPager).currentItem = anyInt()
        
        bottomNav.attachToViewPager(mockViewPager) {
            callOrder.add("callback")
        }
        
        val captor = ArgumentCaptor.forClass(
            BottomNavigationView.OnNavigationItemSelectedListener::class.java
        )
        verify(bottomNav).setOnNavigationItemSelectedListener(captor.capture())
        
        val item = mock(MenuItem::class.java)
        `when`(item.itemId).thenReturn(101)
        captor.value.onNavigationItemSelected(item)
        
        assertEquals(listOf("viewpager", "callback"), callOrder)
    }

    // =====================
    // Edge cases
    // =====================

    @Test
    fun attachToViewPager_with_zero_items() {
        `when`(mockMenu.size()).thenReturn(0)
        
        bottomNav.attachToViewPager(mockViewPager)
        
        val captor = ArgumentCaptor.forClass(
            BottomNavigationView.OnNavigationItemSelectedListener::class.java
        )
        verify(bottomNav).setOnNavigationItemSelectedListener(captor.capture())
        val listener = captor.value
        
        val item = mock(MenuItem::class.java)
        `when`(item.itemId).thenReturn(100)
        listener.onNavigationItemSelected(item)
        
        // With no items, should return -1
        verify(mockViewPager).currentItem = -1
    }

    @Test
    fun multiple_onChange_callbacks_sequential() {
        setupMenuItems(100, 101, 102)
        var callCount = 0
        
        bottomNav.attachToViewPager(mockViewPager) {
            callCount++
        }
        
        val captor = ArgumentCaptor.forClass(
            BottomNavigationView.OnNavigationItemSelectedListener::class.java
        )
        verify(bottomNav).setOnNavigationItemSelectedListener(captor.capture())
        val listener = captor.value
        
        val item1 = mock(MenuItem::class.java)
        `when`(item1.itemId).thenReturn(100)
        listener.onNavigationItemSelected(item1)
        
        val item2 = mock(MenuItem::class.java)
        `when`(item2.itemId).thenReturn(101)
        listener.onNavigationItemSelected(item2)
        
        val item3 = mock(MenuItem::class.java)
        `when`(item3.itemId).thenReturn(102)
        listener.onNavigationItemSelected(item3)
        
        assertEquals(3, callCount)
    }

    @Test
    fun attachToViewPager_without_onChange_callback() {
        setupMenuItems(100, 101)
        
        bottomNav.attachToViewPager(mockViewPager)
        
        val captor = ArgumentCaptor.forClass(
            BottomNavigationView.OnNavigationItemSelectedListener::class.java
        )
        verify(bottomNav).setOnNavigationItemSelectedListener(captor.capture())
        val listener = captor.value
        
        val item = mock(MenuItem::class.java)
        `when`(item.itemId).thenReturn(101)
        
        // Should not throw when no callback provided
        listener.onNavigationItemSelected(item)
        
        verify(mockViewPager).currentItem = 1
    }
}
