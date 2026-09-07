package depollsoft.tagmaster;

import java.util.Locale;

import android.app.ActivityGroup;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import depollsoft.lib.activity.BrowserActivity;

@SuppressWarnings("deprecation")
public class UrlHandlerActivity extends ActivityGroup {

  /**
   * Recognizes tag links with a positive, representable ID. Malformed links
   * stay in the browser rather than launching an invalid detail request.
   */
  public static boolean canHandleUri(Uri uri) {
    if (uri == null) {
      return false;
    }
    String host = uri.getHost();
    if (host == null || !host.toLowerCase(Locale.US).endsWith("barbershoptags.com")) {
      return false;
    }
    if (!"/dbpage.php".equals(uri.getPath())) {
      return false;
    }
    try {
      return "view".equals(uri.getQueryParameter("pg"))
          && "tags".equals(uri.getQueryParameter("dbase"))
          && Integer.parseInt(uri.getQueryParameter("id")) > 0;
    } catch (UnsupportedOperationException | NumberFormatException e) {
      // Opaque URIs cannot be queried for parameters.
      return false;
    }
  }

  public UrlHandlerActivity() {
  }

  public UrlHandlerActivity(boolean singleActivityMode) {
    super(singleActivityMode);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Uri uri = this.getIntent().getData();
    String value;
    try {
      if (UrlHandlerActivity.canHandleUri(uri) && (value = uri.getQueryParameter("id")) != null) {
        Intent tagDetails = new Intent(this, TagDetailActivity.class);
        tagDetails.putExtra(TagDetailActivity.TAG_ID_EXTRA, Integer.parseInt(value));
        this.startActivity(tagDetails);
        return;
      }
    } catch (Exception e) {
    }

    if (uri == null) {
      // Nothing to open: land on the desk rather than crash out of the app.
      this.startActivity(new Intent(this, MeActivity.class));
      return;
    }

    Intent browser = new Intent(this, TagMasterBrowserActivity.class);
    browser.putExtra(BrowserActivity.URL_EXTRA, uri.toString());
    this.startActivity(browser);
  }
}
