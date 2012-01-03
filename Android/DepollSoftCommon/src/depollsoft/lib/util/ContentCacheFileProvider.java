package depollsoft.lib.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

public class ContentCacheFileProvider extends ContentProvider {
  private ContentCache cache;

  public ContentCacheFileProvider() {
  }

  @Override
  public void attachInfo(Context context, ProviderInfo info) {
    // TODO Auto-generated method stub
    super.attachInfo(context, info);
  }

  @Override
  public int delete(Uri arg0, String arg1, String[] arg2) {
    return 0;
  }

  private ContentCache getCache() {
    if (this.cache == null)
      this.cache = new ContentCache(this.getContext());
    return this.cache;
  }

  @Override
  public String getType(Uri uri) {
    return null;
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    return null;
  }

  @Override
  public boolean onCreate() {
    return false;
  }

  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode)
      throws FileNotFoundException {
    List<String> pathSegments = uri.getPathSegments();
    File f = this
        .getCache()
        .loadContentPrivate(Uri.decode(pathSegments.get(1)),
            pathSegments.get(0), false).waitFor();
    ParcelFileDescriptor parcel = ParcelFileDescriptor.open(f,
        ParcelFileDescriptor.MODE_READ_WRITE);
    return parcel;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    return null;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection,
      String[] selectionArgs) {
    return 0;
  }

}
