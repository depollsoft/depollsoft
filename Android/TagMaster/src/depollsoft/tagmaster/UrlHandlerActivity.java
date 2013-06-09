package depollsoft.tagmaster;

import java.util.Locale;

import android.app.ActivityGroup;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.flurry.android.FlurryAgent;

import depollsoft.lib.activity.BrowserActivity;

@SuppressWarnings("deprecation")
public class UrlHandlerActivity extends ActivityGroup {

  public static boolean canHandleUri(Uri uri) {
    return uri.getHost().toLowerCase(Locale.US).endsWith("barbershoptags.com")
        && uri.getPath().equals("/dbpage.php")
        && (uri.getQueryParameter("pg").equals("view")
            && uri.getQueryParameter("dbase").equals("tags") && uri.getQueryParameter("id") != null);
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

    Intent browser = new Intent(this, TagMasterBrowserActivity.class);
    browser.putExtra(BrowserActivity.URL_EXTRA, uri.toString());
    this.startActivity(browser);
  }

  @Override
  protected void onStart() {
    super.onStart();
    FlurryAgent.onStartSession(this, "V5L1948BNDQCKZFPARJ9");
  }

  @Override
  protected void onStop() {
    super.onStop();
    FlurryAgent.onEndSession(this);
  }
}
