package depollsoft.lib.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class AppLog {
  private static final long MAX_BYTES = 256 * 1024;
  private static final Object LOCK = new Object();
  private static File file;

  private AppLog() {}

  public static void initialize(Context context) {
    synchronized (LOCK) {
      file = new File(context.getCacheDir(), "app.log");
    }
    Thread.UncaughtExceptionHandler previous =
        Thread.getDefaultUncaughtExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
      AppLog.error("Crash", "Uncaught exception on " + thread.getName(), error);
      if (previous != null) {
        previous.uncaughtException(thread, error);
      }
    });
    info("App", "Application started");
  }

  public static void info(String tag, String message) {
    Log.i(tag, message);
    write("INFO", tag, message);
  }

  public static void warning(String tag, String message) {
    Log.w(tag, message);
    write("WARN", tag, message);
  }

  public static void error(String tag, String message, Throwable error) {
    Log.e(tag, message, error);
    write("ERROR", tag, message + "\n" + Log.getStackTraceString(error));
  }

  public static String contents() {
    synchronized (LOCK) {
      if (file == null || !file.exists()) {
        return "No logs captured.";
      }
      try {
        return new String(readBytes(file), StandardCharsets.UTF_8);
      } catch (IOException error) {
        return "Unable to read logs: " + error.getMessage();
      }
    }
  }

  private static void write(String level, String tag, String message) {
    synchronized (LOCK) {
      if (file == null) {
        return;
      }
      String timestamp = new SimpleDateFormat(
          "yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(new Date());
      String line = timestamp + " " + level + "/" + tag + ": " + message + "\n";
      try {
        try (FileOutputStream output = new FileOutputStream(file, true)) {
          output.write(line.getBytes(StandardCharsets.UTF_8));
        }
        trimIfNeeded();
      } catch (IOException error) {
        Log.e("AppLog", "Unable to persist log", error);
      }
    }
  }

  private static void trimIfNeeded() throws IOException {
    if (file.length() <= MAX_BYTES) {
      return;
    }
    byte[] bytes = readBytes(file);
    int start = Math.max(0, bytes.length - (int) (MAX_BYTES / 2));
    try (FileOutputStream output = new FileOutputStream(file, false)) {
      output.write(bytes, start, bytes.length - start);
    }
  }

  private static byte[] readBytes(File source) throws IOException {
    try (FileInputStream input = new FileInputStream(source);
         ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];
      int count;
      while ((count = input.read(buffer)) != -1) {
        output.write(buffer, 0, count);
      }
      return output.toByteArray();
    }
  }
}
