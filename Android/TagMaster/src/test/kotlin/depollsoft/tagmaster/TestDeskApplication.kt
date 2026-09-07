package depollsoft.tagmaster

import depollsoft.lib.activity.RichApplication

/**
 * The shared application base without the Firebase wiring.
 *
 * Screen tests need what [RichApplication] sets up — the app context that
 * preferences and version tracking read from — but not an authentication
 * backend, which a JVM test has no business reaching.
 */
class TestDeskApplication : RichApplication()
