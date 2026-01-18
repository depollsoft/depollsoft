package depollsoft.lib.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Stack;
import java.util.concurrent.Callable;

import android.content.Context;
import android.util.Log;

import bolts.Task;
import bolts.TaskCompletionSource;

public class ContentCache {
  private File privateDir;
  private File publicDir;

  public ContentCache(Context context) {
    Context context1 = context;
    this.privateDir = context1.getDir("depollsoft_lib_private",
            Context.MODE_PRIVATE);
    this.publicDir = context1.getDir("depollsoft_lib_public_1",
            Context.MODE_PRIVATE);
  }

  private String canonicalizeFileName(String url, String extension) {
    return url.replaceAll("\\.|/|:", "_d_")
            + (extension != null ? "." + extension : "");
  }

  public void clearCache() {
    Stack<File> files = new Stack<File>();
    files.add(this.privateDir);
    files.add(this.publicDir);
    while (!files.isEmpty()) {
      File cur = files.pop();
      if (cur.isDirectory()) {
        if (cur.listFiles().length == 0)
          cur.delete();
        else {
          files.push(cur);
          for (File f : cur.listFiles())
            files.push(f);
          continue;
        }
      }
      cur.delete();
    }
  }

  public void deletePrivateContent(String url, String extension) {
    File f = new File(this.privateDir,
            this.canonicalizeFileName(url, extension));
    if (f.exists())
      f.delete();
  }

  public void deletePublicContent(String url, String extension) {
    File f = new File(this.publicDir, this.canonicalizeFileName(url, extension));
    if (f.exists())
      f.delete();
  }

  public long getCacheSize() {
    Stack<File> files = new Stack<File>();
    long totalSize = 0;
    files.add(this.privateDir);
    files.add(this.publicDir);
    while (!files.isEmpty()) {
      File cur = files.pop();
      if (cur.isDirectory()) {
        for (File f : cur.listFiles())
          files.push(f);
        continue;
      }
      totalSize += cur.length();
    }
    return totalSize;
  }

  private Task<File> loadContent(final String url, String extension,
                                 final File baseDir, boolean forceRefresh) {
    final String fileName = this.canonicalizeFileName(url, extension);
    final File filePath = new File(baseDir, fileName);
    if (!forceRefresh && filePath.exists()) {
      return Task.forResult(filePath);
    }
    return Task.callInBackground(new Callable<File>() {
      @Override
      public File call() throws Exception {
        InputStream is = null;
        FileOutputStream fos = null;
        try {
          URL connectUrl = new URL(url);
          is = connectUrl.openStream();
          fos = new FileOutputStream(filePath);
          byte[] buffer = new byte[1024];
          int numRead = 0;
          while ((numRead = is.read(buffer)) > 0) {
            fos.write(buffer, 0, numRead);
          }
        } finally {
          try {
            if (is != null)
              is.close();
            if (fos != null)
              fos.close();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        return filePath;
      }
    });
  }

  public Task<File> loadContentPrivate(String url, String extension,
                                       boolean forceRefresh) {
    return this.loadContent(url, extension, this.privateDir, forceRefresh);
  }

  public Task<File> loadContentPublic(String url, String extension,
                                      boolean forceRefresh) {
    return this.loadContent(url, extension, this.publicDir, forceRefresh);
  }
}
