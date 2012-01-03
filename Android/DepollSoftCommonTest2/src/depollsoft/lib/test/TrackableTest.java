package depollsoft.lib.test;

import depollsoft.lib.binding.Trackable;
import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.Tracker;
import depollsoft.lib.util.Action;
import junit.framework.Assert;
import junit.framework.TestCase;

public class TrackableTest extends TestCase {
  private int targetValue = 0;
  private final TrackableField<Integer> trackableField = new TrackableField<Integer>(
      Integer.MIN_VALUE);

  private int getTrackable() {
    return this.trackableField.getValue();
  }

  private void setTrackable(int value) {
    this.trackableField.setValue(value);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testPersistentTrackableField() {
    Assert.assertEquals(this.getTrackable(), Integer.MIN_VALUE);
    Tracker t = new Tracker() {
      @Override
      public void update() {
        TrackableTest.this.targetValue = TrackableTest.this.getTrackable() * 2;
        Trackable.track(this, new Action<Void>() {
          @Override
          public void invoke(Void o) {
            TrackableTest.this.getTrackable();
          }
        });
      }
    };
    Trackable.track(t, new Action<Void>() {
      @Override
      public void invoke(Void o) {
        TrackableTest.this.targetValue = TrackableTest.this.getTrackable();
      }
    });
    Assert.assertEquals(this.targetValue, this.getTrackable());
    this.setTrackable(150);
    Assert.assertEquals(this.getTrackable(), 150);
    Assert.assertEquals(this.targetValue, 300);
    this.setTrackable(300);
    Assert.assertEquals(this.getTrackable(), 300);
    Assert.assertEquals(this.targetValue, 600);
  }

  public void testSimpleTrackableField() {
    Assert.assertEquals(this.getTrackable(), Integer.MIN_VALUE);
    Tracker t = new Tracker() {
      @Override
      public void update() {
        TrackableTest.this.targetValue = TrackableTest.this.getTrackable() * 2;
      }
    };
    Trackable.track(t, new Action<Void>() {
      @Override
      public void invoke(Void o) {
        TrackableTest.this.targetValue = TrackableTest.this.getTrackable();
      }
    });
    Assert.assertEquals(this.targetValue, this.getTrackable());
    this.setTrackable(150);
    Assert.assertEquals(this.getTrackable(), 150);
    Assert.assertEquals(this.targetValue, 300);
    this.setTrackable(300);
    Assert.assertEquals(this.getTrackable(), 300);
    Assert.assertEquals(this.targetValue, 300);
  }

}
