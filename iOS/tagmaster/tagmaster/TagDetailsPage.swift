//
//  TagDetailsPage.swift
//  tagmaster
//
//  The Details page: where the tag came from and who arranged and sang it.
//  Names with a website are links; a name without one is plain information.
//

import SwiftUI

struct TagDetailsPage: View {
    let model: TagDetailModel

    static func refreshedText(_ date: Date?) -> String {
        guard let date else { return "" }
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }

    static func postedText(_ date: Date?) -> String {
        guard let date else { return "" }
        let formatter = DateFormatter()
        formatter.dateStyle = .full
        return formatter.string(from: date)
    }

    static func catalogURL(for tagId: Int32) -> URL {
        URL(string: "https://www.barbershoptags.com/dbpage.php?pg=view&dbase=tags&id=\(tagId)")!
    }

    var body: some View {
        if let tag = model.tag {
            TMPageScroll {
                VStack(alignment: .leading, spacing: 8) {
                    Text(tag.title ?? "")
                        .font(.title)
                        .fixedSize(horizontal: false, vertical: true)
                    TMFactsLayout {
                        TMCaption(text: "Last Refreshed")
                        TMFactText(text: TagDetailsPage.refreshedText(tag.lastRefreshed)).tmFactValue(.text)
                        TMCaption(text: "Downloads")
                        TMFactText(text: "\(tag.downloadCount)").tmFactValue(.text)
                        TMCaption(text: "Link")
                        TMFactLink(title: "BarbershopTags.com", url: TagDetailsPage.catalogURL(for: tag.tagId))
                            .tmFactValue(.link)
                        if let provider = tag.provider, !provider.isEmpty {
                            TMCaption(text: "Posted by")
                            TMFactLink(title: provider, url: tag.providerWebsite).tmFactValue(tag.providerWebsite == nil ? .text : .link)
                        }
                        TMCaption(text: "Posted")
                        TMFactText(text: TagDetailsPage.postedText(tag.posted)).tmFactValue(.text)
                        if let arranger = tag.arranger, !arranger.isEmpty {
                            TMCaption(text: "Arranged by")
                            TMFactLink(title: arranger, url: tag.arrangerWebsite).tmFactValue(tag.arrangerWebsite == nil ? .text : .link)
                        }
                        if tag.yearArranged != 0 {
                            TMCaption(text: "Year arranged")
                            TMFactText(text: "\(tag.yearArranged)").tmFactValue(.text)
                        }
                        if let sungBy = tag.sungBy, !sungBy.isEmpty {
                            TMCaption(text: "Sung by")
                            TMFactLink(title: sungBy, url: tag.sungByWebsite).tmFactValue(tag.sungByWebsite == nil ? .text : .link)
                        }
                        if tag.sungYear != 0 {
                            TMCaption(text: "Year sung")
                            TMFactText(text: "\(tag.sungYear)").tmFactValue(.text)
                        }
                    }
                }
            }
        }
    }
}

/// A value that opens its website, or plain text when it has none.
struct TMFactLink: View {
    @Environment(\.tmAccent) private var accent
    let title: String
    let url: URL?
    @Environment(\.openURL) private var openURL

    var body: some View {
        if let url {
            Button { openURL(url) } label: {
                TMFactText(text: title)
                    .foregroundStyle(accent)
                    .frame(maxHeight: .infinity)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityRemoveTraits(.isButton)
            .accessibilityAddTraits(.isLink)
        } else {
            TMFactText(text: title)
        }
    }
}
