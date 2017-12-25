package depollsoft.pitchperfect;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CropFillImageView extends AppCompatImageView {

  public CropFillImageView(Context context) {
    super(context);
    init();
  }

  public CropFillImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public CropFillImageView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    recomputeImgMatrix();
  }

  @Override
  protected boolean setFrame(int l, int t, int r, int b) {
    recomputeImgMatrix();
    return super.setFrame(l, t, r, b);
  }

  private void init() {
    setScaleType(ScaleType.MATRIX);
  }

  private void recomputeImgMatrix() {

    final Drawable drawable = getDrawable();
    if (drawable == null) {
      return;
    }

    final Matrix matrix = getImageMatrix();

    float scale;
    final int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
    final int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();
    final int drawableWidth = drawable.getIntrinsicWidth();
    final int drawableHeight = drawable.getIntrinsicHeight();

    if (drawableWidth * viewHeight > drawableHeight * viewWidth) {
      scale = (float) viewHeight / (float) drawableHeight;
    } else {
      scale = (float) viewWidth / (float) drawableWidth;
    }

    matrix.setScale(scale, scale);
    setImageMatrix(matrix);
  }
}