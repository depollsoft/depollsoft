package depollsoft.lib.util;

/** A callback taking one argument. */
public interface Action<T> {
  void invoke(T parameter);
}
