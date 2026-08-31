package depollsoft.lib.xml;

import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.M)
public class XmlDocumentTest {
    @Test
    public void parse_simpleXml_builds_tree_with_attributes() {
        String xml = "<tags available=\"1\" count=\"1\"><tag><id>123</id><Title>Foo</Title><Parts>4</Parts></tag></tags>";
        XmlDocument doc = XmlDocument.parse(xml);
        assertNotNull(doc);
        List<XmlElement> tagsList = doc.elements("tags");
        assertEquals(1, tagsList.size());
        XmlElement tags = tagsList.get(0);
        assertEquals("tags", tags.getName());
        assertEquals("1", tags.attribute("available").getValue());
        assertEquals("1", tags.attribute("count").getValue());

        List<XmlElement> tagElems = tags.elements("tag");
        assertEquals(1, tagElems.size());
        XmlElement tag = tagElems.get(0);
        assertEquals("123", tag.elements("id").get(0).getValue().trim());
        assertEquals("Foo", tag.elements("Title").get(0).getValue().trim());
        assertEquals("4", tag.elements("Parts").get(0).getValue().trim());
    }
}
