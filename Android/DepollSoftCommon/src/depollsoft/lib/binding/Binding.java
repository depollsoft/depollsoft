package depollsoft.lib.binding;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;

import depollsoft.lib.util.Property;

public class Binding {
  private static class SourceTracker implements Tracker {
    private WeakReference<Binding> binding;

    public SourceTracker(Binding b) {
      this.binding = b.getWeakRef();
    }

    public void update() {
      Binding b = this.binding.get();
      if (b != null)
        b.applySourceToTarget();
    }
  }

  private static class TargetTracker implements Tracker {
    private WeakReference<Binding> binding;

    public TargetTracker(Binding b) {
      this.binding = b.getWeakRef();
    }

    public void update() {
      Binding b = this.binding.get();
      if (b != null)
        b.applyTargetToSource();
    }
  }

  private static WeakHashMap<Object, List<Binding>> weakBindings;
  static {
    Binding.weakBindings = new WeakHashMap<Object, List<Binding>>();
  }
  private Property<?> targetProperty;
  private Property<?> sourceProperty;
  private Tracker sourceTracker;
  private Tracker targetTracker;
  private WeakReference<Object> target;
  private ValueConverter converter;
  private BindingMode mode;
  private boolean isBound;
  private boolean isLoggingEnabled;

  private WeakReference<Binding> weakToMe;

  public Binding(Property<?> targetProperty, Property<?> sourceProperty) {
    this(targetProperty, sourceProperty, BindingMode.OneWay);
  }

  public Binding(Property<?> targetProperty, Property<?> sourceProperty,
      BindingMode mode) {
    this(targetProperty, sourceProperty, mode, ValueConverter
        .getDefaultConverter());
  }

  public Binding(Property<?> targetProperty, Property<?> sourceProperty,
      BindingMode mode, ValueConverter converter) {
    this.weakToMe = new WeakReference<Binding>(this);
    this.targetProperty = targetProperty;
    this.sourceProperty = sourceProperty;
    this.mode = mode;
    this.converter = converter;
    this.isBound = false;
    this.isLoggingEnabled = false;
    this.sourceTracker = new SourceTracker(this);
    this.targetTracker = new TargetTracker(this);
  }

  @SuppressWarnings("unchecked")
  private void applySourceToTarget() {
    try {
      if (!(this.mode == BindingMode.TwoWay || this.mode == BindingMode.OneWay))
        return;
      if (this.sourceProperty.getGetter() == null
          || this.targetProperty.getSetter() == null)
        return;
      Object sourceValue = Trackable.track(this.sourceTracker,
          this.sourceProperty.getGetter());
      Object convertedValue = this.converter.convertToTarget(sourceValue,
          this.targetProperty.getType());
      // if (convertedValue != null
      // && !this.targetProperty.getType().isInstance(convertedValue))
      // throw new IllegalArgumentException(
      // "Converted value has the wrong type");
      ((Property<Object>) this.targetProperty).setValue(convertedValue);
    }
    catch (Exception e) {
      if (this.isLoggingEnabled) {
        System.err.println("Ignored exception in applySourceToTarget");
        System.err.println(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void applyTargetToSource() {
    try {
      if (!(this.mode == BindingMode.TwoWay || this.mode == BindingMode.OneWayToSource))
        return;
      if (this.targetProperty.getGetter() == null
          || this.sourceProperty.getSetter() == null)
        return;
      Object targetValue = Trackable.track(this.targetTracker,
          this.targetProperty.getGetter());
      Object convertedValue = this.converter.convertToSource(targetValue,
          this.sourceProperty.getType());
      // if (convertedValue != null
      // && !this.sourceProperty.getType().isInstance(convertedValue))
      // throw new IllegalArgumentException(
      // "Converted value has the wrong type");
      ((Property<Object>) this.sourceProperty).setValue(convertedValue);
    }
    catch (Exception e) {
      if (this.isLoggingEnabled) {
        System.err.println("Ignored exception in applyTargetToSource");
        System.err.println(e);
      }
    }
  }

  public Binding bind(Object target) {
    if (this.isBound)
      return this;
    this.isBound = true;
    if (!Binding.weakBindings.containsKey(target))
      Binding.weakBindings.put(target, new LinkedList<Binding>());
    Binding.weakBindings.get(target).add(this);
    this.target = new WeakReference<Object>(target);
    this.initializeBinding();
    return this;
  }

  public ValueConverter getConverter() {
    return this.converter;
  }

  public boolean getIsBound() {
    return this.isBound;
  }

  public boolean getIsLoggingEnabled() {
    return this.isLoggingEnabled;
  }

  public BindingMode getMode() {
    return this.mode;
  }

  public Property<?> getSourceProperty() {
    return this.sourceProperty;
  }

  public Property<?> getTargetProperty() {
    return this.targetProperty;
  }

  public WeakReference<Binding> getWeakRef() {
    return this.weakToMe;
  }

  private void initializeBinding() {
    this.applySourceToTarget();
    this.applyTargetToSource();
  }

  public void setIsLoggingEnabled(boolean log) {
    this.isLoggingEnabled = log;
  }

  public void unbind() {
    if (!this.isBound || this.target == null)
      return;
    Object target = this.target.get();
    if (target == null)
      return;
    Binding.weakBindings.get(target).remove(this);
  }
}
