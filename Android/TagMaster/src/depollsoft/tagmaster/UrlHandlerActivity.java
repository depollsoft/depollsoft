package depollsoft.tagmaster;

import java.net.URLEncoder;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.activity.BrowserActivity;
import android.app.ActivityGroup;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class UrlHandlerActivity extends ActivityGroup {

  public static boolean canHandleUri(Uri uri) {
    return uri.getHost().toLowerCase().endsWith("barbershoptags.com")
        && uri.getPath().equals("/dbpage.php")
        && (uri.getQueryParameter("pg").equals("view")
            && uri.getQueryParameter("dbase").equals("tags") && uri
            .getQueryParameter("id") != null);
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
    GoogleAnalyticsTracker.getInstance().trackPageView(
        "UrlHandlerActivity/" + URLEncoder.encode(uri.toString()));
    String value;
    try {
      if (uri.getQueryParameter("pg").equals("view")
          && uri.getQueryParameter("dbase").equals("tags")
          && (value = uri.getQueryParameter("id")) != null) {
        Intent tagDetails = new Intent(this, TagDetailActivity.class);
        tagDetails.putExtra(TagDetailActivity.TAG_ID_EXTRA,
            Integer.parseInt(value));
        this.setContentView(this.getLocalActivityManager()
            .startActivity("tagDetails", tagDetails).getDecorView());
        return;
      }
    }
    catch (Exception e) {
    }

    Intent browser = new Intent(this, TagMasterBrowserActivity.class);
    browser.putExtra(BrowserActivity.URL_EXTRA, uri.toString());
    this.setContentView(this.getLocalActivityManager()
        .startActivity("browse", browser).getDecorView());
  }
}
