package depollsoft.pitchperfect;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.flurry.android.FlurryAgent;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;

public class LoginPrompt {
  public static final CallbackManager FACEBOOK_CALLBACK_MANAGER = CallbackManager.Factory.create();

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

  public static Dialog buildDialog(final Context context, boolean isHoomiLogout) {
    View view = LayoutInflater.from(context).inflate(R.layout.loginpromptview, null);

    final Capture<AlertDialog> dialog = new Capture<>(null);

    final LoginButton fbLoginButton = (LoginButton) view.findViewById(R.id.fb_login_button);
    // Callback registration
    fbLoginButton.registerCallback(FACEBOOK_CALLBACK_MANAGER, new FacebookCallback<LoginResult>() {
      @Override
      public void onSuccess(final LoginResult loginResult) {
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Please wait...");
        progressDialog.show();
        ParseFacebookUtils.logInInBackground(loginResult.getAccessToken())
            .onSuccess(new Continuation<ParseUser, Void>() {
              @Override
              public Void then(Task<ParseUser> task) throws Exception {
                completeLogin(task.getResult().isNew());
                dialog.get().dismiss();
                return null;
              }
            }).continueWith(new Continuation<Void, Void>() {
          @Override
          public Void then(Task<Void> task) throws Exception {
            progressDialog.dismiss();
            return null;
          }
        });
      }

      @Override
      public void onCancel() {
        // App code
      }

      @Override
      public void onError(FacebookException exception) {
        // App code
      }
    });

    TextView explanationText = (TextView) view.findViewById(R.id.explanationTextView);
    String explanation = context.getResources().getString(isHoomiLogout ?
        R.string.HoomiLoginExplanation : R.string.LoginExplanation);
    explanationText.setText(Html.fromHtml(explanation));

    dialog.set(new AlertDialog.Builder(context)
        .setView(view)
        .setNeutralButton(R.string.SkipLogin, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
              }
            }
        )
        .setTitle(R.string.LoginTitle)
        .create());
    return dialog.get();
  }
}
