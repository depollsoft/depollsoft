package depollsoft.lib.json;

import android.os.Build;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.M)
public class JsonSerializerTest {
    public static class Pojo {
        private String Name;
        private Integer Count;

        public String getName() { return Name; }
        public void setName(String name) { Name = name; }
        public Integer getCount() { return Count; }
        public void setCount(Integer count) { Count = count; }
    }

    @Test
    public void serialize_and_deserialize_pojo_and_list() {
        Pojo p = new Pojo();
        p.setName("A");
        p.setCount(2);

        JSONObject jo = JsonSerializer.serialize(p);
        Object back = JsonSerializer.deserialize(jo);
        Pojo roundTrip = (Pojo) back;
        assertEquals("A", roundTrip.getName());
        assertEquals(Integer.valueOf(2), roundTrip.getCount());

        List<String> list = new ArrayList<>();
        list.add("x"); list.add("y");
        JSONObject arr = JsonSerializer.serialize(list);
        Object listBack = JsonSerializer.deserialize(arr);
        @SuppressWarnings("unchecked")
        List<String> rt = (List<String>) listBack;
        assertEquals(list, rt);
    }
}
