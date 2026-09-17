package depollsoft.pitchperfect

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.google.android.material.button.MaterialButton
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.auth.FirebaseAuth
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.Preferences
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class)
class WatchCompanionTest {
    private lateinit var firebaseAuth: MockedStatic<FirebaseAuth>
    private lateinit var wearable: MockedStatic<Wearable>
    private lateinit var capabilityClient: CapabilityClient
    private lateinit var nodeClient: NodeClient
    private lateinit var context: Context
    private var controller: ActivityController<SettingsActivity>? = null

    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        val auth = Mockito.mock(FirebaseAuth::class.java)
        firebaseAuth = Mockito.mockStatic(FirebaseAuth::class.java)
        firebaseAuth.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(auth)

        capabilityClient = Mockito.mock(CapabilityClient::class.java)
        nodeClient = Mockito.mock(NodeClient::class.java)
        context = ApplicationProvider.getApplicationContext()
        wearable = Mockito.mockStatic(Wearable::class.java)
        wearable.`when`<CapabilityClient> { Wearable.getCapabilityClient(context) }
            .thenReturn(capabilityClient)
        wearable.`when`<NodeClient> { Wearable.getNodeClient(context) }
            .thenReturn(nodeClient)
        Mockito.`when`(capabilityClient.addListener(anyCapabilityListener(), eq(WatchCompanion.CAPABILITY)))
            .thenReturn(Tasks.forResult<Void>(null))
        Mockito.`when`(capabilityClient.removeListener(anyCapabilityListener(), eq(WatchCompanion.CAPABILITY)))
            .thenReturn(Tasks.forResult(false))
    }

    @After
    fun tearDown() {
        controller?.close()
        shadowOf(Looper.getMainLooper()).idle()
        wearable.close()
        firebaseAuth.close()
        Preferences.setTestMode(false)
    }

    @Test
    fun installIntent_targetsTheSuppliedPackage() {
        val intent = WatchCompanion.installIntent("depollsoft.pitchperfect.private")

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertTrue(intent.hasCategory(Intent.CATEGORY_BROWSABLE))
        assertEquals("market://details?id=depollsoft.pitchperfect.private", intent.data.toString())
    }

    @Test
    fun noConnectedWatches_keepsSectionGone() {
        val activity = settings(FakeWatchNodeSource(emptyList()))

        assertEquals(View.GONE, activity.findViewById<View>(R.id.watchSection).visibility)
        assertEquals(0, buttons(activity).childCount)
    }

    @Test
    fun missingApp_showsStatusAndInstallButton() {
        val activity = settings(FakeWatchNodeSource(listOf(WatchNode("watch-1", "Pixel Watch", false))))

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.watchSection).visibility)
        assertEquals("Pitch Perfect is not on Pixel Watch yet.", status(activity))
        assertEquals(1, buttons(activity).childCount)
        assertEquals("Install on Pixel Watch", (buttons(activity).getChildAt(0) as MaterialButton).text.toString())
    }

    @Test
    fun mixedInstallState_showsBothStatusesAndOnlyMissingWatchButton() {
        val source = FakeWatchNodeSource(
            listOf(WatchNode("watch-1", "Pixel Watch", true), WatchNode("watch-2", "Galaxy Watch", false)),
        )
        val activity = settings(source)

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.watchSection).visibility)
        assertEquals("Installed on Pixel Watch\nPitch Perfect is not on Galaxy Watch yet.", status(activity))
        assertEquals(1, buttons(activity).childCount)
        assertEquals("Install on Galaxy Watch", (buttons(activity).getChildAt(0) as MaterialButton).text.toString())
    }

    @Test
    fun capabilityChange_refreshesAndRemovesInstalledWatchButton() {
        val source = FakeWatchNodeSource(listOf(WatchNode("watch-1", "Pixel Watch", false)))
        val activity = settings(source)
        val listener = registeredListener()

        source.nodes = listOf(WatchNode("watch-1", "Pixel Watch", true))
        listener.onCapabilityChanged(Mockito.mock(CapabilityInfo::class.java))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("Installed on Pixel Watch", status(activity))
        assertEquals(0, buttons(activity).childCount)

        source.nodes = emptyList()
        listener.onCapabilityChanged(Mockito.mock(CapabilityInfo::class.java))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(View.GONE, activity.findViewById<View>(R.id.watchSection).visibility)
        assertEquals("", status(activity))
    }

    @Test
    fun pause_removesListenerAndResumeRefreshesWithoutDuplicateButtons() {
        val source = FakeWatchNodeSource(listOf(WatchNode("watch-1", "Pixel Watch", false)))
        val activity = settings(source)
        val listener = registeredListener()
        controller!!.pause()

        Mockito.verify(capabilityClient).removeListener(listener, WatchCompanion.CAPABILITY)
        val callsBefore = source.calls
        listener.onCapabilityChanged(Mockito.mock(CapabilityInfo::class.java))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(callsBefore, source.calls)

        controller!!.resume()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(callsBefore + 1, source.calls)
        assertEquals(1, buttons(activity).childCount)
    }

    @Test
    fun playServicesUnavailable_hidesSectionWithoutCrashing() {
        wearable.`when`<CapabilityClient> { Wearable.getCapabilityClient(context) }
            .thenThrow(IllegalStateException("Unavailable"))
        wearable.`when`<NodeClient> { Wearable.getNodeClient(context) }
            .thenThrow(IllegalStateException("Unavailable"))

        val activity = settings(WearableWatchNodeSource(ApplicationProvider.getApplicationContext()))

        assertEquals(View.GONE, activity.findViewById<View>(R.id.watchSection).visibility)
    }

    @Test
    fun discovery_matchesCapabilityByIdAndIncludesOnlyConnectedNodes() = runBlocking {
        val first = node("watch-1", "Same name")
        val second = node("watch-2", "Same name")
        Mockito.`when`(nodeClient.connectedNodes).thenReturn(Tasks.forResult(listOf(first, second)))
        val offline = node("offline", "Offline watch")
        val capability = Mockito.mock(CapabilityInfo::class.java)
        Mockito.`when`(capability.nodes).thenReturn(setOf(first, offline))
        Mockito.`when`(capabilityClient.getCapability(WatchCompanion.CAPABILITY, CapabilityClient.FILTER_ALL))
            .thenReturn(Tasks.forResult(capability))

        val result = WearableWatchNodeSource(ApplicationProvider.getApplicationContext()).connectedNodes()

        assertEquals(listOf(WatchNode("watch-1", "Same name", true), WatchNode("watch-2", "Same name", false)), result)
    }

    @Test
    fun failedCapabilityQuery_returnsNoNodes() = runBlocking {
        val watch = node("watch-1", "Pixel Watch")
        Mockito.`when`(nodeClient.connectedNodes).thenReturn(Tasks.forResult(listOf(watch)))
        Mockito.`when`(capabilityClient.getCapability(WatchCompanion.CAPABILITY, CapabilityClient.FILTER_ALL))
            .thenReturn(Tasks.forException(IllegalStateException("Unavailable")))

        assertEquals(emptyList<WatchNode>(), WearableWatchNodeSource(ApplicationProvider.getApplicationContext()).connectedNodes())
    }

    @Test
    fun installClick_opensStoreOnSelectedWatchAndReportsSuccess() {
        checkInstallResult(fails = false)
    }

    @Test
    fun installClick_reportsFailedRemoteLaunch() {
        checkInstallResult(fails = true)
    }

    private fun checkInstallResult(fails: Boolean) {
        @Suppress("UNCHECKED_CAST")
        val future = Mockito.mock(ListenableFuture::class.java) as ListenableFuture<Void>
        if (fails) {
            Mockito.`when`(future.get()).thenThrow(ExecutionException(IllegalStateException("Unreachable")))
        }
        var completion: (() -> Unit)? = null
        Mockito.doAnswer { invocation ->
            val listener = invocation.getArgument<Runnable>(0)
            val executor = invocation.getArgument<Executor>(1)
            completion = { executor.execute(listener) }
            null
        }.`when`(future).addListener(any(Runnable::class.java), any(Executor::class.java))
        Mockito.mockConstruction(RemoteActivityHelper::class.java) { helper, _ ->
            Mockito.`when`(helper.startRemoteActivity(any(Intent::class.java) ?: Intent(), eq("watch-2")))
                .thenReturn(future)
        }.use { helpers ->
            val activity = settings(FakeWatchNodeSource(
                listOf(WatchNode("watch-1", "Pixel Watch", true), WatchNode("watch-2", "Galaxy Watch", false)),
            ))
            buttons(activity).getChildAt(0).performClick()

            val intent = ArgumentCaptor.forClass(Intent::class.java)
            Mockito.verify(helpers.constructed().single()).startRemoteActivity(intent.capture() ?: Intent(), eq("watch-2"))
            assertEquals("market://details?id=${activity.applicationContext.packageName}", intent.value.data.toString())
            assertEquals(Intent.ACTION_VIEW, intent.value.action)
            assertTrue(intent.value.hasCategory(Intent.CATEGORY_BROWSABLE))
            assertEquals(0, ShadowToast.shownToastCount())

            completion!!.invoke()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(
                if (fails) "Could not reach Galaxy Watch" else "Opening the Play Store on Galaxy Watch",
                ShadowToast.getTextOfLatestToast(),
            )
        }
    }

    private fun settings(source: WatchNodeSource): SettingsActivity {
        // ActivityController.of supports the constructor-injected source while retaining
        // SettingsActivity's manifest theme and its real onCreate/onResume wiring.
        val activityController = ActivityController.of(SettingsActivity(source))
        controller = activityController
        activityController.setup()
        shadowOf(Looper.getMainLooper()).idle()
        return activityController.get()
    }

    private fun registeredListener(): CapabilityClient.OnCapabilityChangedListener {
        val listener = ArgumentCaptor.forClass(CapabilityClient.OnCapabilityChangedListener::class.java)
        Mockito.verify(capabilityClient).addListener(
            listener.capture() ?: CapabilityClient.OnCapabilityChangedListener {},
            eq(WatchCompanion.CAPABILITY),
        )
        return listener.value
    }

    private fun anyCapabilityListener(): CapabilityClient.OnCapabilityChangedListener =
        any(CapabilityClient.OnCapabilityChangedListener::class.java) ?: CapabilityClient.OnCapabilityChangedListener {}

    private fun buttons(activity: SettingsActivity): LinearLayout = activity.findViewById(R.id.watchButtons)

    private fun status(activity: SettingsActivity): String = activity.findViewById<TextView>(R.id.watchStatus).text.toString()

    private fun node(id: String, name: String): Node = Mockito.mock(Node::class.java).also {
        Mockito.`when`(it.id).thenReturn(id)
        Mockito.`when`(it.displayName).thenReturn(name)
    }

    private class FakeWatchNodeSource(var nodes: List<WatchNode>) : WatchNodeSource {
        var calls = 0

        override suspend fun connectedNodes(): List<WatchNode> {
            calls++
            return nodes
        }
    }
}
