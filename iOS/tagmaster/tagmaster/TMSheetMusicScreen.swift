//
//  TMSheetMusicScreen.swift
//  tagmaster
//
//  The sheet music reader: QuickLook's page inside the detail column, with the
//  key note, a full-screen toggle when beside a list, and Share in the bar.
//
//  Beside a list on iPad the detail column runs under the floating list, and
//  QuickLook centres its page across its full bounds regardless; keeping the
//  preview inside the horizontal safe area keeps the page in the detail.
//

import QuickLook
import SwiftUI

/// Whether the reader sits beside a list and whether it has taken the whole screen.
@Observable @MainActor
final class TMSheetMusicState {
    var besideList = false
    var fullScreen = false
    var toggleFullScreen: () -> Void = {}
}

struct TMSheetMusicScreen: View {
    let document: TMSheetMusicDocument
    let summary: TagSummaryModel
    let state: TMSheetMusicState

    var body: some View {
        TMQuickLookPreview(url: document.fileURL, title: document.title)
            .ignoresSafeArea(.container, edges: .vertical)
            .background(Color(.systemBackground).ignoresSafeArea())
            .navigationTitle(document.title)
            .navigationBarTitleDisplayMode(.inline)
            .tmFollowsUIKitTint()
            .toolbar {
                if let key = document.writtenKey {
                    if #available(iOS 26.0, *) {
                        // The key draws its own outline and fill, outside the bar's shared glass.
                        ToolbarItem(placement: .topBarTrailing) { keyButton(key) }
                            .sharedBackgroundVisibility(.hidden)
                    } else {
                        ToolbarItem(placement: .topBarTrailing) { keyButton(key) }
                    }
                }
                ToolbarItemGroup(placement: .topBarTrailing) {
                    if state.besideList {
                        TMBarButton(state.fullScreen ? "arrow.down.right.and.arrow.up.left" : "arrow.up.left.and.arrow.down.right",
                                    label: state.fullScreen ? "Show list" : "Full screen",
                                    action: state.toggleFullScreen)
                    }
                    ShareLink(item: document.fileURL) { TMBarButton.symbol("square.and.arrow.up") }
                        .accessibilityLabel("Share")
                }
            }
    }
}

extension TMSheetMusicScreen {
    fileprivate func keyButton(_ key: String) -> some View {
        TMKeyNoteButton(model: summary, title: key, singleLine: true)
            .fixedSize()
            .accessibilityIdentifier("sheet.key")
    }
}

/// QuickLook's controller, which SwiftUI has no embeddable equivalent of.
struct TMQuickLookPreview: UIViewControllerRepresentable {
    let url: URL
    let title: String

    final class Coordinator: NSObject, QLPreviewControllerDataSource {
        var item: TMPreviewItem
        init(item: TMPreviewItem) { self.item = item }
        func numberOfPreviewItems(in controller: QLPreviewController) -> Int { 1 }
        func previewController(_ controller: QLPreviewController, previewItemAt index: Int) -> any QLPreviewItem { item }
    }

    final class TMPreviewItem: NSObject, QLPreviewItem {
        let previewItemURL: URL?
        let previewItemTitle: String?
        init(url: URL, title: String) {
            previewItemURL = url
            previewItemTitle = title
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator(item: TMPreviewItem(url: url, title: title)) }

    func makeUIViewController(context: Context) -> QLPreviewController {
        let previewer = QLPreviewController()
        previewer.dataSource = context.coordinator
        return previewer
    }

    func updateUIViewController(_ previewer: QLPreviewController, context: Context) {}
}
