package depollsoft.pitchperfect;

import android.app.ActionBar;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.TypedValue;
import android.widget.RemoteViews;
import android.widget.TextView;

import depollsoft.pitchperfect.converters.PitchPipeNoteTextConverter;
import depollsoft.pitchperfect.lib.Note;

public class PitchPipeAppWidget extends AppWidgetProvider {
  private PitchPipeModel model = new PitchPipeModel();

  public PitchPipeAppWidget() {
  }

  public static void updateWidgets() {
    Intent i = new Intent(PitchPerfectApplication.getAppContext(), PitchPipeAppWidget.class);
    i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    int[] ids = {R.xml.pitchpipe_appwidget_info};
    i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
    PitchPerfectApplication.getAppContext().sendBroadcast(i);
  }

  @Override
  public void onDisabled(Context context) {
    super.onDisabled(context);
  }

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.pitchpipewidgetview);

    int[] ids = {
        R.id.pitchButton0,
        R.id.pitchButton1,
        R.id.pitchButton2,
        R.id.pitchButton3,
        R.id.pitchButton4,
        R.id.pitchButton5,
        R.id.pitchButton6,
        R.id.pitchButton7,
        R.id.pitchButton8,
        R.id.pitchButton9,
        R.id.pitchButton10,
        R.id.pitchButton11,
    };

    PitchPipeNoteTextConverter converter = new PitchPipeNoteTextConverter();

    for (int i = 0; i < ids.length; i++) {
      rv.setImageViewBitmap(ids[i],
          buildUpdate((CharSequence) converter.convertToTarget(model.getNotes().get(i), CharSequence.class)));
      rv.setOnClickPendingIntent(ids[i], getIntentForNote(model.getNotes().get(i), i));
      int pressed = R.drawable.button_background_pressed;
      int normal = R.drawable.button_background_normal;
      int background = model.getNotes().get(i).getIsPlaying() ?
          pressed :
          normal;
      rv.setInt(ids[i], "setBackgroundResource", background);
    }

    PendingIntent launchPitchPipe = PendingIntent.getActivity(context, 0, new Intent(context, PitchPerfectActivity.class), 0);
    rv.setOnClickPendingIntent(R.id.launchIcon, launchPitchPipe);

    appWidgetManager.updateAppWidget(new ComponentName(context, PitchPipeAppWidget.class), rv);
    super.onUpdate(context, appWidgetManager, appWidgetIds);
  }

  private PendingIntent getIntentForNote(Note n, int index) {
    Intent i = new Intent(PitchPerfectApplication.getAppContext(), PitchPerfectService.class);
    i.putExtra("noteName", n.getFriendlyName());
    switch (n.getAccidental()) {
      case Flat:
        i.putExtra("accidental", "b");
        break;
      case Sharp:
        i.putExtra("accidental", "#");
        break;
    }
    i.putExtra("octave", n.getOctave());
    //i.putExtra("play", !n.getIsPlaying());
    return PendingIntent.getService(PitchPerfectApplication.getAppContext(), n.hashCode(), i, 0);
  }

  public Bitmap buildUpdate(CharSequence string) {
    TextView view = new TextView(PitchPerfectApplication.getAppContext());
    view.setText(string);
    view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
    view.setTextColor(Color.WHITE);
    view.measure(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
    view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    view.buildDrawingCache(true);
    Bitmap bmp = view.getDrawingCache(true).copy(Bitmap.Config.ARGB_4444, false);
    view.destroyDrawingCache();
    return bmp;
  }

}
