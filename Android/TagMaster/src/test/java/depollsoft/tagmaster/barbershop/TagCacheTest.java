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
    public void loads_persisted_tag_then_reuses_memory_cache() throws Exception {
        android.content.Context app = RuntimeEnvironment.getApplication();
        depollsoft.lib.activity.RichApplication.setAppContextForTesting(app);
        File dir = new File(app.getFilesDir(), "TagCache");
        dir.mkdirs();
        Tag.Companion.clearCache();
        Tag tag = new Tag();
        tag.setId(456);
        tag.setTitle("Offline tag");
        File file = new File(dir, "456");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(depollsoft.lib.json.JsonSerializer.serialize(tag).toString().getBytes(StandardCharsets.UTF_8));
        }
        bolts.Task<Tag> task = Tag.Companion.loadTagById(456);
        assertTrue(task.waitForCompletion(5, java.util.concurrent.TimeUnit.SECONDS));
        assertFalse(String.valueOf(task.getError()), task.isFaulted());
        assertEquals("Offline tag", task.getResult().getTitle());
        assertTrue(file.delete());
        bolts.Task<Tag> cached = Tag.Companion.loadTagById(456);
        assertTrue(cached.isCompleted());
        assertSame(task.getResult(), cached.getResult());
        Tag.Companion.clearCache();
    }

    @Test
    public void empty_cache_directory_is_safe() throws Exception {
        android.content.Context app = RuntimeEnvironment.getApplication();
        depollsoft.lib.activity.RichApplication.setAppContextForTesting(app);
        Tag.Companion.clearCache();
        new File(app.getFilesDir(), "TagCache").delete();
        assertEquals(0L, Tag.Companion.getCurrentCacheSize());
        Tag.Companion.clearCache();
    }

    @Test
    public void caches_and_clears_cache_directory() throws Exception {
        // Seed RichApplication context
        android.content.Context app = RuntimeEnvironment.getApplication();
        depollsoft.lib.activity.RichApplication.setAppContextForTesting(app);

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
