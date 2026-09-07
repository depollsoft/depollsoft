package depollsoft.tagmaster;

import android.content.Intent;
import android.net.Uri;
import android.view.MenuItem;
import android.webkit.WebView;
import depollsoft.lib.activity.BrowserActivity;
import depollsoft.lib.compat.ui.ActionBars;

public class TagMasterBrowserActivity extends BrowserActivity {

  public TagMasterBrowserActivity() {
  }

  @Override
  protected void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // The shared browser is a platform Activity without the desk action bar.
    // It therefore owns the top inset as well as the sides, navigation and IME.
    android.view.View content = findViewById(android.R.id.content);
    androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
      androidx.core.graphics.Insets safe = insets.getInsets(
          androidx.core.view.WindowInsetsCompat.Type.systemBars()
          | androidx.core.view.WindowInsetsCompat.Type.displayCutout()
          | androidx.core.view.WindowInsetsCompat.Type.ime());
      view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
      return androidx.core.view.WindowInsetsCompat.CONSUMED;
    });
    androidx.core.view.ViewCompat.requestApplyInsets(content);
    if (getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == ActionBars.HOME_MENU_ITEM_ID) {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected boolean shouldOverrideUrlLoading(WebView view, String url) {
    try {
      Uri uri = Uri.parse(url);
      if (UrlHandlerActivity.canHandleUri(uri)) {
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        this.startActivity(i);
        return true;
      }
    }
    catch (Exception e) {
    }
    return super.shouldOverrideUrlLoading(view, url);
  }
}
