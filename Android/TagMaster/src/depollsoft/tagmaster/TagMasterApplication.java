package depollsoft.tagmaster;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.bindroid.trackable.TrackableCollection;
import com.crashlytics.android.Crashlytics;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.parse.Parse;
import com.parse.ParseFacebookUtils;

import depollsoft.lib.activity.RichApplication;
import depollsoft.lib.json.JsonSerializer;
import depollsoft.lib.util.DebugTools;
import io.fabric.sdk.android.Fabric;

public class TagMasterApplication extends RichApplication {

  private static final String DEBUG_SIGNATURE = "308201e53082014ea00302010202044f337962300d06092a864886f70d01010505003037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f6964204465627567301e170d3132303230393037343433345a170d3432303230313037343433345a3037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f696420446562756730819f300d06092a864886f70d010101050003818d0030818902818100b4d99aa8cff1bddc23c6313aebcfc93a869b8f918807e70e8b6ef03d722813b9090044ba2da31b53ae908393ee5fee0e2f6e582f9018bd6013dfef51bf073e049b42c8ba72387ca82687185603696c31c754669b0b3a84452190e87aac5800d7b07dce4475073f7a24cb0da8313d449d29803d3010296c588f3728033b3ddd1f0203010001300d06092a864886f70d0101050500038181002d0527ee1bb08ae465cc273b1b6fdafa4b1476d587712a8cdd47a7f64f3a790f2f7327c3c656da30af86a4c1febda7258a9226edb429365287c3b489dc74fe75160151543790903e3b5c0faac5bdb70192bbce9d41337426038d0061256b7e59abb9120291865ecc4adbc2b16428b0042120a937f5ef6744b2d4cba08e37b660";
  private static final String FACEBOOK_DEBUG = "403828359632347";
  private static final String FACEBOOK_PRODUCTION = "311400242255131";

  @Override
  public void onCreate() {
    super.onCreate();
    Fabric.with(this, new Crashlytics());
    JsonSerializer.registerAlias(TrackableCollection.class,
            "depollsoft.lib.binding.ObservableCollection");

    Parse.initialize(new Parse.Configuration.Builder(this)
            .server("https://tagmaster-api.depollsoft.xyz")
            .applicationId("RhfRllVEF5Qlm0DyVWzx6zi1yjxlmCrnqFtJFwbj")
            .clientKey("7xDIp24FCSz218vpiHhcudEb2Bytn8AzIrBfVLM4")
            .build());
    FacebookSdk.sdkInitialize(this);
    ParseFacebookUtils.initialize(this);
    AppEventsLogger.activateApp(this);
  }

  @Override
  public void onTerminate() {
    super.onTerminate();
  }

  @Override
  protected void attachBaseContext(Context base) {
    MultiDex.install(this);
    super.attachBaseContext(base);
  }
}
