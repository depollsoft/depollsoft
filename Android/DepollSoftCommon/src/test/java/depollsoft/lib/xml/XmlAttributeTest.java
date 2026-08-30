package depollsoft.lib.xml;

import static org.junit.Assert.*;

import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for the XmlAttribute class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.M)
public class XmlAttributeTest {

    private XmlAttribute attribute;

    @Before
    public void setUp() {
        attribute = new XmlAttribute();
    }

    // =====================================================================
    // Constructor tests
    // =====================================================================

    @Test
    public void constructor_nameIsNull() {
        assertNull(attribute.getName());
    }

    @Test
    public void constructor_valueIsNull() {
        assertNull(attribute.getValue());
    }

    // =====================================================================
    // Name property tests
    // =====================================================================

    @Test
    public void setName_storesName() {
        attribute.setName("id");
        assertEquals("id", attribute.getName());
    }

    @Test
    public void setName_withNull_storesNull() {
        attribute.setName("initial");
        attribute.setName(null);
        assertNull(attribute.getName());
    }

    @Test
    public void setName_withEmptyString_storesEmptyString() {
        attribute.setName("");
        assertEquals("", attribute.getName());
    }

    @Test
    public void setName_withNamespacePrefix_storesCorrectly() {
        attribute.setName("xmlns:prefix");
        assertEquals("xmlns:prefix", attribute.getName());
    }

    @Test
    public void setName_withSpecialCharacters_storesCorrectly() {
        attribute.setName("data-custom");
        assertEquals("data-custom", attribute.getName());
    }

    @Test
    public void setName_multipleUpdates_retainsLastValue() {
        attribute.setName("first");
        attribute.setName("second");
        attribute.setName("third");
        assertEquals("third", attribute.getName());
    }

    // =====================================================================
    // Value property tests
    // =====================================================================

    @Test
    public void setValue_storesValue() {
        attribute.setValue("test-value");
        assertEquals("test-value", attribute.getValue());
    }

    @Test
    public void setValue_withNull_storesNull() {
        attribute.setValue("initial");
        attribute.setValue(null);
        assertNull(attribute.getValue());
    }

    @Test
    public void setValue_withEmptyString_storesEmptyString() {
        attribute.setValue("");
        assertEquals("", attribute.getValue());
    }

    @Test
    public void setValue_withWhitespace_preservesWhitespace() {
        attribute.setValue("  spaced value  ");
        assertEquals("  spaced value  ", attribute.getValue());
    }

    @Test
    public void setValue_withNumericString_storesCorrectly() {
        attribute.setValue("12345");
        assertEquals("12345", attribute.getValue());
    }

    @Test
    public void setValue_withUrl_storesCorrectly() {
        String url = "https://example.com/path?query=value&other=123";
        attribute.setValue(url);
        assertEquals(url, attribute.getValue());
    }

    @Test
    public void setValue_withSpecialXmlCharacters_storesCorrectly() {
        // These would normally need escaping in XML, but the attribute stores raw values
        attribute.setValue("<>\"&'");
        assertEquals("<>\"&'", attribute.getValue());
    }

    @Test
    public void setValue_multipleUpdates_retainsLastValue() {
        attribute.setValue("first");
        attribute.setValue("second");
        attribute.setValue("third");
        assertEquals("third", attribute.getValue());
    }

    @Test
    public void setValue_withNewlines_storesCorrectly() {
        attribute.setValue("line1\nline2");
        assertEquals("line1\nline2", attribute.getValue());
    }

    @Test
    public void setValue_withUnicode_storesCorrectly() {
        attribute.setValue("日本語");
        assertEquals("日本語", attribute.getValue());
    }

    // =====================================================================
    // Combined name and value tests
    // =====================================================================

    @Test
    public void nameAndValue_canBeSetIndependently() {
        attribute.setName("key");
        attribute.setValue("value");
        
        assertEquals("key", attribute.getName());
        assertEquals("value", attribute.getValue());
    }

    @Test
    public void nameAndValue_canBothBeNull() {
        attribute.setName(null);
        attribute.setValue(null);
        
        assertNull(attribute.getName());
        assertNull(attribute.getValue());
    }

    @Test
    public void nameAndValue_canBothBeEmpty() {
        attribute.setName("");
        attribute.setValue("");
        
        assertEquals("", attribute.getName());
        assertEquals("", attribute.getValue());
    }

    // =====================================================================
    // toString tests
    // =====================================================================

    @Test
    public void toString_withNameAndValue_formatsCorrectly() {
        attribute.setName("id");
        attribute.setValue("123");
        assertEquals("id -> 123", attribute.toString());
    }

    @Test
    public void toString_withNullNameAndValue_handlesNull() {
        assertEquals("null -> null", attribute.toString());
    }

    @Test
    public void toString_withNameOnly_showsNullValue() {
        attribute.setName("name");
        assertEquals("name -> null", attribute.toString());
    }

    @Test
    public void toString_withValueOnly_showsNullName() {
        attribute.setValue("value");
        assertEquals("null -> value", attribute.toString());
    }

    @Test
    public void toString_withEmptyNameAndValue_showsEmptyStrings() {
        attribute.setName("");
        attribute.setValue("");
        assertEquals(" -> ", attribute.toString());
    }

    @Test
    public void toString_withLongValue_includesFullValue() {
        attribute.setName("long");
        String longValue = "This is a very long attribute value that might be truncated in some implementations";
        attribute.setValue(longValue);
        assertEquals("long -> " + longValue, attribute.toString());
    }

    // =====================================================================
    // Edge case tests
    // =====================================================================

    @Test
    public void attribute_withBooleanLikeValue_storesAsString() {
        attribute.setName("enabled");
        attribute.setValue("true");
        assertEquals("true", attribute.getValue());
    }

    @Test
    public void attribute_withIntegerLikeValue_storesAsString() {
        attribute.setName("count");
        attribute.setValue("42");
        assertEquals("42", attribute.getValue());
    }

    @Test
    public void attribute_withFloatLikeValue_storesAsString() {
        attribute.setName("price");
        attribute.setValue("19.99");
        assertEquals("19.99", attribute.getValue());
    }

    @Test
    public void attribute_commonHtmlAttributes_workCorrectly() {
        // Test common HTML/XML attribute patterns
        
        // class attribute
        XmlAttribute classAttr = new XmlAttribute();
        classAttr.setName("class");
        classAttr.setValue("container main-content");
        assertEquals("class", classAttr.getName());
        assertEquals("container main-content", classAttr.getValue());
        
        // style attribute
        XmlAttribute styleAttr = new XmlAttribute();
        styleAttr.setName("style");
        styleAttr.setValue("color: red; font-size: 12px;");
        assertEquals("color: red; font-size: 12px;", styleAttr.getValue());
    }

    @Test
    public void attribute_xmlNamespaceDeclaration_storesCorrectly() {
        attribute.setName("xmlns");
        attribute.setValue("http://www.w3.org/1999/xhtml");
        assertEquals("xmlns", attribute.getName());
        assertEquals("http://www.w3.org/1999/xhtml", attribute.getValue());
    }

    @Test
    public void attribute_withJsonValue_storesCorrectly() {
        attribute.setName("data");
        attribute.setValue("{\"key\":\"value\",\"number\":123}");
        assertEquals("{\"key\":\"value\",\"number\":123}", attribute.getValue());
    }
}
