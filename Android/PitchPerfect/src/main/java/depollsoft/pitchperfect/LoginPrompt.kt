package depollsoft.pitchperfect

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.Html
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import depollsoft.lib.auth.SignInOutcome
import depollsoft.lib.auth.SignInResult
import depollsoft.pitchperfect.ui.AppCompatAlertDialog
import depollsoft.pitchperfect.ui.DialogButton
import depollsoft.pitchperfect.ui.LegacyText
import depollsoft.pitchperfect.ui.PlateSettingsButton
import depollsoft.pitchperfect.ui.PlateText
import depollsoft.pitchperfect.ui.plateColors
import depollsoft.pitchperfect.ui.plateText

object LoginPrompt {
    internal fun createSignInIntent(context: Context) = Intent(context, SignInActivity::class.java)

    /** FirebaseUI's flow: email, Google and Facebook, always starting from the provider choice. */
    internal fun configuration(context: Context): AuthUIConfiguration =
        authUIConfiguration {
            this.context = context.applicationContext
            providers {
                provider(
                    AuthProvider.Email(
                        isDisplayNameRequired = false,
                        emailLinkActionCodeSettings = null,
                        isNewAccountsAllowed = true,
                        passwordValidationRules = emptyList(),
                    ),
                )
                // The server client ID comes from google-services.json's default_web_client_id.
                provider(AuthProvider.Google(scopes = listOf("email"), serverClientId = null))
                provider(AuthProvider.Facebook(scopes = listOf("email", "public_profile")))
            }
            isDisplayNameRequired = false
            // FirebaseUI's "By continuing, you accept..." line links to these.
            tosUrl = "https://apps.depoll.com/terms-of-use"
            privacyPolicyUrl = "https://apps.depoll.com/privacy/"
            isProviderChoiceAlwaysShown = true
            isCredentialManagerEnabled = false
        }
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
    val view = LocalView.current
    var opening by rememberSaveable { mutableStateOf(false) }
    var status by rememberSaveable { mutableStateOf<Int?>(null) }

    fun reset(message: Int) {
        opening = false
        status = message
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // FirebaseUI can report an error or a cancellation after Firebase has already signed the
            // person in; Firebase's auth state decides.
            when (SignInResult.outcome(result.resultCode, result.data, isSignedIn = Firebase.auth.currentUser != null)) {
                SignInOutcome.SIGNED_IN -> {
                    SongsModel.get().attachToFirestore(SignInResult.isNewUser(result.data))
                    PitchPerfectAnalytics.signedIn(SignInResult.provider(result.data))
                    onDismiss()
                    onAuthenticated()
                }

                SignInOutcome.FAILED -> {
                    reset(R.string.SignInFailed)
                }

                SignInOutcome.CANCELED -> {
                    reset(R.string.SignInCanceled)
                }
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
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            LegacyText(
                explanation,
                size = 16.sp,
                color = colors.inkSecondary,
                typeface = Typeface.DEFAULT,
                lineSpacingExtra = 4.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (opening) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = colors.ink, strokeWidth = 2.dp)
                }
                val message = status ?: if (opening) R.string.OpeningSignIn else null
                if (message != null) {
                    if (opening) Spacer(Modifier.width(10.dp))
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
                // A live region may not speak a line that has only just appeared; say it outright.
                view.announceForAccessibility(context.getString(R.string.OpeningSignIn))
                launcher.launch(LoginPrompt.createSignInIntent(context))
            }
        }
    }
}
