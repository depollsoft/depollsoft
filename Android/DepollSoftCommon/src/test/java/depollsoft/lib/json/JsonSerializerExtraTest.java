package depollsoft.lib.json;

import android.os.Build;

import static org.junit.Assert.*;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.LOLLIPOP)
public class JsonSerializerExtraTest {

    enum Color { RED, GREEN }

    public static class Holder {
        private Color Color;
        private List<Integer> Numbers;
        private String Name;

        public Color getColor() { return Color; }
        public void setColor(Color color) { Color = color; }
        public List<Integer> getNumbers() { return Numbers; }
        public void setNumbers(List<Integer> numbers) { Numbers = numbers; }
        public String getName() { return Name; }
        public void setName(String name) { Name = name; }
    }

    @Test
    public void serialize_handlesEnumsNullsAndNestedCollections() {
        Holder h = new Holder();
        h.setColor(Color.GREEN);
        h.setNumbers(new ArrayList<>(Arrays.asList(1, 2, 3)));
        h.setName(null); // exercise null branch

        JSONObject jo = JsonSerializer.serialize(h);
        assertEquals(Holder.class.getName(), jo.optString("*type"));

        Object back = JsonSerializer.deserialize(jo);
        Holder rt = (Holder) back;
        assertEquals(Color.GREEN, rt.getColor());
        assertEquals(Arrays.asList(1, 2, 3), rt.getNumbers());
        assertNull(rt.getName());
    }

    @Test
    public void primitiveArrayRoundTrip() {
        List<Object> mixed = new ArrayList<>();
        mixed.add("s");
        mixed.add(5);
        mixed.add(true);
        mixed.add(null);

        JSONObject arr = JsonSerializer.serialize(mixed);
        Object back = JsonSerializer.deserialize(arr);
        @SuppressWarnings("unchecked")
        List<Object> rt = (List<Object>) back;
        assertEquals(mixed.size(), rt.size());
        assertEquals("s", rt.get(0));
        assertEquals(5, rt.get(1));
        assertEquals(true, rt.get(2));
        assertNull(rt.get(3));
    }
}
