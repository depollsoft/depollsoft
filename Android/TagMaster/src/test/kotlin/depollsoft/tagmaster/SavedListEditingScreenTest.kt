package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bindroid.trackable.TrackableCollection
import depollsoft.lib.json.JsonSerializer
import depollsoft.tagmaster.ScreenTestSupport.idle
import depollsoft.tagmaster.barbershop.Tag
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Saved-list editing on both Home and the Teachable list, migrated from the instrumented
 * `SavedListEditingTest`.
 *
 * The three drag cases stay instrumented: they inject real pointer streams through
 * `UiAutomation.injectInputEvent` and depend on `ItemTouchHelper`'s autoscroll, neither of which
 * exists on the JVM. Everything else here is editor and row behaviour that does.
 *
 * `ListModel`'s own reorder and snapshot semantics are already covered by `SavedListReorderTest`;
 * this covers the view layer on top of it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SavedListEditingScreenTest {
    private val ids = (2147482800..2147482811).toList()
    private var controller: ActivityController<out AppCompatActivity>? = null

    @Before
    fun setUp() {
        ScreenTestSupport.startClean()
        ScreenTestSupport.clearTagCaches()
        (ids + (ids.last() + 1)).forEachIndexed { index, id ->
            ScreenTestSupport.cacheOnDisk(
                Tag().apply {
                    this.id = id
                    title = "A long title for an arrangement we want to sing together $index"
                    writtenKey = "C"
                    parts = 4
                    rating = 4.75
                    downloadCount = 12345
                    posted = java.util.Date(0)
                },
            )
        }
        FavoritesModel.favoriteIds = TrackableCollection(ids.toMutableList())
        TeachableTagsModel.teachableTagIds = TrackableCollection(ids.toMutableList())
    }

    @After
    fun tearDown() {
        ScreenTestSupport.finish(controller)
        ScreenTestSupport.clearTagCaches()
    }

    /** Which saved-list screen a case is running against. */
    private enum class Kind { HOME, TEACHABLE, CUSTOM }

    /** The three saved-list screens, with the same editor behind different lists. */
    private inner class Screen(
        val activity: AppCompatActivity,
        val kind: Kind,
        val key: String,
    ) {
        /** Home is the one screen whose rows share their list with a static header. */
        val home get() = kind == Kind.HOME

        val list: RecyclerView =
            activity.findViewById(
                when (kind) {
                    Kind.HOME -> R.id.homeList
                    Kind.TEACHABLE -> R.id.teachableTagsItemsControl
                    Kind.CUSTOM -> R.id.tagListItemsControl
                },
            )
        val adapter get() =
            when (kind) {
                Kind.HOME -> (activity as MeActivity).favoritesAdapter
                Kind.TEACHABLE -> (activity as TeachableTagsActivity).teachableAdapter
                Kind.CUSTOM -> (activity as TagListActivity).listAdapter
            }
        val editor get() =
            when (kind) {
                Kind.HOME -> (activity as MeActivity).listEditor
                Kind.TEACHABLE -> (activity as TeachableTagsActivity).listEditor
                Kind.CUSTOM -> (activity as TagListActivity).listEditor
            }
        val model get() = ListModel(key)

        /** Home puts a static header at position 0; the teachable list starts at its rows. */
        fun positionOf(id: Int) = adapter.currentList.indexOf(id) + if (home) 1 else 0

        fun row(id: Int): SavedTagItemView {
            list.scrollToPosition(positionOf(id))
            idle()
            val holder = list.findViewHolderForAdapterPosition(positionOf(id))
            return awaitLoaded((requireNotNull(holder) { "row for $id should be bound" } as SavedTagListAdapter.Holder).row)
        }

        /**
         * A saved row resolves its tag through `Tag.loadTagById` on a background thread, so the
         * row it hands back is only as meaningful as that load. `row()` waits for it; `holder()`
         * deliberately does not, because the pending and failed cases drive those states by hand.
         */
        fun awaitLoaded(row: SavedTagItemView): SavedTagItemView {
            ScreenTestSupport.await("row ${row.tagId} to resolve its tag") { !row.isLoading }
            assertFalse("row ${row.tagId} should resolve from the cache", row.failedToLoad)
            return row
        }

        fun holder(id: Int): SavedTagListAdapter.Holder {
            list.scrollToPosition(positionOf(id))
            idle()
            return list.findViewHolderForAdapterPosition(positionOf(id)) as SavedTagListAdapter.Holder
        }

        fun toggleEdit() {
            val menu = androidx.appcompat.widget.PopupMenu(activity, list).menu
            activity.menuInflater.inflate(R.menu.savedlistmenu, menu)
            editor.selectMenu(menu.findItem(R.id.editSavedList))
            idle()
        }

        fun edit() {
            if (!editor.isEditing) toggleEdit()
            assertTrue("the list should be in edit mode", editor.isEditing)
        }

        fun <T> privateEditorField(name: String): T? {
            @Suppress("UNCHECKED_CAST")
            return SavedListEditor::class.java
                .getDeclaredField(name)
                .apply { isAccessible = true }
                .get(editor) as T?
        }

        fun assertOrder(expected: List<Int>) {
            idle()
            assertEquals("the visible order", expected, adapter.currentList)
            assertEquals("the stored order", expected, model.ids.toList())
            val serialized = JsonSerializer.serialize(model.ids).toString()
            assertEquals(
                "the order survives a serialization round trip",
                expected,
                (JsonSerializer.deserialize(org.json.JSONObject(serialized)) as TrackableCollection<*>).toList(),
            )
        }
    }

    /**
     * Run [block] against Home, the Teachable list and one of the user's own lists.
     *
     * The instrumented original ran the first two; a user-defined list is the same editor over an
     * arbitrary key, so every case here is a case there too.
     */
    private fun screens(block: (ActivityController<out AppCompatActivity>, Screen) -> Unit) {
        for (kind in Kind.entries) {
            FavoritesModel.favoriteIds = TrackableCollection(ids.toMutableList())
            TeachableTagsModel.teachableTagIds = TrackableCollection(ids.toMutableList())
            val key =
                when (kind) {
                    Kind.HOME -> "favorite"
                    Kind.TEACHABLE -> "teachable"
                    Kind.CUSTOM ->
                        TagLists.create("Afterglow set").also {
                            ListModel(it).ids = TrackableCollection(ids.toMutableList())
                        }
                }
            val created =
                when (kind) {
                    Kind.HOME -> ScreenTestSupport.build(MeActivity::class.java)
                    Kind.TEACHABLE -> ScreenTestSupport.build(TeachableTagsActivity::class.java)
                    Kind.CUSTOM ->
                        ScreenTestSupport.build(
                            TagListActivity::class.java,
                            Intent(RuntimeEnvironment.getApplication(), TagListActivity::class.java)
                                .putExtra(TagListActivity.EXTRA_LIST_KEY, key),
                        )
                }
            controller = created
            created.setup()
            idle()
            if (kind == Kind.HOME) ScreenTestSupport.dismissChangelog()
            val screen = Screen(created.get(), kind, key)
            assertEquals(
                "every saved id should be bound before editing",
                screen.model.ids.size,
                screen.adapter.itemCount,
            )
            try {
                block(created, screen)
            } catch (error: Throwable) {
                throw AssertionError("screen=$kind", error)
            } finally {
                created.close()
                idle()
                controller = null
                if (kind == Kind.CUSTOM) ScreenTestSupport.resetLists()
            }
        }
    }

    // ==================== Detached and stale holders ====================

    @Test
    fun invalidDetachedHolderCannotRemoveOrStartDrag() =
        screens { _, screen ->
            screen.edit()
            val stale = screen.holder(ids.first())
            screen.model.remove(ids.first())
            idle()
            assertEquals(
                "the removed holder loses its position",
                RecyclerView.NO_POSITION,
                stale.bindingAdapterPosition,
            )

            stale.row.removeControl.performClick()
            val down = android.view.MotionEvent.obtain(0, 0, android.view.MotionEvent.ACTION_DOWN, 1f, 1f, 0)
            try {
                stale.row.dragHandle.dispatchTouchEvent(down)
            } finally {
                down.recycle()
            }
            idle()

            assertNull("no drag starts from a detached row", screen.privateEditorField<Any>("drag"))
            assertNull("no confirmation opens from a detached row", screen.privateEditorField<Any>("confirmation"))
            screen.assertOrder(ids.drop(1))
        }

    // ==================== Rows do not navigate or reorder while editing ====================

    @Test
    fun longPressAndHandleTapNeverReorderTheList() =
        screens { _, screen ->
            screen.edit()
            val row = screen.row(ids.first())
            assertFalse("an editing row is not long-clickable", row.isLongClickable)
            assertFalse(row.findViewById<View>(R.id.tagItemView).isLongClickable)

            row.performLongClick()
            idle()
            screen.assertOrder(ids)

            // Pressing and releasing the handle without moving must not commit anything.
            for (action in listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
                val event =
                    android.view.MotionEvent.obtain(
                        0,
                        0,
                        action,
                        row.dragHandle.width / 2f,
                        row.dragHandle.height / 2f,
                        0,
                    )
                try {
                    row.dragHandle.dispatchTouchEvent(event)
                } finally {
                    event.recycle()
                }
            }
            idle()
            screen.assertOrder(ids)
        }

    @Test
    fun cachedFailedAndPendingRowsDoNotNavigateInEditMode() =
        screens { _, screen ->
            screen.edit()
            val holder = screen.holder(ids[1])
            val loaded = screen.awaitLoaded(holder.row).tag
            for (state in listOf("cached", "failed", "pending")) {
                holder.row.tag = if (state == "cached") loaded else null
                holder.row.failedToLoad = state == "failed"
                idle()
                assertFalse("a $state editing row is not clickable", holder.row.isClickable)
                val actions = holder.row.createAccessibilityNodeInfo()!!.actionList
                assertFalse(
                    "a $state editing row offers no click action",
                    actions.any { it.id == AccessibilityNodeInfo.ACTION_CLICK },
                )
                for (label in listOf(R.string.saved_list_remove, R.string.MoveUp, R.string.MoveDown)) {
                    assertTrue(
                        "a $state editing row offers ${screen.activity.getString(label)}",
                        actions.any { it.label == screen.activity.getString(label) },
                    )
                }
                holder.row.performClick()
                idle()
                assertNull(
                    "a $state editing row opens nothing",
                    shadowOf(screen.activity).nextStartedActivity,
                )
            }

            // Leaving edit mode restores navigation.
            screen.toggleEdit()
            val current = screen.holder(ids[1])
            current.row.tag = loaded
            current.row.failedToLoad = false
            idle()
            assertTrue("a finished row is clickable again", current.row.isClickable)
            current.row.findViewById<View>(R.id.tagItemView).performClick()
            idle()
            val opened = requireNotNull(shadowOf(screen.activity).nextStartedActivity)
            assertEquals(TagDetailActivity::class.java.name, opened.component!!.className)
            assertEquals(ids[1], opened.getIntExtra(TagDetailActivity.TAG_ID_EXTRA, -1))
        }

    @Test
    fun aFailedRowOpensTheTagOnTheWebOnceEditingEnds() =
        screens { _, screen ->
            val holder = screen.holder(ids[1])
            holder.row.tag = null
            holder.row.failedToLoad = true
            idle()
            assertTrue("a failed row stays clickable outside edit mode", holder.row.isClickable)
            holder.row.performClick()
            idle()
            val opened = requireNotNull(shadowOf(screen.activity).nextStartedActivity)
            assertEquals(Intent.ACTION_VIEW, opened.action)
            assertEquals(Tag.getTagUri(ids[1]), opened.data.toString())
        }

    // ==================== Accessibility and keyboard reordering ====================

    @Test
    fun accessibilityMovesAndKeyboardKeepBoundaryActionsCurrent() =
        screens { _, screen ->
            screen.edit()
            val first = screen.row(ids.first())
            assertTrue(
                "a row announces its place in the list",
                first.contentDescription.contains("position 1 of 12"),
            )
            assertTrue(
                "remove names the tag it would remove",
                first.removeControl.contentDescription.contains(first.displayName),
            )
            val actions = first.createAccessibilityNodeInfo()!!.actionList
            assertFalse(
                "the first row cannot move up",
                actions.any { it.label == screen.activity.getString(R.string.MoveUp) },
            )
            val down = actions.single { it.label == screen.activity.getString(R.string.MoveDown) }
            assertTrue(ViewCompat.performAccessibilityAction(first, down.id, null))
            screen.assertOrder(listOf(ids[1], ids[0]) + ids.drop(2))

            val moved = screen.row(ids.first())
            assertTrue(
                "Alt+Up on the handle moves the row back",
                moved.dragHandle.dispatchKeyEvent(
                    KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP, 0, KeyEvent.META_ALT_ON),
                ),
            )
            screen.assertOrder(ids)

            screen.toggleEdit()
            val finished = screen.row(ids.first())
            assertEquals("the handle hides when editing ends", View.GONE, finished.dragHandle.visibility)
            assertEquals("and so does remove", View.GONE, finished.removeControl.visibility)
            assertFalse(
                "and the move actions go with them",
                finished.createAccessibilityNodeInfo()!!.actionList.any {
                    it.label == screen.activity.getString(R.string.MoveDown)
                },
            )
            assertTrue(finished.findViewById<View>(R.id.tagItemView).isClickable)
        }

    // ==================== Removal confirmation ====================

    @Test
    fun pendingConfirmationRemovesOnlyTheNamedIdAfterExternalChanges() =
        screens { _, screen ->
            screen.edit()
            screen.row(ids.first()).removeControl.performClick()
            idle()
            // The list changes underneath while the confirmation is open.
            screen.model.ids.replaceBackingStore((listOf(ids.last() + 1) + ids.reversed()).toMutableList())
            idle()

            val dialog = requireNotNull(screen.privateEditorField<AlertDialog>("confirmation"))
            assertTrue("the confirmation is open", dialog.isShowing)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            idle()

            screen.assertOrder(listOf(ids.last() + 1) + ids.reversed().filter { it != ids.first() })
        }

    @Test
    fun cancellingAConfirmationRemovesNothing() =
        screens { _, screen ->
            screen.edit()
            screen.row(ids.first()).removeControl.performClick()
            idle()
            val dialog = requireNotNull(screen.privateEditorField<AlertDialog>("confirmation"))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
            idle()
            screen.assertOrder(ids)
        }

    // ==================== Edit mode across recreation, and its boundaries ====================

    @Test
    fun editModeSurvivesRecreation() =
        screens { created, screen ->
            screen.edit()
            created.recreate()
            idle()
            val restored = Screen(created.get() as AppCompatActivity, screen.kind, screen.key)
            if (restored.home) ScreenTestSupport.dismissChangelog()
            assertTrue("editing survives recreation", restored.editor.isEditing)
            assertTrue(
                "the editor is active again",
                SavedListEditor::class.java
                    .getDeclaredField("active")
                    .apply { isAccessible = true }
                    .getBoolean(restored.editor),
            )
            assertTrue("and the rows come back editing", restored.row(ids.first()).isEditing)
        }

    @Test
    fun aSingleRowCannotBeReorderedButCanBeRemoved() =
        screens { _, screen ->
            screen.edit()
            screen.model.ids = TrackableCollection(mutableListOf(ids.first()))
            idle()
            val single = screen.row(ids.first())
            assertFalse("nothing to drag past", single.dragHandle.isEnabled)
            assertTrue("but it can still be removed", single.removeControl.isEnabled)
            val actions = single.createAccessibilityNodeInfo()!!.actionList.map { it.label?.toString() }
            assertFalse(actions.contains(screen.activity.getString(R.string.MoveUp)))
            assertFalse(actions.contains(screen.activity.getString(R.string.MoveDown)))
        }

    @Test
    fun emptyingTheListEndsEditingAndDisablesTheAction() =
        screens { _, screen ->
            screen.edit()
            screen.model.ids = TrackableCollection(mutableListOf(ids.first()))
            idle()
            screen.row(ids.first()).removeControl.performClick()
            idle()
            requireNotNull(screen.privateEditorField<AlertDialog>("confirmation"))
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .performClick()
            idle()

            assertTrue("the list is empty", screen.model.ids.isEmpty())
            assertFalse("an empty list is not editable", screen.editor.isEditing)
            val menu = androidx.appcompat.widget.PopupMenu(screen.activity, screen.list).menu
            screen.activity.menuInflater.inflate(R.menu.savedlistmenu, menu)
            screen.editor.prepareMenu(menu)
            assertFalse(menu.findItem(R.id.editSavedList).isEnabled)
            assertEquals(
                screen.activity.getString(R.string.saved_list_edit),
                menu.findItem(R.id.editSavedList).title,
            )
        }

    // ==================== Row geometry ====================

    @Test
    fun rowsKeepTheirControlsAndWholeTitlesInsideTheirBounds() =
        screens { _, screen ->
            screen.edit()
            val row = screen.row(ids.first())
            val density = row.resources.displayMetrics.density
            for (control in listOf(row.removeControl, row.dragHandle)) {
                assertTrue("a 48dp touch target", control.width >= 48 * density - 1)
                assertTrue("a 48dp touch target", control.height >= 48 * density - 1)
                val bounds = android.graphics.Rect(0, 0, control.width, control.height)
                row.offsetDescendantRectToMyCoords(control, bounds)
                assertTrue(
                    "the control stays inside the row",
                    android.graphics.Rect(0, 0, row.width, row.height).contains(bounds),
                )
            }
            val title = row.findViewById<TextView>(R.id.titleTextView)
            assertTrue("the title is laid out", title.layout.lineCount > 0)
            for (line in 0 until title.layout.lineCount) {
                assertEquals("the title never ellipsizes", 0, title.layout.getEllipsisCount(line))
            }
            assertTrue("every line fits", title.height >= title.layout.height)
            for (id in listOf(R.id.idTextView, R.id.ratingTextView, R.id.postedTextView, R.id.downloadsTextView)) {
                val text = row.findViewById<TextView>(id)
                assertTrue("metadata $id is visible", text.isShown && text.width > 0)
                val bounds = android.graphics.Rect(0, 0, text.width, text.height)
                row.offsetDescendantRectToMyCoords(text, bounds)
                assertTrue(
                    "metadata $id stays inside the row",
                    android.graphics.Rect(0, 0, row.width, row.height).contains(bounds),
                )
                assertEquals(
                    "metadata $id is shown whole",
                    text.text.length,
                    text.layout.getLineEnd(text.layout.lineCount - 1),
                )
            }
        }
}
