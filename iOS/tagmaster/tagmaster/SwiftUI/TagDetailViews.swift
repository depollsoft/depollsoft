import SwiftUI
import AVKit
import QuickLook

struct MetadataRow<Content: View>: View {
    let label: String
    let content: Content

    init(_ label: String, @ViewBuilder content: () -> Content) {
        self.label = label
        self.content = content()
    }

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(label).font(.headline)
            Spacer(minLength: 12)
            content.multilineTextAlignment(.trailing)
        }
    }
}

extension MetadataRow where Content == Text {
    init(_ label: String, value: String) {
        self.init(label) { Text(value) }
    }
}

// MARK: - Tag detail host

struct TagDetailHostView: View {
    let tagID: Int32
    @State private var tag: DPTag?
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var isFavorite = false
    @State private var isTeachable = false

    var body: some View {
        Group {
            if let tag {
                TabView {
                    TagSummaryView(tag: tag)
                        .tabItem { Label("Summary", image: "TagSummary") }
                    TagDetailView(tag: tag)
                        .tabItem { Label("Details", image: "MostViewed") }
                    TagTracksView(tag: tag)
                        .tabItem { Label("Tracks", image: "Tracks") }
                    TagVideoView(tag: tag)
                        .tabItem { Label("Videos", image: "Videos") }
                }
                .tagMasterTabBarStyle()
            } else if isLoading {
                ProgressView("Loading...")
            } else {
                VStack(spacing: 12) {
                    Text(errorMessage ?? "This tag could not be loaded.")
                    Button("Retry") { loadTag(refresh: false) }
                }
            }
        }
        .navigationTitle(tag?.title ?? "Tag")
        .toolbar {
            ToolbarItemGroup(placement: .navigationBarTrailing) {
                if let tag, let uri = tag.tagUri() {
                    ShareLink(
                        item: uri,
                        subject: Text(tag.title ?? "Tag Master"),
                        message: Text("\(tag.title ?? "") - Tag Master for iOS")
                    ) {
                        Image(systemName: "square.and.arrow.up")
                    }
                    .accessibilityLabel("Share")
                    .accessibilityIdentifier("Share")
                }
                Menu {
                    Button(isFavorite ? "Remove Favorite" : "Add Favorite", action: toggleFavorite)
                    Button(isTeachable ? "Unmark as Teachable" : "Mark as Teachable", action: toggleTeachable)
                } label: {
                    Image(systemName: "tag")
                }
                .accessibilityLabel("Tag lists")
                .accessibilityValue("Favorite \(isFavorite ? "yes" : "no"), teachable \(isTeachable ? "yes" : "no")")
                .disabled(tag == nil)
                Button { loadTag(refresh: true) } label: { Image(systemName: "arrow.clockwise") }
                    .accessibilityLabel("Refresh tag")
            }
        }
        .onAppear {
            updateListState()
            if tag == nil { loadTag(refresh: false) }
        }
        .onReceive(NotificationCenter.default.publisher(for: .userDataChanged)) { _ in updateListState() }
    }

    private func loadTag(refresh: Bool) {
        isLoading = true
        errorMessage = nil
        DispatchQueue.global(qos: .userInitiated).async {
            let loaded = DPTag.load(byId: tagID, refresh: refresh)
            DispatchQueue.main.async {
                tag = loaded
                isLoading = false
                if loaded == nil { errorMessage = "Tag \(tagID) was not found." }
            }
        }
    }

    private func updateListState() {
        isFavorite = DPAppDelegate.containsFavorite(tagID)
        isTeachable = DPAppDelegate.containsTeachable(tagID)
    }

    private func toggleFavorite() {
        isFavorite ? DPAppDelegate.removeFavorite(tagID) : DPAppDelegate.addFavorite(tagID)
    }

    private func toggleTeachable() {
        isTeachable ? DPAppDelegate.removeTeachable(tagID) : DPAppDelegate.addTeachable(tagID)
    }
}

// MARK: - Tag pages

struct TagSummaryView: View {
    let tag: DPTag
    @State private var showRating = false
    @State private var sheetMusicURL: URL?
    @State private var sheetMusicError: String?
    @State private var isLoadingSheetMusic = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text(tag.title ?? "Unknown").font(.title2.bold())
                if let alternative = tag.alternativeTitle, !alternative.isEmpty {
                    Text("a.k.a. \(alternative)").font(.headline)
                }
                MetadataRow("Rating") {
                    HStack {
                        Text(tag.rating, format: .number.precision(.fractionLength(2)))
                        Button("Rate") { showRating = true }
                    }
                }
                MetadataRow("Parts", value: "\(tag.parts)")
                if let type = tag.tagType { MetadataRow("Type", value: type) }
                if let key = tag.writtenKey {
                    MetadataRow("Key") {
                        Button(key) { playKey() }
                            .accessibilityHint("Plays the written pitch")
                    }
                }
                if tag.classicTagNumber > 0 {
                    MetadataRow("Classic Tag", value: "\(tag.classicTagNumber)")
                }
                if tag.sheetMusicUri != nil {
                    Button {
                        loadSheetMusic()
                    } label: {
                        if isLoadingSheetMusic { ProgressView() }
                        else { Label("Sheet Music", systemImage: "doc.text") }
                    }
                    .disabled(isLoadingSheetMusic)
                }
                if let lyrics = tag.lyrics, !lyrics.isEmpty {
                    Text("Lyrics").font(.headline)
                    Text(lyrics)
                }
                if let notes = tag.notes, !notes.isEmpty {
                    Text("Notes").font(.headline)
                    Text(notes)
                }
            }
            .padding()
            .frame(maxWidth: 720, alignment: .leading)
            .frame(maxWidth: .infinity)
        }
        .confirmationDialog("Rate this tag", isPresented: $showRating, titleVisibility: .visible) {
            ForEach((1...5).reversed(), id: \.self) { rating in
                Button(String(repeating: "★", count: rating) + String(repeating: "☆", count: 5 - rating)) {
                    DispatchQueue.global(qos: .userInitiated).async { tag.rate(UInt(rating)) }
                }
            }
            Button("Cancel", role: .cancel) {}
        }
        .quickLookPreview($sheetMusicURL)
        .alert("Sheet Music Error", isPresented: Binding(
            get: { sheetMusicError != nil },
            set: { if !$0 { sheetMusicError = nil } }
        )) {
            Button("OK", role: .cancel) { sheetMusicError = nil }
        } message: {
            Text(sheetMusicError ?? "Please try again.")
        }
    }

    private func playKey() {
        guard let note = tag.keyNote() else { return }
        note.play()
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { note.stop() }
    }

    private func loadSheetMusic() {
        guard let remote = tag.sheetMusicUri, let source = remote.uri else { return }
        isLoadingSheetMusic = true
        URLSession.shared.downloadTask(with: source) { temporaryURL, _, error in
            var finalURL: URL?
            if let temporaryURL {
                let destination = FileManager.default.temporaryDirectory.appendingPathComponent(remote.cacheKey)
                try? FileManager.default.removeItem(at: destination)
                do {
                    try FileManager.default.copyItem(at: temporaryURL, to: destination)
                    finalURL = destination
                } catch {
                    DispatchQueue.main.async { sheetMusicError = error.localizedDescription }
                }
            }
            DispatchQueue.main.async {
                isLoadingSheetMusic = false
                sheetMusicURL = finalURL
                if finalURL == nil, let error { sheetMusicError = error.localizedDescription }
            }
        }.resume()
    }
}


struct TagDetailView: View {
    let tag: DPTag

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(tag.title ?? "Unknown").font(.title2.bold())
                MetadataRow("Tag ID", value: "\(tag.tagId)")
                if let refreshed = tag.lastRefreshed {
                    MetadataRow("Last Refreshed", value: refreshed.formatted(date: .numeric, time: .standard))
                }
                MetadataRow("Downloads", value: "\(tag.downloadCount)")
                Link("BarbershopTags.com", destination: URL(string: "http://www.barbershoptags.com/dbpage.php?pg=view&dbase=tags&id=\(tag.tagId)")!)
                linkedValue("Posted By", text: tag.provider, url: tag.providerWebsite)
                if let posted = tag.posted {
                    MetadataRow("Posted", value: posted.formatted(date: .long, time: .omitted))
                }
                linkedValue("Arranged By", text: tag.arranger, url: tag.arrangerWebsite)
                if tag.yearArranged > 0 { MetadataRow("Year Arranged", value: "\(tag.yearArranged)") }
                linkedValue("Sung By", text: tag.sungBy, url: tag.sungByWebsite)
                if tag.sungYear > 0 { MetadataRow("Year Sung", value: "\(tag.sungYear)") }
                linkedValue("Tracks By", text: tag.learningTrackQuartet, url: tag.learningTrackQuartetWebsite)
            }
            .padding()
            .frame(maxWidth: 720, alignment: .leading)
            .frame(maxWidth: .infinity)
        }
    }

    @ViewBuilder
    private func linkedValue(_ label: String, text: String?, url: URL?) -> some View {
        if let text, !text.isEmpty {
            MetadataRow(label) {
                if let url { Link(text, destination: url) } else { Text(text) }
            }
        }
    }
}

struct TagTracksView: View {
    let tag: DPTag

    static func temporaryURL(for cacheKey: String) -> URL {
        FileManager.default.temporaryDirectory.appendingPathComponent(cacheKey)
    }
    @State private var player: AVPlayer?
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        List {
            if let recordingMethod = tag.recordingMethod, !recordingMethod.isEmpty {
                Section("Recording Notes") { Text(recordingMethod) }
            }
            Section("Tracks") {
                if tag.tracks.isEmpty {
                    Text("Sorry, no tracks could be found for this tag.")
                }
                ForEach(Array(tag.tracks.enumerated()), id: \.offset) { _, track in
                    Button(track.title ?? "Unknown Track") { load(track) }
                }
            }
            if isLoading { ProgressView("Loading track...") }
        }
        .sheet(isPresented: Binding(
            get: { player != nil },
            set: { if !$0 { player?.pause(); player = nil } }
        )) {
            if let player { VideoPlayer(player: player).onAppear { player.play() } }
        }
        .alert("Track Error", isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "Please try again.")
        }
    }

    private func load(_ track: DPTrack) {
        guard let source = track.source?.uri else { return }
        isLoading = true
        URLSession.shared.downloadTask(with: source) { temporaryURL, _, error in
            var playableURL: URL?
            if let temporaryURL {
                let destination = Self.temporaryURL(for: track.source.cacheKey)
                try? FileManager.default.removeItem(at: destination)
                do {
                    try FileManager.default.copyItem(at: temporaryURL, to: destination)
                    playableURL = destination
                } catch {
                    DispatchQueue.main.async { errorMessage = error.localizedDescription }
                }
            }
            DispatchQueue.main.async {
                isLoading = false
                if let playableURL { player = AVPlayer(url: playableURL) }
                else if errorMessage == nil {
                    errorMessage = error?.localizedDescription ?? "The track could not be downloaded."
                }
            }
        }.resume()
    }
}

struct TagVideoView: View {
    let tag: DPTag

    private var videos: [DPVideo] { (tag.videos as? [DPVideo]) ?? [] }

    var body: some View {
        List {
            if let code = tag.teachingVideo, !code.isEmpty {
                Section("Teaching Video") {
                    videoRow(code: code, sungBy: tag.teacher, key: nil, posted: nil, multitrack: nil)
                }
            }
            Section("User Submissions") {
                if videos.isEmpty {
                    Text("Sorry, this tag does not have any videos associated with it.")
                }
                ForEach(videos, id: \.videoId) { video in
                    videoRow(
                        code: video.youTubeCode, sungBy: video.sungBy,
                        key: video.sungKey, posted: video.posted,
                        multitrack: video.isMultitrack
                    )
                }
            }
        }
    }

    @ViewBuilder
    private func videoRow(
        code: String?, sungBy: String?, key: String?, posted: Date?, multitrack: Bool?
    ) -> some View {
        if let code, let destination = URL(string: "https://www.youtube.com/watch?v=\(code)") {
            Link(destination: destination) {
                HStack(alignment: .top, spacing: 12) {
                    AsyncImage(url: URL(string: "https://img.youtube.com/vi/\(code)/2.jpg")) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Color.secondary.opacity(0.2)
                    }
                    .frame(width: 88, height: 66)
                    .clipped()
                    VStack(alignment: .leading, spacing: 3) {
                        if let sungBy, !sungBy.isEmpty { Text("Sung By: \(sungBy)") }
                        if let key, !key.isEmpty { Text("Key: \(key)") }
                        if let posted { Text("Posted: \(posted.formatted(date: .long, time: .omitted))") }
                        if let multitrack { Text("Multitrack: \(multitrack ? "Yes" : "No")") }
                    }
                    .font(.subheadline)
                }
            }
        }
    }
}
