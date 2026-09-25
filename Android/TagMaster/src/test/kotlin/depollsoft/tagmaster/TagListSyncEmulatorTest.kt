package depollsoft.tagmaster

import android.app.Application
import android.os.Looper
import depollsoft.lib.state.StateList
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.SetOptions
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.Preferences
import org.junit.After
import org.junit.Assert.assertEquals
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
 * [ListModel.connectToFirestore] listener, [TagLists] edits, and the `users/{uid}` document that
 * comes back out the other side.
 *
 * Start the emulators with `scripts/firestore-emulator.sh tagmaster`. Without them the whole class skips
 * rather than fails, so an ordinary `:TagMaster:testDebugUnitTest` run is unaffected.
 *
 * Two clients are used: the app's default [FirebaseApp], and a second app signed in as the same
 * user, which stands in for the user's other device. Both are built from synthetic options for the
 * reserved `demo-tagmaster` project. No test here may call `FirebaseApp.initializeApp(context)`:
 * that reads the real `google-services.json`, and a Robolectric run once uploaded a crash to the
 * production Crashlytics project that way (see `ScreenTestSupport.startClean`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class TagListSyncEmulatorTest {
    private lateinit var uid: String
    private lateinit var remoteDoc: DocumentReference
    private var localStore: FirebaseFirestore? = null
    private var remoteStore: FirebaseFirestore? = null

    @Before
    fun setUp() {
        Assume.assumeTrue(
            "the Firestore emulator is not listening on $HOST:$FIRESTORE_PORT " +
                "(start it with scripts/firestore-emulator.sh tagmaster)",
            isListening(FIRESTORE_PORT) && isListening(AUTH_PORT),
        )
        Assume.assumeTrue(
            "a screen suite has already blocked http in this JVM; run this class on its own: " +
                "gradlew :TagMaster:testDebugUnitTest --tests 'depollsoft.tagmaster.TagListSyncEmulatorTest'",
            urlsReachTheNetwork(),
        )

        RichApplication.setAppContextForTesting(RuntimeEnvironment.getApplication())
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        clearStoredLists()
        TagLists.resetForTest()

        val local = emulatorApp(FirebaseApp.DEFAULT_APP_NAME)
        val remote = emulatorApp(SECOND_DEVICE_APP_NAME)
        localStore = emulatorFirestore(local)
        remoteStore = emulatorFirestore(remote)

        // A fresh account per test: two clients signed in as the same user, which is the only way
        // the security rules (`request.auth.uid == uid`) let the second one touch the document.
        val email = "tag-lists-${System.nanoTime()}@example.invalid"
        val password = "emulator-password"
        val created = await(FirebaseAuth.getInstance(local).createUserWithEmailAndPassword(email, password))
        uid = created.user!!.uid
        await(FirebaseAuth.getInstance(remote).signInWithEmailAndPassword(email, password))
        assertEquals("both clients must be the same user", uid, FirebaseAuth.getInstance(remote).currentUser!!.uid)

        remoteDoc = remoteStore!!.document("users/$uid")

        ListModel.setTestMode(false)
        ListModel.connectToFirestore()
        // The listener seeds a brand-new user document from this device; let that land first.
        awaitRemote("the user document to exist") { it.exists() }
    }

    @After
    fun tearDown() {
        if (localStore == null && remoteStore == null) return
        ListModel.registration?.remove()
        ListModel.registration = null
        ListModel.setTestMode(true)
        runCatching { await(remoteDoc.delete()) }
        runCatching { FirebaseAuth.getInstance(FirebaseApp.getInstance()).signOut() }
        runCatching { FirebaseAuth.getInstance(FirebaseApp.getInstance(SECOND_DEVICE_APP_NAME)).signOut() }
        // Firestore's worker and gRPC threads outlive the client and keep calling into the
        // framework. Robolectric hands the next test class a fresh sandbox, and a leftover thread
        // reaching into the old one takes the whole test JVM down, so the clients are shut down
        // here rather than left running. `terminate` also drops the instance, so the next test's
        // `FirebaseFirestore.getInstance` hands back a fresh one to point at the emulator again.
        runCatching { localStore?.let { await(it.terminate()) } }
        runCatching { remoteStore?.let { await(it.terminate()) } }
        localStore = null
        remoteStore = null
        clearStoredLists()
        TagLists.resetForTest()
        Preferences.clearTestValues()
    }

    @Test
    fun localEditsProduceTheDocumentedDocumentShape() {
        val key = TagLists.create("Afterglow set")
        ListModel(key).add(1809)
        ListModel(key).add(42)

        var snapshot = awaitRemote("the new list to reach the server") { info(it)[key] != null && ids(it)[key] != null }
        assertEquals(listOf(1809L, 42L), ids(snapshot)[key])
        assertEquals(mapOf("name" to "Afterglow set", "order" to 0L), info(snapshot)[key])
        assertTrue("the built-in lists are never in listInfo", TagLists.RESERVED.none { it in info(snapshot).keys })

        // Renaming touches the metadata only; the tags keep their key and their order.
        TagLists.rename(key, "Afterglow list")
        snapshot = awaitRemote("the rename to reach the server") { (info(it)[key]?.get("name")) == "Afterglow list" }
        assertEquals(listOf(1809L, 42L), ids(snapshot)[key])

        // Reordering rewrites `order` for every custom list.
        val second = TagLists.create("Chorus warmups")
        awaitRemote("the second list to reach the server") { info(it)[second] != null }
        assertTrue(TagLists.reorder(listOf(second, key)))
        snapshot = awaitRemote("the new order to reach the server") { info(it)[second]?.get("order") == 0L }
        assertEquals(mapOf("name" to "Chorus warmups", "order" to 0L), info(snapshot)[second])
        assertEquals(mapOf("name" to "Afterglow list", "order" to 1L), info(snapshot)[key])

        // Reordering a tag inside a list rewrites only that list's array.
        assertTrue(ListModel(key).move(42, 0))
        awaitRemote("the tag order to reach the server") { ids(it)[key] == listOf(42L, 1809L) }

        // Deleting takes the tags and the metadata with it and leaves the other list alone.
        TagLists.delete(key)
        snapshot = awaitRemote("the deletion to reach the server") { info(it)[key] == null }
        assertNull(ids(snapshot)[key])
        assertEquals(mapOf("name" to "Chorus warmups", "order" to 0L), info(snapshot)[second])
    }

    @Test
    fun aRemoteChangeUpdatesTheLocalRegistry() {
        val local = TagLists.create("Afterglow set")
        ListModel(local).add(1809)
        awaitRemote("the local list to reach the server") { info(it)[local] != null }

        // The user's other device adds a list, renames this one and adds a tag to it.
        await(
            remoteDoc.set(
                mapOf(
                    "lists" to mapOf(local to listOf(1809L, 42L), "remote-key" to listOf(7L, 8L)),
                    "listInfo" to
                        mapOf(
                            local to mapOf("name" to "Renamed elsewhere", "order" to 1L),
                            "remote-key" to mapOf("name" to "Remote set", "order" to 0L),
                        ),
                ),
                SetOptions.merge(),
            ),
        )

        pumpUntil("the remote list to reach this device") { "remote-key" in TagLists.customKeys }
        assertEquals(listOf("remote-key", local), TagLists.customKeys.toList())
        assertEquals("Remote set", TagLists.name("remote-key"))
        assertEquals("Renamed elsewhere", TagLists.name(local))
        assertEquals(listOf(7, 8), ListModel("remote-key").ids.toList())
        assertEquals(listOf(1809, 42), ListModel(local).ids.toList())

        // And a remote deletion empties the list here too.
        await(
            remoteDoc.set(
                mapOf(
                    "lists" to mapOf(local to listOf(1809L, 42L)),
                    "listInfo" to mapOf(local to mapOf("name" to "Renamed elsewhere", "order" to 0L)),
                ),
            ),
        )
        pumpUntil("the remote deletion to reach this device") { "remote-key" !in TagLists.customKeys }
        assertEquals(listOf(local), TagLists.customKeys.toList())
        assertEquals(emptyList<Int>(), ListModel("remote-key").ids.toList())
    }

    // MARK: - Reading the document

    @Suppress("UNCHECKED_CAST")
    private fun ids(snapshot: DocumentSnapshot): Map<String, List<Long>> =
        (snapshot.get("lists") as? Map<String, List<Long>>) ?: emptyMap()

    @Suppress("UNCHECKED_CAST")
    private fun info(snapshot: DocumentSnapshot): Map<String, Map<String, Any>> =
        (snapshot.get("listInfo") as? Map<String, Map<String, Any>>) ?: emptyMap()

    /** Re-reads the document from the second client until [condition] holds. */
    private fun awaitRemote(
        description: String,
        condition: (DocumentSnapshot) -> Boolean,
    ): DocumentSnapshot {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        var snapshot = await(remoteDoc.get())
        while (!condition(snapshot) && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(50)
            snapshot = await(remoteDoc.get())
        }
        assertTrue("timed out waiting for $description; document was ${snapshot.data}", condition(snapshot))
        return snapshot
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
                .setApplicationId("1:1:android:tagmaster-emulator")
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
     * The screen suites refuse every outbound http(s) request for the whole test JVM by installing
     * a `URLStreamHandlerFactory` (`ScreenTestSupport.blockNetwork`), which is a once-per-JVM,
     * un-installable hook. Firebase Auth reaches the emulator through `java.net.URL`, so once any
     * screen test has run these tests cannot sign in — and the failure takes the test JVM down
     * rather than failing a test. Skipping is the honest outcome; run the class on its own.
     */
    private fun urlsReachTheNetwork(): Boolean =
        runCatching {
            URL("http://127.0.0.1:$FIRESTORE_PORT/").openConnection() is HttpURLConnection
        }.getOrDefault(false)

    /**
     * The emulator binds the loopback address, and the literal skips name resolution, which can
     * take longer than a short connect timeout the first time in a JVM that has already run a
     * whole screen suite.
     */
    private fun isListening(port: Int): Boolean =
        runCatching {
            Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 5_000) }
            true
        }.getOrDefault(false)

    /** Empties the static list cache `ListModel` shares between instances. */
    @Suppress("UNCHECKED_CAST")
    private fun clearStoredLists() {
        // Both are static fields of ListModel itself: Kotlin hoists a companion object's private
        // properties onto the outer class.
        val delegate =
            ListModel::class.java
                .getDeclaredField("preferences\$delegate")
                .apply { isAccessible = true }
                .get(null) as Lazy<MutableMap<String, StateList<Int>>>
        // Emptying a collection notifies its ListModel, which removes the key: snapshot first.
        val stored = delegate.value
        val collections = stored.values.toList()
        stored.clear()
        collections.forEach { it.clear() }
        (
            ListModel::class.java
                .getDeclaredField("modelInstances")
                .apply { isAccessible = true }
                .get(null) as MutableMap<String, *>
        ).clear()
    }

    private companion object {
        const val PROJECT = "demo-tagmaster"
        const val HOST = "localhost"
        const val FIRESTORE_PORT = 8080
        const val AUTH_PORT = 9099
        const val SECOND_DEVICE_APP_NAME = "tagmaster-emulator-second-device"
        const val TIMEOUT_MS = 30_000L
    }
}
