package depollsoft.pitchperfect

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Shapes
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.theme.AuthUITheme
import com.firebase.ui.auth.configuration.theme.ProviderStyleDefaults
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen
import com.google.firebase.auth.EmailAuthProvider
import depollsoft.lib.auth.SignInResult
import depollsoft.pitchperfect.ui.PlateTheme
import depollsoft.pitchperfect.ui.plateColors
import kotlinx.coroutines.launch

/**
 * FirebaseUI's sign-in screens on the plate.
 *
 * FirebaseUI's own FirebaseAuthActivity draws them in its stock Material theme, so the app hosts
 * [FirebaseAuthScreen] itself and finishes on the auth states that activity finishes on. An
 * AppCompat activity, so the screens follow the app's light or dark choice. [LoginPromptDialog]
 * reads the [SignInResult].
 */
class SignInActivity : AppCompatActivity() {
    private val authUI by lazy { FirebaseAuthUI.getInstance() }
    private var showedError = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState?.getBoolean("signIn.showedError") == true) recordError()
        window.decorView.filterTouchesWhenObscured = true
        enableEdgeToEdge()
        val configuration = LoginPrompt.configuration(this)
        lifecycleScope.launch {
            authUI.authStateFlow().collect { state ->
                when (state) {
                    is AuthState.Success -> {
                        finish(RESULT_OK, SignInResult.signedIn(state.result?.additionalUserInfo?.providerId, state.isNewUser))
                    }

                    // FirebaseUI would hold an unverified email account on a "verify your email"
                    // step; the app has never asked for that, and Firebase has signed them in.
                    is AuthState.RequiresEmailVerification -> {
                        val metadata = state.user.metadata
                        val isNew = SongsModel.get().isNewAccount(metadata?.creationTimestamp, metadata?.lastSignInTimestamp)
                        finish(RESULT_OK, SignInResult.signedIn(EmailAuthProvider.PROVIDER_ID, isNew))
                    }

                    is AuthState.Aborted -> {
                        authUI.updateAuthState(AuthState.Idle)
                        finish(RESULT_CANCELED, null)
                    }

                    // The screen shows the error and stays up for another try.
                    is AuthState.Error -> {
                        recordError()
                    }

                    else -> {
                        Unit
                    }
                }
            }
        }
        setContent {
            PlateTheme {
                AuthUITheme(plateAuthTheme()) {
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

    private fun recordError() {
        showedError = true
        setResult(RESULT_CANCELED, SignInResult.failed())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Activity doesn't save a pending setResult. Back after rotation must still report the error.
        outState.putBoolean("signIn.showedError", showedError)
    }

    override fun onDestroy() {
        // As FirebaseAuthActivity does: a recreated screen starts again from a clean state.
        if (!isFinishing) authUI.updateAuthState(AuthState.Idle)
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

/**
 * The plate in Material 3's roles: ink on the ground, the surface tone for anything raised, bars
 * like the app's own. Errors keep Material's red, as FirebaseUI's screens always have.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun plateAuthTheme(): AuthUITheme {
    val colors = plateColors
    val base = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    val scheme =
        base.copy(
            primary = colors.ink,
            onPrimary = colors.ground,
            primaryContainer = colors.surface,
            onPrimaryContainer = colors.ink,
            inversePrimary = colors.ground,
            secondary = colors.ink,
            onSecondary = colors.ground,
            secondaryContainer = colors.surface,
            onSecondaryContainer = colors.ink,
            tertiary = colors.ink,
            onTertiary = colors.ground,
            tertiaryContainer = colors.surface,
            onTertiaryContainer = colors.ink,
            background = colors.ground,
            onBackground = colors.ink,
            surface = colors.ground,
            onSurface = colors.ink,
            surfaceVariant = colors.surface,
            onSurfaceVariant = colors.inkSecondary,
            surfaceTint = colors.surface,
            inverseSurface = colors.ink,
            inverseOnSurface = colors.ground,
            outline = colors.inkSecondary,
            outlineVariant = colors.hairline,
            surfaceBright = colors.surface,
            surfaceDim = colors.ground,
            surfaceContainerLowest = colors.ground,
            surfaceContainerLow = colors.surface,
            surfaceContainer = colors.surface,
            surfaceContainerHigh = colors.surface,
            surfaceContainerHighest = colors.surface,
        )
    return AuthUITheme(
        colorScheme = scheme,
        typography = Typography(),
        shapes = Shapes(),
        providerStyles = ProviderStyleDefaults.default,
        topAppBarColors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = colors.surface,
                titleContentColor = colors.ink,
                navigationIconContentColor = colors.ink,
                actionIconContentColor = colors.ink,
            ),
    )
}
