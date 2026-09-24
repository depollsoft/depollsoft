package depollsoft.tagmaster

import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.ContentCache
import depollsoft.lib.util.preference

object SettingsModel {
    private const val minimumRandomTagRatingKey = "tagmaster.MinimumRandomTagRating"
    private const val minimumRandomDownloadsKey = "tagmaster.MinimumRandomDownloadsKey"
    private const val sheetMusicRandomKey = "tagmaster.SheetMusicRandomKey"
    private const val learningTracksRandomKey = "tagmaster.LearningTracksRandomKey"
    private const val wakeLockKey = "tagmaster.WakeLockOnSheetMusic"
    fun clearCache() {
        val cc = ContentCache(RichApplication.getAppContext())
        cc.clearCache()
    }

    val cacheSizeInMegabytes: Double
        get() {
            val cc = ContentCache(RichApplication.getAppContext())
            return cc.cacheSize / 1024.0 / 1024.0
        }
    var minimumRandomDownloads: Int by preference(minimumRandomDownloadsKey, 100)
    var minimumRandomTagRating: Double by preference(minimumRandomTagRatingKey, 2.5)
    var randomLearningTracksFilter: Boolean? by preference(learningTracksRandomKey, null)
    var randomSheetMusicFilter: Boolean? by preference(sheetMusicRandomKey, true)
    var wakeLockOnSheetMusic: Boolean by preference(wakeLockKey, true)
}
