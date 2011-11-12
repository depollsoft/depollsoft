package depollsoft.lib.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class FontTextView extends TextView
{

   public FontTextView(Context context)
   {
      super(context);
   }

   public FontTextView(Context context, AttributeSet attrs)
   {
      super(context, attrs);
      this.init(attrs);
   }

   public FontTextView(Context context, AttributeSet attrs, int defStyle)
   {
      super(context, attrs, defStyle);
      this.init(attrs);
   }

   private void init(AttributeSet attrs)
   {
      if (!this.isInEditMode())
      {
         String assetPath = attrs.getAttributeValue("depollsoft",
               "fontAssetPath");
         if (assetPath != null)
         {
            Typeface typeface = Typeface.createFromAsset(this.getContext()
                  .getAssets(), assetPath);
            this.setTypeface(typeface);
         }
      }
   }

}
