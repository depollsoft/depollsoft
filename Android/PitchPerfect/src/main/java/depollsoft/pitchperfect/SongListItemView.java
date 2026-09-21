package depollsoft.pitchperfect;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.bindroid.BindingMode;
import com.bindroid.converters.BoolConverter;
import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.BoundUi;
import com.bindroid.ui.UiBinder;
import com.bindroid.utils.ReflectedProperty;

import depollsoft.pitchperfect.converters.KeyNameConverter;
import depollsoft.pitchperfect.lib.PitchedSong;

public class SongListItemView extends LinearLayout implements
    BoundUi<PitchedSong> {
  private TrackableField<PitchedSong> song = new TrackableField<PitchedSong>();

  public SongListItemView(Context context) {
    super(context);
    this.init();
  }

  public SongListItemView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.init();
  }

  @Override
  public void bind(PitchedSong dataSource) {
    this.setSong(dataSource);
  }

  public PitchedSong getSong() {
    return this.song.get();
  }

  private void init() {
    View.inflate(this.getContext(), R.layout.songlistitemview, this);
    this.setEditing(false);
    this.setBackgroundDrawable(this.getContext().getDrawable(R.drawable.row_lit));
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    UiBinder.bind(this, R.id.songTitleTextView, "Text", "Song.Name");

    UiBinder.bind(this, R.id.songKeyTextView, "Text", "Song.Key",
        new KeyNameConverter());

    UiBinder.bind(this, new ReflectedProperty(this, "Pressed"),
        "Song.IsPlaying", BindingMode.ONE_WAY, BoolConverter.get());

    View editButton = this.findViewById(R.id.editButton);
    editButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent i = new Intent(SongListItemView.this.getContext(),
            AddSongActivity.class);
        i.putExtra(AddSongActivity.ID_EXTRA, SongListItemView.this.getSong()
            .getId());
        i.putExtra(AddSongActivity.LIST_EXTRA, SongsModel.get().getCurrentListId());
        SongListItemView.this.getContext().startActivity(i);
      }
    });
    editButton.setOnLongClickListener(new OnLongClickListener() {
      @Override
      public boolean onLongClick(View arg0) {
        SongListItemView.this.showContextMenu();
        return true;
      }
    });
  }

  @Override
  protected void onCreateContextMenu(ContextMenu menu) {
    MenuInflater mi = new MenuInflater(SongListItemView.this.getContext());
    mi.inflate(R.menu.editcontextmenu, menu);
    menu.findItem(R.id.editMenuItem).setOnMenuItemClickListener(
        new OnMenuItemClickListener() {
          @Override
          public boolean onMenuItemClick(MenuItem item) {
            Intent i = new Intent(SongListItemView.this.getContext(),
                AddSongActivity.class);
            i.putExtra(AddSongActivity.ID_EXTRA, SongListItemView.this
                .getSong().getId());
            i.putExtra(AddSongActivity.LIST_EXTRA, SongsModel.get()
                .getCurrentListId());
            SongListItemView.this.getContext().startActivity(i);
            return true;
          }
        });
    menu.findItem(R.id.deleteMenuItem).setOnMenuItemClickListener(
        new OnMenuItemClickListener() {
          @Override
          public boolean onMenuItemClick(MenuItem item) {
            SongsModel.get().getCurrentList().removeSong(SongListItemView.this.getSong());
            return true;
          }
        });
    menu.findItem(R.id.sortMenuItem).setOnMenuItemClickListener(
        new OnMenuItemClickListener() {
          @Override
          public boolean onMenuItemClick(MenuItem item) {
            SongsModel.get().getCurrentList().sortSongs();
            return true;
          }
        });
    menu.findItem(R.id.moveDownMenuItem).setVisible(
        SongsModel.get().getCurrentList().canMoveDown(this.getSong()));
    menu.findItem(R.id.moveDownMenuItem).setOnMenuItemClickListener(
        new OnMenuItemClickListener() {
          @Override
          public boolean onMenuItemClick(MenuItem item) {
            SongsModel.get().getCurrentList().moveDown(SongListItemView.this.getSong());
            return true;
          }
        });
    menu.findItem(R.id.moveUpMenuItem).setVisible(
        SongsModel.get().getCurrentList().canMoveUp(this.getSong()));
    menu.findItem(R.id.moveUpMenuItem).setOnMenuItemClickListener(
        new OnMenuItemClickListener() {
          @Override
          public boolean onMenuItemClick(MenuItem item) {
            SongsModel.get().getCurrentList().moveUp(SongListItemView.this.getSong());
            return true;
          }
        });
    super.onCreateContextMenu(menu);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (this.getSong() != null) {
      switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        if (SettingsModel.getToggleNotes())
          if (this.getSong().getIsPlaying())
            this.getSong().stop();
          else
            this.play();
        else
          this.play();
        this.setPressed(this.getSong().getIsPlaying());
        return true;
      case MotionEvent.ACTION_MOVE:
        if (SettingsModel.getToggleNotes()
            && event.getEventTime() - event.getDownTime() > 100)
          this.getSong().stop();
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_OUTSIDE:
      case MotionEvent.ACTION_CANCEL:
        if (!SettingsModel.getToggleNotes())
          this.getSong().stop();
        this.setPressed(this.getSong().getIsPlaying());
        return true;
      }
    }
    return super.onTouchEvent(event);
  }

  private void play() {
    for (PitchedSong song : SongsModel.get().getCurrentList().getSongs())
      song.stop();
    this.getSong().play();
  }

  public void setEditing(boolean editing) {
    View editButton = this.findViewById(R.id.editButton);
    View dragHandle = this.findViewById(R.id.dragHandle);
    int visibility = editing ? View.VISIBLE : View.GONE;
    editButton.setVisibility(visibility);
    dragHandle.setVisibility(visibility);
    dragHandle.setImportantForAccessibility(
        editing ? View.IMPORTANT_FOR_ACCESSIBILITY_YES : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
  }

  public void setDragHandleTouchListener(final Runnable onStartDrag) {
    View handle = this.findViewById(R.id.dragHandle);
    if (handle == null) {
      return;
    }
    handle.setOnTouchListener((view, event) -> {
      if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
        onStartDrag.run();
        return true;
      }
      return false;
    });
  }

  public void setSong(PitchedSong value) {
    this.song.set(value);
    this.song.updateTrackers();
  }

}
