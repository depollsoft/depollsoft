package depollsoft.tagmaster;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bindroid.converters.BoolConverter;
import com.bindroid.trackable.Trackable;
import com.bindroid.ui.UiBinder;
import com.flurry.android.FlurryAgent;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;

import depollsoft.lib.compat.ui.ActionBars;
import depollsoft.lib.ui.ChangelogViewer;

public class SettingsActivity extends Activity {
  private Spinner minDownloadSpinner;
  private Spinner minRatingSpinner;
  private Spinner sheetMusicSpinner;
  private Spinner learningTrackSpinner;

  private List<String> minDownloadChoices;
  private List<String> minRatingChoices;

  private boolean loggingIn;

  private Trackable loginTrackable = new Trackable();

  public SettingsActivity() {
  }

  public boolean getLoggedIn() {
    this.loginTrackable.track();
    return ParseUser.getCurrentUser() != null;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    ParseFacebookUtils.finishAuthentication(requestCode, resultCode, data);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.settingsview);

    this.minDownloadSpinner = (Spinner) this.findViewById(R.id.minimumDownloadSpinner);
    this.minRatingSpinner = (Spinner) this.findViewById(R.id.minimumRatingSpinner);
    this.sheetMusicSpinner = (Spinner) this.findViewById(R.id.sheetMusicSpinner);
    this.learningTrackSpinner = (Spinner) this.findViewById(R.id.learningTracksSpinner);

    this.minDownloadChoices = Arrays.asList(this.getResources().getStringArray(
        R.array.MinDownloadChoices));
    this.minRatingChoices = Arrays.asList(this.getResources().getStringArray(
        R.array.MinRatingChoices));
    Arrays.asList(this.getResources().getStringArray(R.array.SheetMusicChoices));
    Arrays.asList(this.getResources().getStringArray(R.array.LearningTracksChoices));

    this.refreshMinDownload();
    this.refreshMinRating();
    this.refreshSheetMusicChoice();
    this.refreshLearningTracksChoice();

    this.refreshCacheSize();

    this.findViewById(R.id.clearCacheButton).setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
        builder
            .setMessage(
                "Are you sure you want to clear your cache?  Cached sheet music and tracks will not be accessible until you are connected to the internet again.")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                SettingsModel.clearCache();
                SettingsActivity.this.refreshCacheSize();
                Toast.makeText(SettingsActivity.this, "Cache cleared.", Toast.LENGTH_SHORT).show();
              }
            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
              }
            }).show();
      }
    });

    this.minDownloadSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        String selected = (String) arg0.getSelectedItem();
        int amount = 0;
        if (!selected.equals("Any"))
          amount = Integer.parseInt(selected);
        SettingsModel.setMinimumRandomDownloads(amount);
      }

      public void onNothingSelected(AdapterView<?> arg0) {
      }
    });
    this.minRatingSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        String selected = (String) arg0.getSelectedItem();
        double amount = 0;
        if (!selected.equals("Any"))
          amount = Double.parseDouble(selected);
        SettingsModel.setMinimumRandomTagRating(amount);
      }

      public void onNothingSelected(AdapterView<?> arg0) {
      }
    });

    this.learningTrackSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        String selected = (String) arg0.getSelectedItem();
        Boolean result = null;
        if (selected.equals("Yes"))
          result = true;
        else if (selected.equals("No"))
          result = false;
        SettingsModel.setRandomLearningTracksFilter(result);
      }

      public void onNothingSelected(AdapterView<?> arg0) {
      }
    });
    this.sheetMusicSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        String selected = (String) arg0.getSelectedItem();
        Boolean result = null;
        if (selected.equals("Yes"))
          result = true;
        else if (selected.equals("No"))
          result = false;
        SettingsModel.setRandomSheetMusicFilter(result);
      }

      public void onNothingSelected(AdapterView<?> arg0) {
      }
    });
    this.findViewById(R.id.clearFavoritesButton).setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
        builder.setMessage("Are you sure you want to clear your favorite tags list?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                FavoritesModel.resetFavorites();
                Toast.makeText(SettingsActivity.this, "Favorite tags cleared.", Toast.LENGTH_SHORT)
                    .show();
              }
            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
              }
            }).show();
      }
    });
    this.findViewById(R.id.clearTeachableTags).setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
        builder.setMessage("Are you sure you want to clear your teachable tags list?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                TeachableTagsModel.resetTeachableTags();
                Toast
                    .makeText(SettingsActivity.this, "Teachable tags cleared.", Toast.LENGTH_SHORT)
                    .show();
              }
            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
              }
            }).show();
      }
    });

    UiBinder.bind(this, R.id.loginButton, "Visibility", "LoggedIn", BoolConverter.get(true));
    UiBinder.bind(this, R.id.logoutButton, "Visibility", "LoggedIn", BoolConverter.get());

    this.findViewById(R.id.loginButton).setOnClickListener(new OnClickListener() {
      public void onClick(final View v) {
        v.setEnabled(false);
        final ProgressDialog progress = new ProgressDialog(SettingsActivity.this);
        progress.setMessage("Logging in...");
        SettingsActivity.this.loggingIn = true;
        progress.show();
        ParseFacebookUtils.logIn(null, SettingsActivity.this, new LogInCallback() {
          @Override
          public void done(ParseUser user, ParseException err) {
            SettingsActivity.this.loggingIn = false;
            progress.dismiss();
            v.setEnabled(true);
            if (err != null) {
              Toast.makeText(SettingsActivity.this, "Facebook login failed.", Toast.LENGTH_SHORT)
                  .show();
              Log.d("Tag Master", "Failed to log in.", err);
              return;
            }

            if (user == null) {
              Log.d("Tag Master", "User cancelled login.");
              return;
            }
            SettingsActivity.this.loginTrackable.updateTrackers();
            if (user.isNew()) {
              FavoritesModel.storeToUser();
              TeachableTagsModel.storeToUser();
              user.saveEventually();
            } else {
              FavoritesModel.restoreFromUser();
              TeachableTagsModel.restoreFromUser();
            }
          }
        });
      }
    });

    this.findViewById(R.id.logoutButton).setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        AsyncTask<Void, Void, Void> logOutTask = new AsyncTask<Void, Void, Void>() {

          @Override
          protected Void doInBackground(Void... params) {
            ParseUser.logOut();
            // TODO:handle logout
            return null;
          }

          @Override
          protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            SettingsActivity.this.loginTrackable.updateTrackers();
          }
        };
        logOutTask.execute();
      }
    });

    this.findViewById(R.id.changelogButton).setOnClickListener(new OnClickListener() {

      public void onClick(View v) {
        ChangelogViewer viewer = new ChangelogViewer(SettingsActivity.this, SettingsActivity.this
            .getString(R.string.Changelog));
        viewer.setTitle("Tag Master Changelog");
        viewer.setIcon(SettingsActivity.this.getResources().getDrawable(R.drawable.icon));
        viewer.show();
      }
    });

    ActionBars.setCustomTitle(this, R.layout.titleview);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK && this.loggingIn) {
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == ActionBars.HOME_MENU_ITEM_ID) {
      Intent intent = new Intent(this, MeActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      this.startActivity(intent);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onStart() {
    super.onStart();
    FlurryAgent.onStartSession(this, "V5L1948BNDQCKZFPARJ9");
  }

  @Override
  protected void onStop() {
    super.onStop();
    FlurryAgent.onEndSession(this);
  }

  private void refreshCacheSize() {
    ((TextView) this.findViewById(R.id.cacheSizeDisplay)).setText(String.format("%1.2f MB",
        SettingsModel.getCacheSizeInMegabytes()));
  }

  private void refreshLearningTracksChoice() {
    int index;
    if (SettingsModel.getRandomLearningTracksFilter() == null)
      index = 0;
    else if (SettingsModel.getRandomLearningTracksFilter().equals(true))
      index = 1;
    else
      index = 2;
    this.learningTrackSpinner.setSelection(index);
  }

  private void refreshMinDownload() {
    int index = Math.max(0,
        this.minDownloadChoices.indexOf("" + SettingsModel.getMinimumRandomDownloads()));
    this.minDownloadSpinner.setSelection(index);
  }

  private void refreshMinRating() {
    int index = Math.max(0,
        this.minRatingChoices.indexOf("" + (int) SettingsModel.getMinimumRandomTagRating()));
    this.minRatingSpinner.setSelection(index);
  }

  private void refreshSheetMusicChoice() {
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
