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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import com.bindroid.BindingMode;
import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.EditTextTextProperty;
import com.bindroid.ui.UiBinder;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;

import depollsoft.lib.compat.ui.ActionBars;
import depollsoft.pitchperfect.lib.Key;
import depollsoft.pitchperfect.lib.PitchedSong;

public class AddSongActivity extends AppCompatActivity {
    public static final String ID_EXTRA = "depollsoft.pitchperfect.AddSong.id";
    public static final String LIST_EXTRA = "depollsoft.pitchperfect.AddSong.listId";
    private boolean editing;
    private SongList targetList;
    private PitchedSong toEdit;
    private TrackableField<PitchedSong> song = new TrackableField<PitchedSong>();
    private SongKeyListAdapter keyList;
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

        // Every song operation targets the list the Songs tab is showing, not the default one.
        this.targetList = SongsModel.get().listOrCurrent(
                this.getIntent().getStringExtra(AddSongActivity.LIST_EXTRA));

        String id = this.getIntent().getStringExtra(AddSongActivity.ID_EXTRA);
        if (id != null) {
            PitchedSong toFind = new PitchedSong();
            toFind.setId(id);
            int index = this.targetList.getSongs().indexOf(toFind);
            if (index >= 0) {
                this.toEdit = this.targetList.getSongs().get(index);
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

        RecyclerView list = this.findViewById(R.id.songKeyList);
        keyList = new SongKeyListAdapter(this.getSong().getKey(), key -> {
            this.getSong().setKey(key);
            return null;
        });
        LinearLayoutManager layout = new LinearLayoutManager(this);
        list.setLayoutManager(layout);
        list.setAdapter(keyList);
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider_hairline));
        list.addItemDecoration(divider);
        list.post(() -> centerSelection(list, layout));

        MaterialButtonToggleGroup modeGroup = this.findViewById(R.id.keyModeGroup);
        modeGroup.check(keyList.isMinor() ? R.id.keyModeMinor : R.id.keyModeMajor);
        modeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            keyList.setMinor(checkedId == R.id.keyModeMinor);
            list.post(() -> centerSelection(list, layout));
        });

        this.findViewById(R.id.saveSongButton).setOnClickListener(v -> okClicked());
    }

    private void centerSelection(RecyclerView list, LinearLayoutManager layout) {
        int index = keyList.getSelectedIndex();
        if (index < 0) {
            return;
        }
        int rowHeight = getResources().getDisplayMetrics().density > 0
                ? (int) (64 * getResources().getDisplayMetrics().density)
                : 0;
        layout.scrollToPositionWithOffset(index, Math.max(0, list.getHeight() / 2 - rowHeight / 2));
    }

    private void okClicked() {
        String name = this.getSong().getName();
        if (name == null || name.trim().isEmpty()) {
            titleLayout.setError(this.getString(R.string.SongTitleRequired));
            View titleField = this.findViewById(R.id.songTitleEditText);
            titleField.requestFocus();
            titleField.performHapticFeedback(HapticFeedbackConstants.REJECT);
            return;
        }
        this.getSong().setName(name.trim());
        if (this.editing) {
            this.toEdit.setName(this.getSong().getName());
            this.toEdit.setKey(this.getSong().getKey());
            this.targetList.notifyOfChange();
        } else {
            this.targetList.addSong(this.getSong());
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
                        this.targetList.removeSong(this.toEdit);
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
