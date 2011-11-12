package depollsoft.tagmaster;

import depollsoft.lib.binding.TrackableField;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.RatingBar;

public class RatingsPopup extends Dialog
{
   private TrackableField<Integer> rating = new TrackableField<Integer>();

   public RatingsPopup(Context context)
   {
      super(context);
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
   }

   public Integer getRating()
   {
      return this.rating.getValue();
   }

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.ratingview);
      this.setCancelable(true);
      this.setTitle("Select a rating");
      OnTouchListener clickHandler = new OnTouchListener()
      {
         public boolean onTouch(View v, MotionEvent event)
         {
            if (event.getAction() == MotionEvent.ACTION_UP)
            {
               RatingsPopup.this.setRating((int) ((RatingBar) v).getRating());
               RatingsPopup.this.dismiss();
               return true;
            }
            return true;
         }
      };
      RatingBar rb;
      rb = (RatingBar) this.findViewById(R.id.ratingBar1);
      rb.setOnTouchListener(clickHandler);
      rb = (RatingBar) this.findViewById(R.id.ratingBar2);
      rb.setOnTouchListener(clickHandler);
      rb = (RatingBar) this.findViewById(R.id.ratingBar3);
      rb.setOnTouchListener(clickHandler);
      rb = (RatingBar) this.findViewById(R.id.ratingBar4);
      rb.setOnTouchListener(clickHandler);
      rb = (RatingBar) this.findViewById(R.id.ratingBar5);
      rb.setOnTouchListener(clickHandler);
   }

   public void setRating(Integer value)
   {
      this.rating.setValue(value);
   }
}
