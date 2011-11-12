package depollsoft.lib.xml;

import depollsoft.lib.binding.TrackableField;

public class XmlAttribute
{

   private TrackableField<String> name = new TrackableField<String>();

   private TrackableField<String> value = new TrackableField<String>();

   public String getName()
   {
      return this.name.getValue();
   }

   public String getValue()
   {
      return this.value.getValue();
   }

   public void setName(String value)
   {
      this.name.setValue(value);
   }

   public void setValue(String value)
   {
      this.value.setValue(value);
   }

   @Override
   public String toString()
   {
      return "" + this.getName() + " -> " + this.getValue();
   }
}
