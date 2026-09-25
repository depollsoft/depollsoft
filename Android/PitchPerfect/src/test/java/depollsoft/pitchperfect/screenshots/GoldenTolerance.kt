package depollsoft.pitchperfect.screenshots

import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions

/**
 * How far a capture may drift from its golden before it counts as changed. The goldens are
 * recorded on macOS and verified on Linux in CI, where Robolectric resamples the score background
 * a level or two differently and can round one edge pixel the other way. A colour distance of
 * 0.02 (about 5 of 255 levels) and 0.001% of the pixels (about 25 on a phone) absorb that, while a
 * changed glyph, colour or position, hundreds of pixels at full strength, still fails.
 */
val GOLDEN_TOLERANCE =
    RoborazziOptions(
        compareOptions =
            RoborazziOptions.CompareOptions(
                changeThreshold = 0.00001f,
                imageComparator = SimpleImageComparator(maxDistance = 0.02f),
            ),
    )
