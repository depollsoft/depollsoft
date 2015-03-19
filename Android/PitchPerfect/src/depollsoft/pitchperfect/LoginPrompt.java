package depollsoft.pitchperfect;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.flurry.android.FlurryAgent;
import com.parse.ParseCloud;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;
import co.hoomi.HoomiAccessToken;
import co.hoomi.HoomiLoginButton;

public class LoginPrompt {

  private static void completeLogin(boolean isNew) {
    FlurryAgent.setUserId(ParseUser.getCurrentUser().getUsername());
    if (!isNew) {
      SettingsModel.restoreUser();
      SongsModel.get().refreshFromParse();
    } else {
      SettingsModel.refreshUser();
      SongsModel.get().saveAllToParse(true);
    }
  }

  public static Dialog buildDialog(Context context) {
    View view = LayoutInflater.from(context).inflate(R.layout.loginpromptview, null);

    final Capture<AlertDialog> dialog = new Capture<>(null);

    HoomiLoginButton button = (HoomiLoginButton) view.findViewById(R.id.login_button);
    button.setRedirectUri(Uri.parse("pitchperfect://login"));
    button.setScopes(Arrays.asList("user:app:data:read", "user:app:data:write"));
    button.addLogInListener(new HoomiLoginButton.LogInListener() {
      @Override
      public void onLogIn(HoomiAccessToken token) {
        if (token == null) {
          return;
        }
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("hoomiToken", token.getTokenString());
        final Capture<Boolean> isNew = new Capture<Boolean>();
        ParseCloud.<Map<String, Object>>callFunctionInBackground("HoomiSignUpOrLogInUser", parameters)
            .continueWithTask(new Continuation<Map<String, Object>, Task<ParseUser>>() {
              @Override
              public Task<ParseUser> then(Task<Map<String, Object>> task) throws Exception {
                isNew.set((Boolean) task.getResult().get("isNew"));
                return ParseUser.becomeInBackground((String) task.getResult().get("token"));
              }
            }).continueWith(new Continuation<ParseUser, Void>() {
          @Override
          public Void then(Task<ParseUser> task) throws Exception {
            completeLogin(isNew.get());
            dialog.get().dismiss();
            return null;
          }
        });
      }
    });

    final LoginButton fbLoginButton = (LoginButton) view.findViewById(R.id.fb_login_button);
    fbLoginButton.setSessionStatusCallback(new Session.StatusCallback() {
      @Override
      public void call(final Session session, SessionState state, Exception exception) {
        if (exception != null || session == null || state != SessionState.OPENED) {
          return;
        }
        Request.newMeRequest(session, new Request.GraphUserCallback() {
          @Override
          public void onCompleted(GraphUser graphUser, Response response) {
            ParseFacebookUtils.logInInBackground(graphUser.getId(), session.getAccessToken(), session.getExpirationDate())
                .continueWith(new Continuation<ParseUser, Void>() {
                  @Override
                  public Void then(Task<ParseUser> task) throws Exception {
                    completeLogin(task.getResult().isNew());
                    dialog.get().dismiss();
                    return null;
                  }
                });
          }
        }).executeAsync();
      }
    });

    final View moreOptionsButton = view.findViewById(R.id.moreOptionsTextView);
    moreOptionsButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        moreOptionsButton.setVisibility(View.GONE);
        fbLoginButton.setVisibility(View.VISIBLE);
      }
    });

    TextView explanationText = (TextView) view.findViewById(R.id.explanationTextView);
    String explanation = context.getResources().getString(R.string.LoginExplanation);
    explanationText.setText(Html.fromHtml(explanation));

    dialog.set(new AlertDialog.Builder(context)
        .setView(view)
        .setNeutralButton(R.string.SkipLogin, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
          }
        })
        .setTitle(R.string.LoginTitle)
        .create());
    return dialog.get();
  }
}
