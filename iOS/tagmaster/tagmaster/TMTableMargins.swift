//
//  TMTableMargins.swift
//  tagmaster
//
//  UITableView's margins where a list stands, measured from real tables rather
//  than assumed: UIKit gives 20 points on most phones but 16 on an iPhone SE and
//  in the iPad split's list column, and the rule behind it is not the width alone.
//

import SwiftUI
import UIKit

/// The margins a UIKit table would have had at a column's size.
struct TMTableMargins: Equatable {
    /// A plain table's layout margin: where its rows' text starts.
    var plain: CGFloat = 20
    /// Where an inset-grouped table's cards start: its layout margin.
    var grouped: CGFloat = 20
    /// Where an inset-grouped row's text starts, from the table's edge.
    var groupedText: CGFloat = 36
}

/// Measures `TMTableMargins` with hidden plain and inset-grouped tables laid out
/// at this view's size, and reports them whenever they change.
struct TMTableMarginReader: UIViewRepresentable {
    let onChange: (TMTableMargins) -> Void

    final class Reader: UIView, UITableViewDataSource {
        let plain = UITableView(frame: .zero, style: .plain)
        let grouped = UITableView(frame: .zero, style: .insetGrouped)
        var onChange: (TMTableMargins) -> Void = { _ in }
        private var reported: TMTableMargins?

        override init(frame: CGRect) {
            super.init(frame: frame)
            isHidden = true
            isUserInteractionEnabled = false
            isAccessibilityElement = false
            accessibilityElementsHidden = true
            grouped.dataSource = self
            grouped.register(UITableViewCell.self, forCellReuseIdentifier: "row")
            addSubview(plain)
            addSubview(grouped)
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

        func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { 1 }

        func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
            let cell = tableView.dequeueReusableCell(withIdentifier: "row", for: indexPath)
            var content = cell.defaultContentConfiguration()
            content.text = "Row"
            cell.contentConfiguration = content
            return cell
        }

        override func layoutSubviews() {
            super.layoutSubviews()
            guard window != nil, bounds.width > 0 else { return }
            let frame = CGRect(x: 0, y: 0, width: bounds.width, height: 200)
            plain.frame = frame
            grouped.frame = frame
            plain.layoutIfNeeded()
            grouped.reloadData()
            grouped.layoutIfNeeded()
            var margins = TMTableMargins(plain: plain.layoutMargins.left, grouped: grouped.layoutMargins.left)
            if let cell = grouped.cellForRow(at: IndexPath(row: 0, section: 0)) {
                // The card starts on the table's margin; its text on the content view's.
                margins.groupedText = cell.convert(CGPoint(x: cell.contentView.layoutMargins.left, y: 0), to: grouped).x
            }
            guard margins != reported else { return }
            reported = margins
            let onChange = onChange
            // Reported after this layout pass, not during SwiftUI's update.
            DispatchQueue.main.async { onChange(margins) }
        }

        override func didMoveToWindow() {
            super.didMoveToWindow()
            setNeedsLayout()
        }
    }

    func makeUIView(context: Context) -> Reader {
        let reader = Reader()
        reader.onChange = onChange
        return reader
    }

    func updateUIView(_ reader: Reader, context: Context) {
        reader.onChange = onChange
    }
}

extension EnvironmentValues {
    /// Where an inset-grouped list's cards start (`TMTableMargins.grouped`).
    @Entry var tmGroupedMargin: CGFloat = 20
    /// Where an inset-grouped row's text starts (`TMTableMargins.groupedText`).
    @Entry var tmGroupedTextInset: CGFloat = 36
}
