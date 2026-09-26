package depollsoft.lib.testing

import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions

/**
 * How far a capture may drift from its golden before it counts as changed, for every app's
 * screenshot tests. The goldens are recorded on macOS and verified on Linux in CI, where
 * Robolectric resamples Pitch Perfect's score background a level or two differently and can round
 * one edge pixel the other way.
 *
 * - A colour distance of 0.02 (differ's RGBA distance, about 5 of 255 levels in one channel)
 *   absorbs the resampling. A colour change bigger than that still fails.
 * - 0.0001% of the pixels may differ beyond that: 4 on a 2560x1600 tablet golden, 2 on a 420dpi
 *   phone, none on the watch. That covers the edge pixel; a removed decimal point on the tablet
 *   golden (12 pixels) still fails.
 */
val GOLDEN_TOLERANCE =
    RoborazziOptions(
        compareOptions =
            RoborazziOptions.CompareOptions(
                changeThreshold = 0.000001f,
                imageComparator = SimpleImageComparator(maxDistance = 0.02f),
            ),
    )
