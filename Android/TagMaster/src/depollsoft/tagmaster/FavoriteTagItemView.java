package depollsoft.tagmaster;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.FrameLayout;

import com.bindroid.converters.BoolConverter;
import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.BoundUi;
import com.bindroid.ui.UiBinder;
import com.bindroid.utils.Action;
import com.bindroid.utils.ObjectUtilities;

import depollsoft.tagmaster.barbershop.Tag;

public class FavoriteTagItemView extends FrameLayout implements BoundUi<Integer> {

  private TrackableField<Integer> tagId = new TrackableField<Integer>();

  private TrackableField<Tag> tag = new TrackableField<Tag>();
  private TagItemView regularView;

  public FavoriteTagItemView(Context context) {
    super(context);
    this.init();
  }

  public FavoriteTagItemView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.init();
  }

  public FavoriteTagItemView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    this.init();
  }

  public void bind(Integer dataSource) {
    if (ObjectUtilities.equals(dataSource, this.getTagId()))
      return;
    this.setTagId(dataSource);
    this.setTag(null);
    Tag.loadTagById(dataSource).continueWith(new Action<Tag>() {

      public void invoke(final Tag parameter) {
        FavoriteTagItemView.this.setTag(parameter);
      }
    }, new Action<Exception>() {
      public void invoke(Exception parameter) {
        Log.e("depollsoft.tagmaster", "Failed to load tag", parameter);
      }
    });
  }

  @Override
  public Tag getTag() {
    return this.tag.get();
  }

  public Integer getTagId() {
    return this.tagId.get();
  }

  private void init() {
    View.inflate(this.getContext(), R.layout.favoritetagitemview, this);
    this.setLongClickable(true);
    this.setOnLongClickListener(new OnLongClickListener() {
      public boolean onLongClick(View v) {
        FavoriteTagItemView.this.showContextMenu();
        return true;
      }
    });

    this.regularView = (TagItemView) this.findViewById(R.id.tagItemView);
    this.regularView.setHideFavoritesMarker(true);

    UiBinder.bind(this, R.id.loadingBar, "Visibility", "Tag", BoolConverter.get(true));
    UiBinder.bind(this, R.id.tagItemView, "Visibility", "Tag", BoolConverter.get());
  }

  @Override
  protected void onCreateContextMenu(ContextMenu menu) {
    MenuInflater mi = new MenuInflater(this.getContext());
    mi.inflate(R.menu.favoritetagcontextmenu, menu);

    menu.findItem(R.id.removeFavoriteMenuItem).setOnMenuItemClickListener(
        new OnMenuItemClickListener() {
          public boolean onMenuItemClick(MenuItem item) {
            FavoritesModel.removeFavorite(FavoriteTagItemView.this.getTagId());
            return true;
          }
        });

    menu.findItem(R.id.moveDownMenuItem).setOnMenuItemClickListener(new OnMenuItemClickListener() {
      public boolean onMenuItemClick(MenuItem item) {
        FavoritesModel.moveDown(FavoriteTagItemView.this.getTagId());
        return true;
      }
    });

    menu.findItem(R.id.moveDownMenuItem).setEnabled(FavoritesModel.canMoveDown(this.getTagId()));

    menu.findItem(R.id.moveUpMenuItem).setEnabled(FavoritesModel.canMoveUp(this.getTagId()));

    menu.findItem(R.id.moveUpMenuItem).setOnMenuItemClickListener(new OnMenuItemClickListener() {
      public boolean onMenuItemClick(MenuItem item) {
        FavoritesModel.moveUp(FavoriteTagItemView.this.getTagId());
        return true;
      }
    });
    super.onCreateContextMenu(menu);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
  }

  public void setTag(Tag value) {
    this.tag.set(value);
    this.regularView.bind(value);
  }

  public void setTagId(Integer value) {
    this.tagId.set(value);
  }
}
