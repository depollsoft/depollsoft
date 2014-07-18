package depollsoft.tagmaster;

import depollsoft.lib.activity.RichApplication;
import depollsoft.lib.util.ContentCache;
import depollsoft.lib.util.Preferences;

public class SettingsModel {
  private static final String minimumRandomTagRatingKey = "tagmaster.MinimumRandomTagRating";
  private static final String minimumRandomDownloadsKey = "tagmaster.MinimumRandomDownloadsKey";
  private static final String sheetMusicRandomKey = "tagmaster.SheetMusicRandomKey";
  private static final String learningTracksRandomKey = "tagmaster.LearningTracksRandomKey";

  static {
    Preferences.initialize(SettingsModel.minimumRandomDownloadsKey, 100,
        Integer.class);
    Preferences.initialize(SettingsModel.minimumRandomTagRatingKey, 2.5d,
        Double.class);
    Preferences.initialize(SettingsModel.sheetMusicRandomKey, true,
        Boolean.class);
    Preferences.initialize(SettingsModel.learningTracksRandomKey, null,
        Boolean.class);
  }

  public static void clearCache() {
    ContentCache cc = new ContentCache(RichApplication.getAppContext());
    cc.clearCache();
  }

  public static double getCacheSizeInMegabytes() {
    ContentCache cc = new ContentCache(RichApplication.getAppContext());
    return cc.getCacheSize() / 1024d / 1024d;
  }

  public static int getMinimumRandomDownloads() {
    return Preferences.<Integer>get(SettingsModel.minimumRandomDownloadsKey);
  }

  public static double getMinimumRandomTagRating() {
    return Preferences.<Double>get(SettingsModel.minimumRandomTagRatingKey);
  }

  public static Boolean getRandomLearningTracksFilter() {
    return Preferences.get(SettingsModel.learningTracksRandomKey);
  }

  public static Boolean getRandomSheetMusicFilter() {
    return Preferences.get(SettingsModel.sheetMusicRandomKey);
  }

  public static void setMinimumRandomDownloads(int value) {
    Preferences.set(SettingsModel.minimumRandomDownloadsKey, value);
  }

  public static void setMinimumRandomTagRating(double value) {
    Preferences.set(SettingsModel.minimumRandomTagRatingKey, value);
  }

  public static void setRandomLearningTracksFilter(Boolean value) {
    Preferences.set(SettingsModel.learningTracksRandomKey, value);
  }

  public static void setRandomSheetMusicFilter(Boolean value) {
    Preferences.set(SettingsModel.sheetMusicRandomKey, value);
  }

}
