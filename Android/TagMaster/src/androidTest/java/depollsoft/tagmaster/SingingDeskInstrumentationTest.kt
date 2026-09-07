package depollsoft.tagmaster

import org.junit.runner.RunWith
import org.junit.runners.Suite

/** Run on compact and >=600dp windows; also repeat with dark theme and large font scale. */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    MeActivityTest::class,
    FavoritesFlowTest::class,
    TagSearchActivityTest::class,
    TagDetailActivityTest::class,
    DeskChildScreensTest::class,
)
class SingingDeskInstrumentationTest
