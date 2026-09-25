//
//  TMFilterControl.swift
//  tagmaster
//
//  A labelled set of choices, as Search and Settings present their filters:
//  segments when they fit, a menu button when they do not (a narrow column or
//  an accessibility text size). Either way the selection is the same index.
//

import SwiftUI
import UIKit

struct TMFilter: Identifiable, Equatable {
    let title: String
    let choices: [String]
    /// Segments sized to their titles (UISegmentedControl's apportionsSegmentWidthsByContent).
    var apportioned = true
    var id: String { title }
}

struct TMFilterRow: View {
    let filter: TMFilter
    @Binding var selection: Int
    @Environment(\.dynamicTypeSize) private var typeSize
    /// The row's width, measured; segments until it proves too narrow.
    @State private var available: CGFloat = .infinity

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(filter.title)
                .font(TMTheme.font(.subheadline))
                .foregroundStyle(Color(uiColor: .secondaryLabel))
                .tmLabelMetrics(.subheadline)
                .accessibilityHidden(true)
            Group {
                if typeSize.isAccessibilitySize || available < segmentsWidth {
                    menu
                } else {
                    TMSegmentedControl(title: filter.title, choices: filter.choices, apportioned: filter.apportioned,
                                       width: available, selection: $selection)
                }
            }
            .frame(maxWidth: .infinity, minHeight: 44)
            .background {
                GeometryReader { proxy in
                    Color.clear
                        .onAppear { available = proxy.size.width }
                        .onChange(of: proxy.size.width) { _, width in available = width }
                }
            }
        }
        // The old cell's stack sat 12pt below the cell top, which starts a point
        // under the separator above; SwiftUI's row starts at the separator.
        .padding(.top, 13)
        .padding(.bottom, 12)
        .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
    }

    /// The width the segments need, as TMFilterControl measured it: each title in
    /// 13-point medium plus 16 points, never under 44; equal widths unless apportioned.
    private var segmentsWidth: CGFloat {
        let font = UIFontMetrics(forTextStyle: .subheadline).scaledFont(for: .systemFont(ofSize: 13, weight: .medium))
        let widths = filter.choices.map { max(44, ceil(($0 as NSString).size(withAttributes: [.font: font]).width) + 16) }
        return filter.apportioned ? widths.reduce(0, +) : (widths.max() ?? 0) * CGFloat(widths.count)
    }

    private var menu: some View {
        Menu {
            Picker(filter.title, selection: $selection) {
                ForEach(Array(filter.choices.enumerated()), id: \.offset) { index, choice in
                    Text(choice).tag(index)
                }
            }
        } label: {
            HStack(spacing: 8) {
                Text(filter.choices[safe: selection] ?? "").font(TMTheme.font(.body))
                Image(systemName: "chevron.up.chevron.down").font(TMTheme.font(.caption1))
            }
            .frame(maxWidth: .infinity, minHeight: 44)
        }
        .buttonStyle(.bordered)
        .accessibilityLabel(filter.title)
        .accessibilityValue(filter.choices[safe: selection] ?? "")
    }
}

/// UISegmentedControl, for the one thing SwiftUI's segmented picker cannot do:
/// size segments to their titles and share out the rest of the row.
struct TMSegmentedControl: UIViewRepresentable {
    /// Spoken with each segment, as the option's name.
    let title: String
    let choices: [String]
    let apportioned: Bool
    let width: CGFloat
    @Binding var selection: Int

    func makeUIView(context: Context) -> UISegmentedControl {
        let control = UISegmentedControl(items: choices)
        control.apportionsSegmentWidthsByContent = apportioned
        control.accessibilityLabel = title
        control.addTarget(context.coordinator, action: #selector(Coordinator.changed(_:)), for: .valueChanged)
        return control
    }

    func updateUIView(_ control: UISegmentedControl, context: Context) {
        context.coordinator.selection = $selection
        if control.selectedSegmentIndex != selection { control.selectedSegmentIndex = selection }
        // Fill the row: each segment its title's width plus an equal share of what is left.
        let font = UIFontMetrics(forTextStyle: .subheadline).scaledFont(for: .systemFont(ofSize: 13, weight: .medium),
                                                                       compatibleWith: control.traitCollection)
        let widths = choices.map { max(44, ceil(($0 as NSString).size(withAttributes: [.font: font]).width) + 16) }
        let total = apportioned ? widths.reduce(0, +) : (widths.max() ?? 0) * CGFloat(widths.count)
        guard width.isFinite, width >= total, !widths.isEmpty else { return }
        for (index, natural) in widths.enumerated() {
            let segment = apportioned ? natural + (width - total) / CGFloat(widths.count) : width / CGFloat(widths.count)
            if abs(control.widthForSegment(at: index) - segment) > 0.01 { control.setWidth(segment, forSegmentAt: index) }
        }
    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: UISegmentedControl, context: Context) -> CGSize? {
        CGSize(width: proposal.width ?? (width.isFinite ? width : uiView.intrinsicContentSize.width),
               height: max(44, uiView.intrinsicContentSize.height))
    }

    func makeCoordinator() -> Coordinator { Coordinator(selection: $selection) }

    final class Coordinator: NSObject {
        var selection: Binding<Int>
        init(selection: Binding<Int>) { self.selection = selection }
        @objc func changed(_ control: UISegmentedControl) { selection.wrappedValue = control.selectedSegmentIndex }
    }
}

extension Array {
    subscript(safe index: Int) -> Element? { indices.contains(index) ? self[index] : nil }
}
