package depollsoft.pitchperfect

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import depollsoft.lib.util.AppLog
import kotlinx.coroutines.tasks.await

data class WatchNode(val id: String, val name: String, val installed: Boolean)

interface WatchNodeSource {
    suspend fun connectedNodes(): List<WatchNode>
}

class WearableWatchNodeSource(context: Context) : WatchNodeSource {
    private val context = context.applicationContext

    override suspend fun connectedNodes(): List<WatchNode> =
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            val installedIds =
                Wearable.getCapabilityClient(context)
                    .getCapability(WatchCompanion.CAPABILITY, CapabilityClient.FILTER_ALL)
                    .await()
                    .nodes
                    .map { it.id }
                    .toSet()
            nodes.map { WatchNode(it.id, it.displayName, it.id in installedIds) }
        } catch (error: Exception) {
            AppLog.info("Settings", "Watch discovery unavailable: ${error.javaClass.simpleName}")
            emptyList()
        }
}

object WatchCompanion {
    const val CAPABILITY = "pitch_perfect_watch_app"

    fun installIntent(packageName: String): Intent =
        Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse("market://details?id=$packageName"))
}
