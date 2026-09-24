package depollsoft.tagmaster

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

/**
 * The shared frame of the Compose screen tests: a clean model and preference state, activities
 * launched through their real lifecycle, and a few semantics helpers. Clicks go through the
 * semantics OnClick action, which is what a screen reader and a keyboard use; a touch goes through
 * `performClick` only where the gesture itself matters.
 */
abstract class ComposeScreenTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    protected var controller: ActivityController<out Activity>? = null
    protected val app get() = RuntimeEnvironment.getApplication()

    @Before
    fun startClean() {
        ScreenTestSupport.startClean()
        ScreenTestSupport.clearTagCaches()
        AuthState.setTestSource { false }
        forgetPrivacy()
    }

    @After
    fun finishScreen() {
        ScreenTestSupport.finish(controller)
        ScreenTestSupport.clearTagCaches()
        AuthState.setTestSource(null)
    }

    /** Keeps the first-run privacy prompt out of the way, as a returning user would see it. */
    private fun forgetPrivacy() {
        depollsoft.lib.privacy.PrivacyChoices(app).save(analytics = false, crashes = false)
    }

    protected fun <A : Activity> launch(
        clazz: Class<A>,
        intent: Intent? = null,
        before: (A) -> Unit = {},
    ): A {
        val created = ScreenTestSupport.build(clazz, intent)
        before(created.get())
        controller = created
        created.setup()
        ScreenTestSupport.dismissChangelog()
        idle()
        return created.get()
    }

    /** Destroys and recreates the current activity with its saved state, as a rotation does. */
    @Suppress("UNCHECKED_CAST")
    protected fun <A : Activity> recreate(): A {
        val current = controller!!
        val state = Bundle()
        current.saveInstanceState(state).pause().stop().destroy()
        val clazz = current.get().javaClass as Class<A>
        val next = ScreenTestSupport.build(clazz, current.intent)
        controller = next
        next.create(state).start().restoreInstanceState(state).postCreate(state).resume().visible()
        idle()
        return next.get()
    }

    protected fun idle() {
        repeat(3) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.waitForIdle()
        }
    }

    protected fun node(tag: String): SemanticsNodeInteraction = compose.onNodeWithTag(tag, useUnmergedTree = true)

    protected fun exists(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    protected fun click(tag: String) {
        node(tag).performSemanticsAction(SemanticsActions.OnClick)
        idle()
    }

    protected fun longClick(tag: String) {
        node(tag).performSemanticsAction(SemanticsActions.OnLongClick)
        idle()
    }

    /** Every text of the node and of the nodes inside it, in order. */
    protected fun text(tag: String): String {
        val parts = mutableListOf<String>()

        fun collect(node: androidx.compose.ui.semantics.SemanticsNode) {
            node.config.getOrNull(SemanticsProperties.EditableText)?.let { parts += it.text }
            node.config.getOrNull(SemanticsProperties.Text)?.forEach { parts += it.text }
            node.children.forEach(::collect)
        }
        collect(node(tag).fetchSemanticsNode())
        return parts.joinToString(" ")
    }

    /** Runs the accessibility action labelled [label] on the node tagged [tag]. */
    protected fun customAction(
        tag: String,
        label: String,
    ) {
        val actions = node(tag).fetchSemanticsNode().config[SemanticsActions.CustomActions]
        val action = actions.firstOrNull { it.label == label } ?: error("no '$label' action on $tag: ${actions.map { it.label }}")
        compose.runOnIdle { action.action() }
        idle()
    }

    protected fun customActions(tag: String): List<String> =
        node(tag)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.CustomActions)
            ?.map { it.label } ?: emptyList()

    protected fun nextStarted(activity: Activity): Intent? = shadowOf(activity).nextStartedActivity

    protected fun string(
        id: Int,
        vararg args: Any,
    ): String = app.getString(id, *args)
}
