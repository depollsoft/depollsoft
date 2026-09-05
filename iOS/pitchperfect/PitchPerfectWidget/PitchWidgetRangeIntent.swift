import AppIntents
import WidgetKit

/// Compiled into the widget extension only, so it always runs in the widget
/// process and WidgetKit's own post-interaction reload redraws the face.
struct SelectWidgetRangeIntent: AppIntent {
    static let title: LocalizedStringResource = "Select Pitch Pipe Range"
    static let openAppWhenRun = false

    @Parameter(title: "Range")
    var rangeRawValue: String

    init() {}

    init(range: PitchRange) {
        rangeRawValue = range.rawValue
    }

    func perform() async throws -> some IntentResult {
        WidgetPlaybackBridge.requestStop()
        WidgetRangeState.set(rangeRawValue)
        WidgetPitchState.set(nil)
        WidgetDiagnostics.record("range \(rangeRawValue)")
        return .result()
    }
}
