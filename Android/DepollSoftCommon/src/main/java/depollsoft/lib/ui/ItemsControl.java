package depollsoft.lib.ui;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.LongSparseArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Adapter;
import android.widget.LinearLayout;


public class ItemsControl extends LinearLayout {
  private class Observer extends DataSetObserver {
    @Override
    public void onChanged() {
      super.onChanged();
      ItemsControl.this.refresh();
    }

    @Override
    public void onInvalidated() {
      super.onInvalidated();
      ItemsControl.this.refresh();
    }
  }

  private LongSparseArray<View> idToView;

  private Observer observer;

  private Adapter adapter;

  public ItemsControl(Context context) {
    super(context);
    this.init();
  }

  public ItemsControl(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.init();
  }

  public Adapter getAdapter() {
    return this.adapter;
  }

  protected void init() {
    this.idToView = new LongSparseArray<View>();
    this.observer = new Observer();
    this.setOrientation(LinearLayout.VERTICAL);
  }

  private void refresh() {
    if (this.getAdapter() == null)
      return;
    if (!this.getAdapter().hasStableIds()) {
      this.removeAllViews();
      this.idToView.clear();
    }
    int count = this.getAdapter().getCount();
    LongSparseArray<View> newViews = new LongSparseArray<View>();
    for (int x = 0; x < count; x++) {
      long itemId = this.getAdapter().getItemId(x);
      View view = this.idToView.get(itemId);
      try {
        if (view == null)
          view = this.getAdapter().getView(x, null, this);
        int index = this.indexOfChild(view);
        if (index == x)
          continue;
        else if (index > 0)
          this.removeViewAt(index);
      } finally {
        newViews.put(itemId, view);
      }
      this.addView(view, x);
    }
    this.removeViews(count, this.getChildCount() - count);
    this.idToView = newViews;
  }

  public void setAdapter(Adapter value) {
    if (this.getAdapter() != null) {
      this.getAdapter().unregisterDataSetObserver(this.observer);
    }
    this.adapter = value;
    if (this.getAdapter() != null) {
      this.getAdapter().registerDataSetObserver(this.observer);
      this.idToView.clear();
      this.refresh();
    }
  }
}
