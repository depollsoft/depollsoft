package depollsoft.lib.binding;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;

import depollsoft.lib.util.Action;
import depollsoft.lib.util.Function;

public class Trackable {
  private static ThreadLocal<Stack<Tracker>> trackersInFrame = new ThreadLocal<Stack<Tracker>>() {
    @Override
    protected synchronized Stack<Tracker> initialValue() {
      return new Stack<Tracker>();
    }
  };

  public static void track(Tracker tracker, Action<Void> action) {
    Trackable.trackersInFrame.get().push(tracker);
    try {
      action.invoke(null);
    }
    finally {
      Trackable.trackersInFrame.get().pop();
    }
  }

  public static <T> T track(Tracker tracker, Function<T> action) {
    Trackable.trackersInFrame.get().push(tracker);
    try {
      return action.evaluate();
    }
    finally {
      Trackable.trackersInFrame.get().pop();
    }
  }

  private LinkedList<Tracker> trackers;

  public Trackable() {
  }

  private LinkedList<Tracker> getTrackers() {
    if (trackers == null)
      trackers = new LinkedList<Tracker>();
    return trackers;
  }

  public void track() {
    if (Trackable.trackersInFrame.get().size() > 0)
      this.getTrackers().addAll(Trackable.trackersInFrame.get());
  }

  public void updateTrackers() {
    LinkedList<Tracker> trackers = this.getTrackers();
    this.trackers = new LinkedList<Tracker>();
    HashSet<Tracker> visited = new HashSet<Tracker>(trackers.size());
    for (Tracker t : trackers) {
      if (visited.contains(t))
        continue;
      visited.add(t);
      t.update();
    }
  }
}
