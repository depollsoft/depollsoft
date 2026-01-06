package depollsoft.tagmaster.lib;

import com.bindroid.ValueConverter;

public class RatingConverter extends ValueConverter {

  @Override
  public Object convertToTarget(Object sourceValue, Class<?> targetType) {
    double value = (Double) sourceValue;
    return (int) (value * 1000);
  }

}
