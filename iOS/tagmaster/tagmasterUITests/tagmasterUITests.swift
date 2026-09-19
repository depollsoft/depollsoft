import XCTest

/**
 * The one XCUITest that cannot run in-process: XCTApplicationLaunchMetric
 * measures a real app launch. Everything else this file used to assert about
 * the main screen, search, favourites and navigation now lives in
 * tagmasterTests (FavoritesBehaviorTests, SearchBehaviorTests, AppPolishTests).
 */
// Keep three launch measurements, each with its own case budget.
// This class has no per-case setup launch before XCTest's measurement warmup.
final class TagMasterLaunchPerformanceUITests: TagMasterUITestCase {
    func testLaunchPerformanceFirstSample() { measureLaunch() }
    func testLaunchPerformanceSecondSample() { measureLaunch() }
    func testLaunchPerformanceThirdSample() { measureLaunch() }

    private func measureLaunch() {
        if #available(macOS 10.15, iOS 13.0, tvOS 13.0, watchOS 7.0, *) {
            let options = XCTMeasureOptions()
            options.iterationCount = 1
            measure(metrics: [XCTApplicationLaunchMetric()], options: options) {
                XCUIApplication().launch()
            }
        }
    }
}
