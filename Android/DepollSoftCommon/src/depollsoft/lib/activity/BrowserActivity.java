package depollsoft.lib.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class BrowserActivity extends Activity {
  private WebView webView;
  public static final String URL_EXTRA = "BrowserUrl";
  public static final String HTML_DATA_EXTRA = "HtmlDataExtra";
  public static final String HIDE_UNTIL_FIRST_LOAD_EXTRA = "HideUntilFirstLoad";

  public BrowserActivity() {
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    boolean hideUntilFirstLoad = this.getIntent().getBooleanExtra(
        BrowserActivity.HIDE_UNTIL_FIRST_LOAD_EXTRA, false);
    final ProgressDialog progress = new ProgressDialog(this);
    progress.setMessage("Loading...");
    progress.setIndeterminate(true);

    this.webView = new WebView(this);
    if (hideUntilFirstLoad) {
      this.webView.setVisibility(View.INVISIBLE);
      this.webView.postDelayed(new Runnable() {
        public void run() {
          if (BrowserActivity.this.webView.getVisibility() == View.INVISIBLE)
            progress.show();
        }
      }, 100);
    }
    this.webView.getSettings().setBuiltInZoomControls(true);
    this.webView.getSettings().setUseWideViewPort(true);
    this.webView.setInitialScale(1);
    this.webView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
    this.webView.setWebViewClient(new WebViewClient() {
      @Override
      public void onPageFinished(WebView view, String url) {
        view.setVisibility(View.VISIBLE);
        progress.dismiss();
        super.onPageFinished(view, url);
      }

      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return BrowserActivity.this.shouldOverrideUrlLoading(view, url);
      }
    });
    this.setContentView(this.webView);

    String url = this.getIntent().getStringExtra(BrowserActivity.URL_EXTRA);
    String htmlData = this.getIntent().getStringExtra(
        BrowserActivity.HTML_DATA_EXTRA);

    if (htmlData == null)
      this.webView.loadUrl(url);
    else
      this.webView.loadData(Uri.encode(htmlData), "text/html", "utf-8");
  }

  protected boolean shouldOverrideUrlLoading(WebView view, String url) {
    return false;
  }
}
