package depollsoft.pitchperfect;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.bindroid.BindingMode;
import com.bindroid.converters.AdapterConverter;
import com.bindroid.trackable.TrackableCollection;
import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.EditTextTextProperty;
import com.bindroid.ui.UiBinder;

import depollsoft.lib.compat.ui.ActionBars;
import depollsoft.lib.compat.ui.MenuItems;
import depollsoft.pitchperfect.lib.Key;
import depollsoft.pitchperfect.lib.PitchedSong;

public class AddSongActivity extends AppCompatActivity {
    public static final String ID_EXTRA = "depollsoft.pitchperfect.AddSong.id";
    private boolean editing;
    private PitchedSong toEdit;
    private TrackableField<PitchedSong> song = new TrackableField<PitchedSong>();

    private TrackableField<TrackableCollection<Key>> allKeys = new TrackableField<TrackableCollection<Key>>();

    public AddSongActivity() {
        this.setAllKeys(new TrackableCollection<Key>());
        this.getAllKeys().addAll(Key.getMajorKeys());
        this.getAllKeys().addAll(Key.getMinorKeys());
    }

    public TrackableCollection<Key> getAllKeys() {
        return this.allKeys.get();
    }

    public PitchedSong getSong() {
        return this.song.get();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setTitle("Edit Song");

        if (!ActionBars.hasActionBar(this)) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        this.setContentView(R.layout.addsongview);

        String id = this.getIntent().getStringExtra(AddSongActivity.ID_EXTRA);
        if (id != null) {
            PitchedSong toFind = new PitchedSong();
            toFind.setId(id);
            int index = SongsModel.get().getDefaultSongList().getSongs().indexOf(toFind);
            if (index >= 0) {
                this.toEdit = SongsModel.get().getDefaultSongList().getSongs().get(index);
                this.setSong(new PitchedSong());
                this.getSong().setName(this.toEdit.getName());
                this.getSong().setKey(this.toEdit.getKey());
                this.editing = true;
            }
        }
        if (this.getSong() == null) {
            this.setSong(new PitchedSong());
            this.getSong().setKey(Key.getMajorKeys().get(Key.getMajorKeys().size() / 2));
        }

        UiBinder.bind(this,
                new EditTextTextProperty((EditText) this.findViewById(R.id.songTitleEditText)),
                "Song.Name", BindingMode.TWO_WAY);

        UiBinder.bind(this, R.id.songKeySpinner, "Adapter", "AllKeys", new AdapterConverter(
                SongKeySignatureSelectedItemView.class, true, false, SongKeySignatureListItemView.class));

        int keyIndex = this.getAllKeys().indexOf(this.getSong().getKey());

        this.findViewById(R.id.saveSongButton).setOnClickListener(v -> okClicked());

        final Spinner spinner = (Spinner) this.findViewById(R.id.songKeySpinner);
        spinner.setSelection(keyIndex);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                AddSongActivity.this.getSong().setKey((Key) spinner.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private void okClicked() {
        String name = AddSongActivity.this.getSong().getName();
        if (name == null || name.trim().isEmpty()) {
            EditText titleField = (EditText) AddSongActivity.this.findViewById(R.id.songTitleEditText);
            titleField.setError(AddSongActivity.this.getString(R.string.SongTitleRequired));
            titleField.requestFocus();
            return;
        }
        if (AddSongActivity.this.editing) {
            AddSongActivity.this.toEdit.setName(AddSongActivity.this.getSong().getName());
            AddSongActivity.this.toEdit.setKey(AddSongActivity.this.getSong().getKey());
            SongsModel.get().getDefaultSongList().notifyOfChange();
        } else {
            SongsModel.get().getDefaultSongList().addSong(AddSongActivity.this.getSong());
        }
        AddSongActivity.this.setResult(1);
        PitchPerfectActivity.handlingResult = true;
        AddSongActivity.this.finish();
    }

    private void cancelClicked() {
        AddSongActivity.this.setResult(Activity.RESULT_CANCELED);
        PitchPerfectActivity.handlingResult = true;
        AddSongActivity.this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        super.onPrepareOptionsMenu(menu);
        MenuInflater mi = new MenuInflater(this);
        mi.inflate(R.menu.songeditmenu, menu);

        menu.findItem(R.id.removeSongMenuItem).setOnMenuItemClickListener(
                item -> {
                    if (AddSongActivity.this.editing) {
                        SongsModel.get().getDefaultSongList().removeSong(AddSongActivity.this.toEdit);
                    }
                    AddSongActivity.this.setResult(1);
                    PitchPerfectActivity.handlingResult = true;
                    AddSongActivity.this.finish();
                    return true;
                });

        menu.findItem(R.id.okMenuItem).setOnMenuItemClickListener(menuItem -> {
            okClicked();
            return true;
        });
        menu.findItem(R.id.cancelMenuItem).setOnMenuItemClickListener(menuItem -> {
            cancelClicked();
            return true;
        });

        return true;
    }

    public void setAllKeys(TrackableCollection<Key> value) {
        this.allKeys.set(value);
    }

    public void setSong(PitchedSong value) {
        this.song.set(value);
    }

}
