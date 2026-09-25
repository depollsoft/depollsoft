package depollsoft.tagmaster.barbershop

import android.net.Uri
import android.util.Log
import kotlin.jvm.JvmOverloads
import depollsoft.lib.activity.RichApplication
import org.json.JSONObject
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.lib.xml.XmlElement
import android.util.SparseArray
import bolts.Task
import bolts.TaskCompletionSource
import depollsoft.lib.json.JsonSerializer
import depollsoft.lib.xml.XmlDocument
import depollsoft.pitchperfect.lib.Note
import depollsoft.tagmaster.await
import depollsoft.tagmaster.parseDate
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*
import java.lang.Exception
import java.lang.ref.SoftReference
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

class Tag {
    var appVersion: Int = CURRENT_APP_VERSION
    var id: Int = 0
    var title: String? = null
    var lastRefreshed: Date = Date(0)
    var alternativeTitle: String? = null
    var version: String? = null
    var writtenKey: String? = null
    var parts: Int = 0
    var tagType: String? = null
    var recordingMethod: String? = null
    var teachingVideo: String? = null
    var notes: String? = null
    var arranger: String? = null
    var arrangerWebsite: String? = null
    var yearArranged: String? = null
    var sungBy: String? = null
    var sungByWebsite: String? = null
    var sungYear: String? = null
    var learningTrackQuartet: String? = null
    var learningTrackQuartetWebsite: String? = null
    var teacher: String? = null
    var teacherWebsite: String? = null
    var provider: String? = null
    var providerWebsite: String? = null
    var posted: Date? = null
    var classicTagNumber: Int? = null
    var rating: Double? = null
    var downloadCount: Int = 0
    var sheetMusicUri: RemoteLocation? = null
    var notationUri: RemoteLocation? = null
    var allPartsTrackUri: RemoteLocation? = null
    var bassTrackUri: RemoteLocation? = null
    var baritoneTrackUri: RemoteLocation? = null
    var leadTrackUri: RemoteLocation? = null
    var tenorTrackUri: RemoteLocation? = null
    var other1TrackUri: RemoteLocation? = null
    var other2TrackUri: RemoteLocation? = null
    var other3TrackUri: RemoteLocation? = null
    var other4TrackUri: RemoteLocation? = null
    var videos: MutableList<Video>? = mutableListOf()
    val tracks: List<Track>?
        get() {
            val tracks = ArrayList<Track>()
            if (allPartsTrackUri != null) tracks.add(Track("All Parts", allPartsTrackUri))
            if (tenorTrackUri != null) tracks.add(Track("Tenor", tenorTrackUri))
            if (leadTrackUri != null) tracks.add(Track("Lead", leadTrackUri))
            if (baritoneTrackUri != null) tracks.add(Track("Baritone", baritoneTrackUri))
            if (bassTrackUri != null) tracks.add(Track("Bass", bassTrackUri))
            if (other1TrackUri != null) tracks.add(Track("Other1", other1TrackUri))
            if (other2TrackUri != null) tracks.add(Track("Other2", other2TrackUri))
            if (other3TrackUri != null) tracks.add(Track("Other3", other3TrackUri))
            if (other4TrackUri != null) tracks.add(Track("Other4", other4TrackUri))
            return tracks
        }
    var lyrics: String? = null

    @JvmOverloads
    fun cache(overwrite: Boolean = true) {
        if (overwrite) synchronized(TagCache) { TagCache.put(id, SoftReference(this)) }
        CoroutineScope(Dispatchers.IO + Job()).launch {
            synchronized(CacheWriteLock) {
                try {
                    val directory = File(RichApplication.getAppContext().filesDir, "TagCache")
                    directory.mkdir()
                    val cacheFile = File(directory, "" + id)
                    if (cacheFile.exists()) {
                        if (!overwrite) return@launch
                        cacheFile.delete()
                    }
                    val fos = FileOutputStream(cacheFile)
                    val serialized = JsonSerializer.serialize(this@Tag)
                    val pw = PrintWriter(fos)
                    pw.println(serialized)
                    pw.close()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun equals(obj: Any?): Boolean {
        return if (obj == null || obj !is Tag) false else (obj as Tag).id == id
    }

    val keyNote: Note?
        get() {
            if (writtenKey == null) return null
            val noteName: String = writtenKey!!.uppercase(Locale.ENGLISH).replace("MAJOR", "")
                    .replace("MINOR", "").replace(":", "").trim { it <= ' ' }
            var acc = Accidental.Natural
            if (noteName.length > 1) acc = if (noteName[1] == '#') Accidental.Sharp else Accidental.Flat
            return Note.findNote("" + noteName[0], acc, 4)
        }

    val sheetMusicSupportedFormat: Boolean
        get() = true
    val tagUri: String
        get() = getTagUri(id)

    override fun hashCode(): Int = id.hashCode()

    protected fun parseFromXml(element: XmlElement) {
        lastRefreshed = Date()
        for (property in element.elements) {
            try {
                var propValue = property.value
                if (propValue == null || propValue.trim { it <= ' ' }.length == 0) propValue = null
                if (propValue != null) propValue = propValue.trim { it <= ' ' }
                
                when (property.name) {
                    "id" -> if (propValue != null) id = propValue.toInt()
                    "Title" -> title = propValue
                    "AltTitle" -> alternativeTitle = propValue
                    "Version" -> version = propValue
                    "WritKey" -> writtenKey = propValue
                    "Parts" -> if (propValue != null) parts = propValue.toInt()
                    "Type" -> tagType = propValue
                    "Recording" -> recordingMethod = propValue
                    "TeachVid" -> teachingVideo = propValue
                    "Lyrics" -> lyrics = propValue
                    "Notes" -> notes = propValue
                    "Arranger" -> arranger = propValue
                    "ArrWebsite" -> arrangerWebsite = propValue
                    "Arranged" -> if (propValue != null) yearArranged = propValue
                    "SungBy" -> sungBy = propValue
                    "SungWebsite" -> sungByWebsite = propValue
                    "SungYear" -> if (propValue != null) sungYear = propValue
                    "Quartet" -> learningTrackQuartet = propValue
                    "QWebsite" -> learningTrackQuartetWebsite = propValue
                    "Teacher" -> teacher = propValue
                    "TWebsite" -> teacherWebsite = propValue
                    "Provider" -> provider = propValue
                    "ProvWebsite" -> providerWebsite = propValue
                    "Posted" -> posted = parseDate(propValue)
                    "Classic" -> if (propValue != null) classicTagNumber = propValue.toInt()
                    "Rating" -> if (propValue != null) rating = propValue.toDouble()
                    "Downloaded" -> if (propValue != null) downloadCount = propValue.replace(",", "").toInt()
                    "SheetMusic" -> parseRemoteLocation(propValue, property)?.let { sheetMusicUri = it }
                    "Notation" -> parseRemoteLocation(propValue, property)?.let { notationUri = it }
                    "AllParts" -> parseRemoteLocation(propValue, property)?.let { allPartsTrackUri = it }
                    "Bass" -> parseRemoteLocation(propValue, property)?.let { bassTrackUri = it }
                    "Bari" -> parseRemoteLocation(propValue, property)?.let { baritoneTrackUri = it }
                    "Lead" -> parseRemoteLocation(propValue, property)?.let { leadTrackUri = it }
                    "Tenor" -> parseRemoteLocation(propValue, property)?.let { tenorTrackUri = it }
                    "Other1" -> parseRemoteLocation(propValue, property)?.let { other1TrackUri = it }
                    "Other2" -> parseRemoteLocation(propValue, property)?.let { other2TrackUri = it }
                    "Other3" -> parseRemoteLocation(propValue, property)?.let { other3TrackUri = it }
                    "Other4" -> parseRemoteLocation(propValue, property)?.let { other4TrackUri = it }
                    "videos" -> {
                        for (elem in property.elements) {
                            if (elem.name != "video") continue
                            val v = Video()
                            v.parseFromXml(elem)
                            videos!!.add(v)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun parseRemoteLocation(propValue: String?, property: XmlElement): RemoteLocation? {
        if (propValue.isNullOrEmpty()) return null
        return RemoteLocation().apply {
            uri = propValue
            type = property.attribute("type").value
        }
    }

    fun rate(rating: Int): Task<Boolean> {
        return Task.callInBackground {
            val url = URL(String.format(RATING_URI_STRING, id, rating))
            val `is` = url.openStream()
            val br = BufferedReader(InputStreamReader(`is`))
            val value = br.readLine()
            value.trim { it <= ' ' }.lowercase() == "ok"
        }
    }

    override fun toString(): String {
        return String.format("id=%s, title=%s", id, title)
    }

    companion object {
        const val CURRENT_APP_VERSION = 2
        private val TagCache = SparseArray<SoftReference<Tag>?>()
        private val CacheWriteLock = Any()
        private const val API_URI_STRING = "https://www.barbershoptags.com/api.php?client=TagMaster&"
        private const val RATING_URI_STRING = "https://www.barbershoptags.com/api.php?client=TagMaster&action=rate&id=%d&rating=%d"

        internal fun buildQueryUrl(
            query: String?,
            numberOfResults: Int,
            start: Int,
            parts: Int?,
            learning: Boolean?,
            sheetMusic: Boolean?,
            collection: TagCollection?,
            sortBy: TagSortOptions?,
            minimumRating: Double?,
            minimumDownloads: Int?,
            fieldList: String?
        ): URL {
            var effectiveCollection = collection
            val sb = StringBuffer()
            sb.append("n=$numberOfResults")
            if (fieldList != null) sb.append("&fldlist=$fieldList")
            sb.append("&start=" + (start + 1))
            if (!(query == null || query.trim { it <= ' ' } == "")) sb.append("&q=" + Uri.encode(query))
            if (parts != null) sb.append("&Parts=$parts")
            if (learning != null) sb.append("&Learning=" + if (learning) "Yes" else "No")
            if (sheetMusic != null) sb.append("&SheetMusic=" + if (sheetMusic) "Yes" else "No")
            if (sortBy != null) {
                sb.append("&Sortby=")
                when (sortBy) {
                    TagSortOptions.Classic -> {
                        sb.append("Classic")
                        effectiveCollection = TagCollection.ClassicTags
                    }
                    TagSortOptions.Downloaded -> sb.append("Downloaded")
                    TagSortOptions.Posted -> sb.append("Posted")
                    TagSortOptions.Rating -> sb.append("Rating")
                    TagSortOptions.Title -> sb.append("Title")
                }
            }
            if (effectiveCollection != null) {
                sb.append("&Collection=")
                when (effectiveCollection) {
                    TagCollection.ClassicTags -> sb.append("classic")
                    TagCollection.EasyTags -> sb.append("easy")
                }
            }
            if (minimumRating != null) sb.append("&MinRating=$minimumRating")
            if (minimumDownloads != null) sb.append("&MinDownloaded=$minimumDownloads")
            return URL(API_URI_STRING + sb.toString())
        }

        internal fun parseTagQueryResult(inputStream: InputStream, start: Int, cache: Boolean): TagQueryResult {
            val result = TagQueryResult()
            val doc = XmlDocument.parse(inputStream)
            val tags = doc.elements("tags")[0]
            result.available = tags.attribute("available").value.toInt()
            result.count = tags.attribute("count").value.toInt()
            result.start = start

            val resultTags = ArrayList<Tag>()
            for (tagXml in tags.elements("tag")) {
                val t = Tag()
                t.parseFromXml(tagXml)
                resultTags.add(t)
            }
            result.tags = resultTags
            if (cache) for (t in resultTags) t.cache()
            return result
        }

        internal fun buildQueryByIdsUrl(ids: List<Int>): URL {
            return URL("${API_URI_STRING}id=${Uri.encode(ids.joinToString("|"))}&n=${ids.size}")
        }

        internal fun parseTagsByIdResponse(inputStream: InputStream): Map<Int, Tag> {
            val doc = XmlDocument.parse(inputStream)
            val tags = doc.elements("tags")[0]
            val resultTags = mutableMapOf<Int, Tag>()
            for (tagXml in tags.elements("tag")) {
                val t = Tag()
                t.parseFromXml(tagXml)
                resultTags[t.id] = t
            }
            return resultTags
        }

        fun clearCache() {
            val directory = File(RichApplication.getAppContext().filesDir, "TagCache")
            for (f in directory.listFiles().orEmpty()) f.delete()
            synchronized(TagCache) { TagCache.clear() }
        }

        val currentCacheSize: Long
            get() {
                val directory = File(RichApplication.getAppContext().filesDir, "TagCache")
                var total: Long = 0
                for (f in directory.listFiles().orEmpty()) total += f.length()
                return total
            }

        fun loadTagById(id: Int): Task<Tag> {
            return loadTagById(id, false)
        }

        fun loadTagById(
            id: Int,
            refresh: Boolean,
        ): Task<Tag> {
            if (!refresh) {
                val cachedTag = synchronized(TagCache) { TagCache[id]?.get() }
                if (cachedTag != null) return Task.forResult(cachedTag)
                return Task
                    .callInBackground {
                        val directory = File(RichApplication.getAppContext().filesDir, "TagCache")
                        val file = File(directory, id.toString())
                        val tag = JsonSerializer.deserialize(JSONObject(file.readText())) as Tag?
                        if (tag == null || tag.appVersion != CURRENT_APP_VERSION) {
                            throw IllegalStateException("Outdated tag cache")
                        }
                        synchronized(TagCache) { TagCache.put(id, SoftReference(tag)) }
                        tag
                    }.continueWithTask { task ->
                        if (task.isFaulted || task.isCancelled) {
                            loadTagById(id, true)
                        } else {
                            Task.forResult(task.result)
                        }
                    }
            }
            return queryById(id).onSuccess { task ->
                task.result.cache()
                synchronized(TagCache) { TagCache.put(id, SoftReference(task.result)) }
                task.result
            }
        }

        fun query(query: String?): Task<TagQueryResult> {
            return query(query, 10)
        }

        fun query(query: String?, numberOfResults: Int): Task<TagQueryResult> {
            return query(query, numberOfResults, 0)
        }

        fun query(query: String?, numberOfResults: Int, start: Int): Task<TagQueryResult> {
            return query(query, numberOfResults, start, null)
        }

        fun query(query: String?, numberOfResults: Int, start: Int,
                  parts: Int?): Task<TagQueryResult> {
            return query(query, numberOfResults, start, parts, null)
        }

        fun query(query: String?, numberOfResults: Int, start: Int,
                  parts: Int?, learning: Boolean?): Task<TagQueryResult> {
            return query(query, numberOfResults, start, parts, learning, null)
        }

        fun query(query: String?, numberOfResults: Int, start: Int,
                  parts: Int?, learning: Boolean?, sheetMusic: Boolean?): Task<TagQueryResult> {
            return query(query, numberOfResults, start, parts, learning, sheetMusic, null)
        }

        fun query(query: String?, numberOfResults: Int, start: Int,
                  parts: Int?, learning: Boolean?, sheetMusic: Boolean?, collection: TagCollection?): Task<TagQueryResult> {
            return query(query, numberOfResults, start, parts, learning, sheetMusic, collection, null)
        }

        fun query(query: String?, numberOfResults: Int, start: Int,
                  parts: Int?, learning: Boolean?, sheetMusic: Boolean?, collection: TagCollection?,
                  sortBy: TagSortOptions?): Task<TagQueryResult> {
            return query(query, numberOfResults, start, parts, learning, sheetMusic, collection,
                    sortBy, null)
        }

        fun query(query: String?, numberOfResults: Int, start: Int,
                  parts: Int?, learning: Boolean?, sheetMusic: Boolean?, collection: TagCollection?,
                  sortBy: TagSortOptions?, minimumRating: Double?): Task<TagQueryResult> {
            return query(query, numberOfResults, start, parts, learning, sheetMusic, collection,
                    sortBy, minimumRating, null)
        }

        fun query(query: String?, numberOfResults: Int, start: Int,
                  parts: Int?, learning: Boolean?, sheetMusic: Boolean?, collection: TagCollection?,
                  sortBy: TagSortOptions?, minimumRating: Double?, minimumDownloads: Int?): Task<TagQueryResult> {
            return query(query, numberOfResults, start, parts, learning, sheetMusic, collection,
                    sortBy, minimumRating, minimumDownloads, false)
        }

        fun query(query: String?, numberOfResults: Int, start: Int,
                  parts: Int?, learning: Boolean?, sheetMusic: Boolean?, collection: TagCollection?,
                  sortBy: TagSortOptions?, minimumRating: Double?, minimumDownloads: Int?, cache: Boolean): Task<TagQueryResult> {
            return query(
                    query,
                    numberOfResults,
                    start,
                    parts,
                    learning,
                    sheetMusic,
                    collection,
                    sortBy,
                    minimumRating,
                    minimumDownloads,
                    cache,
                    "id,Title,AltTitle,Rating,Posted,Downloaded,SheetMusic,Bass,Bari,Lead,Tenor,Other1,Other2,Other3,Other4")
        }

        @JvmStatic
        fun query(query: String?, numberOfResults: Int, start: Int,
                  parts: Int?, learning: Boolean?, sheetMusic: Boolean?, collection: TagCollection?,
                  sortBy: TagSortOptions?, minimumRating: Double?, minimumDownloads: Int?, cache: Boolean,
                  fieldList: String?): Task<TagQueryResult> {
            return Task.callInBackground {
                val url = buildQueryUrl(
                    query,
                    numberOfResults,
                    start,
                    parts,
                    learning,
                    sheetMusic,
                    collection,
                    sortBy,
                    minimumRating,
                    minimumDownloads,
                    fieldList
                )
                url.openStream().use { inputStream ->
                    parseTagQueryResult(inputStream, start, cache)
                }
            }
        }

        private val pendingQueries: MutableList<Pair<List<Int>, TaskCompletionSource<List<Tag>>>>
                = mutableListOf()
        private val requestSynchronizer = Mutex()
        private val pendingQueriesSynchronizer = Mutex()
        fun queryByIds(ids: List<Int>, cache: Boolean = false): Task<List<Tag>> {
            val tcs = TaskCompletionSource<List<Tag>>()
            CoroutineScope(Dispatchers.IO + Job()).launch {
                pendingQueriesSynchronizer.withLock {
                    pendingQueries.add(ids to tcs)
                }
                delay(50)
                var idsToQuery: List<Pair<List<Int>, TaskCompletionSource<List<Tag>>>>
                pendingQueriesSynchronizer.withLock {
                    if (pendingQueries.isEmpty()) {
                        return@launch
                    }
                    idsToQuery = pendingQueries.toMutableList()
                    pendingQueries.clear()
                }
                requestSynchronizer.withLock {
                    try {
                        val allIds = idsToQuery.flatMap { it.first }
                        val url = buildQueryByIdsUrl(allIds)
                        val resultTags = url.openStream().use { inputStream ->
                            parseTagsByIdResponse(inputStream)
                        }

                        idsToQuery.forEach {
                            val result = it.first.mapNotNull { resultTags[it] }
                            it.second.trySetResult(result)
                        }
                    } catch (e: Exception) {
                        idsToQuery.forEach { it.second.trySetError(e) }
                    }
                    if (cache) tcs.task.await().forEach { it.cache() }
                }
            }
            return tcs.task
        }

        fun queryById(id: Int): Task<Tag> = queryByIds(Arrays.asList(id)).onSuccess { it.result.first() }

        fun getTagUri(tagId: Int): String {
            return String.format("http://tags.depoll.com/tag.php?id=%s",
                    tagId)
        }
    }
}
