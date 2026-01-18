package depollsoft.tagmaster.barbershop;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = android.app.Application.class, manifest = Config.NONE)
public class TagCacheTest {
    @Test
    public void caches_and_clears_cache_directory() throws Exception {
        // Seed RichApplication context
        android.content.Context app = RuntimeEnvironment.getApplication();
        java.lang.reflect.Field f = depollsoft.lib.activity.RichApplication.class.getDeclaredField("context");
        f.setAccessible(true);
        f.set(null, app);

        // Ensure cache directory exists and is empty
        File dir = new File(app.getFilesDir(), "TagCache");
        dir.mkdirs();
        File[] toDelete = dir.listFiles();
        if (toDelete != null) {
            for (File fileEntry : toDelete) { fileEntry.delete(); }
        }

        File file = new File(dir, "123");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write("{}".getBytes(StandardCharsets.UTF_8));
        }
        assertTrue(file.exists());
        assertTrue(Tag.Companion.getCurrentCacheSize() > 0);

        // Clear and validate
        Tag.Companion.clearCache();
        assertEquals(0L, Tag.Companion.getCurrentCacheSize());
        File[] remaining = dir.listFiles();
        assertTrue(remaining == null || remaining.length == 0);
    }
}
