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
    @objc static let secondaryText = UIColor { traits in
        UIColor(white: traits.userInterfaceStyle == .dark ? 0.80 : 0.28, alpha: 1)
    }
    /// Page canvas behind grouped content.
    @objc static let canvas = UIColor.systemBackground
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

    /// The original raster already contains its translucency. Never fade it again.
    @objc(installBackgroundIn:)
    static func installBackground(in view: UIView) {
        removeBackground(from: view)
        view.backgroundColor = .systemBackground
        let background = TMPageBackground()
        if let table = view as? UITableView {
            table.backgroundView = background
        } else {
            background.translatesAutoresizingMaskIntoConstraints = false
            view.insertSubview(background, at: 0)
            NSLayoutConstraint.activate([
                background.leadingAnchor.constraint(equalTo: view.leadingAnchor),
                background.trailingAnchor.constraint(equalTo: view.trailingAnchor),
                background.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
                background.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor)
            ])
        }
    }

    @objc(removeBackgroundFrom:)
    static func removeBackground(from view: UIView) {
        view.subviews.filter { $0 is TMPageBackground }.forEach { $0.removeFromSuperview() }
        if let table = view as? UITableView { table.backgroundView = nil }
        view.backgroundColor = .clear
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
        let accessible = true
        let titles = (0..<segments.numberOfSegments).map { segments.titleForSegment(at: $0) ?? "" }
        let signature = "\(accessible)-\(segments.selectedSegmentIndex)-\(titles)"
        guard signature != lastSignature else { return }
        lastSignature = signature
        segments.isHidden = accessible
        choice.isHidden = !accessible
        var config = UIButton.Configuration.plain()
        config.title = titles.indices.contains(segments.selectedSegmentIndex)
            ? titles[segments.selectedSegmentIndex] : segments.accessibilityLabel
        config.title = [segments.accessibilityLabel, config.title].compactMap { $0 }.joined(separator: ": ")
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
        choice.accessibilityLabel = config.title
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

/// One page-scale pole, including a single shared backdrop for tablet panes.
private final class TMPageBackground: UIView {
    private let pole = UIImageView()
    override init(frame: CGRect) {
        super.init(frame: frame)
        isUserInteractionEnabled = false
        isAccessibilityElement = false
        accessibilityIdentifier = "barberPoleBackground"
        pole.contentMode = .scaleAspectFit
        pole.isAccessibilityElement = false
        addSubview(pole)
        updateImage()
    }
    required init?(coder: NSCoder) { fatalError("Use init(frame:)") }
    override func layoutSubviews() {
        super.layoutSubviews()
        pole.frame = bounds.insetBy(dx: 0, dy: 16)
    }
    override func traitCollectionDidChange(_ previous: UITraitCollection?) {
        super.traitCollectionDidChange(previous)
        updateImage()
    }
    private func updateImage() {
        let original = UIImage(named: "screenbackground.png")
        pole.image = traitCollection.userInterfaceStyle == .dark
            ? original?.withRenderingMode(.alwaysTemplate) : original?.withRenderingMode(.alwaysOriginal)
        pole.tintColor = .white
    }
}
