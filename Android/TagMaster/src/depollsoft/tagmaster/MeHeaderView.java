package depollsoft.tagmaster;

import java.util.Random;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bindroid.BindingMode;
import com.bindroid.converters.BoolConverter;
import com.bindroid.ui.UiBinder;
import com.bindroid.utils.Action;
import com.bindroid.utils.Function;
import com.bindroid.utils.Property;
import com.bindroid.utils.ReflectedProperty;

import bolts.Continuation;
import bolts.Task;
import depollsoft.tagmaster.barbershop.Tag;
import depollsoft.tagmaster.barbershop.TagQueryResult;

public class MeHeaderView extends LinearLayout {

  public MeHeaderView(Context context) {
    super(context);
    this.init();
  }

  public MeHeaderView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.init();
  }

  private void init() {
    View.inflate(this.getContext(), R.layout.meviewheader, this);

    if (!this.isInEditMode()) {
      View randomTagButton = this.findViewById(R.id.randomTagButton);
      randomTagButton.setOnClickListener(new OnClickListener() {

        public void onClick(View v) {
          final ProgressDialog progress = new ProgressDialog(MeHeaderView.this.getContext());
          progress.setMessage("Loading...");
          progress.setIndeterminate(true);
          progress.show();
          Tag.query(null, 0, 0, null, SettingsModel.getRandomLearningTracksFilter(),
                  SettingsModel.getRandomSheetMusicFilter(), null, null,
                  SettingsModel.getMinimumRandomTagRating(), SettingsModel.getMinimumRandomDownloads(),
                  false, "id").continueWith(new Continuation<TagQueryResult, Void>() {
            @Override
            public Void then(Task<TagQueryResult> task) throws Exception {
              if (task.isFaulted()) {
                MeHeaderView.this.post(new Runnable() {
                  public void run() {
                    progress.dismiss();
                    Toast.makeText(MeHeaderView.this.getContext(), "Could not load a random tag.",
                            Toast.LENGTH_SHORT).show();
                  }
                });
              } else {
                if (task.getResult().getAvailable() == 0) {
                  MeHeaderView.this.post(new Runnable() {
                    public void run() {
                      progress.dismiss();
                      Toast
                              .makeText(
                                      MeHeaderView.this.getContext(),
                                      "No tags that match your filters could be found.  Please adjust your filters using the Settings menu.",
                                      Toast.LENGTH_SHORT).show();
                    }
                  });
                  return null;
                }
                Random r = new Random();
                int chosenNumber = r.nextInt(task.getResult().getAvailable());
                Tag.query(null, 1, chosenNumber, null, SettingsModel.getRandomLearningTracksFilter(),
                        SettingsModel.getRandomSheetMusicFilter(), null, null,
                        SettingsModel.getMinimumRandomTagRating(),
                        SettingsModel.getMinimumRandomDownloads(), false, "id").continueWith(new Continuation<TagQueryResult, Void>() {
                  @Override
                  public Void then(final Task<TagQueryResult> task) throws Exception {
                    if (task.isFaulted()) {
                      MeHeaderView.this.post(new Runnable() {
                        public void run() {
                          progress.dismiss();
                          Toast.makeText(MeHeaderView.this.getContext(),
                                  "Could not load a random tag.", Toast.LENGTH_SHORT).show();
                        }
                      });
                    } else {
                      MeHeaderView.this.post(new Runnable() {
                        public void run() {
                          Intent i = new Intent(MeHeaderView.this.getContext(),
                                  TagDetailActivity.class);
                          i.putExtra(TagDetailActivity.TAG_ID_EXTRA, task.getResult().getTags().get(0)
                                  .getId());
                          MeHeaderView.this.getContext().startActivity(i);
                          progress.dismiss();
                        }
                      });
                    }
                    return null;
                  }
                });
              }
              return null;
            }
          });
        }
      });

      View browseButton = this.findViewById(R.id.browseButton);
      browseButton.setOnClickListener(new OnClickListener() {

        public void onClick(View v) {
          Intent i = new Intent(MeHeaderView.this.getContext(), TagBrowserActivity.class);
          MeHeaderView.this.getContext().startActivity(i);
        }

      });

      View teachableButton = this.findViewById(R.id.teachableButton);
      teachableButton.setOnClickListener(new OnClickListener() {

        public void onClick(View v) {
          Intent i = new Intent(MeHeaderView.this.getContext(), TeachableTagsActivity.class);
          MeHeaderView.this.getContext().startActivity(i);
        }

      });
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (!this.isInEditMode()) {
      UiBinder.bind(new ReflectedProperty(this.findViewById(R.id.teachableButton), "Visibility"),
              new Property<Boolean>(new Function<Boolean>() {
                public Boolean evaluate() {
                  return TeachableTagsModel.getTeachableTagIds().size() > 0;
                }
              }, null, Boolean.class), BindingMode.ONE_WAY, BoolConverter.get());
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
  }
}
