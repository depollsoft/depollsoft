package depollsoft.lib.testing

import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.isRoot
import java.io.File

/**
 * What a screen reader is told about a screen, as text: every node in the merged semantics tree
 * that says something (text, description, role, state, actions, headings, live regions...), in
 * tree order, indented under the nearest ancestor that also says something. Hidden nodes and
 * their children are left out, as TalkBack leaves them out; test tags and bounds are left out, so
 * a snapshot changes only when what a user hears or can do changes.
 *
 * [verify] compares against `src/test/semantics/<name>.txt`. A missing file is written and fails
 * the test once, so a new snapshot is always looked at; set the environment variable
 * `RECORD_SEMANTICS=1` to rewrite changed ones on purpose.
 */
object SemanticsSnapshot {
    fun of(rule: SemanticsNodeInteractionsProvider): String {
        val out = StringBuilder()
        rule.onAllNodes(isRoot()).fetchSemanticsNodes().forEachIndexed { index, root ->
            if (index > 0) out.append('\n')
            out.append(if (root.config.getOrNull(SemanticsProperties.IsDialog) != null) "window (dialog)\n" else "window\n")
            append(out, root, depth = 1)
        }
        return out.toString()
    }

    fun verify(
        rule: SemanticsNodeInteractionsProvider,
        name: String,
        dir: File = File("src/test/semantics"),
    ) {
        val actual = of(rule)
        val file = File(dir, "$name.txt")
        val record = System.getenv("RECORD_SEMANTICS") == "1"
        if (record || !file.exists()) {
            dir.mkdirs()
            file.writeText(actual)
            if (!record) throw AssertionError("Recorded a new semantics snapshot at $file; check it and run again.")
            return
        }
        val expected = file.readText()
        if (expected != actual) throw AssertionError("Semantics of '$name' changed:\n" + diff(expected, actual))
    }

    private fun append(
        out: StringBuilder,
        node: SemanticsNode,
        depth: Int,
    ) {
        val config = node.config
        if (config.getOrNull(SemanticsProperties.HideFromAccessibility) != null) return
        @Suppress("DEPRECATION")
        if (config.getOrNull(SemanticsProperties.InvisibleToUser) != null) return
        val line = describe(config)
        val childDepth =
            if (line.isEmpty()) {
                depth
            } else {
                out.append("  ".repeat(depth)).append(line).append('\n')
                depth + 1
            }
        node.children.forEach { append(out, it, childDepth) }
    }

    private fun describe(config: SemanticsConfiguration): String {
        val parts = mutableListOf<String>()
        config.getOrNull(SemanticsProperties.Role)?.let { parts += it.toString() }
        if (config.getOrNull(SemanticsProperties.Heading) != null) parts += "heading"
        config.getOrNull(SemanticsProperties.PaneTitle)?.let { parts += "pane=${quote(it)}" }
        config.getOrNull(SemanticsProperties.ContentDescription)?.let { parts += "desc=" + it.joinToString(" | ", transform = ::quote) }
        config.getOrNull(SemanticsProperties.Text)?.let { texts ->
            if (texts.isNotEmpty()) parts += "text=" + texts.joinToString(" | ") { quote(it.text) }
        }
        config.getOrNull(SemanticsProperties.EditableText)?.let { parts += "editable=${quote(it.text)}" }
        config.getOrNull(SemanticsProperties.StateDescription)?.let { parts += "state=${quote(it)}" }
        config.getOrNull(SemanticsProperties.Selected)?.let { parts += if (it) "selected" else "unselected" }
        config.getOrNull(SemanticsProperties.ToggleableState)?.let { parts += "toggle=$it" }
        if (config.getOrNull(SemanticsProperties.Disabled) != null) parts += "disabled"
        if (config.getOrNull(SemanticsProperties.Password) != null) parts += "password"
        config.getOrNull(SemanticsProperties.Error)?.let { parts += "error=${quote(it)}" }
        config.getOrNull(SemanticsProperties.LiveRegion)?.let { parts += "live=$it" }
        config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)?.let {
            parts += "progress=${it.current}/${it.range.start}..${it.range.endInclusive}"
        }
        config.getOrNull(SemanticsProperties.CollectionInfo)?.let { parts += "collection=${it.rowCount}x${it.columnCount}" }
        config.getOrNull(SemanticsProperties.CollectionItemInfo)?.let { parts += "item=${it.rowIndex},${it.columnIndex}" }
        if (config.getOrNull(SemanticsProperties.IsTraversalGroup) == true) parts += "group"
        config.getOrNull(SemanticsProperties.TraversalIndex)?.let { if (it != 0f) parts += "order=$it" }
        val actions = mutableListOf<String>()
        action(config, SemanticsActions.OnClick, "click", actions)
        action(config, SemanticsActions.OnLongClick, "longClick", actions)
        action(config, SemanticsActions.SetText, "setText", actions)
        action(config, SemanticsActions.SetProgress, "setProgress", actions)
        action(config, SemanticsActions.Expand, "expand", actions)
        action(config, SemanticsActions.Collapse, "collapse", actions)
        action(config, SemanticsActions.Dismiss, "dismiss", actions)
        if (config.getOrNull(SemanticsActions.ScrollBy) != null) actions += "scroll"
        config.getOrNull(SemanticsActions.CustomActions)?.forEach { actions += quote(it.label) }
        if (actions.isNotEmpty()) parts += "[" + actions.joinToString(", ") + "]"
        return parts.joinToString(" ")
    }

    private fun <T : Function<Boolean>> action(
        config: SemanticsConfiguration,
        key: androidx.compose.ui.semantics.SemanticsPropertyKey<AccessibilityAction<T>>,
        name: String,
        into: MutableList<String>,
    ) {
        val action = config.getOrNull(key) ?: return
        into += if (action.label != null) "$name(${quote(action.label!!)})" else name
    }

    private fun quote(text: String) = "\"" + text.replace("\n", "\\n") + "\""

    private fun diff(
        expected: String,
        actual: String,
    ): String {
        val a = expected.lines()
        val b = actual.lines()
        val first = (0 until maxOf(a.size, b.size)).firstOrNull { a.getOrNull(it) != b.getOrNull(it) } ?: 0
        val from = (first - 3).coerceAtLeast(0)
        val shown = StringBuilder("first difference at line ${first + 1}\n")
        for (i in from until minOf(maxOf(a.size, b.size), first + 8)) {
            val x = a.getOrNull(i)
            val y = b.getOrNull(i)
            if (x == y) shown.append("  ").append(x).append('\n') else {
                if (x != null) shown.append("- ").append(x).append('\n')
                if (y != null) shown.append("+ ").append(y).append('\n')
            }
        }
        return shown.toString()
    }
}
