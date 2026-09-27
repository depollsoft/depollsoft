package depollsoft.pitchperfect

import android.graphics.Typeface
import android.text.Html
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import depollsoft.lib.util.Versioning
import depollsoft.pitchperfect.ui.AppCompatAlertDialog
import depollsoft.pitchperfect.ui.DialogButton
import depollsoft.pitchperfect.ui.LegacyText
import depollsoft.pitchperfect.ui.plateColors

/** A prompt the main screen may open once it starts. */
enum class StartupPrompt { LOGIN, CHANGELOG }

/**
 * The prompt the main screen opens as it starts. One already [open] stays: it was restored across
 * recreation, and the login prompt may still be waiting for FirebaseUI's result. Otherwise the
 * login prompt when [loginDue], else the changelog when [changelogDue]; each check runs only when
 * reached, since both record that they ran.
 */
internal fun startupPromptFor(
    open: StartupPrompt?,
    loginDue: () -> Boolean,
    changelogDue: () -> Boolean,
): StartupPrompt? =
    open ?: when {
        loginDue() -> StartupPrompt.LOGIN
        changelogDue() -> StartupPrompt.CHANGELOG
        else -> null
    }

/** The changelog's once-per-version showing. */
object Changelog {
    private var shownThisProcess = false

    /** True the first time a new version runs, once per process; records that it was shown. */
    fun shouldShow(): Boolean {
        if (shownThisProcess || !Versioning.isFirstRunOfVersion()) return false
        shownThisProcess = true
        return true
    }
}

@Composable
fun StartupPrompts(activity: PitchPerfectActivity) {
    when (activity.startupPrompt) {
        StartupPrompt.LOGIN -> LoginPromptDialog(onDismiss = { activity.startupPrompt = null })
        StartupPrompt.CHANGELOG -> ChangelogDialog(onDismiss = { activity.startupPrompt = null })
        null -> Unit
    }
}

/** What changed in this version, from the `Changelog` string's HTML. */
@Composable
fun ChangelogDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text = remember { Html.fromHtml(context.getString(R.string.Changelog), Html.FROM_HTML_MODE_LEGACY) }
    AppCompatAlertDialog(
        onDismissRequest = onDismiss,
        title = CHANGELOG_TITLE,
        buttons = listOf(DialogButton(stringResource(android.R.string.ok), onDismiss)),
        icon = R.mipmap.ic_launcher,
    ) {
        LegacyText(
            text,
            size = 16.sp,
            color = plateColors.inkSecondary,
            typeface = Typeface.DEFAULT,
            modifier =
                Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp)
                    .fillMaxWidth(),
        )
    }
}

private const val CHANGELOG_TITLE = "Pitch Perfect Changelog"
