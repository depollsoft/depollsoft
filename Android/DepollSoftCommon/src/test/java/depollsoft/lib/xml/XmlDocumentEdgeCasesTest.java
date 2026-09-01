package depollsoft.lib.xml;

import static org.junit.Assert.*;

import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Edge case and integration tests for XmlDocument parsing.
 * Supplements existing XmlDocument tests with more complex scenarios.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.M)
public class XmlDocumentEdgeCasesTest {

    // =====================================================================
    // Complex nesting tests
    // =====================================================================

    @Test
    public void parse_deeplyNestedElements_buildsCorrectTree() {
        String xml = "<l1><l2><l3><l4><l5><l6>deep value</l6></l5></l4></l3></l2></l1>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement current = doc.elements("l1").get(0);
        for (String name : new String[]{"l2", "l3", "l4", "l5", "l6"}) {
            List<XmlElement> children = current.elements(name);
            assertEquals("Should have one child named " + name, 1, children.size());
            current = children.get(0);
        }
        assertEquals("deep value", current.getValue());
    }

    @Test
    public void parse_manyAttributes_extractsAll() {
        StringBuilder xml = new StringBuilder("<element ");
        for (int i = 0; i < 20; i++) {
            xml.append("attr").append(i).append("=\"value").append(i).append("\" ");
        }
        xml.append("/>");
        
        XmlDocument doc = XmlDocument.parse(xml.toString());
        XmlElement elem = doc.elements("element").get(0);
        
        assertEquals(20, elem.getAttributes().size());
        for (int i = 0; i < 20; i++) {
            assertEquals("value" + i, elem.attribute("attr" + i).getValue());
        }
    }

    @Test
    public void parse_manyChildren_storesAll() {
        StringBuilder xml = new StringBuilder("<root>");
        for (int i = 0; i < 50; i++) {
            xml.append("<item>").append(i).append("</item>");
        }
        xml.append("</root>");
        
        XmlDocument doc = XmlDocument.parse(xml.toString());
        XmlElement root = doc.elements("root").get(0);
        List<XmlElement> items = root.elements("item");
        
        assertEquals(50, items.size());
        for (int i = 0; i < 50; i++) {
            assertEquals(String.valueOf(i), items.get(i).getValue());
        }
    }

    // =====================================================================
    // Mixed content tests
    // =====================================================================

    @Test
    public void parse_mixedTextAndElements_extractsCorrectly() {
        String xml = "<root>Text before<child>child content</child>Text after</root>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement root = doc.elements("root").get(0);
        // Mixed content - value should contain all text
        assertNotNull(root.getValue());
        
        List<XmlElement> children = root.elements("child");
        assertEquals(1, children.size());
        assertEquals("child content", children.get(0).getValue());
    }

    @Test
    public void parse_xmlWithComments_ignoresComments() {
        String xml = "<root><!-- This is a comment --><item>value</item></root>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement root = doc.elements("root").get(0);
        List<XmlElement> items = root.elements("item");
        assertEquals(1, items.size());
        assertEquals("value", items.get(0).getValue());
    }

    @Test
    public void parse_xmlWithCdata_extractsCdataContent() {
        String xml = "<root><![CDATA[<special>content</special>]]></root>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement root = doc.elements("root").get(0);
        assertNotNull(root.getValue());
        assertTrue(root.getValue().contains("<special>content</special>"));
    }

    // =====================================================================
    // Whitespace handling tests
    // =====================================================================

    @Test
    public void parse_preservesSignificantWhitespace() {
        String xml = "<text>  multiple   spaces  </text>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement text = doc.elements("text").get(0);
        assertTrue(text.getValue().contains("  multiple   spaces  "));
    }

    @Test
    public void parse_multilineContent_preservesNewlines() {
        String xml = "<text>Line 1\nLine 2\nLine 3</text>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement text = doc.elements("text").get(0);
        assertTrue(text.getValue().contains("Line 1"));
        assertTrue(text.getValue().contains("Line 2"));
        assertTrue(text.getValue().contains("Line 3"));
    }

    // =====================================================================
    // Unicode and encoding tests
    // =====================================================================

    @Test
    public void parse_unicodeContent_handlesCorrectly() {
        String xml = "<text>日本語のテキスト</text>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement text = doc.elements("text").get(0);
        assertEquals("日本語のテキスト", text.getValue());
    }

    @Test
    public void parse_emojiContent_handlesCorrectly() {
        String xml = "<text>Hello 😀 World 🌍</text>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement text = doc.elements("text").get(0);
        assertTrue(text.getValue().contains("Hello"));
        assertTrue(text.getValue().contains("World"));
    }

    @Test
    public void parse_mixedLanguages_handlesCorrectly() {
        String xml = "<root><en>Hello</en><ja>こんにちは</ja><ar>مرحبا</ar></root>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement root = doc.elements("root").get(0);
        assertEquals("Hello", root.elements("en").get(0).getValue());
        assertEquals("こんにちは", root.elements("ja").get(0).getValue());
        assertEquals("مرحبا", root.elements("ar").get(0).getValue());
    }

    // =====================================================================
    // Attribute value edge cases
    // =====================================================================

    @Test
    public void parse_attributeWithEncodedQuotes_decodesCorrectly() {
        String xml = "<element value=\"He said &quot;Hello&quot;\"/>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement elem = doc.elements("element").get(0);
        assertEquals("He said \"Hello\"", elem.attribute("value").getValue());
    }

    @Test
    public void parse_attributeWithSingleQuotes_extractsCorrectly() {
        String xml = "<element value='single quoted'/>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement elem = doc.elements("element").get(0);
        assertEquals("single quoted", elem.attribute("value").getValue());
    }

    @Test
    public void parse_attributeWithNumericValue_extractsAsString() {
        String xml = "<element count=\"12345\" price=\"99.99\"/>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement elem = doc.elements("element").get(0);
        assertEquals("12345", elem.attribute("count").getValue());
        assertEquals("99.99", elem.attribute("price").getValue());
    }

    // =====================================================================
    // Element query tests
    // =====================================================================

    @Test
    public void elements_nonExistentName_returnsEmptyList() {
        String xml = "<root><item/></root>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement root = doc.elements("root").get(0);
        List<XmlElement> nonExistent = root.elements("nonexistent");
        
        assertNotNull(nonExistent);
        assertEquals(0, nonExistent.size());
    }

    @Test
    public void attribute_nonExistentName_returnsNull() {
        String xml = "<element attr1=\"value1\"/>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement elem = doc.elements("element").get(0);
        XmlAttribute nonExistent = elem.attribute("nonexistent");
        
        assertNull(nonExistent);
    }

    // =====================================================================
    // InputStream parsing tests
    // =====================================================================

    @Test
    public void parse_fromInputStream_worksCorrectly() {
        String xml = "<root><item>value</item></root>";
        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        
        XmlDocument doc = XmlDocument.parse(stream);
        
        assertNotNull(doc);
        assertEquals(1, doc.elements("root").size());
    }

    @Test
    public void parse_fromUtf8Stream_handlesEncoding() {
        String xml = "<root>日本語</root>";
        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        
        XmlDocument doc = XmlDocument.parse(stream);
        
        XmlElement root = doc.elements("root").get(0);
        assertEquals("日本語", root.getValue());
    }

    // =====================================================================
    // Real-world XML format tests
    // =====================================================================

    @Test
    public void parse_rssLikeFeed_extractsCorrectly() {
        String xml = 
            "<rss version=\"2.0\">" +
            "<channel>" +
            "<title>Test Feed</title>" +
            "<item><title>Item 1</title><link>http://example.com/1</link></item>" +
            "<item><title>Item 2</title><link>http://example.com/2</link></item>" +
            "</channel>" +
            "</rss>";
        
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement rss = doc.elements("rss").get(0);
        assertEquals("2.0", rss.attribute("version").getValue());
        
        XmlElement channel = rss.elements("channel").get(0);
        assertEquals("Test Feed", channel.elements("title").get(0).getValue());
        
        List<XmlElement> items = channel.elements("item");
        assertEquals(2, items.size());
        assertEquals("Item 1", items.get(0).elements("title").get(0).getValue());
        assertEquals("Item 2", items.get(1).elements("title").get(0).getValue());
    }

    @Test
    public void parse_configLikeXml_extractsCorrectly() {
        String xml = 
            "<configuration>" +
            "<setting name=\"debug\" value=\"true\"/>" +
            "<setting name=\"timeout\" value=\"30\"/>" +
            "<connection host=\"localhost\" port=\"8080\"/>" +
            "</configuration>";
        
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement config = doc.elements("configuration").get(0);
        List<XmlElement> settings = config.elements("setting");
        
        assertEquals(2, settings.size());
        assertEquals("debug", settings.get(0).attribute("name").getValue());
        assertEquals("true", settings.get(0).attribute("value").getValue());
        
        XmlElement conn = config.elements("connection").get(0);
        assertEquals("localhost", conn.attribute("host").getValue());
        assertEquals("8080", conn.attribute("port").getValue());
    }
}
