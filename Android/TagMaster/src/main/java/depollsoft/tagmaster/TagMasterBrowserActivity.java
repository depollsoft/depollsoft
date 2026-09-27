package depollsoft.tagmaster;

import android.content.Intent;
import android.net.Uri;
import android.view.MenuItem;
import android.webkit.WebView;
import depollsoft.lib.activity.BrowserActivity;

public class TagMasterBrowserActivity extends BrowserActivity {

  public TagMasterBrowserActivity() {
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      Intent intent = new Intent(this, MeActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      this.startActivity(intent);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected boolean shouldOverrideUrlLoading(WebView view, String url) {
    try {
      Uri uri = Uri.parse(url);
      if (UrlHandlerActivity.canHandleUri(uri)) {
        Intent i = new Intent(Intent.ACTION_VIEW, uri, this, UrlHandlerActivity.class);
        this.startActivity(i);
        return true;
      }
    }
    catch (Exception e) {
    }
    return super.shouldOverrideUrlLoading(view, url);
  }
}
