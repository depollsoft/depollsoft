package depollsoft.lib.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.*;
import android.util.Base64;
import android.webkit.MimeTypeMap;

public class ContentCacheFileProvider extends ContentProvider {
  private static final String[] COLUMNS = {
          OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE };
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
    // ContentProvider has already checked granted permissions
    String type = getFileType(getFileDisplayName(uri));
    if (type != null) {
      return type;
    }
    final File file = getFile(uri);
    type = getFileType(file.getName());
    if (type != null) {
      return type;
    }

    return "application/octet-stream";
  }

  private String getFileType(String fileName) {
    final int lastDot = fileName.lastIndexOf('.');
    if (lastDot >= 0) {
      final String extension = fileName.substring(lastDot + 1);
      final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
      if (mime != null) {
        return mime;
      }
    }
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

  private String getFileName(Uri uri) {
    List<String> pathSegments = uri.getPathSegments();
    return new String(android.util.Base64.decode(pathSegments.get(1), Base64.URL_SAFE));
  }

  private String getFileDisplayName(Uri uri) {
    List<String> pathSegments = uri.getPathSegments();
    if (pathSegments.size() > 2) {
      return pathSegments.get(2);
    }
    return getContext().getPackageName();
  }

  private File getFile(Uri uri) {
    List<String> pathSegments = uri.getPathSegments();
    File f = this
            .getCache()
            .loadContentPublic(
                    getFileName(uri),
                    pathSegments.get(1), false).waitFor();
    return f;
  }

  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode)
      throws FileNotFoundException {
    File f = getFile(uri);
    ParcelFileDescriptor parcel = ParcelFileDescriptor.open(f,
        ParcelFileDescriptor.MODE_READ_ONLY);
    return parcel;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    // ContentProvider has already checked granted permissions
    final File file = getFile(uri);
    if (file == null) {
      return null;
    }
    if (projection == null) {
      projection = COLUMNS;
    }
    String[] cols = new String[projection.length];
    Object[] values = new Object[projection.length];
    int i = 0;
    for (String col : projection) {
      if (OpenableColumns.DISPLAY_NAME.equals(col)) {
        cols[i] = OpenableColumns.DISPLAY_NAME;
        values[i++] = getFileDisplayName(uri);
      } else if (OpenableColumns.SIZE.equals(col)) {
        cols[i] = OpenableColumns.SIZE;
        values[i++] = file.length();
      }
    }
    cols = copyOf(cols, i);
    values = copyOf(values, i);
    final MatrixCursor cursor = new MatrixCursor(cols, 1);
    cursor.addRow(values);
    return cursor;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection,
      String[] selectionArgs) {
    return 0;
  }

  private static String[] copyOf(String[] original, int newLength) {
    final String[] result = new String[newLength];
    System.arraycopy(original, 0, result, 0, newLength);
    return result;
  }
  private static Object[] copyOf(Object[] original, int newLength) {
    final Object[] result = new Object[newLength];
    System.arraycopy(original, 0, result, 0, newLength);
    return result;
  }

}
