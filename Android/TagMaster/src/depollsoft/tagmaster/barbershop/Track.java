package depollsoft.tagmaster.barbershop;

import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.util.ObjectUtilities;

public class Track
{

   private TrackableField<String> title = new TrackableField<String>();

   private TrackableField<RemoteLocation> source = new TrackableField<RemoteLocation>();

   public Track()
   {
   }

   public Track(String title, RemoteLocation source)
   {
      this.setTitle(title);
      this.setSource(source);
   }

   public boolean Equals(Object obj)
   {
      if (obj == null)
         return false;
      Track track = (Track) obj;
      return ObjectUtilities.equals(this.getTitle(), track.getTitle())
            && ObjectUtilities.equals(this.getSource(), track.getSource());
   }

   public RemoteLocation getSource()
   {
      return this.source.getValue();
   }

   public String getTitle()
   {
      return this.title.getValue();
   }

   public void setSource(RemoteLocation value)
   {
      this.source.setValue(value);
   }

   public void setTitle(String value)
   {
      this.title.setValue(value);
   }

   @Override
   public String toString()
   {
      return this.getTitle();
   }
}
