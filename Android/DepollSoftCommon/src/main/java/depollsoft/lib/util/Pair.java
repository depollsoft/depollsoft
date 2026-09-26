package depollsoft.lib.util;

import java.util.Objects;

/** An immutable two-value tuple with value equality, usable as a map key. */
public final class Pair<L, R> {
  private final L left;
  private final R right;

  public Pair(L left, R right) {
    this.left = left;
    this.right = right;
  }

  public L getLeft() {
    return left;
  }

  public R getRight() {
    return right;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Pair)) return false;
    Pair<?, ?> pair = (Pair<?, ?>) other;
    return Objects.equals(left, pair.left) && Objects.equals(right, pair.right);
  }

  @Override
  public int hashCode() {
    return Objects.hash(left, right);
  }
}
