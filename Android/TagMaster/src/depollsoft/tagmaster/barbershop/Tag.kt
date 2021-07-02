package depollsoft.tagmaster.barbershop

import android.net.Uri
import com.bindroid.trackable.*
import kotlin.jvm.JvmOverloads
import depollsoft.lib.activity.RichApplication
import org.json.JSONObject
import depollsoft.pitchperfect.lib.Accidental
import com.bindroid.trackable.TrackableCollection
import depollsoft.lib.xml.XmlElement
import android.util.SparseArray
import bolts.Task
import depollsoft.lib.json.JsonSerializer
import depollsoft.lib.xml.XmlDocument
import depollsoft.pitchperfect.lib.Note
import java.io.*
import java.lang.Exception
import java.lang.ref.SoftReference
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

class Tag {
    var appVersion: Int by trackable()
    var id: Int by trackable(0)
    var title: String? by trackable()
    var lastRefreshed: Date by trackable()
    var alternativeTitle: String? by trackable()
    var version: String? by trackable()
    var writtenKey: String? by trackable()
    var parts: Int by trackable()
    var tagType: String? by trackable()
    var recordingMethod: String? by trackable()
    var teachingVideo: String? by trackable()
    var notes: String? by trackable()
    var arranger: String? by trackable()
    var arrangerWebsite: String? by trackable()
    var yearArranged: String? by trackable()
    var sungBy: String? by trackable()
    var sungByWebsite: String? by trackable()
    var sungYear: String? by trackable()
    var learningTrackQuartet: String? by trackable()
    var learningTrackQuartetWebsite: String? by trackable()
    var teacher: String? by trackable()
    var teacherWebsite: String? by trackable()
    var provider: String? by trackable()
    var providerWebsite: String? by trackable()
    var posted: Date? by trackable()
    var classicTagNumber: Int? by trackable()
    var rating: Double? by trackable()
    var downloadCount: Int by trackable()
    var sheetMusicUri: RemoteLocation? by trackable()
    var notationUri: RemoteLocation? by trackable()
    var allPartsTrackUri: RemoteLocation? by trackable()
    var bassTrackUri: RemoteLocation? by trackable()
    var baritoneTrackUri: RemoteLocation? by trackable()
    var leadTrackUri: RemoteLocation? by trackable()
    var tenorTrackUri: RemoteLocation? by trackable()
    var other1TrackUri: RemoteLocation? by trackable()
    var other2TrackUri: RemoteLocation? by trackable()
    var other3TrackUri: RemoteLocation? by trackable()
    var other4TrackUri: RemoteLocation? by trackable()
    var videos: MutableList<Video>? by trackable(mutableListOf<Video>())
    val tracks: List<Track>?
        get() {
            val tracks = TrackableCollection<Track>()
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
    var lyrics by TrackableField<String?>()

    @JvmOverloads
    fun cache(overwrite: Boolean = true) {
        if (overwrite) TagCache.put(id, SoftReference(this))
        val t: Thread = object : Thread() {
            override fun run() {
                synchronized(CacheWriteLock) {
                    try {
                        val directory = File(RichApplication.getAppContext().filesDir, "TagCache")
                        directory.mkdir()
                        val cacheFile = File(directory, "" + id)
                        if (cacheFile.exists()) {
                            if (!overwrite) return
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
        t.start()
    }

    override fun equals(obj: Any?): Boolean {
        return if (obj == null || obj !is Tag) false else (obj as Tag).id == id
    }

    val keyNote: Note?
        get() {
            if (writtenKey == null) return null
            val noteName: String = writtenKey!!.toUpperCase(Locale.ENGLISH).replace("MAJOR", "")
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
                if (property.name == "id" && propValue != null) id = propValue.toInt() else if (property.name == "Title") title = propValue else if (property.name == "AltTitle") alternativeTitle = propValue else if (property.name == "Version") version = propValue else if (property.name == "WritKey") writtenKey = propValue else if (property.name == "Parts" && propValue != null) parts = propValue.toInt() else if (property.name == "Type") tagType = propValue else if (property.name == "Recording") recordingMethod = propValue else if (property.name == "TeachVid") teachingVideo = propValue else if (property.name == "Lyrics") lyrics = propValue else if (property.name == "Notes") notes = propValue else if (property.name == "Arranger") arranger = propValue else if (property.name == "ArrWebsite") arrangerWebsite = propValue else if (property.name == "Arranged" && propValue != null) yearArranged = propValue else if (property.name == "SungBy") sungBy = propValue else if (property.name == "SungWebsite") sungByWebsite = propValue else if (property.name == "SungYear" && propValue != null) sungYear = propValue else if (property.name == "Quartet") learningTrackQuartet = propValue else if (property.name == "QWebsite") learningTrackQuartetWebsite = propValue else if (property.name == "Teacher") teacher = propValue else if (property.name == "TWebsite") teacherWebsite = propValue else if (property.name == "Provider") provider = propValue else if (property.name == "ProvWebsite") providerWebsite = propValue else if (property.name == "Posted") posted = Date(propValue) else if (property.name == "Classic" && propValue != null) classicTagNumber = propValue.toInt() else if (property.name == "Rating" && propValue != null) rating = propValue.toDouble() else if (property.name == "Downloaded" && propValue != null) downloadCount = propValue.replace(",", "").toInt() else if (property.name == "SheetMusic") {
                    if (!(propValue == null || propValue.length == 0)) {
                        val rl = RemoteLocation()
                        rl.uri = propValue
                        rl.type = property.attribute("type").value
                        sheetMusicUri = rl
                    }
                } else if (property.name == "Notation") {
                    if (!(propValue == null || propValue.length == 0)) {
                        val rl = RemoteLocation()
                        rl.uri = propValue
                        rl.type = property.attribute("type").value
                        notationUri = rl
                    }
                } else if (property.name == "AllParts") {
                    if (!(propValue == null || propValue.length == 0)) {
                        val rl = RemoteLocation()
                        rl.uri = propValue
                        rl.type = property.attribute("type").value
                        allPartsTrackUri = rl
                    }
                } else if (property.name == "Bass") {
                    if (!(propValue == null || propValue.length == 0)) {
                        val rl = RemoteLocation()
                        rl.uri = propValue
                        rl.type = property.attribute("type").value
                        bassTrackUri = rl
                    }
                } else if (property.name == "Bari") {
                    if (!(propValue == null || propValue.length == 0)) {
                        val rl = RemoteLocation()
                        rl.uri = propValue
                        rl.type = property.attribute("type").value
                        baritoneTrackUri = rl
                    }
                } else if (property.name == "Lead") {
                    if (!(propValue == null || propValue.length == 0)) {
                        val rl = RemoteLocation()
                        rl.uri = propValue
                        rl.type = property.attribute("type").value
                        leadTrackUri = rl
                    }
                } else if (property.name == "Tenor") {
                    if (!(propValue == null || propValue.length == 0)) {
                        val rl = RemoteLocation()
                        rl.uri = propValue
                        rl.type = property.attribute("type").value
                        tenorTrackUri = rl
                    }
                } else if (property.name == "Other1") {
                    if (!(propValue == null || propValue.length == 0)) {
                        val rl = RemoteLocation()
                        rl.uri = propValue
                        rl.type = property.attribute("type").value
                        other1TrackUri = rl
                    }
                } else if (property.name == "Other2") {
                    if (!(propValue == null || propValue.length == 0)) {
                        val rl = RemoteLocation()
                        rl.uri = propValue
                        rl.type = property.attribute("type").value
                        other2TrackUri = rl
                    }
                } else if (property.name == "Other3") {
                    if (!(propValue == null || propValue.length == 0)) {
                        val rl = RemoteLocation()
                        rl.uri = propValue
                        rl.type = property.attribute("type").value
                        other3TrackUri = rl
                    }
                } else if (property.name == "Other4") {
                    if (!(propValue == null || propValue.length == 0)) {
                        val rl = RemoteLocation()
                        rl.uri = propValue
                        rl.type = property.attribute("type").value
                        other4TrackUri = rl
                    }
                } else if (property.name == "videos") {
                    for (elem in property.elements) {
                        if (elem.name != "video") continue
                        val v = Video()
                        v.parseFromXml(elem)
                        videos!!.add(v)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun rate(rating: Int): Task<Boolean> {
        return Task.callInBackground {
            val url = URL(String.format(RATING_URI_STRING, id, rating))
            val `is` = url.openStream()
            val br = BufferedReader(InputStreamReader(`is`))
            val value = br.readLine()
            value.trim { it <= ' ' }.toLowerCase() == "ok"
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
        fun clearCache() {
            val directory = File(RichApplication.getAppContext().filesDir, "TagCache")
            for (f in directory.listFiles()) f.delete()
            TagCache.clear()
        }

        val currentCacheSize: Long
            get() {
                val directory = File(RichApplication.getAppContext().filesDir, "TagCache")
                var total: Long = 0
                for (f in directory.listFiles()) total += f.length()
                return total
            }

        fun loadTagById(id: Int): Task<Tag> {
            return loadTagById(id, false)
        }

        fun loadTagById(id: Int, refresh: Boolean): Task<Tag> {
            val directory = File(RichApplication.getAppContext().filesDir, "TagCache")
            val file = File(directory, "" + id)
            val loadedFromCache = false
            if (!refresh && TagCache[id] != null) {
                val cachedTag = TagCache[id]!!.get()
                if (cachedTag != null) {
                    return Task.forResult(cachedTag)
                }
            }
            return if (!loadedFromCache && !refresh && file.exists()) {
                try {
                    val inputStream: InputStream = FileInputStream(file)
                    var buffer: ByteArray? = ByteArray(file.length().toInt())
                    inputStream.read(buffer)
                    inputStream.close()
                    val cachedTag = String(buffer!!)
                    buffer = null
                    val jsonObj = JSONObject(cachedTag)
                    val tag = JsonSerializer.deserialize(jsonObj) as Tag
                    if (tag == null || tag.appVersion != CURRENT_APP_VERSION) throw Exception()
                    TagCache.put(id, SoftReference(tag))
                    Task.forResult(tag)
                } catch (e: Exception) {
                    loadTagById(id, true)
                }
            } else {
                queryById(id).onSuccess { task ->
                    task.result.cache()
                    TagCache.put(id, SoftReference(task.result))
                    task.result
                }
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
            var collection = collection
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
                        collection = TagCollection.ClassicTags
                    }
                    TagSortOptions.Downloaded -> sb.append("Downloaded")
                    TagSortOptions.Posted -> sb.append("Posted")
                    TagSortOptions.Rating -> sb.append("Rating")
                    TagSortOptions.Title -> sb.append("Title")
                }
            }
            if (collection != null) {
                sb.append("&Collection=")
                when (collection) {
                    TagCollection.ClassicTags -> sb.append("classic")
                    TagCollection.EasyTags -> sb.append("easy")
                }
            }
            if (minimumRating != null) sb.append("&MinRating=$minimumRating")
            if (minimumDownloads != null) sb.append("&MinDownloaded=$minimumDownloads")
            return Task.callInBackground {
                val url = URL(API_URI_STRING + sb.toString())
                val result = TagQueryResult()
                val `is` = url.openStream()
                val doc = XmlDocument.parse(`is`)
                val tags = doc.elements("tags")[0]
                result.available = tags.attribute("available").value.toInt()
                result.count = tags.attribute("count").value.toInt()
                result.start = start
                val resultTags = ArrayList<Tag>()
                for (tagXml in tags.elements) {
                    val t = Tag()
                    t.parseFromXml(tagXml)
                    resultTags.add(t)
                }
                result.tags = resultTags
                if (cache) for (t in resultTags) t.cache()
                result
            }
        }

        fun queryByIds(ids: List<Int>, cache: Boolean = false): Task<List<Tag>> {
            return Task.callInBackground {
                val url = URL(API_URI_STRING + "id=" + Uri.encode(ids.joinToString("|")))
                val `is` = url.openStream()
                val doc = XmlDocument.parse(`is`)
                val tags = doc.elements("tags")[0]
                val resultTags = ArrayList<Tag>()
                for (tagXml in tags.elements) {
                    val t = Tag()
                    t.parseFromXml(tags.elements("tag")[0])
                    if (cache) {
                        t.cache()
                    }
                    resultTags.add(t)
                }
                resultTags
            }
        }

        fun queryById(id: Int): Task<Tag> = queryByIds(Arrays.asList(id)).onSuccess { it.result.first() }

        fun getTagUri(tagId: Int): String {
            return String.format("http://tags.depoll.com/tag.php?id=%s",
                    tagId)
        }
    }
}