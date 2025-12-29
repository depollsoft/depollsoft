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
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult

object LoginPrompt {
    fun buildDialog(activity: ComponentActivity, isHoomiLogout: Boolean): Dialog {
        val dialog = Capture<AlertDialog?>(null)
        val launcher = activity.registerForActivityResult(
            FirebaseAuthUIActivityResultContract()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                dialog.get()?.dismiss()
                if (!result.idpResponse!!.isNewUser) {
                    SettingsModel.attachToFirestore()
                    SongsModel.get().attachToFirestore(false)
                } else {
                    SettingsModel.attachToFirestore()
                    SongsModel.get().attachToFirestore(true)
                }
            }
        }
        val view = LayoutInflater.from(activity).inflate(R.layout.loginpromptview, null)
        val loginButton = view.findViewById<View>(R.id.login_button)
        loginButton.setOnClickListener {
            val logInIntent = AuthUI.getInstance().createSignInIntentBuilder()
                .setIsSmartLockEnabled(false)
                .setAvailableProviders(
                    listOf(
                        AuthUI.IdpConfig.EmailBuilder()
                            .setRequireName(false)
                            .setAllowNewAccounts(true)
                            .build(),
                        AuthUI.IdpConfig.GoogleBuilder().build(),
                        AuthUI.IdpConfig.FacebookBuilder().build(),
                    )
                )
                .setTheme(R.style.AppTheme)
                .build()
            launcher.launch(logInIntent)
        }
        // Callback registration

        val explanationText = view.findViewById<View>(R.id.explanationTextView) as TextView
        val explanation =
            activity.resources.getString(if (isHoomiLogout) R.string.HoomiLoginExplanation else R.string.LoginExplanation)
        explanationText.text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(explanation)
        }
        dialog.set(AlertDialog.Builder(activity)
            .setView(view)
            .setNeutralButton(
                R.string.SkipLogin
            ) { dialog, which -> dialog.dismiss() }
            .setTitle(R.string.LoginTitle)
            .create())
        return dialog.get()!!
    }
}