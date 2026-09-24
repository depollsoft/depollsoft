package depollsoft.lib.ui;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.LruCache;


public class LoadingImageView extends androidx.appcompat.widget.AppCompatImageView {

  // Three workers and a short queue: a scroll burst enqueues many thumbnails, and the oldest pending
  // request is the one most likely to have scrolled away already, so drop it instead of growing
  // without bound. Stale requests that do run exit early on the version check.
  private static final ExecutorService IMAGE_EXECUTOR = createExecutor();
  private static final int MAX_PENDING_REQUESTS = 24;

  private static ExecutorService createExecutor() {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 3, 30, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(MAX_PENDING_REQUESTS),
        new ThreadPoolExecutor.DiscardOldestPolicy());
    executor.allowCoreThreadTimeOut(true);
    return executor;
  }
  private static final LruCache<String, Bitmap> IMAGE_CACHE =
      new LruCache<String, Bitmap>((int) Math.min(Integer.MAX_VALUE, Runtime.getRuntime().maxMemory() / 8)) {
        @Override protected int sizeOf(String key, Bitmap bitmap) {
          return bitmap.getByteCount();
        }
      };
  private static final int MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024;
  private String source;
  private volatile int requestVersion;

  public LoadingImageView(Context context) {
    super(context);
  }

  public LoadingImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public LoadingImageView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  private void fetchSource(final String currentSource, final int version) {
    int width = getWidth() - getPaddingLeft() - getPaddingRight();
    int height = getHeight() - getPaddingTop() - getPaddingBottom();
    if (width <= 0 && getLayoutParams() != null) width = getLayoutParams().width;
    if (height <= 0 && getLayoutParams() != null) height = getLayoutParams().height;
    final int targetWidth = width > 0 ? width : 320;
    final int targetHeight = height > 0 ? height : 320;
    final String cacheKey = currentSource + "@" + targetWidth + "x" + targetHeight;
    Bitmap cached = IMAGE_CACHE.get(cacheKey);
    if (cached != null) {
      setImageBitmap(cached);
      return;
    }
    IMAGE_EXECUTOR.execute(new Runnable() {
      @Override public void run() {
        if (version != requestVersion) return;
        try {
          Bitmap bitmap = IMAGE_CACHE.get(cacheKey);
          if (bitmap == null) {
            URLConnection connection = new URL(currentSource).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            byte[] data;
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
              byte[] buffer = new byte[8192];
              int count;
              while ((count = input.read(buffer)) != -1) {
                if (version != requestVersion || output.size() + count > MAX_DOWNLOAD_BYTES) return;
                output.write(buffer, 0, count);
              }
              data = output.toByteArray();
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, options);
            if (options.outWidth <= 0 || options.outHeight <= 0) return;
            options.inSampleSize = 1;
            while (options.outWidth / options.inSampleSize > targetWidth * 2L
                || options.outHeight / options.inSampleSize > targetHeight * 2L) {
              options.inSampleSize *= 2;
            }
            options.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            if (bitmap == null) return;
            IMAGE_CACHE.put(cacheKey, bitmap);
          }
          final Bitmap result = bitmap;
          post(new Runnable() {
            @Override public void run() {
              if (version == requestVersion && currentSource.equals(getSource())) {
                setImageBitmap(result);
              }
            }
          });
        } catch (Exception ignored) {
          // Missing, invalid, or offline thumbnails leave this recycled view empty.
        }
      }
    });
  }

  public String getSource() {
    return this.source;
  }

  public void setSource(String value) {
    this.source = value;
    int version = ++requestVersion;
    setImageDrawable(null);
    if (value != null && !value.isEmpty()) fetchSource(value, version);
  }
}
