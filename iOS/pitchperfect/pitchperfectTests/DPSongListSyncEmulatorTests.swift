//
//  DPSongListSyncEmulatorTests.swift
//  pitchperfectTests
//
//  The real sync path against the Firestore and Auth emulators: the set list
//  documents `users/{uid}/songLists/{listId}` that `DPSongsModel` writes, and
//  the changes that come back the other way.
//
//  Start the emulators with `scripts/firestore-emulator.sh pitchperfect`.
//  Without them the whole class skips rather than fails, so an ordinary
//  `xcodebuild test` run is unaffected.
//
//  Two clients are used: this device, and a second app signed in as the same
//  user, which stands in for the user's other device. Both are built from
//  synthetic options for the reserved `demo-pitchperfect` project, so nothing
//  here can address a real Firebase project, and neither touches the process's
//  default `FirebaseApp`.
//

import XCTest
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
@testable import pitchperfect

final class DPSongListSyncEmulatorTests: XCTestCase {

    private static let host = "localhost"
    private static let firestorePort = 8080
    private static let authPort = 9099
    private static let projectID = "demo-pitchperfect"
    private static var configured: (local: FirebaseApp, remote: FirebaseApp)?

    private var uid = ""
    private var model: DPSongsModel!
    private var localDoc: DocumentReference!
    private var remoteDoc: DocumentReference!
    private var savedDefaults: [String: Any] = [:]

    // MARK: - Rig

    override func setUpWithError() throws {
        try XCTSkipUnless(
            DPSongListSyncEmulatorTests.isListening(DPSongListSyncEmulatorTests.firestorePort)
                && DPSongListSyncEmulatorTests.isListening(DPSongListSyncEmulatorTests.authPort),
            "the Firestore emulator is not listening on \(DPSongListSyncEmulatorTests.host):"
                + "\(DPSongListSyncEmulatorTests.firestorePort) "
                + "(start it with scripts/firestore-emulator.sh pitchperfect)"
        )
        saveAndClearDefaults()

        let apps = DPSongListSyncEmulatorTests.emulatorApps()

        // A fresh account per test, and two clients signed in as it: the rules
        // (`request.auth.uid == uid`) only let the second client touch these
        // documents if it is the same user.
        let email = "set-lists-\(UUID().uuidString)@example.invalid"
        let password = "emulator-password"
        let created: AuthDataResult = try signIn("create the emulator user") {
            Auth.auth(app: apps.local).createUser(withEmail: email, password: password, completion: $0)
        }
        uid = created.user.uid
        let signedIn: AuthDataResult = try signIn("sign the second device in") {
            Auth.auth(app: apps.remote).signIn(withEmail: email, password: password, completion: $0)
        }
        XCTAssertEqual(signedIn.user.uid, uid, "both clients must be the same user")

        localDoc = Firestore.firestore(app: apps.local).document("users/\(uid)")
        remoteDoc = Firestore.firestore(app: apps.remote).document("users/\(uid)")

        model = DPSongsModel()
        model.attachToFirestore(userDoc: localDoc, store: true)
        // The attachment uploads this device's home list; let that land first.
        awaitList("default") { $0.exists }
    }

    override func tearDown() {
        model?.detachFromFirestore()
        model = nil
        if let remoteDoc {
            let listed: QuerySnapshot? = awaitCallback("list the set lists to delete") {
                remoteDoc.collection("songLists").getDocuments(completion: $0)
            }
            for document in listed?.documents ?? [] {
                awaitError("delete \(document.documentID)") { document.reference.delete(completion: $0) }
            }
        }
        if let apps = DPSongListSyncEmulatorTests.configured {
            try? Auth.auth(app: apps.local).signOut()
            try? Auth.auth(app: apps.remote).signOut()
        }
        localDoc = nil
        remoteDoc = nil
        restoreDefaults()
        super.tearDown()
    }

    /// Firebase Auth stores its session in the keychain, which an unsigned test
    /// build (CI passes `CODE_SIGNING_ALLOWED=NO`) cannot reach. That is an
    /// environment limit, not a failure of the code under test, so it skips.
    private func signIn<T>(_ description: String,
                           _ work: (@escaping (T?, Error?) -> Void) -> Void) throws -> T {
        let done = expectation(description: description)
        var value: T?
        var failure: Error?
        work { result, error in
            value = result
            failure = error
            done.fulfill()
        }
        wait(for: [done], timeout: 30)
        if let failure = failure as NSError?,
           failure.domain == AuthErrorDomain,
           failure.code == AuthErrorCode.keychainError.rawValue {
            throw XCTSkip("Firebase Auth cannot reach the simulator keychain. Build this suite "
                          + "with code signing enabled (drop CODE_SIGNING_ALLOWED=NO).")
        }
        return try XCTUnwrap(value, "\(description) failed: \(failure.map(String.init(describing:)) ?? "no result")")
    }

    private func saveAndClearDefaults() {
        let defaults = UserDefaults.standard
        for key in [DPSongsModel.legacySongsKey, DPSongsModel.songListsKey, DPSongsModel.currentListKey] {
            if let value = defaults.object(forKey: key) { savedDefaults[key] = value }
            defaults.removeObject(forKey: key)
        }
    }

    private func restoreDefaults() {
        let defaults = UserDefaults.standard
        for key in [DPSongsModel.legacySongsKey, DPSongsModel.songListsKey, DPSongsModel.currentListKey] {
            if let value = savedDefaults[key] {
                defaults.set(value, forKey: key)
            } else {
                defaults.removeObject(forKey: key)
            }
        }
        savedDefaults = [:]
    }

    private func song(_ name: String) -> DPPitchedSong {
        let song = DPPitchedSong()
        song.name = name
        song.key = DPKey.majorKeys()[6] as? DPKey
        return song
    }

    // MARK: - Tests

    func testLocalEditsProduceTheDocumentedDocuments() {
        guard model != nil else { return }
        let list = try! XCTUnwrap(model.createList(named: "Saturday show"))
        list.songs = [song("Blue Skies"), song("Shenandoah")]
        list.storeValue()

        var snapshot = awaitList(list.id) {
            self.titles($0) == ["Blue Skies", "Shenandoah"]
        }
        XCTAssertEqual(snapshot?.get("name") as? String, "Saturday show")
        XCTAssertEqual(order(snapshot), 0)
        XCTAssertEqual(Set(snapshot?.data()?.keys ?? [:].keys), ["name", "songs", "order"])

        // Renaming touches the name only; the songs keep their ids.
        let ids = songIds(snapshot)
        XCTAssertTrue(model.renameList(list, to: "Sunday show"))
        snapshot = awaitList(list.id) { ($0.get("name") as? String) == "Sunday show" }
        XCTAssertEqual(songIds(snapshot), ids, "a rename never moves songs")

        // Reordering rewrites `order` for every custom list.
        let second = try! XCTUnwrap(model.createList(named: "Afterglow"))
        awaitList(second.id) { $0.exists }
        XCTAssertTrue(model.reorderLists([second.id, list.id]))
        XCTAssertEqual(order(awaitList(second.id) { self.order($0) == 0 }), 0)
        XCTAssertEqual(order(awaitList(list.id) { self.order($0) == 1 }), 1)

        // A duplicate is its own document with fresh song ids.
        let copy = try! XCTUnwrap(model.duplicateList(list))
        let copySnapshot = awaitList(copy.id) { self.titles($0) == ["Blue Skies", "Shenandoah"] }
        XCTAssertEqual(copySnapshot?.get("name") as? String, "Sunday show copy")
        XCTAssertEqual(order(copySnapshot), 2)
        XCTAssertTrue(Set(songIds(copySnapshot)).isDisjoint(with: Set(ids)),
                      "copies across lists are independent songs")

        // Songs copied into another list travel the same way.
        model.copySongs(list.songs, to: second)
        let secondSnapshot = awaitList(second.id) { self.titles($0) == ["Blue Skies", "Shenandoah"] }
        XCTAssertTrue(Set(songIds(secondSnapshot)).isDisjoint(with: Set(ids)))

        // Deleting takes the document with it and leaves the others alone.
        XCTAssertTrue(model.deleteList(copy))
        awaitList(copy.id) { !$0.exists }
        XCTAssertEqual(order(awaitList(list.id) { $0.exists }), 1)
    }

    func testRemoteChangesUpdateTheLocalModel() {
        guard model != nil else { return }
        let local = try! XCTUnwrap(model.createList(named: "Saturday show"))
        awaitList(local.id) { $0.exists }

        // The user's other device adds a set list of its own.
        write("remote-show-k3f9", ["name": "Remote show", "songs": [], "order": 1])
        spinUntil("the remote list to reach this device") {
            self.model.songLists["remote-show-k3f9"] != nil
        }
        XCTAssertEqual(model.songLists["remote-show-k3f9"]?.name, "Remote show")
        XCTAssertEqual(model.songLists["remote-show-k3f9"]?.order, 1)

        // …renames it, and moves it ahead of this device's list.
        write("remote-show-k3f9", ["name": "Remote encore", "order": 0])
        spinUntil("the remote rename to reach this device") {
            self.model.songLists["remote-show-k3f9"]?.name == "Remote encore"
        }
        write(local.id, ["order": 1])
        spinUntil("the remote reorder to reach this device") {
            self.model.songLists["remote-show-k3f9"]?.order == 0 && self.model.songLists[local.id]?.order == 1
        }
        XCTAssertEqual(model.orderedLists.map(\.id), ["default", "remote-show-k3f9", local.id])

        // Deleting the current list elsewhere falls this device back to My Songs
        // without taking the Songs tab anywhere.
        model.currentListId = local.id
        XCTAssertEqual(model.currentListId, local.id)
        awaitError("the other device to delete its list") {
            self.remoteDoc.collection("songLists").document(local.id).delete(completion: $0)
        }
        spinUntil("the remote deletion to reach this device") {
            self.model.songLists[local.id] == nil
        }
        XCTAssertEqual(model.currentListId, "default")
        XCTAssertTrue(model.currentList === model.defaultSongList)
        let stored = UserDefaults.standard.dictionary(forKey: DPSongsModel.songListsKey)
        XCTAssertNil(stored?[local.id], "a remote delete drops the local entry too")
        XCTAssertNotNil(stored?["remote-show-k3f9"], "and never resurrects what it deleted")
    }

    // MARK: - Reading and writing from the second client

    private func write(_ listId: String, _ value: [String: Any]) {
        awaitError("the other device to write \(listId)") {
            self.remoteDoc.collection("songLists").document(listId)
                .setData(value, merge: true, completion: $0)
        }
    }

    private func titles(_ snapshot: DocumentSnapshot?) -> [String] {
        songField(snapshot, "Name")
    }

    private func songIds(_ snapshot: DocumentSnapshot?) -> [String] {
        songField(snapshot, "Id")
    }

    private func songField(_ snapshot: DocumentSnapshot?, _ key: String) -> [String] {
        let raw = snapshot?.get("songs") as? [[String: Any]] ?? []
        return raw.compactMap { $0[key] as? String }
    }

    private func order(_ snapshot: DocumentSnapshot?) -> Int? {
        (snapshot?.get("order") as? NSNumber)?.intValue
    }

    /// Re-reads one set list document from the second client, straight from the
    /// server, until it matches.
    @discardableResult
    private func awaitList(_ listId: String,
                           timeout: TimeInterval = 30,
                           file: StaticString = #filePath,
                           line: UInt = #line,
                           _ matches: @escaping (DocumentSnapshot) -> Bool) -> DocumentSnapshot? {
        let reference = remoteDoc.collection("songLists").document(listId)
        let deadline = Date(timeIntervalSinceNow: timeout)
        var latest: DocumentSnapshot?
        repeat {
            latest = awaitCallback("read \(listId)", timeout: timeout) {
                reference.getDocument(source: .server, completion: $0)
            }
            if let latest, matches(latest) { return latest }
            RunLoop.current.run(mode: .default, before: Date(timeIntervalSinceNow: 0.05))
        } while Date() < deadline
        XCTFail("Timed out waiting for \(listId); document was \(latest?.data() ?? [:])",
                file: file, line: line)
        return latest
    }

    // MARK: - Waiting

    private func spinUntil(_ description: String,
                           timeout: TimeInterval = 30,
                           file: StaticString = #filePath,
                           line: UInt = #line,
                           _ condition: () -> Bool) {
        let deadline = Date(timeIntervalSinceNow: timeout)
        while !condition(), Date() < deadline {
            RunLoop.current.run(mode: .default, before: Date(timeIntervalSinceNow: 0.02))
        }
        XCTAssertTrue(condition(), "Timed out waiting for \(description)", file: file, line: line)
    }

    @discardableResult
    private func awaitCallback<T>(_ description: String,
                                  timeout: TimeInterval = 30,
                                  file: StaticString = #filePath,
                                  line: UInt = #line,
                                  _ work: (@escaping (T?, Error?) -> Void) -> Void) -> T? {
        let done = expectation(description: description)
        var value: T?
        var failure: Error?
        work { result, error in
            value = result
            failure = error
            done.fulfill()
        }
        wait(for: [done], timeout: timeout)
        if let failure { XCTFail("\(description) failed: \(failure)", file: file, line: line) }
        return value
    }

    private func awaitError(_ description: String,
                            timeout: TimeInterval = 30,
                            file: StaticString = #filePath,
                            line: UInt = #line,
                            _ work: (@escaping (Error?) -> Void) -> Void) {
        let done = expectation(description: description)
        var failure: Error?
        work { error in
            failure = error
            done.fulfill()
        }
        wait(for: [done], timeout: timeout)
        if let failure { XCTFail("\(description) failed: \(failure)", file: file, line: line) }
    }

    // MARK: - Emulator plumbing

    /// Two Firebase apps pointed at the emulators, configured once for the whole process.
    private static func emulatorApps() -> (local: FirebaseApp, remote: FirebaseApp) {
        if let configured { return configured }
        let apps = (local: configure(named: "pitchperfectEmulatorDevice"),
                    remote: configure(named: "pitchperfectEmulatorOtherDevice"))
        configured = apps
        return apps
    }

    private static func configure(named name: String) -> FirebaseApp {
        if let existing = FirebaseApp.app(name: name) { return existing }
        // Well-formed but meaningless credentials: they belong to no Firebase project at all.
        // `FirebaseApp.configure` validates the shape of the app id (`1:<number>:ios:<hex>`) and
        // refuses to register the app otherwise, so this one is shaped like a real id.
        let options = FirebaseOptions(googleAppID: "1:1:ios:0000000000000001", gcmSenderID: "1")
        options.projectID = projectID
        // Firebase Installations refuses an app whose key is not shaped like a real one (`A`
        // plus 38 characters), and refusing takes Firestore's auth provider down with it.
        options.apiKey = "AIzaSyEmulatorOnlyEmulatorOnlyEmulator0"
        FirebaseApp.configure(name: name, options: options)
        let app = FirebaseApp.app(name: name)!
        Auth.auth(app: app).useEmulator(withHost: host, port: authPort)
        let firestore = Firestore.firestore(app: app)
        let settings = firestore.settings
        settings.host = "\(host):\(firestorePort)"
        settings.isSSLEnabled = false
        settings.cacheSettings = MemoryCacheSettings()
        firestore.settings = settings
        return app
    }

    private static func isListening(_ port: Int) -> Bool {
        guard let socket = try? Socket(host: host, port: port) else { return false }
        socket.close()
        return true
    }

    /// A one-shot TCP connect, so a missing emulator skips the suite instead of timing out in it.
    private final class Socket {
        private let descriptor: Int32

        init(host: String, port: Int) throws {
            var hints = addrinfo(ai_flags: 0, ai_family: AF_INET, ai_socktype: SOCK_STREAM,
                                 ai_protocol: 0, ai_addrlen: 0, ai_canonname: nil,
                                 ai_addr: nil, ai_next: nil)
            var info: UnsafeMutablePointer<addrinfo>?
            guard getaddrinfo(host, String(port), &hints, &info) == 0, let first = info else {
                throw CocoaError(.fileNoSuchFile)
            }
            defer { freeaddrinfo(info) }
            descriptor = socket(first.pointee.ai_family, first.pointee.ai_socktype, first.pointee.ai_protocol)
            guard descriptor >= 0 else { throw CocoaError(.fileNoSuchFile) }
            guard connect(descriptor, first.pointee.ai_addr, first.pointee.ai_addrlen) == 0 else {
                Darwin.close(descriptor)
                throw CocoaError(.fileNoSuchFile)
            }
        }

        func close() { Darwin.close(descriptor) }
    }
}
