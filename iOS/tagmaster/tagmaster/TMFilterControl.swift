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
    @Environment(\.tmInsetRowMargin) private var rowMargin
    /// The row's width, measured; segments until it proves too narrow.
    @State private var available: CGFloat = .infinity

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(filter.title)
                .tmFont(.subheadline)
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
        .listRowInsets(EdgeInsets(top: 0, leading: rowMargin, bottom: 0, trailing: rowMargin))
    }

    /// The width the segments need, as TMFilterControl measured it: each title in
    /// 13-point medium plus 16 points, never under 44; equal widths unless apportioned.
    private var segmentsWidth: CGFloat {
        let font = UIFontMetrics(forTextStyle: .subheadline).scaledFont(for: .systemFont(ofSize: 13, weight: .medium))
        let widths = filter.choices.map { max(44, ceil(($0 as NSString).size(withAttributes: [.font: font]).width) + 16) }
        return filter.apportioned ? widths.reduce(0, +) : (widths.max() ?? 0) * CGFloat(widths.count)
    }

    private var menu: some View {
        TMFilterMenuButton(title: filter.title, choices: filter.choices, selection: $selection)
    }
}

/// The menu the filter falls back to, as UIKit drew it: a gray configured button
/// titled with the choice, a small up/down chevron after it, a menu of the choices
/// (the current one checked) on tap, at least 44 points tall.
struct TMFilterMenuButton: UIViewRepresentable {
    let title: String
    let choices: [String]
    @Binding var selection: Int

    func makeUIView(context: Context) -> UIButton {
        var configuration = UIButton.Configuration.gray()
        configuration.image = UIImage(systemName: "chevron.up.chevron.down")
        configuration.imagePlacement = .trailing
        configuration.imagePadding = 8
        configuration.preferredSymbolConfigurationForImage = UIImage.SymbolConfiguration(textStyle: .caption1)
        configuration.titleTextAttributesTransformer = UIConfigurationTextAttributesTransformer { attributes in
            var attributes = attributes
            attributes.font = UIFont.preferredFont(forTextStyle: .body)
            return attributes
        }
        let button = UIButton(configuration: configuration)
        button.titleLabel?.adjustsFontForContentSizeCategory = true
        button.titleLabel?.numberOfLines = 0
        button.showsMenuAsPrimaryAction = true
        button.accessibilityLabel = title
        return button
    }

    func updateUIView(_ button: UIButton, context: Context) {
        let selected = choices[safe: selection] ?? ""
        if button.title(for: .normal) != selected { button.setTitle(selected, for: .normal) }
        button.accessibilityValue = selected
        let binding = $selection
        button.menu = UIMenu(children: choices.enumerated().map { index, choice in
            UIAction(title: choice, state: index == selection ? .on : .off) { _ in binding.wrappedValue = index }
        })
    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiView button: UIButton, context: Context) -> CGSize? {
        guard let width = proposal.width, width.isFinite else {
            let natural = button.intrinsicContentSize
            return CGSize(width: natural.width, height: max(44, natural.height))
        }
        let fitted = button.systemLayoutSizeFitting(CGSize(width: width, height: UIView.layoutFittingCompressedSize.height),
                                                    withHorizontalFittingPriority: .required,
                                                    verticalFittingPriority: .fittingSizeLevel)
        return CGSize(width: width, height: max(44, fitted.height))
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
