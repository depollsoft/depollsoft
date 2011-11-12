package depollsoft.lib.ui;

import depollsoft.lib.binding.TrackableField;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class Hyperlink extends TextView
{

   private TrackableField<String> hyperlinkUri = new TrackableField<String>();

   private boolean isSetting;
   private boolean initialized;

   public Hyperlink(Context context)
   {
      super(context);
      this.init();
   }

   public Hyperlink(Context context, AttributeSet attrs)
   {
      super(context, attrs);
      this.init();
   }

   public Hyperlink(Context context, AttributeSet attrs, int defStyle)
   {
      super(context, attrs, defStyle);
      this.init();
   }

   private void ensureUnderlined()
   {
      if (this.initialized && !this.isSetting
            && (this.getHyperlinkUri() != null || this.getTag() != null)
            && this.getText() != null)
      {
         SpannableString ss = new SpannableString(this.getText());
         ss.setSpan(new UnderlineSpan(), 0, ss.length(), 0);
         this.isSetting = true;
         this.setText(ss);
         this.isSetting = false;
      }
   }

   public String getHyperlinkUri()
   {
      return this.hyperlinkUri.getValue();
   }

   private void init()
   {
      this.setOnClickListener(new OnClickListener()
      {

         public void onClick(View v)
         {
            if (Hyperlink.this.getHyperlinkUri() != null)
            {
               Intent i = new Intent(Intent.ACTION_VIEW);
               i.setData(Uri.parse(Hyperlink.this.getHyperlinkUri()));
               Hyperlink.this.getContext().startActivity(i);
            }
            else if (Hyperlink.this.getTag() != null)
            {
               Intent i = new Intent(Intent.ACTION_VIEW);
               i.setData(Uri.parse(Hyperlink.this.getTag().toString()));
               Hyperlink.this.getContext().startActivity(i);
            }
         }
      });
      this.setClickable(true);
      this.initialized = true;
      this.ensureUnderlined();
   }

   @Override
   protected void onTextChanged(CharSequence text, int start, int before,
         int after)
   {
      super.onTextChanged(text, start, before, after);
      this.ensureUnderlined();
   }

   public void setHyperlinkUri(String value)
   {
      this.hyperlinkUri.setValue(value);
      this.ensureUnderlined();
   }
}
