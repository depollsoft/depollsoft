package depollsoft.pitchperfect

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import bolts.Capture
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.android.material.button.MaterialButton
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth

object LoginPrompt {
    internal const val CREDENTIAL_MANAGER_ENABLED = false
    internal const val ALWAYS_SHOW_PROVIDER_CHOICE = false
    internal val PROVIDER_IDS =
        setOf(
            EmailAuthProvider.PROVIDER_ID,
            GoogleAuthProvider.PROVIDER_ID,
            FacebookAuthProvider.PROVIDER_ID,
        )
    internal val FIREBASE_UI_PROVIDER_IDS =
        setOf(EmailAuthProvider.PROVIDER_ID, GoogleAuthProvider.PROVIDER_ID)

    fun buildDialog(
        activity: ComponentActivity,
        isHoomiLogout: Boolean,
        onAuthenticated: () -> Unit = {},
    ): Dialog {
        val dialog = Capture<AlertDialog?>(null)
        val view = LayoutInflater.from(activity).inflate(R.layout.loginpromptview, null)
        val emailButton: MaterialButton = view.findViewById(R.id.email_login_button)
        val googleButton: MaterialButton = view.findViewById(R.id.google_login_button)
        val facebookButton: MaterialButton = view.findViewById(R.id.facebook_login_button)
        val statusText: TextView = view.findViewById(R.id.loginStatusText)
        val progress: ProgressBar = view.findViewById(R.id.loginProgress)
        val buttons = listOf(emailButton, googleButton, facebookButton)

        fun setBusy(message: Int) {
            buttons.forEach { it.isEnabled = false }
            statusText.setText(message)
            statusText.visibility = View.VISIBLE
            progress.visibility = View.VISIBLE
            statusText.announceForAccessibility(statusText.text)
        }

        fun reset(message: Int) {
            buttons.forEach { it.isEnabled = true }
            progress.visibility = View.GONE
            showStatus(statusText, message)
        }

        fun completeAuthentication(isNewUser: Boolean) {
            if (Firebase.auth.currentUser == null) {
                reset(R.string.SignInFailed)
                return
            }
            SongsModel.get().attachToFirestore(isNewUser)
            dialog.get()?.dismiss()
            onAuthenticated()
        }

        val firebaseUiLauncher =
            activity.registerForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
                if (result.resultCode == RESULT_OK && result.idpResponse != null) {
                    completeAuthentication(result.idpResponse!!.isNewUser)
                } else {
                    reset(if (result.idpResponse?.error != null) R.string.SignInFailed else R.string.SignInCanceled)
                }
            }

        val callbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    setBusy(R.string.ConnectingFacebook)
                    val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                    Firebase.auth.signInWithCredential(credential).addOnCompleteListener(activity) { task ->
                        if (task.isSuccessful && task.result.user != null) {
                            completeAuthentication(task.result.additionalUserInfo?.isNewUser == true)
                        } else {
                            reset(R.string.SignInFailed)
                        }
                    }
                }

                override fun onCancel() {
                    reset(R.string.SignInCanceled)
                }

                override fun onError(error: FacebookException) {
                    reset(R.string.SignInFailed)
                }
            },
        )
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    LoginManager.getInstance().unregisterCallback(callbackManager)
                }
            },
        )

        emailButton.setOnClickListener {
            setBusy(R.string.OpeningEmail)
            firebaseUiLauncher.launch(createSignInIntent(emailProvider()))
        }
        googleButton.setOnClickListener {
            setBusy(R.string.OpeningGoogle)
            firebaseUiLauncher.launch(createSignInIntent(googleProvider()))
        }
        facebookButton.setOnClickListener {
            setBusy(R.string.ConnectingFacebook)
            LoginManager.getInstance().logInWithReadPermissions(
                activity,
                callbackManager,
                listOf("email", "public_profile"),
            )
        }

        val explanationText: TextView = view.findViewById(R.id.explanationTextView)
        val explanation =
            activity.getString(
                if (isHoomiLogout) R.string.HoomiLoginExplanation else R.string.LoginExplanation,
            )
        explanationText.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
        dialog.set(
            AlertDialog
                .Builder(activity)
                .setView(view)
                .setNeutralButton(R.string.SkipLogin) { prompt, _ -> prompt.dismiss() }
                .setTitle(R.string.LoginTitle)
                .create(),
        )
        return dialog.get()!!
    }

    internal fun createSignInIntent(provider: AuthUI.IdpConfig) =
        AuthUI
            .getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(listOf(provider))
            .setAlwaysShowSignInMethodScreen(ALWAYS_SHOW_PROVIDER_CHOICE)
            .setCredentialManagerEnabled(CREDENTIAL_MANAGER_ENABLED)
            .setTheme(R.style.AuthTheme)
            .build()

    internal fun emailProvider() =
        AuthUI.IdpConfig
            .EmailBuilder()
            .setRequireName(false)
            .setAllowNewAccounts(true)
            .build()

    internal fun googleProvider() = AuthUI.IdpConfig.GoogleBuilder().build()

    private fun showStatus(
        status: TextView,
        message: Int,
    ) {
        status.setText(message)
        status.visibility = View.VISIBLE
        status.announceForAccessibility(status.text)
    }
}
