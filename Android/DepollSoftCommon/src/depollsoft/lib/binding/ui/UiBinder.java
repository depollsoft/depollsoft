package depollsoft.lib.binding.ui;

import java.util.LinkedList;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.WeakHashMap;

import depollsoft.lib.binding.Binding;
import depollsoft.lib.binding.BindingMode;
import depollsoft.lib.binding.ValueConverter;
import depollsoft.lib.util.Property;
import depollsoft.lib.util.ReflectedProperty;
import android.app.Activity;
import android.view.View;

public class UiBinder {
  private static WeakHashMap<View, List<WeakReference<Binding>>> viewBindings = new WeakHashMap<View, List<WeakReference<Binding>>>();
  private static WeakHashMap<Activity, List<WeakReference<Binding>>> activityBindings = new WeakHashMap<Activity, List<WeakReference<Binding>>>();

  public static Binding bind(Activity activity, int targetId,
      String targetProperty, String sourceProperty) {
    return UiBinder.bind(activity, targetId, targetProperty, sourceProperty,
        BindingMode.OneWay, ValueConverter.getDefaultConverter());
  }

  public static Binding bind(Activity activity, int targetId,
      String targetProperty, String sourceProperty, BindingMode mode) {
    return UiBinder.bind(activity, targetId, targetProperty, sourceProperty,
        mode, ValueConverter.getDefaultConverter());
  }

  public static Binding bind(Activity activity, int targetId,
      String targetProperty, String sourceProperty, BindingMode mode,
      ValueConverter converter) {
    Binding b = new Binding(new ReflectedProperty(
        activity.findViewById(targetId), targetProperty),
        new ReflectedProperty(activity, sourceProperty), mode, converter);
    b.bind(activity);
    UiBinder.registerBinding(activity, b);
    return b;
  }

  public static Binding bind(Activity activity, int targetId,
      String targetProperty, String sourceProperty, ValueConverter converter) {
    return UiBinder.bind(activity, targetId, targetProperty, sourceProperty,
        BindingMode.OneWay, converter);
  }

  public static Binding bind(Activity activity, Property<?> targetProperty,
      String sourceProperty, BindingMode mode) {
    Binding b = new Binding(targetProperty, new ReflectedProperty(activity,
        sourceProperty), mode);
    b.bind(activity);
    UiBinder.registerBinding(activity, b);
    return b;
  }

  public static Binding bind(View view, int targetId, String targetProperty,
      String sourceProperty) {
    return UiBinder.bind(view, targetId, targetProperty, sourceProperty,
        BindingMode.OneWay, ValueConverter.getDefaultConverter());
  }

  public static Binding bind(View view, int targetId, String targetProperty,
      String sourceProperty, BindingMode mode) {
    return UiBinder.bind(view, targetId, targetProperty, sourceProperty, mode,
        ValueConverter.getDefaultConverter());
  }

  public static Binding bind(View view, int targetId, String targetProperty,
      String sourceProperty, BindingMode mode, ValueConverter converter) {
    Binding b = new Binding(new ReflectedProperty(view.findViewById(targetId),
        targetProperty), new ReflectedProperty(view, sourceProperty), mode,
        converter);
    b.bind(view);
    UiBinder.registerBinding(view, b);
    return b;
  }

  public static Binding bind(View view, int targetId, String targetProperty,
      String sourceProperty, ValueConverter converter) {
    return UiBinder.bind(view, targetId, targetProperty, sourceProperty,
        BindingMode.OneWay, converter);
  }

  public static Binding bind(View view, Property<?> targetProperty,
      String sourceProperty, BindingMode mode) {
    Binding b = new Binding(targetProperty, new ReflectedProperty(view,
        sourceProperty), mode);
    b.bind(view);
    UiBinder.registerBinding(view, b);
    return b;
  }

  public static void registerBinding(Activity activity, Binding binding) {
    if (!UiBinder.activityBindings.containsKey(activity))
      UiBinder.activityBindings.put(activity,
          new LinkedList<WeakReference<Binding>>());
    UiBinder.activityBindings.get(activity).add(binding.getWeakRef());
  }

  public static void registerBinding(View view, Binding binding) {
    if (!UiBinder.viewBindings.containsKey(view))
      UiBinder.viewBindings.put(view, new LinkedList<WeakReference<Binding>>());
    UiBinder.viewBindings.get(view).add(binding.getWeakRef());
  }

  public static void unbind(Activity activity) {
    if (!UiBinder.activityBindings.containsKey(activity))
      return;
    for (WeakReference<Binding> wb : UiBinder.activityBindings.get(activity)) {
      Binding b = wb.get();
      if (b != null)
        b.unbind();
    }
    UiBinder.activityBindings.remove(activity);
  }

  public static void unbind(View view) {
    if (!UiBinder.viewBindings.containsKey(view))
      return;
    for (WeakReference<Binding> wb : UiBinder.viewBindings.get(view)) {
      Binding b = wb.get();
      if (b != null)
        b.unbind();
    }
    UiBinder.viewBindings.remove(view);
  }

  private UiBinder() {
  }
}
