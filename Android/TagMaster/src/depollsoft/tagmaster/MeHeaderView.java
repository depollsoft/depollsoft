package depollsoft.tagmaster;

import java.util.Random;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.Binding;
import depollsoft.lib.binding.BindingMode;
import depollsoft.lib.binding.ui.BoolConverter;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.util.Action;
import depollsoft.lib.util.Function;
import depollsoft.lib.util.Property;
import depollsoft.lib.util.ReflectedProperty;
import depollsoft.tagmaster.barbershop.Tag;
import depollsoft.tagmaster.barbershop.TagQueryResult;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

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
          final ProgressDialog progress = new ProgressDialog(MeHeaderView.this
              .getContext());
          GoogleAnalyticsTracker.getInstance().trackEvent("MediaViews",
              "RandomTag", null, 0);
          progress.setMessage("Loading...");
          progress.setIndeterminate(true);
          progress.show();
          Tag.query(null, 0, 0, null,
              SettingsModel.getRandomLearningTracksFilter(),
              SettingsModel.getRandomSheetMusicFilter(), null, null,
              SettingsModel.getMinimumRandomTagRating(),
              SettingsModel.getMinimumRandomDownloads(), false, "id")
              .continueWith(new Action<TagQueryResult>() {

                public void invoke(TagQueryResult parameter) {
                  if (parameter.getAvailable() == 0) {
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
                    return;
                  }
                  Random r = new Random();
                  int chosenNumber = r.nextInt(parameter.getAvailable());
                  Tag.query(null, 1, chosenNumber, null,
                      SettingsModel.getRandomLearningTracksFilter(),
                      SettingsModel.getRandomSheetMusicFilter(), null, null,
                      SettingsModel.getMinimumRandomTagRating(),
                      SettingsModel.getMinimumRandomDownloads(), false, "id")
                      .continueWith(new Action<TagQueryResult>() {
                        public void invoke(final TagQueryResult parameter) {
                          MeHeaderView.this.post(new Runnable() {
                            public void run() {
                              Intent i = new Intent(MeHeaderView.this
                                  .getContext(), TagDetailActivity.class);
                              i.putExtra(TagDetailActivity.TAG_ID_EXTRA,
                                  parameter.getTags().get(0).getId());
                              MeHeaderView.this.getContext().startActivity(i);
                              progress.dismiss();
                            }
                          });
                        }
                      }, new Action<Exception>() {
                        public void invoke(Exception parameter) {
                          MeHeaderView.this.post(new Runnable() {
                            public void run() {
                              progress.dismiss();
                              Toast.makeText(MeHeaderView.this.getContext(),
                                  "Could not load a random tag.",
                                  Toast.LENGTH_SHORT).show();
                            }
                          });
                        }
                      });
                }
              }, new Action<Exception>() {

                public void invoke(Exception parameter) {
                  MeHeaderView.this.post(new Runnable() {
                    public void run() {
                      progress.dismiss();
                      Toast.makeText(MeHeaderView.this.getContext(),
                          "Could not load a random tag.", Toast.LENGTH_SHORT)
                          .show();
                    }
                  });
                }
              });
        }
      });

      View searchButton = this.findViewById(R.id.searchButton);
      searchButton.setOnClickListener(new OnClickListener() {

        public void onClick(View v) {
          Intent i = new Intent(MeHeaderView.this.getContext(),
              TagSearchActivity.class);
          MeHeaderView.this.getContext().startActivity(i);
        }

      });

      View browseButton = this.findViewById(R.id.browseButton);
      browseButton.setOnClickListener(new OnClickListener() {

        public void onClick(View v) {
          Intent i = new Intent(MeHeaderView.this.getContext(),
              TagBrowser.class);
          MeHeaderView.this.getContext().startActivity(i);
        }

      });

      View teachableButton = this.findViewById(R.id.teachableButton);
      teachableButton.setOnClickListener(new OnClickListener() {

        public void onClick(View v) {
          Intent i = new Intent(MeHeaderView.this.getContext(),
              TeachableTagsActivity.class);
          MeHeaderView.this.getContext().startActivity(i);
        }

      });
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (!this.isInEditMode()) {
      UiBinder.registerBinding(
          this,
          new Binding(new ReflectedProperty(this
              .findViewById(R.id.teachableButton), "Visibility"),
              new Property<Boolean>(new Function<Boolean>() {

                public Boolean evaluate() {
                  return TeachableTagsModel.getTeachableTagIds().size() > 0;
                }
              }, null, Boolean.class), BindingMode.OneWay, BoolConverter.get())
              .bind(this));
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    UiBinder.unbind(this);
    super.onDetachedFromWindow();
  }
}
