package depollsoft.lib.ui;

import android.app.Activity;
import depollsoft.lib.util.Action;
import depollsoft.lib.util.Function;
import depollsoft.lib.util.Property;

public class ThreadSwitchProperty<T> extends Property<T> {
  private Activity activity;
  private Property<T> property;

  public ThreadSwitchProperty(Property<T> property, Activity activity) {
    this.property = property;
    this.activity = activity;
    if (property.getGetter() != null)
      this.getter = new Function<T>() {
        public T evaluate() {
          return ThreadSwitchProperty.this.property.getValue();
        }
      };
    if (property.getSetter() != null)
      this.setter = new Action<T>() {
        public void invoke(final T parameter) {
          ThreadSwitchProperty.this.activity.runOnUiThread(new Runnable() {
            public void run() {
              ThreadSwitchProperty.this.property.setValue(parameter);
            }
          });
        }
      };
  }

  @Override
  public Class<?> getType() {
    return this.property.getType();
  }
}
