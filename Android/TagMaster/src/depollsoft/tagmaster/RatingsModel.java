package depollsoft.tagmaster;

import java.util.ArrayList;
import java.util.List;

import depollsoft.lib.util.Preferences;

@SuppressWarnings("unchecked")
public class RatingsModel
{
   static
   {
      RatingsModel.ratedIds = RatingsModel.getRatedIds();
   }
   private static final String ratedIdsKey = "tagmaster.RatedIds";
   private static List<Integer> ratedIds;

   public static void addRating(int id)
   {
      RatingsModel.ratedIds.add(id);
      Preferences.set(RatingsModel.ratedIdsKey, RatingsModel.ratedIds);
   }

   private static List<Integer> getRatedIds()
   {
      List<Integer> ratedIds;
      if ((ratedIds = (List<Integer>) Preferences.get(RatingsModel.ratedIdsKey)) == null)
         Preferences.set(RatingsModel.ratedIdsKey,
               ratedIds = new ArrayList<Integer>());
      return ratedIds;
   }

   public static boolean isRated(int id)
   {
      return RatingsModel.getRatedIds().contains(id);
   }

   public static void removeRating(int id)
   {
      RatingsModel.ratedIds.remove(id);
      Preferences.set(RatingsModel.ratedIdsKey, RatingsModel.ratedIds);
   }
}
