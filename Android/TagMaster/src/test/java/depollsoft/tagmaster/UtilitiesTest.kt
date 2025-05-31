package depollsoft.tagmaster

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Typeface
import android.text.SpannableString
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = 28)
class UtilitiesTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockAssetManager: AssetManager
    
    @Mock
    private lateinit var mockTypeface: Typeface

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.assets).thenReturn(mockAssetManager)
    }

    @Test
    fun testMakeTitleString_WithSimpleText() {
        // Mock the Typeface creation
        mockStatic(Typeface::class.java).use { typefaceMock ->
            typefaceMock.`when`<Typeface> { 
                Typeface.createFromAsset(mockAssetManager, "fonts/wickhop-handwriting.ttf") 
            }.thenReturn(mockTypeface)
            
            val input = "Test Title"
            val result = input.makeTitleString(mockContext)
            
            assertNotNull("Result should not be null", result)
            assertTrue("Result should be SpannableString", result is SpannableString)
            assertEquals("Text content should match input", input, result.toString())
        }
    }

    @Test
    fun testMakeTitleString_WithEmptyString() {
        mockStatic(Typeface::class.java).use { typefaceMock ->
            typefaceMock.`when`<Typeface> { 
                Typeface.createFromAsset(mockAssetManager, "fonts/wickhop-handwriting.ttf") 
            }.thenReturn(mockTypeface)
            
            val input = ""
            val result = input.makeTitleString(mockContext)
            
            assertNotNull("Result should not be null", result)
            assertEquals("Empty string should remain empty", "", result.toString())
        }
    }

    @Test
    fun testMakeTitleString_WithSpecialCharacters() {
        mockStatic(Typeface::class.java).use { typefaceMock ->
            typefaceMock.`when`<Typeface> { 
                Typeface.createFromAsset(mockAssetManager, "fonts/wickhop-handwriting.ttf") 
            }.thenReturn(mockTypeface)
            
            val input = "Title with @#$%^&*() special chars!"
            val result = input.makeTitleString(mockContext)
            
            assertNotNull("Result should not be null", result)
            assertEquals("Special characters should be preserved", input, result.toString())
        }
    }

    @Test
    fun testMakeTitleString_WithUnicodeCharacters() {
        mockStatic(Typeface::class.java).use { typefaceMock ->
            typefaceMock.`when`<Typeface> { 
                Typeface.createFromAsset(mockAssetManager, "fonts/wickhop-handwriting.ttf") 
            }.thenReturn(mockTypeface)
            
            val input = "Title with émojis 🎵🎶 and ñiño"
            val result = input.makeTitleString(mockContext)
            
            assertNotNull("Result should not be null", result)
            assertEquals("Unicode characters should be preserved", input, result.toString())
        }
    }

    @Test
    fun testAsList_WithValidList() {
        val originalList = listOf("a", "b", "c")
        val result: List<String>? = originalList.asList()
        
        assertNotNull("Result should not be null", result)
        assertEquals("List should have same size", 3, result!!.size)
        assertEquals("First element should match", "a", result[0])
        assertEquals("Second element should match", "b", result[1])
        assertEquals("Third element should match", "c", result[2])
    }

    @Test
    fun testAsList_WithEmptyList() {
        val originalList = emptyList<String>()
        val result: List<String>? = originalList.asList()
        
        assertNotNull("Result should not be null", result)
        assertTrue("Result should be empty", result!!.isEmpty())
    }

    @Test
    fun testAsList_WithNullList() {
        val originalList: List<String>? = null
        val result: List<String>? = originalList.asList()
        
        assertNull("Result should be null for null input", result)
    }

    @Test
    fun testAsList_WithIntegerList() {
        val originalList = listOf(1, 2, 3, 4, 5)
        val result: List<Int>? = originalList.asList()
        
        assertNotNull("Result should not be null", result)
        assertEquals("List should have same size", 5, result!!.size)
        assertEquals("Elements should match", Integer.valueOf(1), result[0])
        assertEquals("Elements should match", Integer.valueOf(5), result[4])
    }

    @Test
    fun testAsList_WithMixedTypesList() {
        val originalList = listOf("string", 123, true, null)
        val result: List<Any?>? = originalList.asList()
        
        assertNotNull("Result should not be null", result)
        assertEquals("List should have same size", 4, result!!.size)
        assertEquals("String element should match", "string", result[0])
        assertEquals("Integer element should match", 123, result[1])
        assertEquals("Boolean element should match", true, result[2])
        assertNull("Null element should remain null", result[3])
    }

    @Test
    fun testAsList_TypeSafety() {
        val originalList = listOf("a", "b", "c")
        
        // This should work (compatible types)
        val stringResult: List<String>? = originalList.asList()
        assertNotNull("String cast should work", stringResult)
        
        // This should return null (incompatible types)
        val intResult: List<Int>? = originalList.asList()
        assertNull("Incompatible type cast should return null", intResult)
    }

    @Test
    fun testAsList_WithSingleElementList() {
        val originalList = listOf("single")
        val result: List<String>? = originalList.asList()
        
        assertNotNull("Result should not be null", result)
        assertEquals("List should have one element", 1, result!!.size)
        assertEquals("Element should match", "single", result[0])
    }

    @Test
    fun testMakeTitleString_CallsCorrectAssetPath() {
        mockStatic(Typeface::class.java).use { typefaceMock ->
            typefaceMock.`when`<Typeface> { 
                Typeface.createFromAsset(mockAssetManager, "fonts/wickhop-handwriting.ttf") 
            }.thenReturn(mockTypeface)
            
            "Test".makeTitleString(mockContext)
            
            // Verify the correct font file is requested
            typefaceMock.verify { 
                Typeface.createFromAsset(mockAssetManager, "fonts/wickhop-handwriting.ttf") 
            }
        }
    }

    @Test
    fun testMakeTitleString_ReturnsSpannableWithCorrectLength() {
        mockStatic(Typeface::class.java).use { typefaceMock ->
            typefaceMock.`when`<Typeface> { 
                Typeface.createFromAsset(mockAssetManager, "fonts/wickhop-handwriting.ttf") 
            }.thenReturn(mockTypeface)
            
            val input = "Test Title String"
            val result = input.makeTitleString(mockContext)
            
            assertEquals("Spannable length should match input length", 
                        input.length, result.length)
        }
    }
}