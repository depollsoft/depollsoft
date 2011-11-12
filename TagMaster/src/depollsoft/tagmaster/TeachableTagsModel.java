package depollsoft.tagmaster;

import depollsoft.lib.binding.ObservableCollection;
import depollsoft.lib.binding.Trackable;
import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.Tracker;
import depollsoft.lib.util.Action;
import depollsoft.lib.util.Preferences;

@SuppressWarnings("unchecked")
public class TeachableTagsModel
{
   private static final String TeachableTagsPreference = "tagmaster.TeachableTags";
   private static TrackableField<ObservableCollection<Integer>> teachableTagIds = new TrackableField<ObservableCollection<Integer>>();

   static
   {
      if (Preferences.get(TeachableTagsModel.TeachableTagsPreference) == null)
         TeachableTagsModel.setTeachableTagIds(new ObservableCollection<Integer>());
      else
         TeachableTagsModel
               .setTeachableTagIds((ObservableCollection<Integer>) Preferences
                     .get(TeachableTagsModel.TeachableTagsPreference));
      Trackable.track(new Tracker()
      {

         public void update()
         {
            TeachableTagsModel.storeValue();
            Trackable.track(this, new Action<Void>()
            {

               public void invoke(Void parameter)
               {
                  TeachableTagsModel.getTeachableTagIds().track();
               }
            });
         }
      }, new Action<Void>()
      {

         public void invoke(Void parameter)
         {
            TeachableTagsModel.getTeachableTagIds().track();
         }
      });
   }

   public static void addTeachableTag(int id)
   {
      if (!TeachableTagsModel.getTeachableTagIds().contains(id))
         TeachableTagsModel.getTeachableTagIds().add(id);
   }

   public static boolean canMoveDown(int id)
   {
      int index = TeachableTagsModel.getTeachableTagIds().indexOf(id);
      return index < TeachableTagsModel.getTeachableTagIds().size() - 1;
   }

   public static boolean canMoveUp(int id)
   {
      int index = TeachableTagsModel.getTeachableTagIds().indexOf(id);
      return index > 0;
   }

   public static ObservableCollection<Integer> getTeachableTagIds()
   {
      return TeachableTagsModel.teachableTagIds.getValue();
   }

   public static boolean getIsTeachableTag(int id)
   {
      return TeachableTagsModel.getTeachableTagIds().contains(id);
   }

   public static void moveDown(int id)
   {
      int index = TeachableTagsModel.getTeachableTagIds().indexOf(id);
      TeachableTagsModel.getTeachableTagIds().remove(index);
      TeachableTagsModel.getTeachableTagIds().add(index + 1, id);
   }

   public static void moveUp(int id)
   {
      int index = TeachableTagsModel.getTeachableTagIds().indexOf(id);
      TeachableTagsModel.getTeachableTagIds().remove(index);
      TeachableTagsModel.getTeachableTagIds().add(index - 1, id);
   }

   public static void removeTeachableTag(int id)
   {
      TeachableTagsModel.getTeachableTagIds().remove(new Integer(id));
   }

   public static void resetTeachableTags()
   {
      TeachableTagsModel.getTeachableTagIds().clear();
   }

   public static void setTeachableTagIds(ObservableCollection<Integer> value)
   {
      TeachableTagsModel.teachableTagIds.setValue(value);
   }

   private static void storeValue()
   {
      Preferences.set(TeachableTagsModel.TeachableTagsPreference,
            TeachableTagsModel.getTeachableTagIds());
   }
}
