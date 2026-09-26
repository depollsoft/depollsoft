package depollsoft.pitchperfect

import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Keeps state an activity holds across recreation (rotation, or the process being reclaimed),
 * the way a DialogFragment kept itself. Call from `onCreate`, after `super.onCreate`.
 */
fun ComponentActivity.keepAcrossRecreation(
    key: String,
    save: () -> Bundle,
    restore: (Bundle) -> Unit,
) {
    savedStateRegistry.consumeRestoredStateForKey(key)?.let(restore)
    savedStateRegistry.registerSavedStateProvider(key, save)
}

/** Keeps which of [E]'s dialogs is open across recreation. */
inline fun <reified E : Enum<E>> ComponentActivity.keepDialogOpen(
    key: String,
    crossinline get: () -> E?,
    crossinline set: (E?) -> Unit,
) = keepAcrossRecreation(
    key,
    save = { Bundle().apply { putString(OPEN_DIALOG, get()?.name) } },
    restore = { saved -> set(saved.getString(OPEN_DIALOG)?.let { enumValueOf<E>(it) }) },
)

@PublishedApi
internal const val OPEN_DIALOG = "open"

/** The set list prompts a screen can have open: naming a list, and confirming a delete. */
interface SetListPrompts {
    var nameRequest: NameRequest?
    var pendingDelete: String?
}

/** Keeps [prompts] open across recreation; the name typed so far is kept by the dialog itself. */
fun ComponentActivity.keepSetListPromptsOpen(prompts: SetListPrompts) =
    keepAcrossRecreation(
        PROMPTS_KEY,
        save = {
            Bundle().apply {
                putBoolean(NAMING, prompts.nameRequest != null)
                putString(NAMING_LIST, prompts.nameRequest?.listId)
                putString(DELETING, prompts.pendingDelete)
            }
        },
        restore = { saved ->
            if (saved.getBoolean(NAMING)) prompts.nameRequest = NameRequest(saved.getString(NAMING_LIST))
            prompts.pendingDelete = saved.getString(DELETING)
        },
    )

private const val PROMPTS_KEY = "depollsoft.pitchperfect.SetListPrompts"
private const val NAMING = "naming"
private const val NAMING_LIST = "namingList"
private const val DELETING = "deleting"
