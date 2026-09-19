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
import bolts.Capture
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.android.material.button.MaterialButton
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import depollsoft.lib.auth.SignInOutcome

object LoginPrompt {
    internal const val CREDENTIAL_MANAGER_ENABLED = false
    internal const val ALWAYS_SHOW_PROVIDER_CHOICE = true
    internal val PROVIDER_IDS =
        setOf(
            EmailAuthProvider.PROVIDER_ID,
            GoogleAuthProvider.PROVIDER_ID,
            FacebookAuthProvider.PROVIDER_ID,
        )

    fun buildDialog(
        activity: ComponentActivity,
        isHoomiLogout: Boolean,
        onAuthenticated: () -> Unit = {},
    ): Dialog {
        val dialog = Capture<AlertDialog?>(null)
        val view = LayoutInflater.from(activity).inflate(R.layout.loginpromptview, null)
        val loginButton: MaterialButton = view.findViewById(R.id.login_button)
        val statusText: TextView = view.findViewById(R.id.loginStatusText)
        val progress: ProgressBar = view.findViewById(R.id.loginProgress)

        fun clearState() {
            loginButton.isEnabled = true
            loginButton.setText(R.string.ChooseLoginMethod)
            progress.visibility = View.GONE
            statusText.text = ""
            statusText.visibility = View.GONE
        }

        fun reset(message: Int) {
            clearState()
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
                val response = result.idpResponse
                // Firebase's auth state, not FirebaseUI's result code, decides whether the
                // person is signed in: FirebaseUI can report an error or cancellation after
                // Firebase has already accepted a Facebook account that has no email address.
                when (
                    SignInOutcome.resolve(
                        isSignedIn = Firebase.auth.currentUser != null,
                        resultOk = result.resultCode == RESULT_OK && response != null,
                        hasError = response?.error != null,
                    )
                ) {
                    SignInOutcome.SIGNED_IN -> completeAuthentication(response?.isNewUser == true)
                    SignInOutcome.FAILED -> reset(R.string.SignInFailed)
                    SignInOutcome.CANCELED -> reset(R.string.SignInCanceled)
                }
            }

        loginButton.setOnClickListener {
            loginButton.isEnabled = false
            loginButton.setText(R.string.OpeningSignIn)
            progress.visibility = View.VISIBLE
            showStatus(statusText, R.string.OpeningSignIn)
            firebaseUiLauncher.launch(createSignInIntent())
        }

        val explanationText: TextView = view.findViewById(R.id.explanationTextView)
        val explanation =
            activity.getString(
                if (isHoomiLogout) R.string.HoomiLoginExplanation else R.string.LoginExplanation,
            )
        explanationText.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
        val alertDialog =
            AlertDialog
                .Builder(activity)
                .setView(view)
                .setNeutralButton(R.string.SkipLogin) { prompt, _ -> prompt.dismiss() }
                .setTitle(R.string.LoginTitle)
                .create()
        alertDialog.setOnShowListener { clearState() }
        dialog.set(alertDialog)
        return alertDialog
    }

    internal fun createSignInIntent() =
        AuthUI
            .getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers())
            .setAlwaysShowSignInMethodScreen(ALWAYS_SHOW_PROVIDER_CHOICE)
            .setCredentialManagerEnabled(CREDENTIAL_MANAGER_ENABLED)
            .setTheme(R.style.AuthTheme)
            .build()

    internal fun providers() =
        listOf(
            AuthUI.IdpConfig
                .EmailBuilder()
                .setRequireName(false)
                .setAllowNewAccounts(true)
                .build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig
                .FacebookBuilder()
                .setPermissions(listOf("email", "public_profile"))
                .build(),
        )

    private fun showStatus(
        status: TextView,
        message: Int,
    ) {
        status.setText(message)
        status.visibility = View.VISIBLE
        status.announceForAccessibility(status.text)
    }
}
