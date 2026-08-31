package depollsoft.lib.xml;

import static org.junit.Assert.*;

import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

/**
 * Unit tests for the XmlElement class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.M)
public class XmlElementTest {

    private XmlElement element;

    @Before
    public void setUp() {
        element = new XmlElement();
    }

    // =====================================================================
    // Constructor tests
    // =====================================================================

    @Test
    public void constructor_initializesEmptyElementsList() {
        assertNotNull(element.getElements());
        assertEquals(0, element.getElements().size());
    }

    @Test
    public void constructor_initializesEmptyAttributesList() {
        assertNotNull(element.getAttributes());
        assertEquals(0, element.getAttributes().size());
    }

    @Test
    public void constructor_nameIsNull() {
        assertNull(element.getName());
    }

    @Test
    public void constructor_valueIsNull() {
        assertNull(element.getValue());
    }

    // =====================================================================
    // Name property tests
    // =====================================================================

    @Test
    public void setName_storesName() {
        element.setName("testElement");
        assertEquals("testElement", element.getName());
    }

    @Test
    public void setName_withNull_storesNull() {
        element.setName("initial");
        element.setName(null);
        assertNull(element.getName());
    }

    @Test
    public void setName_withEmptyString_storesEmptyString() {
        element.setName("");
        assertEquals("", element.getName());
    }

    @Test
    public void setName_withSpecialCharacters_storesCorrectly() {
        element.setName("element:with:colons");
        assertEquals("element:with:colons", element.getName());
    }

    // =====================================================================
    // Value property tests
    // =====================================================================

    @Test
    public void setValue_storesValue() {
        element.setValue("some content");
        assertEquals("some content", element.getValue());
    }

    @Test
    public void setValue_withNull_storesNull() {
        element.setValue("initial");
        element.setValue(null);
        assertNull(element.getValue());
    }

    @Test
    public void setValue_withEmptyString_storesEmptyString() {
        element.setValue("");
        assertEquals("", element.getValue());
    }

    @Test
    public void setValue_withWhitespace_preservesWhitespace() {
        element.setValue("  spaces  ");
        assertEquals("  spaces  ", element.getValue());
    }

    @Test
    public void setValue_withNewlines_preservesNewlines() {
        element.setValue("line1\nline2\nline3");
        assertEquals("line1\nline2\nline3", element.getValue());
    }

    // =====================================================================
    // Elements list tests
    // =====================================================================

    @Test
    public void getElements_returnsModifiableList() {
        XmlElement child = new XmlElement();
        child.setName("child");
        element.getElements().add(child);
        
        assertEquals(1, element.getElements().size());
        assertEquals("child", element.getElements().get(0).getName());
    }

    @Test
    public void getElements_multipleChildren_storesAll() {
        for (int i = 0; i < 5; i++) {
            XmlElement child = new XmlElement();
            child.setName("child" + i);
            element.getElements().add(child);
        }
        
        assertEquals(5, element.getElements().size());
    }

    // =====================================================================
    // Attributes list tests
    // =====================================================================

    @Test
    public void getAttributes_returnsModifiableList() {
        XmlAttribute attr = new XmlAttribute();
        attr.setName("id");
        attr.setValue("123");
        element.getAttributes().add(attr);
        
        assertEquals(1, element.getAttributes().size());
        assertEquals("id", element.getAttributes().get(0).getName());
    }

    @Test
    public void getAttributes_multipleAttributes_storesAll() {
        for (int i = 0; i < 3; i++) {
            XmlAttribute attr = new XmlAttribute();
            attr.setName("attr" + i);
            attr.setValue("value" + i);
            element.getAttributes().add(attr);
        }
        
        assertEquals(3, element.getAttributes().size());
    }

    // =====================================================================
    // attribute(String) method tests
    // =====================================================================

    @Test
    public void attribute_existingAttribute_returnsAttribute() {
        XmlAttribute attr = new XmlAttribute();
        attr.setName("href");
        attr.setValue("http://example.com");
        element.getAttributes().add(attr);
        
        XmlAttribute found = element.attribute("href");
        assertNotNull(found);
        assertEquals("http://example.com", found.getValue());
    }

    @Test
    public void attribute_nonExistingAttribute_returnsNull() {
        XmlAttribute attr = new XmlAttribute();
        attr.setName("existing");
        element.getAttributes().add(attr);
        
        assertNull(element.attribute("nonexistent"));
    }

    @Test
    public void attribute_multipleAttributes_returnsCorrectOne() {
        String[] names = {"first", "second", "third"};
        for (String name : names) {
            XmlAttribute attr = new XmlAttribute();
            attr.setName(name);
            attr.setValue("value_" + name);
            element.getAttributes().add(attr);
        }
        
        XmlAttribute found = element.attribute("second");
        assertNotNull(found);
        assertEquals("value_second", found.getValue());
    }

    @Test
    public void attribute_emptyAttributesList_returnsNull() {
        assertNull(element.attribute("any"));
    }

    @Test
    public void attribute_returnsFirstMatchingAttribute() {
        // Add duplicate attribute names (unusual but possible)
        XmlAttribute attr1 = new XmlAttribute();
        attr1.setName("duplicate");
        attr1.setValue("first");
        element.getAttributes().add(attr1);
        
        XmlAttribute attr2 = new XmlAttribute();
        attr2.setName("duplicate");
        attr2.setValue("second");
        element.getAttributes().add(attr2);
        
        XmlAttribute found = element.attribute("duplicate");
        assertEquals("first", found.getValue());
    }

    // =====================================================================
    // elements(String) method tests
    // =====================================================================

    @Test
    public void elements_existingChildren_returnsMatchingList() {
        XmlElement child1 = new XmlElement();
        child1.setName("item");
        child1.setValue("A");
        
        XmlElement child2 = new XmlElement();
        child2.setName("item");
        child2.setValue("B");
        
        XmlElement child3 = new XmlElement();
        child3.setName("other");
        child3.setValue("C");
        
        element.getElements().add(child1);
        element.getElements().add(child2);
        element.getElements().add(child3);
        
        List<XmlElement> items = element.elements("item");
        assertEquals(2, items.size());
        assertEquals("A", items.get(0).getValue());
        assertEquals("B", items.get(1).getValue());
    }

    @Test
    public void elements_noMatchingChildren_returnsEmptyList() {
        XmlElement child = new XmlElement();
        child.setName("existing");
        element.getElements().add(child);
        
        List<XmlElement> result = element.elements("nonexistent");
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void elements_emptyChildrenList_returnsEmptyList() {
        List<XmlElement> result = element.elements("any");
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void elements_allMatching_returnsAll() {
        for (int i = 0; i < 5; i++) {
            XmlElement child = new XmlElement();
            child.setName("sameName");
            element.getElements().add(child);
        }
        
        List<XmlElement> result = element.elements("sameName");
        assertEquals(5, result.size());
    }

    @Test
    public void elements_noneMatching_returnsEmptyList() {
        for (int i = 0; i < 3; i++) {
            XmlElement child = new XmlElement();
            child.setName("name" + i);
            element.getElements().add(child);
        }
        
        List<XmlElement> result = element.elements("differentName");
        assertEquals(0, result.size());
    }

    // =====================================================================
    // toString tests
    // =====================================================================

    @Test
    public void toString_withNameAndValue_formatsCorrectly() {
        element.setName("root");
        element.setValue("content");
        assertEquals("root -> content", element.toString());
    }

    @Test
    public void toString_withNullNameAndValue_handlesNull() {
        String result = element.toString();
        assertEquals("null -> null", result);
    }

    @Test
    public void toString_withNameOnly_showsNullValue() {
        element.setName("element");
        assertEquals("element -> null", element.toString());
    }

    @Test
    public void toString_withValueOnly_showsNullName() {
        element.setValue("value");
        assertEquals("null -> value", element.toString());
    }

    // =====================================================================
    // Nested structure tests
    // =====================================================================

    @Test
    public void nestedElements_threeLevelsDeep_worksCorrectly() {
        element.setName("root");
        
        XmlElement level1 = new XmlElement();
        level1.setName("level1");
        element.getElements().add(level1);
        
        XmlElement level2 = new XmlElement();
        level2.setName("level2");
        level1.getElements().add(level2);
        
        XmlElement level3 = new XmlElement();
        level3.setName("level3");
        level3.setValue("deepValue");
        level2.getElements().add(level3);
        
        // Navigate through the structure
        XmlElement foundLevel1 = element.elements("level1").get(0);
        XmlElement foundLevel2 = foundLevel1.elements("level2").get(0);
        XmlElement foundLevel3 = foundLevel2.elements("level3").get(0);
        
        assertEquals("deepValue", foundLevel3.getValue());
    }

    @Test
    public void elementWithAttributesAndChildren_storesBothCorrectly() {
        element.setName("parent");
        
        // Add attributes
        XmlAttribute attr = new XmlAttribute();
        attr.setName("id");
        attr.setValue("123");
        element.getAttributes().add(attr);
        
        // Add children
        XmlElement child = new XmlElement();
        child.setName("child");
        child.setValue("childValue");
        element.getElements().add(child);
        
        assertEquals(1, element.getAttributes().size());
        assertEquals(1, element.getElements().size());
        assertEquals("123", element.attribute("id").getValue());
        assertEquals("childValue", element.elements("child").get(0).getValue());
    }
}
