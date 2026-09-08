package depollsoft.tagmaster;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import depollsoft.lib.activity.BrowserActivity;

/** Routes supported links without leaving an empty activity on the back stack. */
public class UrlHandlerActivity extends AppCompatActivity {

  private static boolean isWebUri(Uri uri) {
    return uri != null && uri.isHierarchical() && uri.getHost() != null
        && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
  }

  private static boolean isTagPath(Uri uri) {
    if (!isWebUri(uri)) return false;
    String host = uri.getHost();
    if ("tags.depoll.com".equalsIgnoreCase(host)) return "/tag.php".equals(uri.getPath());
    return ("barbershoptags.com".equalsIgnoreCase(host) || "www.barbershoptags.com".equalsIgnoreCase(host))
        && "/dbpage.php".equals(uri.getPath())
        && "view".equals(uri.getQueryParameter("pg"))
        && "tags".equals(uri.getQueryParameter("dbase"));
  }

  private static Integer tagId(Uri uri) {
    if (!isTagPath(uri)) return null;
    String value = uri.getQueryParameter("id");
    if (value == null || !value.matches("[0-9]+")) return null;
    try {
      int id = Integer.parseInt(value);
      return id > 0 ? id : null;
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  /** Safe for arbitrary URLs passed by the shared browser, including absent query parameters. */
  public static boolean canHandleUri(Uri uri) {
    return tagId(uri) != null;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    try {
      Uri uri = getIntent() == null ? null : getIntent().getData();
      if (!isWebUri(uri)) return;
      Integer id = tagId(uri);
      Intent target;
      if (id != null) {
        target = new Intent(this, TagDetailActivity.class);
        target.putExtra(TagDetailActivity.TAG_ID_EXTRA, id.intValue());
      } else {
        // A malformed tag ID is not a tag. Other web pages retain the browser fallback.
        if (isTagPath(uri)) return;
        target = new Intent(this, TagMasterBrowserActivity.class);
        target.putExtra(BrowserActivity.URL_EXTRA, uri.toString());
      }
      startActivity(target);
    } finally {
      finish();
    }
  }
}
