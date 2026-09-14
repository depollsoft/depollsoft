package depollsoft.tagmaster.lib;

import com.bindroid.ValueConverter;

/** Converts the API's average rating to the Float expected by RatingBar. */
public class RatingConverter extends ValueConverter {
  @Override
  public Object convertToTarget(Object sourceValue, Class<?> targetType) {
    if (targetType == Float.class || targetType == Float.TYPE) {
      return sourceValue instanceof Number ? ((Number) sourceValue).floatValue() : 0f;
    }
    // Retain the scaled integer form for callers that still use a ProgressBar.
    double value = sourceValue instanceof Number ? ((Number) sourceValue).doubleValue() : 0d;
    return (int) (value * 1000);
  }
}
