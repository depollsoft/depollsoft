package depollsoft.tagmaster.barbershop;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TagUriTest {
    @Test
    public void builds_tag_uri() {
        assertEquals("http://tags.depoll.com/tag.php?id=42", Tag.Companion.getTagUri(42));
    }
}

