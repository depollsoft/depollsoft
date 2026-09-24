package depollsoft.tagmaster.screenshots

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Looper
import depollsoft.lib.util.Preferences
import depollsoft.tagmaster.FavoritesModel
import depollsoft.tagmaster.ListModel
import depollsoft.tagmaster.ScreenTestSupport
import depollsoft.tagmaster.TagLists
import depollsoft.tagmaster.TeachableTagsModel
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.barbershop.Video
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.GregorianCalendar
import java.util.TimeZone

/** Deterministic data for the screenshot suite: fixed tags, lists and catalog pages. */
internal object ScreenshotFixtures {
    const val HEART = 2147483101
    const val SUNSHINE = 2147483102
    const val SWEET = 2147483103

    private fun location(
        part: String,
        type: String = "mp3",
    ) = RemoteLocation().apply {
        uri = "https://example.invalid/screenshot-$part.$type"
        this.type = type
    }

    private fun date(
        year: Int,
        month: Int,
        day: Int,
    ) = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month - 1, day, 12, 0)
    }.time

    /** A tag with every field the detail pages show filled in. */
    fun heart(): Tag =
        Tag().apply {
            id = HEART
            title = "Heart of My Heart"
            alternativeTitle = "The Story of the Rose"
            version = "Barbershop"
            writtenKey = "Bb"
            parts = 4
            tagType = "Barbershop"
            classicTagNumber = 12
            rating = 4.26
            downloadCount = 48213
            posted = date(2008, 12, 7)
            lastRefreshed = date(2026, 9, 1)
            arranger = "Val Hicks"
            arrangerWebsite = "https://example.invalid/arranger"
            yearArranged = "1987"
            sungBy = "The Buffalo Bills"
            sungByWebsite = "https://example.invalid/bills"
            sungYear = "1950"
            provider = "Barbershop Tags"
            providerWebsite = "https://example.invalid/provider"
            teacher = "Tim Waurick"
            teachingVideo = "teach123"
            recordingMethod = "Each part recorded separately, with the melody panned left."
            notes = "Learned at every afterglow. Hold the post as long as the bass can breathe."
            lyrics = "Heart of my heart, I love that melody.\nHeart of my heart brings back a memory."
            sheetMusicUri = location("sheet", "pdf")
            allPartsTrackUri = location("all")
            tenorTrackUri = location("tenor")
            leadTrackUri = location("lead")
            baritoneTrackUri = location("baritone")
            bassTrackUri = location("bass")
            videos =
                mutableListOf(
                    Video().apply {
                        id = 1
                        sungBy = "Main Street"
                        sungKey = "Bb"
                        isMultitrack = false
                        youTubeCode = "video-one"
                        posted = date(2019, 4, 2)
                    },
                    Video().apply {
                        id = 2
                        sungBy = "A one-person quartet"
                        sungKey = "A"
                        isMultitrack = true
                        youTubeCode = "video-two"
                        posted = date(2021, 11, 20)
                    },
                )
        }

    /** A sparse tag: no rating, tracks, videos, sheet music or prose. */
    fun sunshine(): Tag =
        Tag().apply {
            id = SUNSHINE
            title = "You Are My Sunshine"
            writtenKey = "F"
            parts = 4
            tagType = "Barbershop"
            downloadCount = 912
            posted = date(2014, 6, 30)
            lastRefreshed = date(2026, 9, 1)
            videos = mutableListOf()
        }

    fun sweet(): Tag =
        Tag().apply {
            id = SWEET
            title = "Sweet Adeline, the one I dream of when the harmonies ring all night long"
            alternativeTitle = "You're the Flower of My Heart"
            writtenKey = "C"
            parts = 4
            rating = 3.5
            downloadCount = 2290
            posted = date(2011, 2, 14)
            lastRefreshed = date(2026, 9, 1)
            sheetMusicUri = location("sweet-sheet", "pdf")
            allPartsTrackUri = location("sweet-all")
            videos = mutableListOf()
        }

    /** Writes every fixture tag into the disk cache the screens read. */
    fun cacheTags() {
        for (tag in listOf(heart(), sunshine(), sweet())) ScreenTestSupport.cacheOnDisk(tag)
    }

    /** Favorites, a teachable tag and two custom lists (one empty). */
    fun populateLists(): Pair<String, String> {
        FavoritesModel.addFavorite(HEART)
        FavoritesModel.addFavorite(SWEET)
        FavoritesModel.addFavorite(SUNSHINE)
        TeachableTagsModel.addTeachableTag(HEART)
        val afterglow = TagLists.create("Afterglow set")
        ListModel(afterglow).add(HEART)
        ListModel(afterglow).add(SWEET)
        val easy = TagLists.create("Easy tags")
        return afterglow to easy
    }

    /** Lets background tag loads finish and the looper settle. */
    fun settle(rounds: Int = 40) {
        repeat(rounds) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(15)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Removes the changelog and privacy prompts that launch screens open over themselves. */
    fun dismissStartupDialogs() {
        ShadowDialog.getShownDialogs().filter { it.isShowing }.forEach { it.dismiss() }
        shadowOf(Looper.getMainLooper()).idle()
    }

    fun choosePrivacy(activity: Activity) {
        depollsoft.lib.privacy.PrivacyChoices(activity).save(analytics = false, crashes = false)
    }

    fun forgetPrivacy() {
        RuntimeEnvironment
            .getApplication()
            .getSharedPreferences("telemetry_consent", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    /** One page of the catalog, in the XML shape barbershoptags.com returns. */
    fun catalogPage(
        url: URL,
        available: Int = 26,
        pageSize: Int = 20,
    ): InputStream {
        val start =
            Regex("[?&]start=(\\d+)")
                .find(url.toString())
                ?.groupValues
                ?.get(1)
                ?.toInt()
                ?.minus(1) ?: 0
        val count = maxOf(0, minOf(pageSize, available - start))
        val titles =
            listOf(
                "Heart of My Heart",
                "You Are My Sunshine",
                "Sweet Adeline",
                "The Old Songs",
                "Down Our Way",
                "Shenandoah",
                "Blue Skies",
                "When You Were Sweet Sixteen",
            )
        val body =
            buildString {
                append("<tags count=\"$count\" available=\"$available\">")
                for (index in start until start + count) {
                    append("<tag>")
                    append("<id>${2147483200 + index}</id>")
                    append("<Title>${titles[index % titles.size]} ${index + 1}</Title>")
                    if (index % 3 == 0) append("<AltTitle>Also known as number ${index + 1}</AltTitle>")
                    append("<Parts>4</Parts><WritKey>C</WritKey>")
                    append("<Rating>${3 + (index % 20) / 10.0}</Rating>")
                    append("<Downloaded>${1000 + index * 37}</Downloaded>")
                    append("<Posted>Sun, ${1 + index % 27} Jan 2012</Posted>")
                    if (index % 2 == 0) append("<SheetMusic>https://example.invalid/s$index.pdf</SheetMusic>")
                    if (index % 4 == 1) append("<AllParts>https://example.invalid/a$index.mp3</AllParts>")
                    append("</tag>")
                }
                append("</tags>")
            }
        return ByteArrayInputStream(body.toByteArray(Charsets.UTF_8))
    }

    /** A page of sheet music as an image file, for the sheet-music viewer. */
    fun sheetImage(): File {
        val bitmap = Bitmap.createBitmap(850, 1100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = 2f }
        for (system in 0 until 4) {
            val top = 120f + system * 240f
            for (line in 0 until 5) {
                val y = top + line * 16f
                canvas.drawLine(60f, y, 790f, y, paint)
            }
            for (note in 0 until 12) {
                canvas.drawCircle(110f + note * 58f, top + 8f * ((note * 3) % 9), 8f, paint)
            }
        }
        val file = File(RuntimeEnvironment.getApplication().cacheDir, "screenshot-sheet.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }
}
