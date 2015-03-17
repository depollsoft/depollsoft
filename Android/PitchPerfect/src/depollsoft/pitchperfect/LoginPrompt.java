package depollsoft.pitchperfect;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.widget.LoginButton;

import java.util.Arrays;

import bolts.Capture;
import co.hoomi.HoomiAccessToken;
import co.hoomi.HoomiLoginButton;

public class LoginPrompt {

  public static Dialog buildDialog(Context context) {
    View view = LayoutInflater.from(context).inflate(R.layout.loginpromptview, null);

    final Capture<AlertDialog> dialog = new Capture<>(null);

    HoomiLoginButton button = (HoomiLoginButton) view.findViewById(R.id.login_button);
    button.setRedirectUri(Uri.parse("pitchperfect://login"));
    button.setScopes(Arrays.asList("user:app:data:read", "user:app:data:write"));
    button.addLogInListener(new HoomiLoginButton.LogInListener() {
      @Override
      public void onLogIn(HoomiAccessToken token) {
        dialog.get().dismiss();
      }
    });

    LoginButton fbLoginButton = (LoginButton)view.findViewById(R.id.fb_login_button);
    fbLoginButton.setSessionStatusCallback(new Session.StatusCallback() {
      @Override
      public void call(Session session, SessionState state, Exception exception) {
        dialog.get().dismiss();
      }
    });
    dialog.set(new AlertDialog.Builder(context)
        .setView(view)
        .create());
    return dialog.get();
  }
}
