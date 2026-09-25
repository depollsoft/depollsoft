package depollsoft.pitchperfect

import android.app.Activity.RESULT_OK
import android.text.Html
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import depollsoft.pitchperfect.ui.PlateText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import depollsoft.lib.auth.SignInOutcome
import depollsoft.pitchperfect.ui.AppCompatAlertDialog
import depollsoft.pitchperfect.ui.DialogButton
import depollsoft.pitchperfect.ui.LegacyText
import depollsoft.pitchperfect.ui.PlateSettingsButton
import depollsoft.pitchperfect.ui.plateColors
import depollsoft.pitchperfect.ui.plateText

object LoginPrompt {
    internal const val CREDENTIAL_MANAGER_ENABLED = false
    internal const val ALWAYS_SHOW_PROVIDER_CHOICE = true
    internal val PROVIDER_IDS =
        setOf(
            EmailAuthProvider.PROVIDER_ID,
            GoogleAuthProvider.PROVIDER_ID,
            FacebookAuthProvider.PROVIDER_ID,
        )

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
}

/**
 * Offers to sign in: why, a button that opens FirebaseUI's provider choice, its progress, and
 * "Not now". Firebase's own auth state decides the outcome, not FirebaseUI's result code.
 */
@Composable
fun LoginPromptDialog(
    onDismiss: () -> Unit,
    onAuthenticated: () -> Unit = {},
    isHoomiLogout: Boolean = false,
) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    var opening by rememberSaveable { mutableStateOf(false) }
    var status by rememberSaveable { mutableStateOf<Int?>(null) }

    fun reset(message: Int) {
        opening = false
        status = message
    }

    val launcher =
        rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
            val response = result.idpResponse
            // FirebaseUI can report an error or a cancellation after Firebase has already accepted a
            // Facebook account that has no email address.
            when (
                SignInOutcome.resolve(
                    isSignedIn = Firebase.auth.currentUser != null,
                    resultOk = result.resultCode == RESULT_OK && response != null,
                    hasError = response?.error != null,
                )
            ) {
                SignInOutcome.SIGNED_IN -> {
                    if (Firebase.auth.currentUser == null) {
                        reset(R.string.SignInFailed)
                    } else {
                        SongsModel.get().attachToFirestore(response?.isNewUser == true)
                        onDismiss()
                        onAuthenticated()
                    }
                }
                SignInOutcome.FAILED -> reset(R.string.SignInFailed)
                SignInOutcome.CANCELED -> reset(R.string.SignInCanceled)
            }
        }

    val colors = plateColors
    val explanation =
        remember(isHoomiLogout) {
            Html.fromHtml(
                context.getString(if (isHoomiLogout) R.string.HoomiLoginExplanation else R.string.LoginExplanation),
                Html.FROM_HTML_MODE_LEGACY,
            )
        }
    AppCompatAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.LoginTitle),
        neutral = DialogButton(stringResource(R.string.SkipLogin), onDismiss),
    ) {
        androidx.compose.foundation.layout.Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            LegacyText(
                explanation,
                size = 16.sp,
                color = colors.inkSecondary,
                typeface = android.graphics.Typeface.DEFAULT,
                lineSpacingExtra = 4.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (opening) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = colors.ink, strokeWidth = 2.dp)
                }
                val message = status ?: if (opening) R.string.OpeningSignIn else null
                if (message != null) {
                    if (opening) androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
                    PlateText(
                        stringResource(message),
                        style = plateText(14.sp, colors.ink),
                        // "Opening sign-in" is announced outright when the button is pressed; the
                        // live region speaks what comes back (cancelled, failed) without repeating it.
                        modifier = Modifier.weight(1f).semantics { if (status != null) liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            PlateSettingsButton(
                stringResource(if (opening) R.string.OpeningSignIn else R.string.ChooseLoginMethod),
                enabled = !opening,
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth().testTag(TestTags.LOGIN_BUTTON),
            ) {
                opening = true
                status = null
                // A live region may not speak a line that has only just appeared; say it outright,
                // as the View did.
                view.announceForAccessibility(context.getString(R.string.OpeningSignIn))
                launcher.launch(LoginPrompt.createSignInIntent())
            }
        }
    }
}
