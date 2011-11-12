package depollsoft.tagmaster.lib;

import depollsoft.lib.binding.ValueConverter;

public class RatingConverter extends ValueConverter
{

   @Override
   public Object convertToTarget(Object sourceValue, Class<?> targetType)
   {
      double value = (Double) sourceValue;
      return (int) (value * 1000);
   }

}
