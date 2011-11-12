package depollsoft.tagmaster;

import depollsoft.lib.binding.ui.BoolConverter;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.ui.ThreadSwitchContext;
import depollsoft.lib.util.ReflectedProperty;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class LoadMoreItemsView extends LinearLayout
{

   public LoadMoreItemsView(Context context)
   {
      super(context);
      this.init();
   }

   public LoadMoreItemsView(Context context, AttributeSet attrs)
   {
      super(context, attrs);
      this.init();
   }

   private void init()
   {
      LayoutInflater inflater = (LayoutInflater) this.getContext()
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      inflater.inflate(R.layout.loadmoreitemview, this, true);

      Button b = (Button) this.findViewById(R.id.loadMoreItemsButton);
      b.setOnClickListener(new OnClickListener()
      {

         public void onClick(View v)
         {
            QueryModel qm = (QueryModel) (new ReflectedProperty(
                  LoadMoreItemsView.this, "Context.Model").getValue());
            qm.fetchResults(new ThreadSwitchContext(LoadMoreItemsView.this));
         }
      });
   }

   @Override
   protected void onAttachedToWindow()
   {
      super.onAttachedToWindow();
      UiBinder.bind(this, R.id.loadMoreItemsLayout, "Visibility",
            "Context.Model.HasMoreResults", BoolConverter.get());
      UiBinder.bind(this, R.id.loadMoreItemsButton, "Enabled",
            "Context.Model.IsLoading", BoolConverter.get(true));
   }

   @Override
   protected void onDetachedFromWindow()
   {
      super.onDetachedFromWindow();
      UiBinder.unbind(this);
   }
}
