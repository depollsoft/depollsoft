//
//  TagSummaryModel.swift
//  tagmaster
//
//  The Summary page's behaviour: the key note (held while pressed, or timed
//  when VoiceOver activates it), rating, opening the sheet music, and the
//  "In lists" chips with their undoable removal.
//

import Foundation
import Observation
import UIKit

@Observable @MainActor
final class TagSummaryModel {
    weak var detail: TagDetailModel?
    let busy: TMBusyCount

    /// How long a timed (accessibility-activated) key note sounds. Tests shorten it.
    static var timedKeyNoteDuration: TimeInterval = 1.5

    init(busy: TMBusyCount) {
        self.busy = busy
    }

    var tag: DPTag? { detail?.tag }

    // MARK: Key note

    /// Whether the tag's key note is sounding, refreshed by `syncKeyNote()`:
    /// DPNote keeps playback in an ivar, so nothing can observe it directly.
    private(set) var keyNotePlaying = false
    private var keyActivation = 0

    var keyNote: DPNote? { tag?.keyNote() }

    func syncKeyNote() {
        let playing = keyNote?.isPlaying ?? false
        if playing != keyNotePlaying { keyNotePlaying = playing }
    }

    /// A press: sounds until released.
    func pressKey() {
        keyActivation += 1
        keyNote?.play()
        syncKeyNote()
    }

    func releaseKey() {
        keyActivation += 1
        keyNote?.stop()
        syncKeyNote()
    }

    /// VoiceOver or Switch Control: sounds for a moment and stops by itself.
    func playTimedKeyNote() {
        keyActivation += 1
        let generation = keyActivation
        guard let note = keyNote else { return }
        note.play()
        syncKeyNote()
        DispatchQueue.main.asyncAfter(deadline: .now() + TagSummaryModel.timedKeyNoteDuration) { [weak self] in
            MainActor.assumeIsolated {
                // A later press or activation owns this shared note's cleanup now.
                guard let self, self.keyActivation == generation else { return }
                note.stop()
                self.syncKeyNote()
            }
        }
    }

    /// Leaving the page silences the note, however it was started.
    func stopKeyNote() {
        keyActivation += 1
        keyNote?.stop()
        syncKeyNote()
    }

    // MARK: Rating

    var ratingDialogPresented = false
    private(set) var ratingBusy = false
    private var ratedTagId: Int32?

    var rated: Bool { ratedTagId != nil && ratedTagId == tag?.tagId }

    func showRating() {
        guard tag != nil, !rated, !ratingBusy else { return }
        ratingDialogPresented = true
    }

    /// Sends a rating. Completion (and any retry) happen on the main queue.
    func rate(_ stars: Int, perform: ((DPTag, Int) -> Bool)? = nil) {
        guard let tag, !ratingBusy else { return }
        let send = perform ?? { tag, stars in
            TMObjC.catching { tag.rate(UInt(stars)); return NSNull() } != nil
        }
        busy.begin()
        ratingBusy = true
        DispatchQueue.global(qos: .userInitiated).async {
            let sent = send(tag, stars)
            DispatchQueue.main.async {
                MainActor.assumeIsolated { [weak self] in
                    guard let self else { return }
                    self.busy.end()
                    self.ratingBusy = false
                    if sent {
                        self.ratedTagId = tag.tagId
                    } else {
                        self.detail?.error = TMRecovery(
                            message: "Your rating couldn't be sent. Check your connection and try again.",
                            retry: { [weak self] in self?.rate(stars, perform: perform) })
                    }
                }
            }
        }
    }

    // MARK: Sheet music

    private(set) var sheetMusicBusy = false

    /// Downloads the sheet music into the file cache if it is not there, then opens the reader.
    func openSheetMusic(fetch: ((URL) -> Data?)? = nil) {
        guard let tag, let location = tag.sheetMusicUri, !sheetMusicBusy else { return }
        let fetch = fetch ?? { url in try? DPRemoteLocation.data(withContentsOf: url) }
        let key = location.cacheKey
        let title = tag.title ?? ""
        let writtenKey = tag.keyNote() != nil ? tag.writtenKey : nil
        busy.begin()
        sheetMusicBusy = true
        DispatchQueue.global(qos: .userInitiated).async {
            var fileURL: URL?
            let path = DPFileCache.path(forKey: key)
            if let path, FileManager.default.fileExists(atPath: path) {
                fileURL = URL(fileURLWithPath: path)
            } else if let url = location.uri, let data = fetch(url), !data.isEmpty {
                DPFileCache.write(data, forKey: key)
                if let path = DPFileCache.path(forKey: key), FileManager.default.fileExists(atPath: path) {
                    fileURL = URL(fileURLWithPath: path)
                }
            }
            let opened = fileURL
            DispatchQueue.main.async {
                MainActor.assumeIsolated { [weak self] in
                    guard let self else { return }
                    self.busy.end()
                    self.sheetMusicBusy = false
                    if let opened {
                        self.detail?.navigator.showSheetMusic(
                            TMSheetMusicDocument(fileURL: opened, title: title, writtenKey: writtenKey))
                    } else {
                        self.detail?.error = TMRecovery(
                            message: "Sheet music couldn't be opened. Check your connection and try again.",
                            retry: { [weak self] in self?.openSheetMusic(fetch: fetch) })
                    }
                }
            }
        }
    }

    // MARK: Lists

    /// The lists this tag is on, in the registry's order.
    var memberships: [String] {
        _ = detail?.listsRevision
        guard let tagId = tag?.tagId, tagId > 0 else { return [] }
        return TMTagLists.keysContaining(Int(tagId))
    }

    func openList(_ key: String) {
        detail?.navigator.showList(key)
    }

    func showPicker() {
        detail?.showListPicker(from: .chip)
    }

    /// Takes the tag off `key`, undoably: undo restores the list exactly as it was.
    func remove(from key: String) {
        guard let tagId = tag?.tagId, tagId > 0 else { return }
        let before = TMTagLists.ids(for: key)
        guard before.contains(Int(tagId)) else { return }
        let name = TMTagLists.name(for: key)
        TMTagLists.remove(Int(tagId), from: key)
        if let manager = detail?.undoManager {
            TagSummaryModel.registerRemoval(from: key, restoring: before, named: name, with: manager)
        }
        UIAccessibility.post(notification: .announcement, argument: "Removed from \(name)")
    }

    /// The manager is its own undo target: nothing here needs the page, which may be
    /// gone by the time the user shakes.
    private static func registerRemoval(from key: String, restoring previous: [Int], named name: String,
                                        with manager: UndoManager) {
        manager.setActionName("Remove from \(name)")
        manager.registerUndo(withTarget: manager) { target in
            TMTagLists.setIds(previous, for: key)
            UIAccessibility.post(notification: .announcement, argument: "Added to \(name)")
            registerRemoval(from: key, restoring: previous, named: name, with: target)
        }
    }

    static func symbolName(for key: String) -> String {
        switch key {
        case TMTagLists.favoriteKey: "heart.fill"
        case TMTagLists.teachableKey: "person.2.fill"
        default: "list.bullet"
        }
    }
}
