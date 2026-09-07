//
//  TMTheme.swift
//  tagmaster
//
//  ── Tag Master refresh contract ───────────────────────────────────────────────
//  Thesis:  Tag Master is a singing desk, not a catalog. Everything a group needs
//           to pick a tag and sing it together is one reach away; study material
//           sits behind that, never in front of it.
//  World:   Charcoal ink and barbershop blue, the wickhop handwriting wordmark,
//           and the barber pole shrunk from a page-filling watermark to a quiet
//           mark at the foot of the desk. Body copy is San Francisco through
//           semantic tokens so light, dark, and large text all hold. This is Tag
//           Master's own character; Pitch Perfect's instrument aesthetic is not
//           imported here.
//  Home:    Find a tag · Random Tag · Open Tag ID · Teachable Tags, then the
//           Favorites list itself. Settings is a utility in the navigation bar,
//           never a desk action. Home / Browse / Search are the destinations.
//  Tablet:  Persistent native tabs, a tiled sidebar in wide windows,
//           readable content widths everywhere, and a split tag detail that keeps
//           the summary on screen while details, tracks, or videos sit beside it.
//  Seed:    a858fb14 candidate 6 — established identity, refined not replaced.
//  Finish: listed native review fixes resolved; app-scoped DESIGN.md records
//  the shipped UI. Hardware and account verification remain separate.
//  ─────────────────────────────────────────────────────────────────────────────
//

import UIKit

/// Semantic design tokens for Tag Master. Every colour resolves per trait
/// collection for light and dark appearances. System fonts use preferred
/// descriptors; only the custom wordmark uses UIFontMetrics.
@objc(TMTheme)
final class TMTheme: NSObject {

    // MARK: - Colour

    /// Charcoal ink — the app's own voice for headings and the wordmark.
    @objc static let ink = UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(white: 0.96, alpha: 1)
            : UIColor(red: 0.216, green: 0.216, blue: 0.216, alpha: 1) // #373737
    }

    /// The one interactive tint. Barbershop blue, darkened in light mode so body
    /// links clear 4.5:1 and lifted in dark mode so they clear it there too.
    @objc static let tint = UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 0.361, green: 0.780, blue: 0.941, alpha: 1) // #5CC7F0
            : UIColor(red: 0.000, green: 0.404, blue: 0.576, alpha: 1) // #006793
    }

    /// Body text.
    @objc static let primaryText = UIColor.label
    /// Supporting text: metadata rows, footnotes, empty-state prose.
    @objc static let secondaryText = UIColor.secondaryLabel
    /// Page canvas behind grouped content.
    @objc static let canvas = UIColor.systemGroupedBackground
    /// Raised content — list rows, cards, panes.
    @objc static let surface = UIColor.secondarySystemGroupedBackground
    /// Hairlines.
    @objc static let separator = UIColor.separator
    /// A positive state marker (this tag has sheet music / tracks).
    @objc static let affirmative = UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 0.36, green: 0.82, blue: 0.55, alpha: 1)
            : UIColor(red: 0.09, green: 0.47, blue: 0.27, alpha: 1)
    }

    // MARK: - Spacing

    @objc static let spaceXS: CGFloat = 4
    @objc static let spaceS: CGFloat = 8
    @objc static let spaceM: CGFloat = 12
    @objc static let spaceL: CGFloat = 16
    @objc static let spaceXL: CGFloat = 24
    /// Apple's minimum comfortable target.
    @objc static let minimumTarget: CGFloat = 44

    // MARK: - Type

    /// The handwriting wordmark, scaled against the requested text style so it
    /// grows with the reader's setting instead of sitting at a frozen 20pt.
    @objc(wordmarkFontWithSize:textStyle:)
    static func wordmarkFont(size: CGFloat, textStyle: UIFont.TextStyle) -> UIFont {
        let base = UIFont(name: "wickhop handwriting", size: size)
            ?? UIFont.systemFont(ofSize: size, weight: .semibold)
        return UIFontMetrics(forTextStyle: textStyle).scaledFont(for: base)
    }

    /// Scaled system font at a given style and weight.
    @objc(fontWithStyle:weight:)
    static func font(style: UIFont.TextStyle, weight: UIFont.Weight) -> UIFont {
        // The preferred descriptor already includes Dynamic Type. Add weight
        // without running the scaled point size through UIFontMetrics again.
        let descriptor = UIFontDescriptor.preferredFontDescriptor(
            withTextStyle: style, compatibleWith: .current)
            .addingAttributes([.traits: [UIFontDescriptor.TraitKey.weight: weight]])
        return UIFont(descriptor: descriptor, size: 0)
    }

    /// Label above a value in a definition row.
    @objc static func fieldLabelFont() -> UIFont { font(style: .subheadline, weight: .semibold) }
    /// Value in a definition row, and general body copy.
    @objc static func bodyFont() -> UIFont { UIFont.preferredFont(forTextStyle: .body) }
    /// Metadata under a title.
    @objc static func metadataFont() -> UIFont { UIFont.preferredFont(forTextStyle: .footnote) }
    /// Screen-level title inside scrolling content.
    @objc static func screenTitleFont() -> UIFont { font(style: .title1, weight: .bold) }
    /// Group heading inside scrolling content.
    @objc static func groupTitleFont() -> UIFont { font(style: .title3, weight: .semibold) }

    // MARK: - Global appearance

    /// Applies the tint and navigation-bar type once at launch.
    @objc(applyTo:)
    static func apply(to window: UIWindow) {
        window.tintColor = tint

        let navAppearance = UINavigationBarAppearance()
        navAppearance.configureWithDefaultBackground()
        navAppearance.titleTextAttributes = [
            .foregroundColor: ink,
            .font: font(style: .headline, weight: .semibold)
        ]
        navAppearance.largeTitleTextAttributes = [
            .foregroundColor: ink,
            .font: font(style: .largeTitle, weight: .bold)
        ]
        UINavigationBar.appearance().standardAppearance = navAppearance
        UINavigationBar.appearance().compactAppearance = navAppearance
        UINavigationBar.appearance().scrollEdgeAppearance = navAppearance

        let tabAppearance = UITabBarAppearance()
        tabAppearance.configureWithDefaultBackground()
        UITabBar.appearance().standardAppearance = tabAppearance
        UITabBar.appearance().scrollEdgeAppearance = tabAppearance
    }

    // MARK: - Identity

    /// The wordmark, for use as a navigation title view.
    @objc(wordmarkLabel:)
    static func wordmarkLabel(_ text: String) -> UILabel {
        let label = UILabel()
        label.font = wordmarkFont(size: 24, textStyle: .headline)
        label.adjustsFontForContentSizeCategory = true
        label.textColor = ink
        label.text = text
        label.accessibilityLabel = text
        label.sizeToFit()
        return label
    }

    /// The barber pole, shrunk to a quiet mark: template-tinted so it reads in
    /// both appearances, sized in points, and never in the way of content.
    @objc(identityMarkWithHeight:)
    static func identityMark(height: CGFloat) -> UIImageView {
        let image = UIImage(named: "screenbackground.png")?
            .withRenderingMode(.alwaysTemplate)
        let view = UIImageView(image: image)
        view.contentMode = .scaleAspectFit
        view.tintColor = UIColor.label.withAlphaComponent(0.10)
        view.isUserInteractionEnabled = false
        view.isAccessibilityElement = false
        view.translatesAutoresizingMaskIntoConstraints = false
        view.heightAnchor.constraint(equalToConstant: height).isActive = true
        return view
    }

    // MARK: - Feedback

    /// A tag moved into or out of a list — a save the singer should feel.
    @objc static func saved() {
        UINotificationFeedbackGenerator().notificationOccurred(.success)
    }

    /// A row, part, or section was chosen.
    @objc static func selected() {
        UISelectionFeedbackGenerator().selectionChanged()
    }
}

/// Native menus keep choices readable when accessibility text outgrows segments.
@objc(TMAdaptiveChoiceView)
final class TMAdaptiveChoiceView: UIStackView {
    private let segments: UISegmentedControl
    private let choice = UIButton(type: .system)
    private var lastSignature = ""

    @objc(initWithSegments:)
    init(segments: UISegmentedControl) {
        self.segments = segments
        super.init(frame: .zero)
        axis = .vertical
        alignment = .fill
        addArrangedSubview(segments)
        addArrangedSubview(choice)
        choice.showsMenuAsPrimaryAction = true
        for control in [choice, segments] {
            let minimum = control.heightAnchor.constraint(greaterThanOrEqualToConstant: 44)
            minimum.priority = .defaultHigh
            minimum.isActive = true
        }
        segments.addTarget(self, action: #selector(refreshChoice), for: .valueChanged)
        refreshChoice()
    }

    required init(coder: NSCoder) { fatalError("Use init(segments:)") }

    override func layoutSubviews() {
        refreshChoice()
        super.layoutSubviews()
    }

    override func traitCollectionDidChange(_ previous: UITraitCollection?) {
        super.traitCollectionDidChange(previous)
        lastSignature = ""
        refreshChoice()
    }

    @objc private func refreshChoice() {
        let accessible = traitCollection.preferredContentSizeCategory.isAccessibilityCategory
        let titles = (0..<segments.numberOfSegments).map { segments.titleForSegment(at: $0) ?? "" }
        let signature = "\(accessible)-\(segments.selectedSegmentIndex)-\(titles)"
        guard signature != lastSignature else { return }
        lastSignature = signature
        segments.isHidden = accessible
        choice.isHidden = !accessible
        var config = UIButton.Configuration.bordered()
        config.title = titles.indices.contains(segments.selectedSegmentIndex)
            ? titles[segments.selectedSegmentIndex] : segments.accessibilityLabel
        config.image = UIImage(systemName: "chevron.up.chevron.down")
        config.imagePlacement = .trailing
        config.imagePadding = 12
        config.contentInsets = NSDirectionalEdgeInsets(top: 12, leading: 16, bottom: 12, trailing: 16)
        config.titleTextAttributesTransformer = UIConfigurationTextAttributesTransformer { incoming in
            var attributes = incoming
            attributes.font = UIFont.preferredFont(forTextStyle: .body)
            return attributes
        }
        choice.configuration = config
        choice.accessibilityLabel = [segments.accessibilityLabel, config.title].compactMap { $0 }.joined(separator: ", ")
        choice.menu = UIMenu(children: titles.enumerated().map { index, title in
            UIAction(title: title, state: index == segments.selectedSegmentIndex ? .on : .off) { [weak self] _ in
                guard let self else { return }
                self.segments.selectedSegmentIndex = index
                self.segments.sendActions(for: .valueChanged)
                self.refreshChoice()
            }
        })
    }
}

/// A label-and-value row. Side by side at normal reading sizes; stacked once
/// the reader turns text up, so neither half is squeezed to two characters.
@objc(TMFieldRow)
final class TMFieldRow: UIStackView {

    @objc let fieldLabel = UILabel()
    @objc private(set) var valueView: UIView = UIView()
    private var labelWidth: NSLayoutConstraint?

    @objc(fieldRowWithTitle:value:)
    static func make(title: String, value: UIView) -> TMFieldRow {
        let row = TMFieldRow()
        row.fieldLabel.text = title
        row.fieldLabel.font = TMTheme.fieldLabelFont()
        row.fieldLabel.adjustsFontForContentSizeCategory = true
        row.fieldLabel.textColor = TMTheme.secondaryText
        row.fieldLabel.numberOfLines = 0
        row.valueView = value
        row.addArrangedSubview(row.fieldLabel)
        row.addArrangedSubview(value)
        row.spacing = TMTheme.spaceM
        row.isAccessibilityElement = false
        let width = row.fieldLabel.widthAnchor.constraint(equalToConstant: 132)
        width.priority = .defaultHigh
        row.labelWidth = width
        row.applyAxis()
        return row
    }

    override func traitCollectionDidChange(_ previous: UITraitCollection?) {
        super.traitCollectionDidChange(previous)
        if previous?.preferredContentSizeCategory != traitCollection.preferredContentSizeCategory {
            applyAxis()
        }
    }

    private func applyAxis() {
        let stacked = traitCollection.preferredContentSizeCategory.isAccessibilityCategory
        axis = stacked ? .vertical : .horizontal
        alignment = stacked ? .leading : .firstBaseline
        labelWidth?.isActive = !stacked
    }
}

/// A single, reusable state for "nothing here yet", "nothing matched", and
/// "that failed". Every use names the problem and, where the user can act,
/// offers the recovery.
@objc(TMEmptyStateView)
final class TMEmptyStateView: UIView {

    private let symbolView = UIImageView()
    private let titleLabel = UILabel()
    private let messageLabel = UILabel()
    private let actionButton = UIButton(type: .system)
    private var action: (() -> Void)?

    @objc override init(frame: CGRect) {
        super.init(frame: frame)
        build()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        build()
    }

    private func build() {
        symbolView.contentMode = .scaleAspectFit
        symbolView.tintColor = TMTheme.secondaryText
        symbolView.preferredSymbolConfiguration = UIImage.SymbolConfiguration(
            textStyle: .largeTitle, scale: .large)
        symbolView.setContentHuggingPriority(.required, for: .vertical)

        titleLabel.font = TMTheme.font(style: .headline, weight: .semibold)
        titleLabel.adjustsFontForContentSizeCategory = true
        titleLabel.textColor = TMTheme.primaryText
        titleLabel.textAlignment = .center
        titleLabel.numberOfLines = 0

        messageLabel.font = TMTheme.bodyFont()
        messageLabel.adjustsFontForContentSizeCategory = true
        messageLabel.textColor = TMTheme.secondaryText
        messageLabel.textAlignment = .center
        messageLabel.numberOfLines = 0

        actionButton.titleLabel?.font = TMTheme.font(style: .body, weight: .semibold)
        actionButton.titleLabel?.adjustsFontForContentSizeCategory = true
        actionButton.addTarget(self, action: #selector(runAction), for: .touchUpInside)
        actionButton.isHidden = true
        var configuration = UIButton.Configuration.bordered()
        configuration.contentInsets = NSDirectionalEdgeInsets(
            top: TMTheme.spaceM, leading: TMTheme.spaceXL,
            bottom: TMTheme.spaceM, trailing: TMTheme.spaceXL)
        actionButton.configuration = configuration
        actionButton.heightAnchor
            .constraint(greaterThanOrEqualToConstant: TMTheme.minimumTarget).isActive = true

        let stack = UIStackView(arrangedSubviews: [
            symbolView, titleLabel, messageLabel, actionButton
        ])
        stack.axis = .vertical
        stack.alignment = .center
        stack.spacing = TMTheme.spaceM
        stack.setCustomSpacing(TMTheme.spaceL, after: symbolView)
        stack.setCustomSpacing(TMTheme.spaceXL, after: messageLabel)
        stack.translatesAutoresizingMaskIntoConstraints = false
        addSubview(stack)

        NSLayoutConstraint.activate([
            stack.centerYAnchor.constraint(equalTo: centerYAnchor),
            stack.leadingAnchor.constraint(greaterThanOrEqualTo: leadingAnchor,
                                           constant: TMTheme.spaceXL),
            stack.trailingAnchor.constraint(lessThanOrEqualTo: trailingAnchor,
                                            constant: -TMTheme.spaceXL),
            stack.centerXAnchor.constraint(equalTo: centerXAnchor),
            stack.widthAnchor.constraint(lessThanOrEqualToConstant: 420),
            stack.topAnchor.constraint(greaterThanOrEqualTo: topAnchor, constant: TMTheme.spaceXL),
            stack.bottomAnchor.constraint(lessThanOrEqualTo: bottomAnchor,
                                          constant: -TMTheme.spaceXL)
        ])
    }

    @objc(configureWithSymbolName:title:message:actionTitle:action:)
    func configure(symbolName: String,
                   title: String,
                   message: String,
                   actionTitle: String?,
                   action: (() -> Void)?) {
        symbolView.image = UIImage(systemName: symbolName)
        titleLabel.text = title
        messageLabel.text = message
        self.action = action
        if let actionTitle, action != nil {
            actionButton.setTitle(actionTitle, for: .normal)
            actionButton.isHidden = false
        } else {
            actionButton.isHidden = true
        }
        isAccessibilityElement = false
        accessibilityElements = [titleLabel, messageLabel, actionButton]
    }

    @objc private func runAction() {
        action?()
    }
}
