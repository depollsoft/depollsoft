package depollsoft.pitchperfect;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

public class PitchPipeAppWidget extends AppWidgetProvider {

  public PitchPipeAppWidget() {
  }

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    for (int appWidgetId : appWidgetIds) {
    }
    super.onUpdate(context, appWidgetManager, appWidgetIds);
  }

}
