package depollsoft.pitchperfect;

import depollsoft.lib.activity.RichApplication;

import android.graphics.Typeface;

public class CommonModel {
  private static Typeface musiQwik;
  private static Typeface musiQwikB;
  private static Typeface musiSync;
  private static Typeface noteHedz;

  public static final String sharpString = "“";
  public static final String flatString = "’";

  public static Typeface getMusiQwik() {
    if (CommonModel.musiQwik == null)
      CommonModel.musiQwik = Typeface.createFromAsset(RichApplication.getAppContext().getAssets(),
          "fonts/MusiQwik.ttf");
    return CommonModel.musiQwik;
  }

  public static Typeface getMusiQwikB() {
    if (CommonModel.musiQwikB == null)
      CommonModel.musiQwikB = Typeface.createFromAsset(RichApplication.getAppContext().getAssets(),
          "fonts/MusiQwikB.ttf");
    return CommonModel.musiQwikB;
  }

  public static Typeface getMusiSync() {
    if (CommonModel.musiSync == null)
      CommonModel.musiSync = Typeface.createFromAsset(RichApplication.getAppContext().getAssets(),
          "fonts/MusiSync.ttf");
    return CommonModel.musiSync;
  }

  public static Typeface getNoteHedz() {
    if (CommonModel.noteHedz == null)
      CommonModel.noteHedz = Typeface.createFromAsset(RichApplication.getAppContext().getAssets(),
          "fonts/NoteHedz170.ttf");
    return CommonModel.noteHedz;
  }
}
