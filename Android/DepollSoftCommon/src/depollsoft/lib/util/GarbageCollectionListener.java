package depollsoft.lib.util;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

public class GarbageCollectionListener {
  private static List<Action<Void>> listeners;
  @SuppressWarnings("unused")
  private static WeakReference<GarbageCollectionListener> gcListener;
  static {
    GarbageCollectionListener.listeners = new LinkedList<Action<Void>>();
    GarbageCollectionListener.gcListener = new WeakReference<GarbageCollectionListener>(
        new GarbageCollectionListener());
  }

  public static synchronized void addListener(Action<Void> action) {
    GarbageCollectionListener.listeners.add(action);
  }

  private static synchronized void notifyListeners() {
    GarbageCollectionListener.gcListener = new WeakReference<GarbageCollectionListener>(
        new GarbageCollectionListener());
    for (Action<Void> l : GarbageCollectionListener.listeners)
      l.invoke(null);
  }

  public static synchronized void removeListener(Action<Void> action) {
    GarbageCollectionListener.listeners.remove(action);
  }

  private GarbageCollectionListener() {
  }

  @Override
  protected void finalize() {
    GarbageCollectionListener.notifyListeners();
  }
}
