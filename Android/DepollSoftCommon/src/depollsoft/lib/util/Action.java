package depollsoft.lib.util;

public interface Action<T> {
  void invoke(T parameter);
}
