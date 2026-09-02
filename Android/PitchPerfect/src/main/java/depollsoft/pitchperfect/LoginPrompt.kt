package depollsoft.pitchperfect

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import bolts.Capture
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider

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
    ): Dialog {
        val dialog = Capture<AlertDialog?>(null)
        lateinit var loginButton: MaterialButton
        lateinit var statusText: TextView

        val launcher =
            activity.registerForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
                loginButton.isEnabled = true
                loginButton.setText(R.string.ChooseLoginMethod)

                if (result.resultCode == RESULT_OK) {
                    val response = result.idpResponse
                    if (response == null) {
                        showStatus(statusText, R.string.SignInFailed)
                        return@registerForActivityResult
                    }
                    dialog.get()?.dismiss()
                    SettingsModel.attachToFirestore()
                    SongsModel.get().attachToFirestore(response.isNewUser)
                } else {
                    showStatus(
                        statusText,
                        if (result.idpResponse?.error != null) R.string.SignInFailed else R.string.SignInCanceled,
                    )
                }
            }

        val view = LayoutInflater.from(activity).inflate(R.layout.loginpromptview, null)
        loginButton = view.findViewById(R.id.login_button)
        statusText = view.findViewById(R.id.loginStatusText)
        loginButton.setOnClickListener {
            loginButton.isEnabled = false
            loginButton.setText(R.string.OpeningSignIn)
            statusText.visibility = View.GONE

            launcher.launch(createSignInIntent())
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

    internal fun createSignInIntent() =
        AuthUI
            .getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers())
            .setAlwaysShowSignInMethodScreen(ALWAYS_SHOW_PROVIDER_CHOICE)
            // FirebaseUI 9.x crashes while saving a Facebook credential when
            // Facebook legitimately returns no email. Credential Manager is
            // optional; Firebase Auth itself remains fully enabled.
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
