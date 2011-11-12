package depollsoft.tagmaster;

import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import depollsoft.lib.activity.BrowserActivity;

public class TagMasterBrowserActivity extends BrowserActivity
{

   public TagMasterBrowserActivity()
   {
   }

   @Override
   protected boolean shouldOverrideUrlLoading(WebView view, String url)
   {
      try
      {
         Uri uri = Uri.parse(url);
         if (UrlHandlerActivity.canHandleUri(uri))
         {
            Intent i = new Intent(Intent.ACTION_VIEW, uri);
            this.startActivity(i);
            return true;
         }
      }
      catch (Exception e)
      {
      }
      return super.shouldOverrideUrlLoading(view, url);
   }

}
