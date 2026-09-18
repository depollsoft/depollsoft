package depollsoft.tagmaster

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bindroid.trackable.Trackable
import com.bindroid.trackable.TrackableCollection
import com.bindroid.trackable.Tracker
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import depollsoft.lib.json.JsonSerializer
import depollsoft.tagmaster.barbershop.Tag
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.lang.ref.SoftReference

/** Real production activities and ItemTouchHelper, driven by native touchscreen events. */
@RunWith(AndroidJUnit4::class)
class SavedListEditingTest {
    private val instrument = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrument.targetContext
    private val ids = (2147482800..2147482811).toList()
    private var originals: Pair<TrackableCollection<Int>, TrackableCollection<Int>>? = null
    private val files = mutableMapOf<File, ByteArray?>()
    private val memory = mutableMapOf<Int, SoftReference<Tag>?>()
    private val tagCache get() =
        Tag::class.java
            .getDeclaredField("TagCache")
            .apply { isAccessible = true }
            .get(null) as android.util.SparseArray<SoftReference<Tag>?>

    @Before fun setUp() {
        when (InstrumentationRegistry.getArguments().getString("expectedOrientation")) {
            "2" -> instrument.uiAutomation.setRotation(android.app.UiAutomation.ROTATION_FREEZE_90)
            "1" -> instrument.uiAutomation.setRotation(android.app.UiAutomation.ROTATION_FREEZE_0)
        }
        instrument.runOnMainSync {
            // Fail closed before ANY model or cache mutation. Never sign out a real user.
            assertNull("Native saved-list fixtures require a signed-out app", Firebase.auth.currentUser)
            ListModel.setTestMode(true)
            originals = FavoritesModel.favoriteIds to TeachableTagsModel.teachableTagIds
        }
        (ids + (ids.last() + 1)).forEachIndexed { index, id ->
            val tag =
                Tag().apply {
                    this.id = id
                    title = "A long title for an arrangement we want to sing together $index"
                    writtenKey = "C"
                    parts = 4
                    if (index == 0) {
                        rating = 4.75
                        downloadCount = 12345
                        posted = java.util.Date(0)
                        sheetMusicUri =
                            depollsoft.tagmaster.barbershop.RemoteLocation().apply {
                                uri = "https://example.invalid/saved-list-fixture.pdf"
                                type = "pdf"
                            }
                    }
                }
            val file = File(context.filesDir, "TagCache/$id")
            files[file] = file.takeIf { it.exists() }?.readBytes()
            file.parentFile!!.mkdirs()
            file.writeText(JsonSerializer.serialize(tag).toString())
            synchronized(tagCache) {
                memory[id] = tagCache[id]
                tagCache.put(id, SoftReference(tag))
            }
        }
        instrument.runOnMainSync {
            FavoritesModel.favoriteIds = TrackableCollection(ids.toMutableList())
            TeachableTagsModel.teachableTagIds = TrackableCollection(ids.toMutableList())
        }
    }

    @After fun tearDown() {
        if (InstrumentationRegistry.getArguments().getString("expectedOrientation") != null) {
            instrument.uiAutomation.setRotation(android.app.UiAutomation.ROTATION_UNFREEZE)
        }
        if (originals == null) return
        instrument.runOnMainSync {
            ListModel.setTestMode(true)
            FavoritesModel.favoriteIds = originals!!.first
            TeachableTagsModel.teachableTagIds = originals!!.second
            ListModel.setTestMode(false)
        }
        files.forEach { (file, original) -> if (original == null) file.delete() else file.writeBytes(original) }
        synchronized(tagCache) {
            memory.forEach { (id, original) -> if (original == null) tagCache.remove(id) else tagCache.put(id, original) }
        }
    }

    private class Screen(
        val activity: AppCompatActivity,
        val home: Boolean,
    ) {
        val list: RecyclerView = activity.findViewById(if (home) R.id.homeList else R.id.teachableTagsItemsControl)
        val adapter get() = if (home) (activity as MeActivity).favoritesAdapter else (activity as TeachableTagsActivity).teachableAdapter
        val editor get() = if (home) (activity as MeActivity).listEditor else (activity as TeachableTagsActivity).listEditor
        val model get() = ListModel(if (home) "favorite" else "teachable")

        fun holder(id: Int) =
            (0 until list.childCount)
                .map { list.getChildViewHolder(list.getChildAt(it)) }
                .filterIsInstance<SavedTagListAdapter.Holder>()
                .firstOrNull { it.row.tagId == id }
    }

    private fun screens(block: (ActivityScenario<out AppCompatActivity>, () -> Screen) -> Unit) {
        for (home in listOf(true, false)) {
            val type = if (home) MeActivity::class.java else TeachableTagsActivity::class.java
            ActivityScenario.launch(type).use { scenario ->
                val expectedOrientation = InstrumentationRegistry.getArguments().getString("expectedOrientation")?.toInt()
                if (expectedOrientation != null) {
                    scenario.onActivity {
                        it.requestedOrientation =
                            if (expectedOrientation == 2) {
                                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            } else {
                                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                    }
                    waitFor("requested native orientation") {
                        var matches = false
                        scenario.onActivity { matches = it.resources.configuration.orientation == expectedOrientation }
                        matches
                    }
                }
                if (home) {
                    try {
                        onView(withId(android.R.id.button1)).perform(click())
                    } catch (
                        _: androidx.test.espresso.NoMatchingViewException,
                    ) {
                    }
                }
                lateinit var screen: Screen
                scenario.onActivity { screen = Screen(it, home) }
                val current = {
                    scenario.onActivity { screen = Screen(it, home) }
                    screen
                }
                waitFor { current().adapter.itemCount == current().model.ids.size }
                block(scenario, current)
            }
        }
    }

    private fun waitFor(
        message: String = "UI readiness",
        predicate: () -> Boolean,
    ) {
        val deadline = SystemClock.uptimeMillis() + 10000
        do {
            instrument.waitForIdleSync()
            if (predicate()) return
            SystemClock.sleep(16)
        } while (SystemClock.uptimeMillis() < deadline)
        fail("Timed out: $message")
    }

    private fun main(block: () -> Unit) = instrument.runOnMainSync(block)

    private fun row(
        screen: Screen,
        id: Int,
    ): SavedTagListAdapter.Holder {
        main { screen.list.scrollToPosition(screen.adapter.currentList.indexOf(id) + if (screen.home) 1 else 0) }
        waitFor("saved row $id") {
            var ready = false
            main {
                val holder = screen.holder(id)
                ready = holder != null && holder.row.tag != null && holder.row.height > 0 &&
                    holder.bindingAdapterPosition != RecyclerView.NO_POSITION &&
                    !screen.list.isComputingLayout && !screen.list.hasPendingAdapterUpdates() &&
                    screen.list.itemAnimator?.isRunning != true && screen.activity.hasWindowFocus()
            }
            ready
        }
        lateinit var holder: SavedTagListAdapter.Holder
        main {
            holder = screen.holder(id)!!
            val viewport = Rect()
            screen.list.getGlobalVisibleRect(viewport)
            val bounds = Rect()
            holder.row.getGlobalVisibleRect(bounds)
            if (bounds.height() < holder.row.height) screen.list.scrollBy(0, holder.row.top - screen.list.paddingTop)
        }
        instrument.waitForIdleSync()
        return holder
    }

    private fun edit(screen: Screen) {
        onView(withId(R.id.editSavedList)).perform(click())
        waitFor { screen.editor.isEditing }
    }

    private fun point(view: View): Pair<Float, Float> {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return location[0] + view.width / 2f to location[1] + view.height / 2f
    }

    private class Pointer(
        val down: Long,
        var x: Float,
        var y: Float,
    )

    private fun inject(
        pointer: Pointer,
        action: Int,
        x: Float = pointer.x,
        y: Float = pointer.y,
    ) {
        pointer.x = x
        pointer.y = y
        val event = MotionEvent.obtain(pointer.down, SystemClock.uptimeMillis(), action, x, y, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        try {
            assertTrue("Native input accepted", instrument.uiAutomation.injectInputEvent(event, true))
        } finally {
            event.recycle()
        }
    }

    private fun begin(
        screen: Screen,
        id: Int,
    ): Pointer {
        var holder = row(screen, id)
        waitFor("laid-out visible drag handle") {
            var ready = false
            main {
                screen.holder(id)?.let { holder = it }
                val handle = holder.row.dragHandle
                val rect = Rect()
                ready = handle.isShown && handle.width > 0 && !holder.row.isLayoutRequested &&
                    !screen.list.isLayoutRequested && handle.getGlobalVisibleRect(rect) &&
                    rect.width() == handle.width && rect.height() == handle.height
            }
            ready
        }
        var location = 0f to 0f
        main { location = point(holder.row.dragHandle) }
        return Pointer(SystemClock.uptimeMillis(), location.first, location.second).also { inject(it, MotionEvent.ACTION_DOWN) }
    }

    private fun moveTo(
        screen: Screen,
        pointer: Pointer,
        id: Int,
        destination: Int,
    ) {
        var edge = 0f
        main {
            val bounds = Rect()
            screen.list.getGlobalVisibleRect(bounds)
            edge = if (destination == 0) bounds.top + 32f else bounds.bottom - 32f
        }
        val start = pointer.y
        for (step in 1..20) {
            inject(pointer, MotionEvent.ACTION_MOVE, y = start + (edge - start) * step / 20f)
            SystemClock.sleep(16) // Pointer sampling cadence, not a readiness delay.
        }
        val deadline = SystemClock.uptimeMillis() + 12000
        var reached = false
        while (SystemClock.uptimeMillis() < deadline) {
            main { reached = screen.adapter.currentList.indexOf(id) == destination }
            if (reached) break
            inject(pointer, MotionEvent.ACTION_MOVE)
            SystemClock.sleep(32)
        }
        assertTrue("Native drag/autoscroll reached position $destination; preview=${screen.adapter.currentList}", reached)
    }

    private fun assertOrder(
        screen: Screen,
        expected: List<Int>,
    ) {
        waitFor("visible and model order agree") {
            var equal = false
            main { equal = screen.adapter.currentList == expected && screen.model.ids.toList() == expected }
            equal
        }
        main {
            assertEquals(expected, screen.adapter.currentList)
            assertEquals(expected, screen.model.ids.toList())
            val serialized = JsonSerializer.serialize(screen.model.ids).toString()
            assertEquals(expected, (JsonSerializer.deserialize(serialized) as TrackableCollection<*>).toList())
            if (screen.home) {
                val concat = screen.list.adapter as ConcatAdapter
                assertEquals(3, concat.adapters.size)
                assertTrue(concat.adapters.first() is StaticViewAdapter)
                assertTrue(concat.adapters.last() is StaticViewAdapter)
                assertEquals(expected.size + 2, concat.itemCount)
            }
        }
    }

    @Test fun invalid_detached_holder_cannot_remove_or_start_drag() =
        screens { _, current ->
            val screen = current()
            edit(screen)
            val stale = row(screen, ids.first())
            main { screen.model.remove(ids.first()) }
            waitFor("removed holder loses position") {
                var invalid = false
                main { invalid = stale.bindingAdapterPosition == RecyclerView.NO_POSITION }
                invalid
            }
            main {
                stale.row.removeControl.performClick()
                val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 1f, 1f, 0)
                try {
                    stale.row.dragHandle.dispatchTouchEvent(down)
                } finally {
                    down.recycle()
                }
                assertNull(
                    SavedListEditor::class.java
                        .getDeclaredField("drag")
                        .apply { isAccessible = true }
                        .get(screen.editor),
                )
                assertNull(
                    SavedListEditor::class.java
                        .getDeclaredField("confirmation")
                        .apply { isAccessible = true }
                        .get(screen.editor),
                )
            }
            assertOrder(screen, ids.drop(1))
        }

    @Test fun row_long_press_and_handle_noop_never_reorder_or_open_menus() =
        screens { _, current ->
            val screen = current()
            edit(screen)
            val saved = row(screen, ids.first()).row
            var location = 0f to 0f
            main { location = point(saved.findViewById(R.id.tagItemView)) }
            val rowPointer = Pointer(SystemClock.uptimeMillis(), location.first, location.second)
            inject(rowPointer, MotionEvent.ACTION_DOWN)
            SystemClock.sleep(
                android.view.ViewConfiguration
                    .getLongPressTimeout()
                    .toLong() + 100,
            )
            inject(rowPointer, MotionEvent.ACTION_UP)
            assertOrder(screen, ids)
            assertTrue(screen.activity.hasWindowFocus())
            val handlePointer = begin(screen, ids.first())
            inject(handlePointer, MotionEvent.ACTION_UP)
            assertOrder(screen, ids)
            onView(withId(R.id.editSavedList)).perform(click())
            main {
                assertFalse(saved.isLongClickable)
                assertFalse(saved.findViewById<View>(R.id.tagItemView).isLongClickable)
            }
        }

    @Test fun pointer_drag_first_to_last_and_back_commits_once_per_drop() =
        screens { _, current ->
            val screen = current()
            val original = screen.model.ids.toList()
            capture(screen, "before")
            edit(screen)
            row(screen, original.first())
            capture(screen, "edit")
            var changes = 0
            main {
                val tracker =
                    object : Tracker {
                        override fun update() {
                            changes++
                            Trackable.track(this) { screen.model.ids.track() }
                        }
                    }
                Trackable.track(tracker) { screen.model.ids.track() }
            }
            val pointer = begin(screen, original.first())
            moveTo(screen, pointer, original.first(), original.lastIndex)
            assertEquals("No writes while crossing rows", 0, changes)
            assertEquals(original, screen.model.ids.toList())
            inject(pointer, MotionEvent.ACTION_UP)
            assertOrder(screen, original.drop(1) + original.first())
            assertEquals(1, changes)
            capture(screen, "dragged")
            val back = begin(screen, original.first())
            moveTo(screen, back, original.first(), 0)
            inject(back, MotionEvent.ACTION_UP)
            assertOrder(screen, original)
            assertEquals(2, changes)
        }

    @Test fun cancel_done_and_pause_discard_preview_even_after_clearView() =
        screens { scenario, current ->
            var screen = current()
            val original = screen.model.ids.toList()
            edit(screen)
            for (cancel in listOf("cancel", "done", "pause")) {
                val pointer = begin(screen, original.first())
                try {
                    moveTo(screen, pointer, original.first(), original.lastIndex)
                } catch (error: Throwable) {
                    inject(pointer, MotionEvent.ACTION_CANCEL)
                    throw AssertionError("$cancel phase, home=${screen.home}", error)
                }
                when (cancel) {
                    "cancel" -> {
                        inject(pointer, MotionEvent.ACTION_CANCEL)
                    }

                    "done" -> {
                        main {
                            screen.activity.onOptionsItemSelected(
                                androidx.appcompat.widget
                                    .PopupMenu(screen.activity, screen.list)
                                    .menu
                                    .add(0, R.id.editSavedList, 0, "Done"),
                            )
                        }
                        inject(pointer, MotionEvent.ACTION_UP)
                    }

                    else -> {
                        scenario.moveToState(Lifecycle.State.CREATED)
                        scenario.moveToState(Lifecycle.State.RESUMED)
                        screen = current()
                        // Losing the window cancels the native pointer stream. Older
                        // Android versions reject an UP for that canceled stream.
                    }
                }
                main { screen.list.itemAnimator?.endAnimations() }
                assertOrder(screen, original)
                if (!screen.editor.isEditing) edit(screen)
            }
        }

    @Test fun external_changes_during_drag_cancel_without_stale_overwrite() =
        screens { _, current ->
            val screen = current()
            edit(screen)
            for (change in listOf("insert", "remove", "reset", "replace", "backing")) {
                main { screen.model.ids = TrackableCollection(ids.toMutableList()) }
                if (!screen.editor.isEditing) edit(screen)
                val pointer = begin(screen, ids.first())
                moveTo(screen, pointer, ids.first(), ids.lastIndex)
                main {
                    when (change) {
                        "insert" -> screen.model.add(ids.last() + 1)
                        "remove" -> screen.model.remove(ids[2])
                        "reset" -> screen.model.reset()
                        "replace" -> screen.model.ids = TrackableCollection(ids.reversed().toMutableList())
                        else -> screen.model.ids.replaceBackingStore(ids.reversed().toMutableList())
                    }
                }
                val latest = screen.model.ids.toList()
                inject(pointer, MotionEvent.ACTION_UP)
                assertOrder(screen, latest)
            }
        }

    @Test fun edit_survives_recreation_empty_and_single_have_correct_actions() =
        screens { scenario, current ->
            var screen = current()
            edit(screen)
            scenario.recreate()
            screen = current()
            assertTrue(screen.editor.isEditing)
            assertTrue(row(screen, ids.first()).row.isEditing)
            main { screen.model.ids = TrackableCollection(mutableListOf(ids.first())) }
            val single = row(screen, ids.first()).row
            main {
                assertFalse(single.dragHandle.isEnabled)
                assertTrue(single.removeControl.isEnabled)
                val actions = single.createAccessibilityNodeInfo().actionList.map { it.label?.toString() }
                assertFalse(actions.contains(context.getString(R.string.MoveUp)))
                assertFalse(actions.contains(context.getString(R.string.MoveDown)))
            }
            main {
                assertTrue(
                    "Editor active after recreation",
                    SavedListEditor::class.java
                        .getDeclaredField("active")
                        .apply {
                            isAccessible =
                                true
                        }.getBoolean(screen.editor),
                )
                assertSame(screen.adapter, screen.holder(ids.first())!!.bindingAdapter)
                assertEquals(0, screen.holder(ids.first())!!.bindingAdapterPosition)
                assertEquals(ids.first(), screen.adapter.currentList.first())
                assertSame(single, screen.holder(ids.first())!!.row)
                single.removeControl.performClick()
                val dialog =
                    SavedListEditor::class.java
                        .getDeclaredField("confirmation")
                        .apply {
                            isAccessible = true
                        }.get(screen.editor) as? androidx.appcompat.app.AlertDialog
                assertTrue("Remove opens current editor confirmation", dialog?.isShowing == true)
            }
            onView(withId(android.R.id.button2))
                .inRoot(
                    androidx.test.espresso.matcher.RootMatchers
                        .isDialog(),
                ).perform(click())
            assertEquals(1, screen.model.ids.size)
            val currentSingle = row(screen, ids.first()).row
            main { currentSingle.removeControl.performClick() }
            onView(withId(android.R.id.button1))
                .inRoot(
                    androidx.test.espresso.matcher.RootMatchers
                        .isDialog(),
                ).perform(click())
            waitFor { screen.model.ids.isEmpty() && !screen.editor.isEditing }
            main {
                val menu =
                    androidx.appcompat.widget
                        .PopupMenu(screen.activity, screen.list)
                        .menu
                screen.activity.menuInflater.inflate(R.menu.savedlistmenu, menu)
                screen.editor.prepareMenu(menu)
                assertFalse(menu.findItem(R.id.editSavedList).isEnabled)
                assertEquals(context.getString(R.string.saved_list_edit), menu.findItem(R.id.editSavedList).title)
            }
        }

    @Test fun pending_confirmation_removes_only_named_id_after_external_changes() =
        screens { _, current ->
            val screen = current()
            edit(screen)
            val target = row(screen, ids.first()).row
            main { target.removeControl.performClick() }
            main { screen.model.ids.replaceBackingStore((listOf(ids.last() + 1) + ids.reversed()).toMutableList()) }
            onView(withId(android.R.id.button1))
                .inRoot(
                    androidx.test.espresso.matcher.RootMatchers
                        .isDialog(),
                ).perform(click())
            assertOrder(screen, listOf(ids.last() + 1) + ids.reversed().filter { it != ids.first() })
            val next = row(screen, ids[1]).row
            main {
                next.removeControl.performClick()
                screen.model.remove(ids[1])
            }
            val latest = screen.model.ids.toList()
            onView(withId(android.R.id.button1))
                .inRoot(
                    androidx.test.espresso.matcher.RootMatchers
                        .isDialog(),
                ).perform(click())
            assertOrder(screen, latest)
        }

    @Test fun accessibility_moves_and_keyboard_keep_boundary_actions_current() =
        screens { _, current ->
            val screen = current()
            edit(screen)
            val first = row(screen, ids.first()).row
            main {
                assertTrue(first.contentDescription.contains("position 1 of 12"))
                assertTrue(first.removeControl.contentDescription.contains(first.displayName))
                var actions = first.createAccessibilityNodeInfo().actionList
                assertFalse(actions.any { it.label == context.getString(R.string.MoveUp) })
                val down = actions.single { it.label == context.getString(R.string.MoveDown) }
                assertTrue(ViewCompat.performAccessibilityAction(first, down.id, null))
            }
            assertOrder(screen, listOf(ids[1], ids[0]) + ids.drop(2))
            val moved = row(screen, ids.first()).row
            main {
                val event =
                    android.view.KeyEvent(
                        0,
                        0,
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_DPAD_UP,
                        0,
                        android.view.KeyEvent.META_ALT_ON,
                    )
                assertTrue(moved.dragHandle.dispatchKeyEvent(event))
            }
            assertOrder(screen, ids)
            onView(withId(R.id.editSavedList)).perform(click())
            main {
                assertEquals(View.GONE, moved.dragHandle.visibility)
                assertEquals(View.GONE, moved.removeControl.visibility)
                assertFalse(moved.createAccessibilityNodeInfo().actionList.any { it.label == context.getString(R.string.MoveDown) })
                assertTrue(moved.findViewById<View>(R.id.tagItemView).isClickable)
            }
        }

    @Test fun cached_failed_pending_rows_do_not_navigate_in_edit_mode() =
        screens { _, current ->
            val screen = current()
            edit(screen)
            val holder = row(screen, ids[1])
            val loaded = holder.row.tag
            for (state in listOf("cached", "failed", "pending")) {
                main {
                    holder.row.tag = if (state == "cached") loaded else null
                    holder.row.failedToLoad = state == "failed"
                    assertFalse("$state editing row is not clickable", holder.row.isClickable)
                    val actions = holder.row.createAccessibilityNodeInfo().actionList
                    assertFalse(actions.any { it.id == android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK })
                    for (label in listOf(R.string.saved_list_remove, R.string.MoveUp, R.string.MoveDown)) {
                        assertTrue(actions.any { it.label == context.getString(label) })
                    }
                }
                var location = 0f to 0f
                main { location = point(holder.row.findViewById(R.id.tagItemView)) }
                val pointer = Pointer(SystemClock.uptimeMillis(), location.first, location.second)
                inject(pointer, MotionEvent.ACTION_DOWN)
                inject(pointer, MotionEvent.ACTION_UP)
                NavigationTestFixture.onResumed<AppCompatActivity> { assertSame(screen.activity, it) }
            }
            onView(withId(R.id.editSavedList)).perform(click())
            Intents.init()
            try {
                Intents.intending(hasAction(Intent.ACTION_VIEW)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))
                main {
                    assertTrue(holder.row.isClickable)
                    assertTrue(
                        holder.row.createAccessibilityNodeInfo().actionList.any {
                            it.id == android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK
                        },
                    )
                    // A pending row may fail after Done, without another setEditing call.
                    holder.row.failedToLoad = true
                    assertTrue(holder.row.isClickable)
                    holder.row.performClick()
                }
                Intents.intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(Tag.getTagUri(ids[1]))))
            } finally {
                Intents.release()
            }
            main {
                holder.row.tag = loaded
                holder.row.failedToLoad = false
                assertTrue(holder.row.isClickable)
                holder.row.findViewById<View>(R.id.tagItemView).performClick()
            }
            NavigationTestFixture.onResumed<TagDetailActivity> {
                assertEquals(
                    ids[1],
                    it.intent.getIntExtra(TagDetailActivity.TAG_ID_EXTRA, -1),
                )
            }
            instrument.runOnMainSync {
                androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
                    .getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                    .forEach { it.finish() }
            }
        }

    @Test fun responsive_rows_keep_controls_and_long_titles_inside_bounds() =
        screens { _, current ->
            val screen = current()
            edit(screen)
            val row = row(screen, ids.first()).row
            main {
                val args = InstrumentationRegistry.getArguments()
                args.getString("expectedFont")?.let { assertEquals(it.toFloat(), row.resources.configuration.fontScale, 0.01f) }
                args.getString("expectedOrientation")?.let { assertEquals(it.toInt(), row.resources.configuration.orientation) }
                args.getString("minimumWidthDp")?.let { assertTrue(row.resources.configuration.screenWidthDp >= it.toInt()) }
                val density = row.resources.displayMetrics.density
                for (control in listOf(row.removeControl, row.dragHandle)) {
                    assertTrue(control.width >= 48 * density - 1)
                    assertTrue(control.height >= 48 * density - 1)
                    val bounds = Rect(0, 0, control.width, control.height)
                    row.offsetDescendantRectToMyCoords(control, bounds)
                    assertTrue(Rect(0, 0, row.width, row.height).contains(bounds))
                }
                val title = row.findViewById<TextView>(R.id.titleTextView)
                assertTrue(title.layout.lineCount > 0)
                for (line in 0 until title.layout.lineCount) assertEquals(0, title.layout.getEllipsisCount(line))
                assertTrue(title.height >= title.layout.height)
                for (id in listOf(R.id.idTextView, R.id.ratingTextView, R.id.postedTextView, R.id.downloadsTextView)) {
                    val text = row.findViewById<TextView>(id)
                    assertTrue("Metadata is visible", text.isShown && text.width > 0)
                    val bounds = Rect(0, 0, text.width, text.height)
                    row.offsetDescendantRectToMyCoords(text, bounds)
                    assertTrue("Metadata stays inside saved row", Rect(0, 0, row.width, row.height).contains(bounds))
                    assertEquals(text.text.length, text.layout.getLineEnd(text.layout.lineCount - 1))
                    for (line in 0 until text.layout.lineCount) assertTrue(text.layout.getLineWidth(line) <= text.width + 1)
                }
                val titleBounds = Rect(0, 0, title.width, title.height)
                row.offsetDescendantRectToMyCoords(title, titleBounds)
                assertTrue(Rect(0, 0, row.width, row.height).contains(titleBounds))
            }
            capture(screen, "responsive")
        }

    private fun capture(
        screen: Screen,
        name: String,
    ) {
        val label = InstrumentationRegistry.getArguments().getString("listCapture") ?: return
        val bitmap = instrument.uiAutomation.takeScreenshot() ?: error("No screenshot")
        val scale = minOf(800f / bitmap.width, 800f / bitmap.height, 1f)
        val small = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        val file = File(context.cacheDir, "saved-list-captures/$label-${if (screen.home) "home" else "teachable"}-$name.jpg")
        file.parentFile!!.mkdirs()
        file.outputStream().use { small.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        if (small !== bitmap) small.recycle()
        bitmap.recycle()
    }
}
