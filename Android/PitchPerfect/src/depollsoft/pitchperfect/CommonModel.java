package depollsoft.pitchperfect;

import depollsoft.lib.activity.RichApplication;

import android.content.Context;
import android.graphics.Typeface;

public class CommonModel {
  private static Typeface musiQwik;
  private static Typeface musiQwikB;
  private static Typeface musiSync;
  private static Typeface noteHedz;
  private static boolean testMode = false;

  public static final String sharpString = "ì";
  public static final String flatString = "í";

  /**
   * Enable test mode to allow tests to run without requiring Android assets.
   * When in test mode, typeface getters return null instead of loading from assets.
   */
  public static void setTestMode(boolean enabled) {
    testMode = enabled;
  }

  public static boolean isTestMode() {
    return testMode;
  }

  public static Typeface getMusiQwik() {
    if (testMode) return null;
    if (CommonModel.musiQwik == null) {
      Context context = RichApplication.getAppContext();
      if (context != null) {
        CommonModel.musiQwik = Typeface.createFromAsset(context.getAssets(),
            "fonts/MusiQwik.ttf");
      }
    }
    return CommonModel.musiQwik;
  }

  public static Typeface getMusiQwikB() {
    if (testMode) return null;
    if (CommonModel.musiQwikB == null) {
      Context context = RichApplication.getAppContext();
      if (context != null) {
        CommonModel.musiQwikB = Typeface.createFromAsset(context.getAssets(),
            "fonts/MusiQwikB.ttf");
      }
    }
    return CommonModel.musiQwikB;
  }

  public static Typeface getMusiSync() {
    if (testMode) return null;
    if (CommonModel.musiSync == null) {
      Context context = RichApplication.getAppContext();
      if (context != null) {
        CommonModel.musiSync = Typeface.createFromAsset(context.getAssets(),
            "fonts/MusiSync.ttf");
      }
    }
    return CommonModel.musiSync;
  }

  public static Typeface getNoteHedz() {
    if (testMode) return null;
    if (CommonModel.noteHedz == null) {
      Context context = RichApplication.getAppContext();
      if (context != null) {
        CommonModel.noteHedz = Typeface.createFromAsset(context.getAssets(),
            "fonts/NoteHedz170.ttf");
      }
    }
    return CommonModel.noteHedz;
  }
}
