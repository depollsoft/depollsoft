package depollsoft.pitchperfect;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.bindroid.trackable.TrackableCollection;
import com.flurry.android.FlurryAgent;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.RefreshCallback;

import depollsoft.lib.activity.RichApplication;
import depollsoft.lib.json.JsonSerializer;
import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Key;
import depollsoft.pitchperfect.lib.KeyType;
import depollsoft.pitchperfect.lib.Note;
import depollsoft.pitchperfect.lib.PitchedSong;

public class PitchPerfectApplication extends RichApplication {
  @SuppressWarnings("unused")
  private static final String DEBUG_SIGNATURE = "308201e53082014ea00302010202044f337962300d06092a864886f70d01010505003037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f6964204465627567301e170d3132303230393037343433345a170d3432303230313037343433345a3037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f696420446562756730819f300d06092a864886f70d010101050003818d0030818902818100b4d99aa8cff1bddc23c6313aebcfc93a869b8f918807e70e8b6ef03d722813b9090044ba2da31b53ae908393ee5fee0e2f6e582f9018bd6013dfef51bf073e049b42c8ba72387ca82687185603696c31c754669b0b3a84452190e87aac5800d7b07dce4475073f7a24cb0da8313d449d29803d3010296c588f3728033b3ddd1f0203010001300d06092a864886f70d0101050500038181002d0527ee1bb08ae465cc273b1b6fdafa4b1476d587712a8cdd47a7f64f3a790f2f7327c3c656da30af86a4c1febda7258a9226edb429365287c3b489dc74fe75160151543790903e3b5c0faac5bdb70192bbce9d41337426038d0061256b7e59abb9120291865ecc4adbc2b16428b0042120a937f5ef6744b2d4cba08e37b660";
  private static final String FACEBOOK_DEBUG = "292538514135026";
  private static final String FACEBOOK_PRODUCTION = "263872380333771";

  public PitchPerfectApplication() {
  }

  @Override
  public void onCreate() {
    super.onCreate();
    boolean isDebugSigned = false;

    Note.setPlayer(new Note.NotePlayer() {
      @Override
      public void play(Note n) {
        Note.DEFAULT_PLAYER.play(n);
        PitchPipeAppWidget.updateWidgets();
      }

      @Override
      public void stop(Note n) {
        Note.DEFAULT_PLAYER.stop(n);
        PitchPipeAppWidget.updateWidgets();
      }
    });

    FlurryAgent.setUseHttps(true);
    FlurryAgent.setVersionName(String.format("%s (%s)", getString(R.string.app_version),
            getString(R.string.app_store)) + (isDebugSigned ? " debug" : ""));

    JsonSerializer.registerAlias(Integer.class, "Integer");
    JsonSerializer.registerAlias(Integer.TYPE, "int");
    JsonSerializer.registerAlias(Key.class, "Key");
    JsonSerializer.registerAlias(KeyType.class, "KeyType");
    JsonSerializer.registerAlias(Accidental.class, "Accidental");
    JsonSerializer.registerAlias(Note.class, "Note");
    JsonSerializer.registerAlias(PitchedSong.class, "PitchedSong");
    JsonSerializer.registerAlias(String.class, "String");
    JsonSerializer.registerAlias(JsonSerializer.PRIMITIVE_CLASS, "Primitive");
    JsonSerializer.registerAlias(Boolean.class, "Boolean");
    JsonSerializer.registerAlias(Boolean.TYPE, "bool");
    JsonSerializer.registerAlias(Double.class, "Double");
    JsonSerializer.registerAlias(Double.TYPE, "double");
    JsonSerializer.registerAlias(TrackableCollection.class, "List");

    Parse.initialize(new Parse.Configuration.Builder(this)
            .applicationId("cXYwcCUUP2f78OBfMlXu7dk03f2JRMQYXpCnv7H9")
            .clientKey("Y9ZIP3kLs1Jbh9Mpr2s8tRw9tjdGt6GuseuRHNdE")
            .server("https://pitchperfect-api.depollsoft.xyz")
            .build());
    ParseFacebookUtils.initialize(this);
  }

  public static void startupRefreshFromParse() {
    if (ParseUser.getCurrentUser() != null) {
      FlurryAgent.setUserId(ParseUser.getCurrentUser().getUsername());
      SettingsModel.restoreUser();
      try {
        ParseUser.getCurrentUser().refreshInBackground(new RefreshCallback() {
          @Override
          public void done(ParseObject obj, ParseException e) {
            if (obj != null && e == null) {
              SettingsModel.restoreUser();
              SongsModel.get().refreshFromParse();
            }
          }
        });
      } catch (Exception e) {
        // It's ok -- it just means there's already a query going on.
      }
    }
  }

  @Override
  public void onTerminate() {
    super.onTerminate();
  }

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    MultiDex.install(this);
  }
}
