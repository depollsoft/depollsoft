package depollsoft.lib.ui;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

public class ScaledBitmapView extends View {
  private static ReferenceQueue<Bitmap> cacheQueue = new ReferenceQueue<Bitmap>();
  private static final SparseArray<SoftReference<Bitmap>> resourceCache = new SparseArray<SoftReference<Bitmap>>();

  private static void cleanupCache() {
    Reference<? extends Bitmap> cur;
    while ((cur = cacheQueue.poll()) != null) {
      Bitmap bmp = cur.get();
      if (bmp != null) {
        Log.d("Cache", "Cleaning up a bitmap.");
        bmp.recycle();
      }
    }
  }

  private static Bitmap getBitmapResource(int resId, Resources res) {
    cleanupCache();
    Bitmap result;
    SoftReference<Bitmap> ref = resourceCache.get(resId);
    if (ref != null && !ref.isEnqueued()) {
      result = ref.get();
      if (result != null) {
        return result;
      }
    }
    result = BitmapFactory.decodeResource(res, resId);
    resourceCache.put(resId, new SoftReference<Bitmap>(result, cacheQueue));
    return result;
  }

  private int resId;
  private Bitmap bitmap;
  private Bitmap scaledBitmap;
  private final Matrix identityMatrix = new Matrix();
  private final Paint simplePaint = new Paint();

  public ScaledBitmapView(Context context) {
    super(context, null);
    initialize(null);
  }

  public ScaledBitmapView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize(attrs);
  }

  public ScaledBitmapView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    initialize(attrs);
  }

  private void initialize(AttributeSet attrs) {
    if (attrs != null) {
      resId = attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "src",
          0);
    }
  }

  public void setSourceBitmap(Bitmap bitmap) {
    this.bitmap = bitmap;
    if (scaledBitmap != null) {
      scaledBitmap.recycle();
    }
    scaledBitmap = null;
    resId = 0;
    this.requestLayout();
  }

  public void setSourceResource(int resourceId) {
    this.resId = resourceId;
    bitmap = null;
    if (scaledBitmap != null) {
      scaledBitmap.recycle();
    }
    scaledBitmap = null;
    this.requestLayout();
  }

  @SuppressLint("DrawAllocation")
  @Override
  protected void onLayout(boolean changed, final int left, final int top, final int right,
      final int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    if (!changed && scaledBitmap != null) {
      return;
    }
    if (scaledBitmap != null) {
      scaledBitmap.recycle();
      scaledBitmap = null;
    }
    if (bitmap == null) {
      bitmap = getBitmapResource(resId, getResources());
    }
    int width = right - left;
    int height = bottom - top;
    if (width == 0 || height == 0) {
      return;
    }
    double horizontalScale = 1.0 * width / bitmap.getWidth();
    double verticalScale = 1.0 * height / bitmap.getHeight();
    int horizontalHeight = (int) (horizontalScale * bitmap.getHeight());
    int verticalWidth = (int) (verticalScale * bitmap.getWidth());
    Bitmap interim;
    if (horizontalHeight > height) {
      // Scaling to fit horizontally will more than fill the height.
      interim = Bitmap.createScaledBitmap(bitmap, width, horizontalHeight, true);
    } else {
      // Scaling to fit vertically will more than fill the width.
      interim = Bitmap.createScaledBitmap(bitmap, verticalWidth, height, true);
    }
    scaledBitmap = Bitmap.createBitmap(interim, 0, 0, width, height);
    interim.recycle();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (scaledBitmap != null) {
      scaledBitmap.recycle();
      scaledBitmap = null;
    }
    cleanupCache();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (scaledBitmap != null) {
      canvas.drawBitmap(scaledBitmap, identityMatrix, simplePaint);
    }
  }
}
