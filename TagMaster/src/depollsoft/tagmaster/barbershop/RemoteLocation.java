package depollsoft.tagmaster.barbershop;

import depollsoft.lib.binding.TrackableField;

public class RemoteLocation
{

   private TrackableField<String> uri = new TrackableField<String>();

   private TrackableField<String> type = new TrackableField<String>();

   public String getType()
   {
      return this.type.getValue();
   }

   public String getUri()
   {
      return this.uri.getValue();
   }

   public void setType(String value)
   {
      this.type.setValue(value);
   }

   public void setUri(String value)
   {
      this.uri.setValue(value);
   }
}
