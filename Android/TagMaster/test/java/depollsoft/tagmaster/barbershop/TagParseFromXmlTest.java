package depollsoft.tagmaster.barbershop;

import depollsoft.lib.xml.XmlAttribute;
import depollsoft.lib.xml.XmlElement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class TagParseFromXmlTest {
    private static XmlElement elem(String name, String value) {
        XmlElement e = new XmlElement();
        e.setName(name);
        e.setValue(value);
        return e;
    }

    @Test
    public void parses_fields_and_tracks_from_xml() throws Exception {
        Tag t = new Tag();
        XmlElement tagElem = new XmlElement();
        tagElem.setName("tag");
        tagElem.getElements().add(elem("id", "777"));
        tagElem.getElements().add(elem("Title", "Hello"));
        tagElem.getElements().add(elem("AltTitle", "Alt"));
        tagElem.getElements().add(elem("Version", "1.2"));
        tagElem.getElements().add(elem("Parts", "4"));
        tagElem.getElements().add(elem("Type", "Tag"));
        tagElem.getElements().add(elem("Recording", "Studio"));
        tagElem.getElements().add(elem("TeachVid", "tv"));
        tagElem.getElements().add(elem("Lyrics", "lyrics"));
        tagElem.getElements().add(elem("Notes", "notes"));
        tagElem.getElements().add(elem("Arranger", "arr"));
        tagElem.getElements().add(elem("ArrWebsite", "http://arr"));
        tagElem.getElements().add(elem("Arranged", "1999"));
        tagElem.getElements().add(elem("SungBy", "group"));
        tagElem.getElements().add(elem("SungWebsite", "http://group"));
        tagElem.getElements().add(elem("SungYear", "2000"));
        tagElem.getElements().add(elem("Quartet", "quartet"));
        tagElem.getElements().add(elem("QWebsite", "http://q"));
        tagElem.getElements().add(elem("Teacher", "teacher"));
        tagElem.getElements().add(elem("TWebsite", "http://t"));
        tagElem.getElements().add(elem("Provider", "prov"));
        tagElem.getElements().add(elem("ProvWebsite", "http://prov"));
        tagElem.getElements().add(elem("Classic", "9"));
        tagElem.getElements().add(elem("Rating", "3.7"));
        tagElem.getElements().add(elem("Downloaded", "12,345"));
        tagElem.getElements().add(elem("WritKey", "C Major"));

        String[] trackNames = new String[] {"SheetMusic","Notation","AllParts","Bass","Bari","Lead","Tenor","Other1","Other2","Other3","Other4"};
        for (String n : trackNames) {
            XmlElement tr = elem(n, "http://" + n.toLowerCase());
            XmlAttribute typeAttr = new XmlAttribute();
            typeAttr.setName("type"); typeAttr.setValue("audio");
            tr.getAttributes().add(typeAttr);
            tagElem.getElements().add(tr);
        }

        Method m = Tag.class.getDeclaredMethod("parseFromXml", XmlElement.class);
        m.setAccessible(true);
        m.invoke(t, tagElem);

        assertEquals(777, t.getId());
        assertEquals("Hello", t.getTitle());
        assertEquals(4, t.getParts());
        assertEquals(Integer.valueOf(9), t.getClassicTagNumber());
        assertEquals(3.7, t.getRating(), 0.0001);
        assertEquals(12345, t.getDownloadCount());
        assertNotNull(t.getTracks());
        assertFalse(t.getTracks().isEmpty());
        assertNotNull(t.getKeyNote());
        assertEquals("Alt", t.getAlternativeTitle());
        assertEquals("1.2", t.getVersion());
        assertEquals("Tag", t.getTagType());
        assertEquals("Studio", t.getRecordingMethod());
        assertEquals("tv", t.getTeachingVideo());
        assertEquals("lyrics", t.getLyrics());
        assertEquals("notes", t.getNotes());
        assertEquals("arr", t.getArranger());
        assertEquals("http://arr", t.getArrangerWebsite());
        assertEquals("1999", t.getYearArranged());
        assertEquals("group", t.getSungBy());
        assertEquals("http://group", t.getSungByWebsite());
        assertEquals("2000", t.getSungYear());
        assertEquals("quartet", t.getLearningTrackQuartet());
        assertEquals("http://q", t.getLearningTrackQuartetWebsite());
        assertEquals("teacher", t.getTeacher());
        assertEquals("http://t", t.getTeacherWebsite());
        assertEquals("prov", t.getProvider());
        assertEquals("http://prov", t.getProviderWebsite());

        Tag other = new Tag();
        other.setId(777);
        assertTrue(t.equals(other));
    }
}
