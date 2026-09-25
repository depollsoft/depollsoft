package depollsoft.pitchperfect

import android.app.Application
import android.os.Looper
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.json.JsonSerializer
import depollsoft.lib.toMap
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.lib.PitchedSong
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

/**
 * The real sync path against the Firestore and Auth emulators: the app's own
 * [SongsModel.attachToFirestore] listener, the set list edits, and the
 * `users/{uid}/songLists/{listId}` documents that come back out the other side.
 *
 * Start the emulators with `scripts/firestore-emulator.sh pitchperfect`. Without them the whole
 * class skips rather than fails, so an ordinary `:PitchPerfect:testDebugUnitTest` run is
 * unaffected.
 *
 * Two clients are used: the app's default [FirebaseApp], and a second app signed in as the same
 * user, standing in for the user's other device. Both are built from synthetic options for the
 * reserved `demo-pitchperfect` project. No test here may call `FirebaseApp.initializeApp(context)`:
 * that reads the real `google-services.json`, and a Robolectric run once uploaded a crash to the
 * production Crashlytics project that way (see `ScreenTestSupport.ensureFirebaseApp`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class SongListSyncEmulatorTest {
    private lateinit var uid: String
    private lateinit var remoteLists: CollectionReference
    private var localStore: FirebaseFirestore? = null
    private var remoteStore: FirebaseFirestore? = null

    private val model get() = SongsModel.get()

    @Before
    fun setUp() {
        Assume.assumeTrue(
            "the Firestore emulator is not listening on $HOST:$FIRESTORE_PORT " +
                "(start it with scripts/firestore-emulator.sh pitchperfect)",
            isListening(FIRESTORE_PORT) && isListening(AUTH_PORT),
        )
        Assume.assumeTrue(
            "a screen suite has already blocked http in this JVM; run this class on its own: " +
                "gradlew :PitchPerfect:testDebugUnitTest " +
                "--tests 'depollsoft.pitchperfect.SongListSyncEmulatorTest'",
            urlsReachTheNetwork(),
        )

        RichApplication.setAppContextForTesting(RuntimeEnvironment.getApplication())
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        resetLists()

        val local = emulatorApp(FirebaseApp.DEFAULT_APP_NAME)
        val remote = emulatorApp(SECOND_DEVICE_APP_NAME)
        localStore = emulatorFirestore(local)
        remoteStore = emulatorFirestore(remote)

        // A fresh account per test: two clients signed in as the same user, which is the only way
        // the security rules (`request.auth.uid == uid`) let the second one touch the documents.
        val email = "set-lists-${System.nanoTime()}@example.invalid"
        val password = "emulator-password"
        val created =
            await(FirebaseAuth.getInstance(local).createUserWithEmailAndPassword(email, password))
        uid = created.user!!.uid
        await(FirebaseAuth.getInstance(remote).signInWithEmailAndPassword(email, password))
        assertEquals(
            "both clients must be the same user",
            uid,
            FirebaseAuth.getInstance(remote).currentUser!!.uid,
        )

        remoteLists = remoteStore!!.collection("users/$uid/songLists")
        model.attachToFirestore(localStore!!.document("users/$uid"), uid, store = true)
        // The attachment seeds the default list from this device; let that land first.
        awaitRemote("the default list to exist") { it.containsKey(SongsModel.DEFAULT_ID) }
    }

    @After
    fun tearDown() {
        if (localStore == null && remoteStore == null) return
        model.detachFromFirestore()
        runCatching { remoteLists().keys.forEach { await(remoteLists.document(it).delete()) } }
        runCatching { FirebaseAuth.getInstance(FirebaseApp.getInstance()).signOut() }
        runCatching {
            FirebaseAuth.getInstance(FirebaseApp.getInstance(SECOND_DEVICE_APP_NAME)).signOut()
        }
        // Firestore's worker and gRPC threads outlive the client and keep calling into the
        // framework. Robolectric hands the next test class a fresh sandbox, and a leftover thread
        // reaching into the old one takes the whole test JVM down, so the clients are shut down
        // here rather than left running.
        runCatching { localStore?.let { await(it.terminate()) } }
        runCatching { remoteStore?.let { await(it.terminate()) } }
        localStore = null
        remoteStore = null
        resetLists()
        Preferences.clearTestValues()
    }

    // ==================== Signing in ====================

    @Test
    fun signingInToAnExistingAccountDropsListsTheAccountDoesNotHave() {
        // The account already has a list of its own …
        await(
            remoteLists.document("remote-set").set(
                mapOf("name" to "Remote set", "songs" to emptyList<Any>(), "order" to 0L),
            ),
        )
        // … and this device made one while signed out.
        model.detachFromFirestore()
        val localOnly = model.createList("Made offline")
        model.currentListId = localOnly

        model.attachToFirestore(localStore!!.document("users/$uid"), uid, store = false, remoteWins = true)

        pumpUntil("the account's list to arrive and the local-only one to go") {
            "remote-set" in model.songLists && localOnly !in model.songLists
        }
        assertEquals("the tab falls back to My Songs", SongsModel.DEFAULT_ID, model.currentListId)
        assertEquals("Remote set", model.displayName("remote-set"))
        // The discarded list was never uploaded.
        val documents = awaitRemote("the account's lists") { "remote-set" in it }
        assertFalse(localOnly in documents)
    }

    // ==================== Local edits ====================

    @Test
    fun localEditsProduceTheDocumentedDocuments() {
        val showId = model.createList("Saturday show")
        model.songLists[showId]!!.addSong(song("Blue Skies"))

        var documents = awaitRemote("the new list to reach the server") { showId in it }
        assertEquals("Saturday show", documents[showId]!!["name"])
        assertEquals(0L, documents[showId]!!["order"])
        assertEquals(listOf("Blue Skies"), songNames(documents[showId]!!))

        // Renaming touches the name only; the songs keep their ids and their order.
        val originalIds = songIds(documents[showId]!!)
        model.renameList(showId, "Saturday set")
        documents = awaitRemote("the rename to reach the server") { it[showId]?.get("name") == "Saturday set" }
        assertEquals(originalIds, songIds(documents[showId]!!))

        // Reordering rewrites `order` densely over every custom list.
        val afterglowId = model.createList("Afterglow")
        awaitRemote("the second list to reach the server") { afterglowId in it }
        model.reorderLists(listOf(afterglowId, showId))
        // Two documents, two writes: wait for both before asserting on either.
        documents =
            awaitRemote("the new order to reach the server") {
                it[afterglowId]?.get("order") == 0L && it[showId]?.get("order") == 1L
            }
        assertEquals(0L, documents[afterglowId]!!["order"])
        assertEquals(1L, documents[showId]!!["order"])

        // Duplicating writes a new document whose songs are fresh.
        val copyId = model.duplicateList(showId)!!
        documents = awaitRemote("the copy to reach the server") { copyId in it }
        assertEquals("Saturday set copy", documents[copyId]!!["name"])
        assertEquals(2L, documents[copyId]!!["order"])
        assertEquals(listOf("Blue Skies"), songNames(documents[copyId]!!))
        assertNotEquals(
            "a duplicated song is its own song",
            songIds(documents[showId]!!),
            songIds(documents[copyId]!!),
        )

        // Copying songs between lists appends fresh songs to the target.
        model.defaultSongList.addSong(song("Shenandoah", 3))
        model.copySongs(model.defaultSongList.songs.toList(), showId)
        documents =
            awaitRemote("the copied song to reach the server") {
                songNames(it[showId] ?: emptyMap()).size == 2
            }
        assertEquals(listOf("Blue Skies", "Shenandoah"), songNames(documents[showId]!!))

        // Deleting takes the document with it and leaves the others alone.
        model.deleteList(copyId)
        documents = awaitRemote("the deletion to reach the server") { copyId !in it }
        assertTrue(showId in documents)
        assertTrue(SongsModel.DEFAULT_ID in documents)
    }

    @Test
    fun theDefaultListKeepsItsLegacyShape() {
        model.defaultSongList.addSong(song("Blue Skies"))
        val documents = awaitRemote("the song to reach the server") {
            songNames(it[SongsModel.DEFAULT_ID] ?: emptyMap()).isNotEmpty()
        }
        val default = documents[SongsModel.DEFAULT_ID]!!
        assertEquals("Default", default["name"])
        // `order` is ignored for `default` and is never written for it, so an older app version
        // that stores only {name, songs} keeps working.
        assertNull(default["order"])
    }

    // ==================== Remote changes ====================

    @Test
    fun remoteChangesUpdateTheLocalModel() {
        // The user's other device creates a list.
        await(
            remoteLists.document("remote-list-abcd").set(
                mapOf("name" to "Remote set", "songs" to emptyList<Any>(), "order" to 0L),
            ),
        )
        pumpUntil("the remote list to reach this device") {
            model.songLists.containsKey("remote-list-abcd")
        }
        assertEquals("Remote set", model.displayName("remote-list-abcd"))
        assertEquals(0L, model.songLists["remote-list-abcd"]!!.order)
        assertEquals(
            listOf(SongsModel.DEFAULT_ID, "remote-list-abcd"),
            model.orderedLists.map { it.id },
        )

        // …then renames it and gives it a song.
        await(
            remoteLists.document("remote-list-abcd").set(
                mapOf(
                    "name" to "Renamed elsewhere",
                    "songs" to listOf(serializedSong("Shenandoah")),
                    "order" to 0L,
                ),
            ),
        )
        pumpUntil("the remote rename to reach this device") {
            model.displayName("remote-list-abcd") == "Renamed elsewhere"
        }
        assertEquals(
            listOf("Shenandoah"),
            model.songLists["remote-list-abcd"]!!.songs.map { it.name },
        )

        // …then adds a second list and reorders the two.
        await(
            remoteLists.document("remote-list-efgh").set(
                mapOf("name" to "Second remote", "songs" to emptyList<Any>(), "order" to 0L),
            ),
        )
        pumpUntil("the second remote list to reach this device") {
            model.songLists.containsKey("remote-list-efgh")
        }
        await(remoteLists.document("remote-list-abcd").update(mapOf("order" to 1L)))
        pumpUntil("the remote reorder to reach this device") {
            model.songLists["remote-list-abcd"]!!.order == 1L
        }
        assertEquals(
            listOf(SongsModel.DEFAULT_ID, "remote-list-efgh", "remote-list-abcd"),
            model.orderedLists.map { it.id },
        )

        // Deleting the list this device is showing falls the tab back to My Songs.
        model.currentListId = "remote-list-abcd"
        assertEquals("remote-list-abcd", model.currentListId)
        val removed = model.songLists["remote-list-abcd"]!!
        await(remoteLists.document("remote-list-abcd").delete())
        pumpUntil("the remote deletion to reach this device") {
            !model.songLists.containsKey("remote-list-abcd")
        }
        assertTrue("a list deleted elsewhere can never be stored back from here", removed.isDeleted)
        assertEquals(SongsModel.DEFAULT_ID, model.currentListId)
        assertEquals(SongsModel.DEFAULT_ID, model.currentList.id)
        assertFalse(model.songLists.containsKey("remote-list-abcd"))
    }

    // MARK: - Reading the documents

    private fun remoteLists(): Map<String, Map<String, Any?>> =
        await(remoteLists.get()).documents.associate { it.id to (it.data ?: emptyMap()) }

    @Suppress("UNCHECKED_CAST")
    private fun songs(document: Map<String, Any?>): List<Map<String, Any?>> =
        (document["songs"] as? List<Map<String, Any?>>) ?: emptyList()

    private fun songNames(document: Map<String, Any?>): List<String> =
        songs(document).mapNotNull { it["Name"] as? String }

    private fun songIds(document: Map<String, Any?>): List<String> =
        songs(document).mapNotNull { it["Id"] as? String }

    /** Re-reads the collection from the second client until [condition] holds. */
    private fun awaitRemote(
        description: String,
        condition: (Map<String, Map<String, Any?>>) -> Boolean,
    ): Map<String, Map<String, Any?>> {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        var documents = remoteLists()
        while (!condition(documents) && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(50)
            documents = remoteLists()
        }
        assertTrue("timed out waiting for $description; documents were $documents", condition(documents))
        return documents
    }

    /** Runs the main looper (where Firestore delivers snapshots) until [condition] holds. */
    private fun pumpUntil(
        description: String,
        condition: () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (!condition() && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        assertTrue("timed out waiting for $description", condition())
    }

    /**
     * Robolectric runs the test on the main looper, which `Tasks.await` refuses to block, and that
     * looper is paused, so nothing posted to it runs unless the test drives it.
     */
    private fun <T> await(task: Task<T>): T {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (!task.isComplete && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue("timed out waiting for a Firebase task", task.isComplete)
        task.exception?.let { throw AssertionError("a Firebase task failed", it) }
        @Suppress("UNCHECKED_CAST")
        return task.result as T
    }

    // MARK: - Fixtures

    private fun song(
        title: String,
        keyIndex: Int = 0,
    ): PitchedSong =
        PitchedSong().apply {
            name = title
            key = Key.getMajorKeys()[keyIndex]
        }

    /** A song in the on-the-wire shape the app writes, for the "other device" to publish. */
    private fun serializedSong(title: String): Map<String, Any?> =
        JsonSerializer.serialize(song(title)).toMap()

    private fun resetLists() {
        model.detachFromFirestore()
        val default = SongList(SongsModel.DEFAULT_ID)
        default.name = "Default"
        model.songLists = mapOf(SongsModel.DEFAULT_ID to default)
        model.currentListId = SongsModel.DEFAULT_ID
    }

    // MARK: - Emulator plumbing

    /**
     * A [FirebaseApp] pointed at the emulators and built from options that belong to no real
     * project, so nothing here can address production even if the emulator is missing.
     */
    private fun emulatorApp(name: String): FirebaseApp {
        val context = RuntimeEnvironment.getApplication()
        val existing = FirebaseApp.getApps(context).firstOrNull { it.name == name }
        if (existing != null) return existing
        val options =
            FirebaseOptions
                .Builder()
                .setProjectId(PROJECT)
                .setApplicationId("1:1:android:pitchperfect-emulator")
                .setApiKey("fake-api-key")
                .build()
        val app =
            if (name == FirebaseApp.DEFAULT_APP_NAME) {
                FirebaseApp.initializeApp(context, options)
            } else {
                FirebaseApp.initializeApp(context, options, name)
            }
        app.setDataCollectionDefaultEnabled(false as Boolean?)
        FirebaseAuth.getInstance(app).useEmulator(HOST, AUTH_PORT)
        return app
    }

    /** A Firestore client for [app], pointed at the emulator and holding nothing on disk. */
    private fun emulatorFirestore(app: FirebaseApp): FirebaseFirestore =
        FirebaseFirestore.getInstance(app).apply {
            useEmulator(HOST, FIRESTORE_PORT)
            firestoreSettings =
                FirebaseFirestoreSettings
                    .Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()
        }

    /**
     * Whether `java.net.URL` still opens real http connections.
     *
     * A screen suite can refuse every outbound http(s) request for the whole test JVM by
     * installing a `URLStreamHandlerFactory`, which is a once-per-JVM, un-installable hook.
     * Firebase Auth reaches the emulator through `java.net.URL`, so once that has happened these
     * tests cannot sign in — and the failure takes the test JVM down rather than failing a test.
     * Skipping is the honest outcome; run the class on its own.
     */
    private fun urlsReachTheNetwork(): Boolean =
        runCatching {
            URL("http://127.0.0.1:$FIRESTORE_PORT/").openConnection() is HttpURLConnection
        }.getOrDefault(false)

    /**
     * The emulator binds the loopback address, and the literal skips name resolution, which can
     * take longer than a short connect timeout in a JVM that has already run a whole suite.
     */
    private fun isListening(port: Int): Boolean =
        runCatching {
            Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 5_000) }
            true
        }.getOrDefault(false)

    private companion object {
        const val PROJECT = "demo-pitchperfect"
        const val HOST = "localhost"
        const val FIRESTORE_PORT = 8080
        const val AUTH_PORT = 9099
        const val SECOND_DEVICE_APP_NAME = "pitchperfect-emulator-second-device"
        const val TIMEOUT_MS = 30_000L
    }
}
