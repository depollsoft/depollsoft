package depollsoft.tagmaster;

import java.util.Arrays;
import java.util.List;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.ui.UiBinder;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;

public class SettingsActivity extends Activity
{
   private Spinner minDownloadSpinner;
   private Spinner minRatingSpinner;
   private Spinner sheetMusicSpinner;
   private Spinner learningTrackSpinner;

   private List<String> minDownloadChoices;
   private List<String> minRatingChoices;

   public SettingsActivity()
   {
   }

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.settingsview);

      this.minDownloadSpinner = (Spinner) this
            .findViewById(R.id.minimumDownloadSpinner);
      this.minRatingSpinner = (Spinner) this
            .findViewById(R.id.minimumRatingSpinner);
      this.sheetMusicSpinner = (Spinner) this
            .findViewById(R.id.sheetMusicSpinner);
      this.learningTrackSpinner = (Spinner) this
            .findViewById(R.id.learningTracksSpinner);

      this.minDownloadChoices = Arrays.asList(this.getResources()
            .getStringArray(R.array.MinDownloadChoices));
      this.minRatingChoices = Arrays.asList(this.getResources().getStringArray(
            R.array.MinRatingChoices));
      Arrays.asList(this.getResources().getStringArray(
            R.array.SheetMusicChoices));
      Arrays.asList(this.getResources().getStringArray(
            R.array.LearningTracksChoices));

      this.refreshMinDownload();
      this.refreshMinRating();
      this.refreshSheetMusicChoice();
      this.refreshLearningTracksChoice();

      this.refreshCacheSize();

      this.findViewById(R.id.clearCacheButton).setOnClickListener(
            new OnClickListener()
            {
               public void onClick(View v)
               {
                  AlertDialog.Builder builder = new AlertDialog.Builder(
                        SettingsActivity.this);
                  builder
                        .setMessage(
                              "Are you sure you want to clear your cache?  Cached sheet music and tracks will not be accessible until you are connected to the internet again.")
                        .setPositiveButton("Yes",
                              new DialogInterface.OnClickListener()
                              {
                                 public void onClick(DialogInterface dialog,
                                       int which)
                                 {
                                    SettingsModel.clearCache();
                                    SettingsActivity.this.refreshCacheSize();
                                    Toast.makeText(SettingsActivity.this,
                                          "Cache cleared.", Toast.LENGTH_SHORT)
                                          .show();
                                 }
                              })
                        .setNegativeButton("No",
                              new DialogInterface.OnClickListener()
                              {
                                 public void onClick(DialogInterface dialog,
                                       int which)
                                 {
                                 }
                              }).show();
               }
            });

      this.minDownloadSpinner
            .setOnItemSelectedListener(new OnItemSelectedListener()
            {
               public void onItemSelected(AdapterView<?> arg0, View arg1,
                     int arg2, long arg3)
               {
                  String selected = (String) arg0.getSelectedItem();
                  int amount = 0;
                  if (!selected.equals("Any"))
                     amount = Integer.parseInt(selected);
                  SettingsModel.setMinimumRandomDownloads(amount);
               }

               public void onNothingSelected(AdapterView<?> arg0)
               {
               }
            });
      this.minRatingSpinner
            .setOnItemSelectedListener(new OnItemSelectedListener()
            {
               public void onItemSelected(AdapterView<?> arg0, View arg1,
                     int arg2, long arg3)
               {
                  String selected = (String) arg0.getSelectedItem();
                  double amount = 0;
                  if (!selected.equals("Any"))
                     amount = Double.parseDouble(selected);
                  SettingsModel.setMinimumRandomTagRating(amount);
               }

               public void onNothingSelected(AdapterView<?> arg0)
               {
               }
            });

      this.learningTrackSpinner
            .setOnItemSelectedListener(new OnItemSelectedListener()
            {
               public void onItemSelected(AdapterView<?> arg0, View arg1,
                     int arg2, long arg3)
               {
                  String selected = (String) arg0.getSelectedItem();
                  Boolean result = null;
                  if (selected.equals("Yes"))
                     result = true;
                  else if (selected.equals("No"))
                     result = false;
                  SettingsModel.setRandomLearningTracksFilter(result);
               }

               public void onNothingSelected(AdapterView<?> arg0)
               {
               }
            });
      this.sheetMusicSpinner
            .setOnItemSelectedListener(new OnItemSelectedListener()
            {
               public void onItemSelected(AdapterView<?> arg0, View arg1,
                     int arg2, long arg3)
               {
                  String selected = (String) arg0.getSelectedItem();
                  Boolean result = null;
                  if (selected.equals("Yes"))
                     result = true;
                  else if (selected.equals("No"))
                     result = false;
                  SettingsModel.setRandomSheetMusicFilter(result);
               }

               public void onNothingSelected(AdapterView<?> arg0)
               {
               }
            });
      this.findViewById(R.id.clearFavoritesButton).setOnClickListener(
            new OnClickListener()
            {
               public void onClick(View v)
               {
                  AlertDialog.Builder builder = new AlertDialog.Builder(
                        SettingsActivity.this);
                  builder
                        .setMessage(
                              "Are you sure you want to clear your favorite tags list?")
                        .setPositiveButton("Yes",
                              new DialogInterface.OnClickListener()
                              {
                                 public void onClick(DialogInterface dialog,
                                       int which)
                                 {
                                    FavoritesModel.resetFavorites();
                                    Toast.makeText(SettingsActivity.this,
                                          "Favorite tags cleared.",
                                          Toast.LENGTH_SHORT).show();
                                 }
                              })
                        .setNegativeButton("No",
                              new DialogInterface.OnClickListener()
                              {
                                 public void onClick(DialogInterface dialog,
                                       int which)
                                 {
                                 }
                              }).show();
               }
            });
      this.findViewById(R.id.clearTeachableTags).setOnClickListener(
            new OnClickListener()
            {
               public void onClick(View v)
               {
                  AlertDialog.Builder builder = new AlertDialog.Builder(
                        SettingsActivity.this);
                  builder
                        .setMessage(
                              "Are you sure you want to clear your teachable tags list?")
                        .setPositiveButton("Yes",
                              new DialogInterface.OnClickListener()
                              {
                                 public void onClick(DialogInterface dialog,
                                       int which)
                                 {
                                    TeachableTagsModel.resetTeachableTags();
                                    Toast.makeText(SettingsActivity.this,
                                          "Teachable tags cleared.",
                                          Toast.LENGTH_SHORT).show();
                                 }
                              })
                        .setNegativeButton("No",
                              new DialogInterface.OnClickListener()
                              {
                                 public void onClick(DialogInterface dialog,
                                       int which)
                                 {
                                 }
                              }).show();
               }
            });
   }

   @Override
   protected void onDestroy()
   {
      UiBinder.unbind(this);
      super.onDestroy();
   }

   @Override
   protected void onResume()
   {
      super.onResume();
      GoogleAnalyticsTracker.getInstance().trackPageView("SettingsActivity");
   }

   private void refreshCacheSize()
   {
      ((TextView) this.findViewById(R.id.cacheSizeDisplay)).setText(String
            .format("%1.2f MB", SettingsModel.getCacheSizeInMegabytes()));
   }

   private void refreshLearningTracksChoice()
   {
      int index;
      if (SettingsModel.getRandomLearningTracksFilter() == null)
         index = 0;
      else if (SettingsModel.getRandomLearningTracksFilter().equals(true))
         index = 1;
      else
         index = 2;
      this.learningTrackSpinner.setSelection(index);
   }

   private void refreshMinDownload()
   {
      int index = Math.max(
            0,
            this.minDownloadChoices.indexOf(""
                  + SettingsModel.getMinimumRandomDownloads()));
      this.minDownloadSpinner.setSelection(index);
   }

   private void refreshMinRating()
   {
      int index = Math.max(
            0,
            this.minRatingChoices.indexOf(""
                  + (int) SettingsModel.getMinimumRandomTagRating()));
      this.minRatingSpinner.setSelection(index);
   }

   private void refreshSheetMusicChoice()
   {
      int index;
      if (SettingsModel.getRandomSheetMusicFilter() == null)
         index = 0;
      else if (SettingsModel.getRandomSheetMusicFilter().equals(true))
         index = 1;
      else
         index = 2;
      this.sheetMusicSpinner.setSelection(index);
   }
}
