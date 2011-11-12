package depollsoft.pitchperfect.lib;

import java.util.UUID;

import depollsoft.lib.binding.TrackableField;

public class PitchedSong implements Comparable<PitchedSong>
{
   private TrackableField<String> name = new TrackableField<String>();

   private TrackableField<Key> key = new TrackableField<Key>();

   private TrackableField<String> uuid = new TrackableField<String>();

   private TrackableField<Boolean> isPlaying = new TrackableField<Boolean>(
         false);

   public PitchedSong()
   {
      this.setId(UUID.randomUUID().toString());
   }

   public int compareTo(PitchedSong another)
   {
      return this.getName().compareToIgnoreCase(another.getName());
   }

   @Override
   public boolean equals(Object o)
   {
      if (!(o instanceof PitchedSong))
         return false;
      return ((PitchedSong) o).getId().equals(this.getId());
   }

   public String getId()
   {
      return this.uuid.getValue();
   }

   public boolean getIsPlaying()
   {
      return this.isPlaying.getValue()
            && this.getKey().getNote().getIsPlaying();
   }

   public Key getKey()
   {
      return this.key.getValue();
   }

   public String getName()
   {
      return this.name.getValue();
   }

   @Override
   public int hashCode()
   {
      return this.getId().hashCode();
   }

   public void play()
   {
      this.isPlaying.setValue(true);
      this.getKey().getNote().play();
   }

   public void setId(String value)
   {
      this.uuid.setValue(value);
   }

   public void setKey(Key value)
   {
      this.key.setValue(value);
   }

   public void setName(String value)
   {
      this.name.setValue(value);
   }

   public void stop()
   {
      this.isPlaying.setValue(false);
      this.getKey().getNote().stop();
   }
}
