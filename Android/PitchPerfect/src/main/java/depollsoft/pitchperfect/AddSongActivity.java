package depollsoft.pitchperfect;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.bindroid.BindingMode;
import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.EditTextTextProperty;
import com.bindroid.ui.UiBinder;
import com.google.android.material.textfield.TextInputLayout;

import depollsoft.lib.compat.ui.ActionBars;
import depollsoft.pitchperfect.lib.Key;
import depollsoft.pitchperfect.lib.PitchedSong;

public class AddSongActivity extends AppCompatActivity {
    public static final String ID_EXTRA = "depollsoft.pitchperfect.AddSong.id";
    private boolean editing;
    private PitchedSong toEdit;
    private TrackableField<PitchedSong> song = new TrackableField<PitchedSong>();
    private KeyDialView keyDial;
    private TextInputLayout titleLayout;

    public PitchedSong getSong() {
        return this.song.get();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        this.setTitle(this.editing ? R.string.EditSong : R.string.AddSong);

        EditText titleField = this.findViewById(R.id.songTitleEditText);
        UiBinder.bind(this, new EditTextTextProperty(titleField), "Song.Name", BindingMode.TWO_WAY);
        titleLayout = this.findViewById(R.id.songTitleLayout);
        titleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (titleLayout.getError() != null && s.toString().trim().length() > 0) {
                    titleLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        keyDial = this.findViewById(R.id.songKeyDial);
        keyDial.select(this.getSong().getKey());
        keyDial.setOnKeyChange(key -> this.getSong().setKey(key));

        this.findViewById(R.id.saveSongButton).setOnClickListener(v -> okClicked());
    }

    @Override
    protected void onPause() {
        keyDial.stopPreview();
        super.onPause();
    }

    private void okClicked() {
        String name = this.getSong().getName();
        if (name == null || name.trim().isEmpty()) {
            titleLayout.setError(this.getString(R.string.SongTitleRequired));
            titleLayout.requestFocus();
            View titleField = this.findViewById(R.id.songTitleEditText);
            titleField.requestFocus();
            titleField.performHapticFeedback(HapticFeedbackConstants.REJECT);
            return;
        }
        this.getSong().setName(name.trim());
        if (this.editing) {
            this.toEdit.setName(this.getSong().getName());
            this.toEdit.setKey(this.getSong().getKey());
            SongsModel.get().getDefaultSongList().notifyOfChange();
        } else {
            SongsModel.get().getDefaultSongList().addSong(this.getSong());
        }
        View feedbackView = this.findViewById(R.id.saveSongButton);
        int feedback = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? HapticFeedbackConstants.CONFIRM
                : HapticFeedbackConstants.CONTEXT_CLICK;
        feedbackView.performHapticFeedback(feedback);
        this.setResult(1);
        PitchPerfectActivity.handlingResult = true;
        this.finish();
    }

    private void cancelClicked() {
        this.setResult(Activity.RESULT_CANCELED);
        PitchPerfectActivity.handlingResult = true;
        this.finish();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        super.onPrepareOptionsMenu(menu);
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.songeditmenu, menu);

        menu.findItem(R.id.removeSongMenuItem).setOnMenuItemClickListener(
                item -> {
                    if (this.editing) {
                        SongsModel.get().getDefaultSongList().removeSong(this.toEdit);
                    }
                    this.setResult(1);
                    PitchPerfectActivity.handlingResult = true;
                    this.finish();
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

    public void setSong(PitchedSong value) {
        this.song.set(value);
    }

}
