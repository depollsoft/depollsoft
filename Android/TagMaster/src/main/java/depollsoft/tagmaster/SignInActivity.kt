package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.theme.AuthUITheme
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen
import com.google.firebase.auth.EmailAuthProvider
import depollsoft.lib.auth.SignInResult
import depollsoft.tagmaster.ui.TagMasterTheme
import kotlinx.coroutines.launch
import com.firebase.ui.auth.AuthState as FirebaseUiState

/**
 * FirebaseUI's sign-in screens in Tag Master's colors.
 *
 * FirebaseUI's own FirebaseAuthActivity draws them in its stock Material theme, so the app hosts
 * [FirebaseAuthScreen] itself and finishes on the auth states that activity finishes on, as Pitch
 * Perfect's SignInActivity does. [SettingsActivity] reads the [SignInResult].
 */
class SignInActivity : AppCompatActivity() {
    private val authUI by lazy { FirebaseAuthUI.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.filterTouchesWhenObscured = true
        enableEdgeToEdge()
        val configuration = SettingsActivity.signInConfiguration(this)
        lifecycleScope.launch {
            authUI.authStateFlow().collect { state ->
                when (state) {
                    is FirebaseUiState.Success -> {
                        finish(RESULT_OK, SignInResult.signedIn(state.result?.additionalUserInfo?.providerId, state.isNewUser))
                    }

                    // FirebaseUI would hold an unverified email account on a "verify your email"
                    // step; the app has never asked for that, and Firebase has signed them in.
                    is FirebaseUiState.RequiresEmailVerification -> {
                        finish(RESULT_OK, SignInResult.signedIn(EmailAuthProvider.PROVIDER_ID, isNewUser = false))
                    }

                    is FirebaseUiState.Aborted -> {
                        authUI.updateAuthState(FirebaseUiState.Idle)
                        finish(RESULT_CANCELED, null)
                    }

                    // The screen shows the error and stays up for another try.
                    is FirebaseUiState.Error -> {
                        setResult(RESULT_CANCELED, SignInResult.failed())
                    }

                    else -> {
                        Unit
                    }
                }
            }
        }
        setContent {
            TagMasterTheme {
                AuthUITheme(tagMasterAuthTheme()) {
                    FirebaseAuthScreen(
                        configuration = configuration,
                        // The auth states above finish the screen.
                        onSignInSuccess = {},
                        onSignInFailure = {},
                        onSignInCancelled = {},
                        authUI = authUI,
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        // As FirebaseAuthActivity does: a recreated screen starts again from a clean state.
        if (!isFinishing) authUI.updateAuthState(FirebaseUiState.Idle)
        super.onDestroy()
    }

    private fun finish(
        resultCode: Int,
        data: Intent?,
    ) {
        if (isFinishing) return
        setResult(resultCode, data)
        finish()
    }
}

/** The app's Material 3 roles, with the charcoal chrome for the bars, as the app's own screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun tagMasterAuthTheme(): AuthUITheme {
    val colors = TagMasterTheme.colors
    return AuthUITheme.fromMaterialTheme().copy(
        topAppBarColors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = colors.chrome,
                titleContentColor = colors.onChrome,
                navigationIconContentColor = colors.onChrome,
                actionIconContentColor = colors.onChrome,
            ),
    )
}
