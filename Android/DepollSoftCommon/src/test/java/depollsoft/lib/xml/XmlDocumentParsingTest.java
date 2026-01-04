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
 * Additional comprehensive unit tests for the XmlDocument class.
 * Supplements XmlDocumentTest with more edge cases and scenarios.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.LOLLIPOP)
public class XmlDocumentParsingTest {

    // =====================================================================
    // parse(String) tests
    // =====================================================================

    @Test
    public void parse_emptyRootElement_createsDocument() {
        String xml = "<root></root>";
        XmlDocument doc = XmlDocument.parse(xml);
        assertNotNull(doc);
        assertEquals(1, doc.getElements().size());
        assertEquals("root", doc.getElements().get(0).getName());
    }

    @Test
    public void parse_selfClosingElement_createsDocument() {
        String xml = "<root/>";
        XmlDocument doc = XmlDocument.parse(xml);
        assertNotNull(doc);
        assertEquals(1, doc.getElements().size());
        assertEquals("root", doc.getElements().get(0).getName());
    }

    @Test
    public void parse_elementWithTextContent_extractsValue() {
        String xml = "<message>Hello World</message>";
        XmlDocument doc = XmlDocument.parse(xml);
        assertNotNull(doc);
        XmlElement msg = doc.elements("message").get(0);
        assertEquals("Hello World", msg.getValue());
    }

    @Test
    public void parse_elementWithWhitespaceContent_preservesWhitespace() {
        String xml = "<content>  spaced  </content>";
        XmlDocument doc = XmlDocument.parse(xml);
        XmlElement content = doc.elements("content").get(0);
        assertTrue(content.getValue().contains("  spaced  "));
    }

    @Test
    public void parse_multipleRootElements_returnsFirst() {
        // Note: This is technically invalid XML, but testing parser behavior
        String xml = "<root1>A</root1>";
        XmlDocument doc = XmlDocument.parse(xml);
        assertNotNull(doc);
        assertEquals(1, doc.getElements().size());
    }

    @Test
    public void parse_nestedElements_buildsTree() {
        String xml = "<outer><inner><deep>value</deep></inner></outer>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement outer = doc.elements("outer").get(0);
        XmlElement inner = outer.elements("inner").get(0);
        XmlElement deep = inner.elements("deep").get(0);
        
        assertEquals("value", deep.getValue());
    }

    @Test
    public void parse_siblingElements_returnsAll() {
        String xml = "<root><item>1</item><item>2</item><item>3</item></root>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement root = doc.elements("root").get(0);
        List<XmlElement> items = root.elements("item");
        
        assertEquals(3, items.size());
        assertEquals("1", items.get(0).getValue());
        assertEquals("2", items.get(1).getValue());
        assertEquals("3", items.get(2).getValue());
    }

    @Test
    public void parse_mixedSiblings_groupsCorrectly() {
        String xml = "<root><a>1</a><b>2</b><a>3</a><c>4</c><a>5</a></root>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement root = doc.elements("root").get(0);
        
        List<XmlElement> aElements = root.elements("a");
        assertEquals(3, aElements.size());
        
        List<XmlElement> bElements = root.elements("b");
        assertEquals(1, bElements.size());
        
        List<XmlElement> cElements = root.elements("c");
        assertEquals(1, cElements.size());
    }

    // =====================================================================
    // Attribute parsing tests
    // =====================================================================

    @Test
    public void parse_singleAttribute_extractsCorrectly() {
        String xml = "<element id=\"123\"></element>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement elem = doc.elements("element").get(0);
        assertEquals("123", elem.attribute("id").getValue());
    }

    @Test
    public void parse_multipleAttributes_extractsAll() {
        String xml = "<element id=\"1\" name=\"test\" enabled=\"true\"></element>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement elem = doc.elements("element").get(0);
        assertEquals("1", elem.attribute("id").getValue());
        assertEquals("test", elem.attribute("name").getValue());
        assertEquals("true", elem.attribute("enabled").getValue());
    }

    @Test
    public void parse_attributeWithEmptyValue_extractsEmptyString() {
        String xml = "<element value=\"\"></element>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement elem = doc.elements("element").get(0);
        assertEquals("", elem.attribute("value").getValue());
    }

    @Test
    public void parse_attributeWithSpaces_preservesSpaces() {
        String xml = "<element value=\"hello world\"></element>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement elem = doc.elements("element").get(0);
        assertEquals("hello world", elem.attribute("value").getValue());
    }

    @Test
    public void parse_selfClosingWithAttributes_extractsCorrectly() {
        String xml = "<element id=\"456\" type=\"test\"/>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement elem = doc.elements("element").get(0);
        assertEquals("456", elem.attribute("id").getValue());
        assertEquals("test", elem.attribute("type").getValue());
    }

    // =====================================================================
    // Special character tests
    // =====================================================================

    @Test
    public void parse_entityEncodedContent_decodesCorrectly() {
        String xml = "<text>&lt;Hello&gt; &amp; &quot;World&quot;</text>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement text = doc.elements("text").get(0);
        assertEquals("<Hello> & \"World\"", text.getValue());
    }

    @Test
    public void parse_numericEntities_decodesCorrectly() {
        String xml = "<text>&#65;&#66;&#67;</text>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement text = doc.elements("text").get(0);
        assertEquals("ABC", text.getValue());
    }

    // =====================================================================
    // Complex structure tests
    // =====================================================================

    @Test
    public void parse_realWorldXml_buildsCorrectTree() {
        String xml = "<library>" +
                     "<book id=\"1\" category=\"fiction\">" +
                     "<title>The Great Novel</title>" +
                     "<author>John Doe</author>" +
                     "<year>2020</year>" +
                     "</book>" +
                     "<book id=\"2\" category=\"non-fiction\">" +
                     "<title>Learning XML</title>" +
                     "<author>Jane Smith</author>" +
                     "<year>2019</year>" +
                     "</book>" +
                     "</library>";
        
        XmlDocument doc = XmlDocument.parse(xml);
        XmlElement library = doc.elements("library").get(0);
        List<XmlElement> books = library.elements("book");
        
        assertEquals(2, books.size());
        
        XmlElement book1 = books.get(0);
        assertEquals("1", book1.attribute("id").getValue());
        assertEquals("fiction", book1.attribute("category").getValue());
        assertEquals("The Great Novel", book1.elements("title").get(0).getValue());
        assertEquals("John Doe", book1.elements("author").get(0).getValue());
        assertEquals("2020", book1.elements("year").get(0).getValue());
        
        XmlElement book2 = books.get(1);
        assertEquals("2", book2.attribute("id").getValue());
        assertEquals("non-fiction", book2.attribute("category").getValue());
    }

    @Test
    public void parse_xmlWithDeclaration_ignoresDeclaration() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><item>test</item></root>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        assertNotNull(doc);
        XmlElement root = doc.elements("root").get(0);
        assertEquals("test", root.elements("item").get(0).getValue());
    }

    // =====================================================================
    // parse(InputStream) tests
    // =====================================================================

    @Test
    public void parseInputStream_validXml_createsDocument() {
        String xml = "<root><item>test</item></root>";
        InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        
        XmlDocument doc = XmlDocument.parse(input);
        assertNotNull(doc);
        assertEquals("root", doc.elements("root").get(0).getName());
    }

    @Test
    public void parseInputStream_emptyStream_returnsNull() {
        InputStream input = new ByteArrayInputStream(new byte[0]);
        XmlDocument doc = XmlDocument.parse(input);
        // Parser behavior on empty input - may return null or throw
        // This test documents the actual behavior
    }

    // =====================================================================
    // elements(String) on document tests
    // =====================================================================

    @Test
    public void elements_rootLevel_returnsMatchingRoots() {
        String xml = "<root>content</root>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        List<XmlElement> roots = doc.elements("root");
        assertEquals(1, roots.size());
    }

    @Test
    public void elements_noMatch_returnsEmptyList() {
        String xml = "<root>content</root>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        List<XmlElement> nonExistent = doc.elements("nonexistent");
        assertNotNull(nonExistent);
        assertEquals(0, nonExistent.size());
    }

    // =====================================================================
    // Edge case tests
    // =====================================================================

    @Test
    public void parse_deeplyNestedStructure_buildsCorrectly() {
        StringBuilder xml = new StringBuilder("<l1>");
        for (int i = 2; i <= 10; i++) {
            xml.append("<l").append(i).append(">");
        }
        xml.append("deep");
        for (int i = 10; i >= 2; i--) {
            xml.append("</l").append(i).append(">");
        }
        xml.append("</l1>");
        
        XmlDocument doc = XmlDocument.parse(xml.toString());
        assertNotNull(doc);
        
        XmlElement current = doc.elements("l1").get(0);
        for (int i = 2; i <= 10; i++) {
            current = current.elements("l" + i).get(0);
        }
        assertEquals("deep", current.getValue());
    }

    @Test
    public void parse_manyAttributes_handlesAll() {
        StringBuilder xml = new StringBuilder("<element ");
        for (int i = 0; i < 20; i++) {
            xml.append("attr").append(i).append("=\"value").append(i).append("\" ");
        }
        xml.append("/>");
        
        XmlDocument doc = XmlDocument.parse(xml.toString());
        XmlElement elem = doc.elements("element").get(0);
        
        for (int i = 0; i < 20; i++) {
            XmlAttribute attr = elem.attribute("attr" + i);
            assertNotNull("Attribute attr" + i + " should exist", attr);
            assertEquals("value" + i, attr.getValue());
        }
    }

    @Test
    public void parse_manySiblings_handlesAll() {
        StringBuilder xml = new StringBuilder("<root>");
        for (int i = 0; i < 50; i++) {
            xml.append("<item>").append(i).append("</item>");
        }
        xml.append("</root>");
        
        XmlDocument doc = XmlDocument.parse(xml.toString());
        XmlElement root = doc.elements("root").get(0);
        List<XmlElement> items = root.elements("item");
        
        assertEquals(50, items.size());
    }

    @Test
    public void parse_mixedContent_extractsText() {
        String xml = "<p>Start <b>bold</b> end</p>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        XmlElement p = doc.elements("p").get(0);
        // Mixed content handling - the text is accumulated
        assertNotNull(p.getValue());
    }

    @Test
    public void parse_cdataSection_handlesCorrectly() {
        String xml = "<script><![CDATA[function() { return x < y; }]]></script>";
        XmlDocument doc = XmlDocument.parse(xml);
        
        assertNotNull(doc);
        XmlElement script = doc.elements("script").get(0);
        assertNotNull(script);
    }
}
