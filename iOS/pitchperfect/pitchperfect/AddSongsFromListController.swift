//
//  AddSongsFromListController.swift
//  pitchperfect
//
//  "Add songs": the checklist of every other set list's songs the current list
//  does not already have, one section per source list in list order. Confirming
//  appends deep copies with fresh song ids, in their source order.
//

import Foundation
import UIKit

/// The picker row: condensed title, monospaced key readout, checkmark when chosen.
final class AddSongRowCell: UITableViewCell {
    static let identifier = "AddSongRow"

    private static let sharpGlyph = "\u{00EC}"
    private static let flatGlyph = "\u{00ED}"
    private static let keySize: CGFloat = 18

    private let titleLabel = UILabel()
    private let keyLabel = UILabel()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        selectionStyle = .none
        DPTheme.styleListCell(self)

        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.font = DPTheme.listTitleFont(size: 20)
        titleLabel.textColor = DPTheme.plateInk
        titleLabel.lineBreakMode = .byTruncatingTail

        keyLabel.translatesAutoresizingMaskIntoConstraints = false
        keyLabel.setContentCompressionResistancePriority(.required, for: .horizontal)
        keyLabel.setContentHuggingPriority(.required, for: .horizontal)

        contentView.addSubview(titleLabel)
        contentView.addSubview(keyLabel)
        NSLayoutConstraint.activate([
            titleLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 20),
            titleLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 14),
            titleLabel.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -14),
            keyLabel.leadingAnchor.constraint(greaterThanOrEqualTo: titleLabel.trailingAnchor, constant: 16),
            keyLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -20),
            keyLabel.centerYAnchor.constraint(equalTo: titleLabel.centerYAnchor),
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    /// The Songs row's readout: the key's name in the measurement face plus its
    /// NoteHedz accidental glyph.
    static func keyReadout(for key: DPKey?, color: UIColor) -> NSAttributedString {
        let mono = DPTheme.monospacedFont(size: keySize)
        guard let key else { return NSAttributedString(string: "") }
        let readout = NSMutableAttributedString(
            string: key.friendlyName() ?? "",
            attributes: [.font: mono, .foregroundColor: color, .kern: keySize * 0.06]
        )
        let accidental = Int(key.note.accidental.get())
        let glyph = accidental == Int(Sharp.rawValue)
            ? sharpGlyph
            : (accidental == Int(Flat.rawValue) ? flatGlyph : nil)
        if let glyph {
            let noteHedz = UIFont(name: "NoteHedz", size: keySize * 1.2) ?? mono
            readout.append(NSAttributedString(
                string: glyph,
                attributes: [.font: noteHedz, .foregroundColor: color]
            ))
        }
        return readout
    }

    func show(song: DPPitchedSong, selected: Bool) {
        titleLabel.text = song.name
        keyLabel.attributedText = AddSongRowCell.keyReadout(for: song.key,
                                                            color: DPTheme.plateInkSecondary)
        accessoryType = selected ? .checkmark : .none
        accessibilityLabel = "\(song.name ?? ""), \(song.key?.friendlyName() ?? "")"
        accessibilityTraits = selected ? [.button, .selected] : .button
    }
}

/// The engraved section label: tracked monospaced capitals in secondary ink.
final class PlateSectionHeader: UITableViewHeaderFooterView {
    static let identifier = "PlateSectionHeader"

    private let label = UILabel()

    override init(reuseIdentifier: String?) {
        super.init(reuseIdentifier: reuseIdentifier)
        let background = UIView()
        background.backgroundColor = .clear
        backgroundView = background
        label.translatesAutoresizingMaskIntoConstraints = false
        label.textColor = DPTheme.plateInkSecondary
        contentView.addSubview(label)
        NSLayoutConstraint.activate([
            label.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 20),
            label.trailingAnchor.constraint(lessThanOrEqualTo: contentView.trailingAnchor, constant: -20),
            label.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -6),
            label.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 14),
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    func show(_ text: String) {
        label.attributedText = NSAttributedString(
            string: text,
            attributes: [
                .font: DPTheme.monospacedFont(size: 12),
                .foregroundColor: DPTheme.plateInkSecondary,
                .kern: 12 * 0.14,
            ]
        )
        accessibilityLabel = text
    }
}

@objc public class AddSongsFromListController: UITableViewController {
    private let target: DPSongList
    private var groups: [DPAddableSongs] = []
    private var chosen: Set<ObjectIdentifier> = []
    private var addItem: UIBarButtonItem?

    /// Called with the number of songs appended once the picker closes.
    @objc public var onFinish: ((Int) -> Void)?

    @objc public init(target: DPSongList) {
        self.target = target
        super.init(style: .plain)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    /// The picker as it is presented: a sheet on iPhone, a form sheet on iPad.
    @objc public func embeddedInNavigation() -> UINavigationController {
        let navigation = UINavigationController(rootViewController: self)
        DPCommon.configureInstrumentChrome(navigation)
        if UIDevice.current.userInterfaceIdiom == .pad {
            navigation.modalPresentationStyle = .formSheet
        } else {
            navigation.modalPresentationStyle = .pageSheet
            navigation.sheetPresentationController?.detents = [.medium(), .large()]
        }
        return navigation
    }

    override public func viewDidLoad() {
        super.viewDidLoad()
        navigationItem.title = "Add songs"
        groups = DPSongsModel.sharedInstance.addableSongs(for: target)
        tableView.register(AddSongRowCell.self, forCellReuseIdentifier: AddSongRowCell.identifier)
        tableView.register(PlateSectionHeader.self,
                           forHeaderFooterViewReuseIdentifier: PlateSectionHeader.identifier)
        tableView.backgroundColor = DPTheme.staffBackgroundColor()
        let background = UIView()
        background.backgroundColor = DPTheme.staffBackgroundColor()
        tableView.backgroundView = background
        tableView.separatorColor = DPTheme.plateHairline
        tableView.separatorInset = UIEdgeInsets(top: 0, left: 20, bottom: 0, right: 20)

        navigationItem.leftBarButtonItem = DPCommon.barButton(
            systemName: "xmark", target: self, selector: #selector(close)
        )
        let add = UIBarButtonItem(title: "Add", style: .done, target: self, action: #selector(confirm))
        add.accessibilityIdentifier = "setlist.addSongs.confirm"
        addItem = add
        navigationItem.rightBarButtonItem = add
        refreshAddItem()
    }

    private func refreshAddItem() {
        let count = chosen.count
        addItem?.isEnabled = count > 0
        switch count {
        case 0: addItem?.title = "Add"
        case 1: addItem?.title = "Add 1 song"
        default: addItem?.title = "Add \(count) songs"
        }
    }

    // MARK: - Rows

    override public func numberOfSections(in tableView: UITableView) -> Int { groups.count }

    override public func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        groups.indices.contains(section) ? groups[section].songs.count : 0
    }

    override public func tableView(_ tableView: UITableView,
                                   viewForHeaderInSection section: Int) -> UIView? {
        guard groups.indices.contains(section),
              let header = tableView.dequeueReusableHeaderFooterView(
                withIdentifier: PlateSectionHeader.identifier) as? PlateSectionHeader else { return nil }
        header.show(DPSongsModel.sharedInstance.displayName(for: groups[section].list))
        return header
    }

    override public func tableView(_ tableView: UITableView,
                                   titleForHeaderInSection section: Int) -> String? {
        guard groups.indices.contains(section) else { return nil }
        return DPSongsModel.sharedInstance.displayName(for: groups[section].list)
    }

    private func song(at indexPath: IndexPath) -> DPPitchedSong? {
        guard groups.indices.contains(indexPath.section),
              groups[indexPath.section].songs.indices.contains(indexPath.row) else { return nil }
        return groups[indexPath.section].songs[indexPath.row]
    }

    override public func tableView(_ tableView: UITableView,
                                   cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: AddSongRowCell.identifier, for: indexPath)
        DPTheme.styleListCell(cell)
        if let row = cell as? AddSongRowCell, let song = song(at: indexPath) {
            row.show(song: song, selected: chosen.contains(ObjectIdentifier(song)))
        }
        return cell
    }

    override public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: false)
        guard let song = song(at: indexPath) else { return }
        let key = ObjectIdentifier(song)
        if chosen.contains(key) { chosen.remove(key) } else { chosen.insert(key) }
        tableView.reloadRows(at: [indexPath], with: .none)
        refreshAddItem()
    }

    // MARK: - Commands

    @objc public func close() {
        finish(added: 0)
    }

    /// Deep copies appended to the current list, in their source order.
    @objc public func confirm() {
        let selected = groups.flatMap(\.songs).filter { chosen.contains(ObjectIdentifier($0)) }
        guard !selected.isEmpty else { return }
        DPSongsModel.sharedInstance.copySongs(selected, to: target)
        finish(added: selected.count)
    }

    private func finish(added: Int) {
        let report: () -> Void = { [weak self] in self?.onFinish?(added) }
        if let presenting = presentingViewController {
            presenting.dismiss(animated: true, completion: report)
        } else {
            report()
        }
    }
}
