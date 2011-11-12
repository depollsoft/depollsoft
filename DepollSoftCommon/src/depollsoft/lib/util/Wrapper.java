package depollsoft.lib.util;

public class Wrapper<T>
{
   private T value;

   public Wrapper(T initialValue)
   {
      this.value = initialValue;
   }

   public T getValue()
   {
      return this.value;
   }

   public void setValue(T value)
   {
      this.value = value;
   }
}
