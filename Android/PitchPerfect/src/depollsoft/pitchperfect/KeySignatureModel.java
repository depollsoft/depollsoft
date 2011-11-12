package depollsoft.pitchperfect;

import depollsoft.lib.binding.ObservableCollection;
import depollsoft.lib.binding.TrackableField;
import depollsoft.pitchperfect.lib.Key;

public class KeySignatureModel
{

   private TrackableField<Boolean> isMajor = new TrackableField<Boolean>(true);

   private TrackableField<ObservableCollection<Key>> majorKeys = new TrackableField<ObservableCollection<Key>>();

   private TrackableField<ObservableCollection<Key>> minorKeys = new TrackableField<ObservableCollection<Key>>();

   public KeySignatureModel()
   {
      this.setMajorKeys(Key.getMajorKeys());
      this.setMinorKeys(Key.getMinorKeys());
   }

   public boolean getIsMajor()
   {
      return this.isMajor.getValue();
   }

   public ObservableCollection<Key> getMajorKeys()
   {
      return this.majorKeys.getValue();
   }

   public ObservableCollection<Key> getMinorKeys()
   {
      return this.minorKeys.getValue();
   }

   public void setIsMajor(boolean value)
   {
      this.isMajor.setValue(value);
   }

   public void setMajorKeys(ObservableCollection<Key> value)
   {
      this.majorKeys.setValue(value);
   }

   public void setMinorKeys(ObservableCollection<Key> value)
   {
      this.minorKeys.setValue(value);
   }
}
