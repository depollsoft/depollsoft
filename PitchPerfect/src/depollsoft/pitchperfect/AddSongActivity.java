package depollsoft.pitchperfect;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.BindingMode;
import depollsoft.lib.binding.ObservableCollection;
import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.ui.AdapterConverter;
import depollsoft.lib.binding.ui.EditTextTextProperty;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.pitchperfect.lib.Key;
import depollsoft.pitchperfect.lib.PitchedSong;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.Spinner;

public class AddSongActivity extends Activity
{
   public static final String ID_EXTRA = "depollsoft.pitchperfect.AddSong.id";
   private boolean editing;
   private PitchedSong toEdit;
   private TrackableField<PitchedSong> song = new TrackableField<PitchedSong>();

   private TrackableField<ObservableCollection<Key>> allKeys = new TrackableField<ObservableCollection<Key>>();

   public AddSongActivity()
   {
      this.setAllKeys(new ObservableCollection<Key>());
      this.getAllKeys().addAll(Key.getMajorKeys());
      this.getAllKeys().addAll(Key.getMinorKeys());
   }

   public ObservableCollection<Key> getAllKeys()
   {
      return this.allKeys.getValue();
   }

   public PitchedSong getSong()
   {
      return this.song.getValue();
   }

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.addsongview);

      String id = this.getIntent().getStringExtra(AddSongActivity.ID_EXTRA);
      if (id != null)
      {
         PitchedSong toFind = new PitchedSong();
         toFind.setId(id);
         int index = SongsModel.get().getSongs().indexOf(toFind);
         if (index >= 0)
         {
            this.toEdit = SongsModel.get().getSongs().get(index);
            this.setSong(new PitchedSong());
            this.getSong().setName(this.toEdit.getName());
            this.getSong().setKey(this.toEdit.getKey());
            this.editing = true;
         }
      }
      if (this.getSong() == null)
      {
         this.setSong(new PitchedSong());
         this.getSong().setKey(
               Key.getMajorKeys().get(Key.getMajorKeys().size() / 2));
      }

      UiBinder.bind(
            this,
            new EditTextTextProperty((EditText) this
                  .findViewById(R.id.songTitleEditText)), "Song.Name",
            BindingMode.TwoWay);

      UiBinder.bind(this, R.id.songKeySpinner, "Adapter", "AllKeys",
            new AdapterConverter(SongKeySignatureSelectedItemView.class, true,
                  false, SongKeySignatureListItemView.class));

      int keyIndex = this.getAllKeys().indexOf(this.getSong().getKey());

      final Spinner spinner = (Spinner) this.findViewById(R.id.songKeySpinner);
      spinner.setSelection(keyIndex);
      spinner.setOnItemSelectedListener(new OnItemSelectedListener()
      {
         @Override
         public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
               long arg3)
         {
            AddSongActivity.this.getSong().setKey(
                  (Key) spinner.getSelectedItem());
         }

         @Override
         public void onNothingSelected(AdapterView<?> arg0)
         {
         }
      });

      View okButton = this.findViewById(R.id.okButton);
      okButton.setOnClickListener(new OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            if (AddSongActivity.this.editing)
            {
               AddSongActivity.this.toEdit.setName(AddSongActivity.this
                     .getSong().getName());
               AddSongActivity.this.toEdit.setKey(AddSongActivity.this
                     .getSong().getKey());
            }
            else
            {
               SongsModel.get().addSong(AddSongActivity.this.getSong());
            }
            AddSongActivity.this.setResult(Activity.RESULT_OK);
            AddSongActivity.this.finish();
         }
      });

      View cancelButton = this.findViewById(R.id.cancelButton);
      cancelButton.setOnClickListener(new OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            AddSongActivity.this.setResult(Activity.RESULT_CANCELED);
            AddSongActivity.this.finish();
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
      GoogleAnalyticsTracker.getInstance().trackPageView("AddSongActivity");
   }

   public void setAllKeys(ObservableCollection<Key> value)
   {
      this.allKeys.setValue(value);
   }

   public void setSong(PitchedSong value)
   {
      this.song.setValue(value);
   }

}
