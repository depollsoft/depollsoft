package depollsoft.tagmaster

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.state.SnapshotNotifications
import depollsoft.lib.state.StateList
import depollsoft.lib.state.watchState
import depollsoft.lib.util.Preferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SavedListReorderTest {
    @Before fun setup() {
        RichApplication::class.java
            .getDeclaredField("context")
            .apply { isAccessible = true }
            .set(null, RuntimeEnvironment.getApplication())
        ListModel.setTestMode(true)
        Preferences.setTestMode(false)
        // Robolectric gives each test a new Application; reset the cached preferences Context too.
        Preferences::class.java
            .getDeclaredField("initialized")
            .apply { isAccessible = true }
            .setBoolean(null, false)
        // The list registry is a process-wide object; make every test start from stored state.
        TagLists.resetForTest()
    }

    private fun model() =
        ListModel("reorder-test").apply {
            ids = listOf(1, 2, 3, 4)
        }

    /** Counts how often observers of [model]'s ids hear about a change; each read delivers pending ones. */
    private fun notifications(model: ListModel): () -> Int {
        var calls = 0
        watchState(read = { model.ids.toList() }) { calls++ }
        return {
            SnapshotNotifications.flush()
            calls
        }
    }

    @Test fun empty_and_single_reorders_do_not_notify() {
        for (values in listOf(emptyList(), listOf(1))) {
            val model = model().apply { ids = values }
            val calls = notifications(model)
            assertFalse(model.reorder(model.snapshot(), values))
            assertFalse(model.move(1, 0))
            assertFalse(model.move(1, 1))
            assertEquals(0, calls())
        }
    }

    @Test fun firestore_replacement_cancels_snapshot_without_writing_back() {
        val model = model()
        val baseline = model.snapshot()
        mockStatic(Preferences::class.java).use { prefs ->
            mockStatic(FirebaseAuth::class.java).use { auth ->
                try {
                    val fromFirestore =
                        ListModel.Companion::class.java
                            .getDeclaredMethod("fromFirestore", Map::class.java, Map::class.java)
                            .apply { isAccessible = true }
                    // A snapshot with no `listInfo`: the registry keeps the list under its key.
                    fromFirestore.invoke(ListModel.Companion, mapOf("reorder-test" to listOf(9L, 3L, 2L, 1L)), null)
                    assertEquals(listOf(9, 3, 2, 1), model.ids.toList())
                    assertFalse(model.reorder(baseline, listOf(4, 3, 2, 1)))
                    auth.verifyNoInteractions()
                    prefs.verify({ Preferences.setAsync(eq("tagmaster.lists"), any()) }, times(1))
                    // Applying the snapshot reloads and rewrites the list registry, and nothing else.
                    prefs.verify({ Preferences.get<Any>(eq("tagmaster.listNames")) }, times(1))
                    prefs.verify({ Preferences.get<Any>(eq("tagmaster.listOrder")) }, times(1))
                    prefs.verify({ Preferences.setAsync(eq("tagmaster.listNames"), any()) }, times(1))
                    prefs.verify({ Preferences.setAsync(eq("tagmaster.listOrder"), any()) }, times(1))
                    prefs.verifyNoMoreInteractions()
                } finally {
                    ListModel.setTestMode(true)
                }
            }
        }
    }

    @Test fun first_middle_last_moves_are_complete_permutations() {
        for (id in 1..4) {
            for (destination in 0..3) {
                val model = model()
                val expected = mutableListOf(1, 2, 3, 4).apply { add(destination, removeAt(id - 1)) }
                val calls = notifications(model)
                assertEquals(id - 1 != destination, model.move(id, destination))
                assertEquals(expected, model.ids.toList())
                assertEquals(if (id - 1 == destination) 0 else 1, calls())
            }
        }
    }

    @Test fun invalid_missing_and_noop_moves_never_notify() {
        val model = model()
        val calls = notifications(model)
        assertFalse(model.move(999, 0))
        assertFalse(model.move(1, -1))
        assertFalse(model.move(1, 4))
        assertFalse(model.move(1, 0))
        model.moveUp(1)
        model.moveDown(4)
        model.moveUp(999)
        model.moveDown(999)
        assertFalse(model.canMoveDown(999))
        assertEquals(0, calls())
        assertEquals(listOf(1, 2, 3, 4), model.ids.toList())
    }

    @Test fun rejects_duplicates_missing_added_ids_and_noop_orders() {
        val model = model()
        val baseline = model.snapshot()
        val calls = notifications(model)
        for (order in listOf(listOf(1, 1, 3, 4), listOf(1, 2, 3), listOf(1, 2, 3, 5), baseline.ids)) {
            assertFalse(model.reorder(baseline, order))
        }
        assertEquals(0, calls())
    }

    @Test fun preview_and_cancel_are_not_model_mutations() {
        val model = model()
        val baseline = model.snapshot()
        val preview = baseline.ids.toMutableList()
        val calls = notifications(model)
        repeat(3) { preview.add(it + 1, preview.removeAt(it)) }
        assertEquals(listOf(2, 3, 4, 1), preview)
        assertEquals(baseline.ids, model.ids.toList())
        assertEquals(0, calls())
        assertTrue(model.reorder(baseline, preview))
        assertEquals(1, calls())
        assertFalse(model.reorder(baseline, preview))
        assertEquals(1, calls())
    }

    @Test fun concurrent_insert_remove_reset_and_collection_replacement_win() {
        val changes: List<(ListModel) -> Unit> =
            listOf(
                { it.add(9) },
                { it.remove(2) },
                { it.reset() },
                { it.ids = it.ids.toList() },
                { it.ids = listOf(1, 2, 3, 4) },
                {
                    it.add(9)
                    it.remove(9)
                },
            )
        for (change in changes) {
            val model = model()
            val baseline = model.snapshot()
            change(model)
            val latest = model.ids.toList()
            val calls = notifications(model)
            assertFalse(model.reorder(baseline, listOf(4, 3, 2, 1)))
            assertEquals(latest, model.ids.toList())
            assertEquals(0, calls())
        }
    }

    @Test fun pending_delete_uses_id_after_external_reorder_and_addition() {
        val model = model()
        val pendingId = model.ids[1]
        model.ids = listOf(9, 4, 3, 2, 1)
        model.remove(pendingId)
        assertEquals(listOf(9, 4, 3, 1), model.ids.toList())
        model.remove(pendingId)
        assertEquals(listOf(9, 4, 3, 1), model.ids.toList())
    }

    @Test fun drop_writes_preferences_and_firestore_once_with_existing_contract() {
        val model = model()
        val auth = mock(FirebaseAuth::class.java)
        val user = mock(FirebaseUser::class.java)
        val firestore = mock(FirebaseFirestore::class.java)
        val document = mock(DocumentReference::class.java)
        `when`(auth.currentUser).thenReturn(user)
        `when`(user.uid).thenReturn("isolated-unit-user")
        `when`(firestore.document("users/isolated-unit-user")).thenReturn(document)
        mockStatic(FirebaseAuth::class.java).use { authStatic ->
            authStatic.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(auth)
            mockStatic(FirebaseFirestore::class.java).use { firestoreStatic ->
                firestoreStatic.`when`<FirebaseFirestore> { FirebaseFirestore.getInstance() }.thenReturn(firestore)
                mockStatic(Preferences::class.java).use { prefs ->
                    ListModel.setTestMode(false)
                    try {
                        val baseline = model.snapshot()
                        assertTrue(model.reorder(baseline, listOf(4, 2, 3, 1)))
                        prefs.verify({ Preferences.setAsync(eq("tagmaster.lists"), any()) }, times(1))
                        verify(document, times(1)).set(
                            // The write sends a snapshot of the ids, not the live collection.
                            mapOf("lists" to mapOf("reorder-test" to model.ids.toList())),
                            SetOptions.mergeFields("lists.reorder-test"),
                        )
                        assertFalse(model.reorder(model.snapshot(), model.ids.toList()))
                        assertFalse(model.reorder(baseline, baseline.ids))
                        prefs.verifyNoMoreInteractions()
                        verifyNoMoreInteractions(document)
                    } finally {
                        ListModel.setTestMode(true)
                    }
                }
            }
        }
    }

    @Test fun serialized_order_uses_existing_alias_and_survives_preferences_reload() {
        val model = model()
        TagMasterApplication.registerStorageAliases()
        assertTrue(model.move(1, 3))
        Preferences.set("tagmaster.lists", mapOf("favorite" to StateList(model.ids)))
        val raw =
            RuntimeEnvironment
                .getApplication()
                .getSharedPreferences("depollsoft.lib.Preferences", 0)
                .getString("tagmaster.lists", null)!!
        assertTrue(raw.contains("depollsoft.lib.binding.ObservableCollection"))
        val restored = Preferences.get<Map<String, StateList<Int>>>("tagmaster.lists")
        assertEquals(listOf(2, 3, 4, 1), restored["favorite"]!!.toList())
        val constructor = ListModel::class.java.getDeclaredConstructor(String::class.java).apply { isAccessible = true }
        val reloaded = constructor.newInstance("reorder-test")
        assertEquals(model.ids.toList(), reloaded.ids.toList())
    }
}
