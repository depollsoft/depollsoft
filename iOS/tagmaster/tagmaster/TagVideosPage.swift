//
//  TagVideosPage.swift
//  tagmaster
//
//  The Videos page: the teaching video, if the tag has one, then every video
//  people have posted of it. Each opens on YouTube inside the app.
//

import SafariServices
import SwiftUI

/// One row's content, worked out once from the tag.
struct TMVideoRow: Identifiable {
    let id: String
    let lines: [String]
    let spoken: String
    let thumbnail: URL
    let watch: URL

    static func thumbnail(_ code: String) -> URL {
        URL(string: "https://img.youtube.com/vi/\(code)/2.jpg")!
    }

    static func watch(_ code: String) -> URL {
        URL(string: "https://www.youtube.com/watch?v=\(code)")!
    }

    static func teaching(_ tag: DPTag) -> TMVideoRow? {
        guard let code = tag.teachingVideo else { return nil }
        return TMVideoRow(id: "teaching-\(code)",
                          lines: ["Teacher: \(tag.teacher ?? "Unknown")"],
                          spoken: "Teaching video by \(tag.teacher ?? "an unknown teacher")",
                          thumbnail: thumbnail(code), watch: watch(code))
    }

    static func submission(_ video: DPVideo, index: Int) -> TMVideoRow {
        let code = video.youTubeCode ?? ""
        var lines: [String] = []
        if let sungBy = video.sungBy { lines.append("Sung by: \(sungBy)") }
        if let key = video.sungKey { lines.append("Key: \(key)") }
        let formatter = DateFormatter()
        formatter.dateStyle = .long
        let posted = video.posted.map(formatter.string(from:)) ?? "Unknown"
        lines.append("Posted: \(posted)")
        lines.append("Multitrack: \(video.isMultitrack ? "Yes" : "No")")
        let spoken = "Video sung by \(video.sungBy ?? "an unknown group")"
            + (video.sungKey.map { " in \($0)" } ?? "")
            + ". Posted \(posted). \(video.isMultitrack ? "Multitrack" : "Single track")."
        return TMVideoRow(id: "video-\(index)-\(code)", lines: lines, spoken: spoken,
                          thumbnail: thumbnail(code), watch: watch(code))
    }
}

struct TagVideosPage: View {
    let model: TagDetailModel
    @State private var watching: URL?

    var body: some View {
        if let tag = model.tag {
            let teaching = TMVideoRow.teaching(tag)
            let videos = (tag.videos as? [DPVideo] ?? []).enumerated().map { TMVideoRow.submission($1, index: $0) }
            List {
                if let teaching {
                    Section {
                        row(teaching)
                    } header: {
                        TMGroupedHeader(text: "Teaching Video")
                    }
                }
                Section {
                    ForEach(videos) { row($0) }
                } header: {
                    TMGroupedHeader(text: "User Submissions")
                } footer: {
                    Text(videos.isEmpty
                         ? "Sorry, this tag does not have any videos associated with it."
                         : "Videos open on YouTube inside Tag Master.")
                        .padding(.leading, 4)
                }
            }
            .tmGroupedTableMetrics()
            .fullScreenCover(item: Binding(get: { watching.map(TMWatchedURL.init) },
                                           set: { watching = $0?.url })) { watched in
                TMSafariView(url: watched.url) { watching = nil }
                    .ignoresSafeArea()
            }
        }
    }

    private func row(_ video: TMVideoRow) -> some View {
        Button { watching = video.watch } label: {
            HStack(alignment: .top, spacing: 12) {
                TMVideoThumbnail(url: video.thumbnail)
                VStack(alignment: .leading, spacing: 4) {
                    ForEach(Array(video.lines.enumerated()), id: \.offset) { index, line in
                        if line.hasPrefix("Multitrack:") {
                            let available = line == "Multitrack: Yes"
                            HStack(spacing: 8) {
                                Image(systemName: available ? "checkmark.circle.fill" : "circle")
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 20, height: 20)
                                    .foregroundStyle(available ? Color(.systemGreen) : Color(.secondaryLabel))
                                Text(line)
                                    .font(index == 0 ? .headline : .subheadline)
                                    .foregroundStyle(Color(.label))
                            }
                        } else {
                            Text(line)
                                .font(index == 0 ? .headline : .subheadline)
                                .foregroundStyle(index == 0 ? Color(.label) : Color(.secondaryLabel))
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                }
                Spacer(minLength: 0)
            }
            .padding(.top, 12)
            // UITableView counts the 1 pt separator into the row; SwiftUI draws it over the row.
            .padding(.bottom, 13)
            .frame(maxWidth: .infinity, alignment: .leading)
            .overlay(alignment: .trailing) { TMDisclosureChevron() }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .listRowBackground(Color.clear)
        .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 21))
        .alignmentGuide(.listRowSeparatorLeading) { $0[.leading] }
        .accessibilityLabel(video.spoken)
        .accessibilityHint("Opens the video")
        .accessibilityAddTraits(.isButton)
    }
}

private struct TMWatchedURL: Identifiable {
    let url: URL
    var id: URL { url }
}

/// The video's YouTube still, from the file cache or the network, over a neutral well.
struct TMVideoThumbnail: View {
    let url: URL
    @State private var image: UIImage?

    var body: some View {
        RoundedRectangle(cornerRadius: 6, style: .continuous)
            .fill(Color(.secondarySystemFill))
            .overlay {
                if let image {
                    Image(uiImage: image).resizable().scaledToFill()
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
            .frame(width: 80, height: 60)
            .accessibilityHidden(true)
            .task(id: url) { image = await TMVideoThumbnail.load(url) }
    }

    static func load(_ url: URL) async -> UIImage? {
        let key = DPFileCache.key(for: url)
        if let path = DPFileCache.path(forKey: key), FileManager.default.fileExists(atPath: path),
           let data = DPFileCache.readData(forKey: key) {
            return UIImage(data: data)
        }
        return await Task.detached(priority: .utility) {
            guard let data = try? DPRemoteLocation.data(withContentsOf: url), !data.isEmpty else { return nil }
            DPFileCache.write(data, forKey: key)
            return UIImage(data: data)
        }.value
    }
}

/// YouTube in the in-app browser, which still hands off to the YouTube app when installed.
struct TMSafariView: UIViewControllerRepresentable {
    let url: URL
    /// Safari's own Done dismisses its presenter behind SwiftUI's back; this tells
    /// the page the video is closed, so the same video can be opened again.
    var onFinish: () -> Void = {}

    final class Coordinator: NSObject, SFSafariViewControllerDelegate {
        var onFinish: () -> Void = {}
        func safariViewControllerDidFinish(_ controller: SFSafariViewController) { onFinish() }
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIViewController(context: Context) -> SFSafariViewController {
        let browser = SFSafariViewController(url: url)
        browser.preferredControlTintColor = DPAppDelegate.accentColor()
        browser.delegate = context.coordinator
        context.coordinator.onFinish = onFinish
        return browser
    }

    func updateUIViewController(_ controller: SFSafariViewController, context: Context) {
        context.coordinator.onFinish = onFinish
    }
}


/// A grouped table section title, where UITableView put it.
struct TMGroupedHeader: View {
    let text: String
    var body: some View {
        Text(text)
            .padding(.leading, 4)
            .padding(.bottom, -3.67)
    }
}

extension View {
    /// The spacing a plain grouped UITableView had: its first header further down,
    /// more room between sections.
    func tmGroupedTableMetrics() -> some View {
        listStyle(.grouped)
            .scrollContentBackground(.hidden)
            .contentMargins(.top, 18.67, for: .scrollContent)
            .listSectionSpacing(26)
    }
}
