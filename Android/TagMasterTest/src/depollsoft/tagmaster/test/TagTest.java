package depollsoft.tagmaster.test;

import depollsoft.tagmaster.barbershop.Tag;
import depollsoft.tagmaster.barbershop.TagQueryResult;
import junit.framework.Assert;
import junit.framework.TestCase;

public class TagTest extends TestCase {
  public void testCacheTag() {
    Tag t = Tag.queryById(2).waitFor();
    t.cache();
  }

  public void testFetchSingleTag() {
    Tag t = Tag.queryById(2).waitFor();
    Assert.assertEquals("I Love to Sing 'Em", t.getTitle());
  }

  public void testFetchTags() {
    TagQueryResult ts = Tag.query("Love", 20).waitFor();
    Assert.assertEquals(20, ts.getTags().size());
  }

  public void testLoadSingleTag() {
    Tag t = Tag.loadTagById(2).waitFor();
    Assert.assertEquals("I Love to Sing 'Em", t.getTitle());
  }
}
