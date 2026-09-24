package depollsoft.pitchperfect

import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf

/** The set list prompts a screen can have open: naming a list, and confirming a delete. */
interface SetListPrompts {
    var nameRequest: NameRequest?
    var pendingDelete: String?
}

/**
 * Keeps [prompts] open across activity recreation (rotation, or the process being reclaimed),
 * as the DialogFragments they replace were. The name typed so far is kept by the dialog itself.
 */
fun ComponentActivity.keepSetListPromptsOpen(prompts: SetListPrompts) {
    val registry = savedStateRegistry
    registry.consumeRestoredStateForKey(PROMPTS_KEY)?.let { saved ->
        if (saved.getBoolean(NAMING)) prompts.nameRequest = NameRequest(saved.getString(NAMING_LIST))
        prompts.pendingDelete = saved.getString(DELETING)
    }
    registry.registerSavedStateProvider(PROMPTS_KEY) {
        bundleOf(
            NAMING to (prompts.nameRequest != null),
            NAMING_LIST to prompts.nameRequest?.listId,
            DELETING to prompts.pendingDelete,
        )
    }
}

private const val PROMPTS_KEY = "depollsoft.pitchperfect.SetListPrompts"
private const val NAMING = "naming"
private const val NAMING_LIST = "namingList"
private const val DELETING = "deleting"
