package depollsoft.tagmaster

import android.content.res.Resources
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import androidx.core.graphics.PathParser

/** Native contours from ic_barberpole.xml, the canonical 299.75076 × 513.52234 logo.
 * The watermark is never changed. XML/path parsing happens once per view, not per frame.
 */
internal class BarberPoleLogo(
    resources: Resources,
) {
    val metal: Path
    val shaft: Path
    val stripe: Path

    init {
        val data =
            resources.getXml(R.drawable.ic_barberpole).use { xml ->
                while (xml.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    if (xml.eventType == org.xmlpull.v1.XmlPullParser.START_TAG && xml.name == "path") break
                }
                xml.getAttributeValue("http://schemas.android.com/apk/res/android", "pathData")
            }
        val source = requireNotNull(PathParser.createPathFromPathData(data))
        val measure = PathMeasure(source, false)
        val contours = mutableListOf<Path>()
        do {
            contours.add(
                Path().apply {
                    measure.getSegment(0f, measure.length, this, true)
                    close()
                },
            )
        } while (measure.nextContour())
        check(contours.size == 9) { "Canonical barber pole contour structure changed" }
        metal =
            Path(contours[0]).apply {
                // Keep the original curved ball and collar highlight cutouts.
                for (index in intArrayOf(1, 2, 7, 8)) op(contours[index], Path.Op.DIFFERENCE)
            }
        // Connect the canonical top stripe's upper curve to the bottom shaft cutout.
        // Side rails follow the logo's slant; only this inset cylinder receives color.
        shaft =
            requireNotNull(
                PathParser.createPathFromPathData(
                    "M159.01,138.81 C190,131 228,127 253.47,140.72 " +
                        "L122.06,413.54 C105,404 65,387 43,379 L159.01,138.81 Z",
                ),
            )
        stripe =
            Path(contours[5]).apply {
                // Reuse the original helical curve in axial coordinates. Extend only the hidden
                // stripe ends across the rails so small source irregularities cannot leave seams.
                transform(Matrix().apply { setRotate(-AXIS_ANGLE) })
                val bounds = RectF()
                computeBounds(bounds, true)
                transform(Matrix().apply { setScale(1.4f, 1f, bounds.centerX(), bounds.centerY()) })
            }
    }

    companion object {
        const val WIDTH = 299.75076f
        const val HEIGHT = 513.52234f
        const val AXIS_ANGLE = 26f
        const val STRIPE_STEP = 108f
    }
}
