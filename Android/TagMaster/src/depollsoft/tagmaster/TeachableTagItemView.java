package depollsoft.tagmaster;

import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.ui.BoolConverter;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.ui.BoundUi;
import depollsoft.lib.util.Action;
import depollsoft.lib.util.ObjectUtilities;
import depollsoft.tagmaster.barbershop.Tag;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.FrameLayout;

public class TeachableTagItemView extends FrameLayout implements
      BoundUi<Integer>
{

   private TrackableField<Integer> tagId = new TrackableField<Integer>();

   private TrackableField<Tag> tag = new TrackableField<Tag>();
   private TagItemView regularView;
   private Thread uiThread;

   public TeachableTagItemView(Context context)
   {
      super(context);
      this.init();
   }

   public TeachableTagItemView(Context context, AttributeSet attrs)
   {
      super(context, attrs);
      this.init();
   }

   public TeachableTagItemView(Context context, AttributeSet attrs, int defStyle)
   {
      super(context, attrs, defStyle);
      this.init();
   }

   public void bind(Integer dataSource)
   {
      if (ObjectUtilities.equals(dataSource, this.getTagId()))
         return;
      this.setTagId(dataSource);
      this.setTag(null);
      Tag.loadTagById(dataSource).continueWith(new Action<Tag>()
      {

         public void invoke(final Tag parameter)
         {
            if (Thread.currentThread() != TeachableTagItemView.this.uiThread)
               TeachableTagItemView.this.post(new Runnable()
               {
                  public void run()
                  {
                     TeachableTagItemView.this.setTag(parameter);
                  }
               });
            else
               TeachableTagItemView.this.setTag(parameter);
         }
      }, new Action<Exception>()
      {

         public void invoke(Exception parameter)
         {
         }
      });
   }

   @Override
   public Tag getTag()
   {
      return this.tag.getValue();
   }

   public Integer getTagId()
   {
      return this.tagId.getValue();
   }

   private void init()
   {
      View.inflate(this.getContext(), R.layout.favoritetagitemview, this);
      this.uiThread = Thread.currentThread();
      this.setLongClickable(true);
      this.setOnLongClickListener(new OnLongClickListener()
      {
         public boolean onLongClick(View v)
         {
            TeachableTagItemView.this.showContextMenu();
            return true;
         }
      });

      this.regularView = (TagItemView) this.findViewById(R.id.tagItemView);
      //this.regularView.setHideFavoritesMarker(true);
   }

   @Override
   protected void onAttachedToWindow()
   {
      super.onAttachedToWindow();

      UiBinder.bind(this, R.id.loadingBar, "Visibility", "Tag",
            BoolConverter.get(true));
      UiBinder.bind(this, R.id.tagItemView, "Visibility", "Tag",
            BoolConverter.get());
   }

   @Override
   protected void onCreateContextMenu(ContextMenu menu)
   {
      MenuInflater mi = new MenuInflater(this.getContext());
      mi.inflate(R.menu.teachabletagcontextmenu, menu);

      menu.findItem(R.id.removeTeachableTagMenuItem)
            .setOnMenuItemClickListener(new OnMenuItemClickListener()
            {
               public boolean onMenuItemClick(MenuItem item)
               {
                  TeachableTagsModel
                        .removeTeachableTag(TeachableTagItemView.this
                              .getTagId());
                  return true;
               }
            });

      menu.findItem(R.id.moveDownMenuItem).setOnMenuItemClickListener(
            new OnMenuItemClickListener()
            {
               public boolean onMenuItemClick(MenuItem item)
               {
                  TeachableTagsModel.moveDown(TeachableTagItemView.this
                        .getTagId());
                  return true;
               }
            });

      menu.findItem(R.id.moveDownMenuItem).setEnabled(
            TeachableTagsModel.canMoveDown(this.getTagId()));

      menu.findItem(R.id.moveUpMenuItem).setEnabled(
            TeachableTagsModel.canMoveUp(this.getTagId()));

      menu.findItem(R.id.moveUpMenuItem).setOnMenuItemClickListener(
            new OnMenuItemClickListener()
            {
               public boolean onMenuItemClick(MenuItem item)
               {
                  TeachableTagsModel.moveUp(TeachableTagItemView.this
                        .getTagId());
                  return true;
               }
            });
      super.onCreateContextMenu(menu);
   }

   @Override
   protected void onDetachedFromWindow()
   {
      super.onDetachedFromWindow();
      UiBinder.unbind(this);
   }

   public void setTag(Tag value)
   {
      this.tag.setValue(value);
      this.regularView.bind(value);
   }

   public void setTagId(Integer value)
   {
      this.tagId.setValue(value);
   }
}
