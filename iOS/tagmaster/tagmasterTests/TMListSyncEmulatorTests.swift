//
//  TMListSyncEmulatorTests.swift
//  tagmasterTests
//
//  The real sync path against the Firestore and Auth emulators: `DPAppDelegate.connectLists(to:)`,
//  `TMTagLists` edits, and the `users/{uid}` document that comes back out the other side.
//
//  Start the emulators with `scripts/firestore-emulator.sh tagmaster`. Without them the whole class skips
//  rather than fails, so an ordinary `xcodebuild test` run is unaffected.
//
//  Two clients are used: this device, and a second app signed in as the same user, which stands in
//  for the user's other device. Both are built from synthetic options for the reserved
//  `demo-tagmaster` project, so nothing here can address a real Firebase project. The test host
//  (`TMTestAppDelegate`) configures no Firebase app at all, so these named apps are the only ones
//  in the process and the app's own `extraInit` never runs.
//

import XCTest
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
@testable import tagmaster

final class TMListSyncEmulatorTests: TMBehaviorTestCase {

    private static let host = "localhost"
    private static let firestorePort = 8080
    private static let authPort = 9099
    private static let projectID = "demo-tagmaster"
    private static var configured: (local: FirebaseApp, remote: FirebaseApp)?

    private var uid = ""
    private var localDoc: DocumentReference!
    private var remoteDoc: DocumentReference!
    private var listener: ListenerRegistration?

    // MARK: - Rig

    override func setUpWithError() throws {
        try XCTSkipUnless(
            TMListSyncEmulatorTests.isListening(TMListSyncEmulatorTests.firestorePort)
                && TMListSyncEmulatorTests.isListening(TMListSyncEmulatorTests.authPort),
            "the Firestore emulator is not listening on \(TMListSyncEmulatorTests.host):"
                + "\(TMListSyncEmulatorTests.firestorePort) (start it with scripts/firestore-emulator.sh tagmaster)"
        )
    }

    override func setUp() {
        super.setUp()
        clearStoredLists()

        let apps = TMListSyncEmulatorTests.emulatorApps()

        // A fresh account per test, and two clients signed in as it: the security rules
        // (`request.auth.uid == uid`) only let the second client touch the same document if it is
        // the same user, which anonymous sign-in cannot arrange.
        let email = "tag-lists-\(UUID().uuidString)@example.invalid"
        let password = "emulator-password"
        guard let created: AuthDataResult = awaitCallback("create the emulator user", {
            Auth.auth(app: apps.local).createUser(withEmail: email, password: password, completion: $0)
        }) else { return }
        uid = created.user.uid
        guard let signedIn: AuthDataResult = awaitCallback("sign the second device in", {
            Auth.auth(app: apps.remote).signIn(withEmail: email, password: password, completion: $0)
        }) else { return }
        XCTAssertEqual(signedIn.user.uid, uid, "both clients must be the same user")

        localDoc = Firestore.firestore(app: apps.local).document("users/\(uid)")
        remoteDoc = Firestore.firestore(app: apps.remote).document("users/\(uid)")

        listener = DPAppDelegate.connectLists(to: localDoc)
        // The listener seeds a brand-new user document from this device; let that land first.
        awaitRemote("the user document to exist") { $0.exists }
    }

    override func tearDown() {
        listener?.remove()
        listener = nil
        DPAppDelegate.disconnectLists()
        if let remoteDoc {
            awaitError("delete the user document") { remoteDoc.delete(completion: $0) }
        }
        if let apps = TMListSyncEmulatorTests.configured {
            try? Auth.auth(app: apps.local).signOut()
            try? Auth.auth(app: apps.remote).signOut()
        }
        localDoc = nil
        remoteDoc = nil
        clearStoredLists()
        super.tearDown()
    }

    /// `TMBehaviorTestCase.clearLists()` only knows the ids key; the registry also has a metadata key.
    private func clearStoredLists() {
        clearLists()
        UserDefaults.standard.removeObject(forKey: TMTagLists.listsDefaultsKey)
        UserDefaults.standard.removeObject(forKey: TMTagLists.infoDefaultsKey)
    }

    // MARK: - Tests

    func testLocalEditsProduceTheDocumentedDocumentShape() {
        let key = TMTagLists.createList(named: "Afterglow set")!
        TMTagLists.add(1809, to: key)
        TMTagLists.add(42, to: key)

        var snapshot = awaitRemote("the new list to reach the server") {
            self.info($0)[key] != nil && self.ids($0)[key] == [1809, 42]
        }
        XCTAssertEqual(ids(snapshot)[key], [1809, 42])
        assertInfo(info(snapshot)[key], name: "Afterglow set", order: 0)
        for reserved in TMTagLists.reservedKeys {
            XCTAssertNil(info(snapshot)[reserved], "the built-in lists are never in listInfo")
        }

        // Renaming touches the metadata only; the tags keep their key and their order.
        XCTAssertTrue(TMTagLists.renameList(key, to: "Afterglow list"))
        snapshot = awaitRemote("the rename to reach the server") {
            (self.info($0)[key]?["name"] as? String) == "Afterglow list"
        }
        XCTAssertEqual(ids(snapshot)[key], [1809, 42])

        // Reordering rewrites `order` for every custom list.
        let second = TMTagLists.createList(named: "Chorus warmups")!
        awaitRemote("the second list to reach the server") { self.info($0)[second] != nil }
        XCTAssertTrue(TMTagLists.reorderLists([second, key]))
        snapshot = awaitRemote("the new order to reach the server") {
            (self.info($0)[second]?["order"] as? NSNumber)?.intValue == 0
        }
        assertInfo(info(snapshot)[second], name: "Chorus warmups", order: 0)
        assertInfo(info(snapshot)[key], name: "Afterglow list", order: 1)

        // Reordering tags inside a list rewrites only that list's array.
        TMTagLists.move(in: key, from: 1, to: 0)
        awaitRemote("the tag order to reach the server") { self.ids($0)[key] == [42, 1809] }

        // Deleting takes the tags and the metadata with it and leaves the other list alone.
        TMTagLists.deleteList(key)
        snapshot = awaitRemote("the deletion to reach the server") { self.info($0)[key] == nil }
        XCTAssertNil(ids(snapshot)[key])
        assertInfo(info(snapshot)[second], name: "Chorus warmups", order: 0)
    }

    func testARemoteChangeUpdatesTheLocalRegistry() {
        let local = TMTagLists.createList(named: "Afterglow set")!
        TMTagLists.add(1809, to: local)
        awaitRemote("the local list to reach the server") { self.info($0)[local] != nil }

        var posts = 0
        let token = NotificationCenter.default.addObserver(forName: .userDataChanged,
                                                          object: nil, queue: nil) { _ in posts += 1 }
        defer { NotificationCenter.default.removeObserver(token) }

        // The user's other device adds a list, renames this one and adds a tag to it.
        awaitError("the other device to write") {
            self.remoteDoc.setData(
                [
                    "lists": [local: [1809, 42], "remote-key": [7, 8]],
                    "listInfo": [
                        local: ["name": "Renamed elsewhere", "order": 1],
                        "remote-key": ["name": "Remote set", "order": 0]
                    ]
                ],
                merge: true,
                completion: $0
            )
        }

        spinUntil("the remote list to reach this device", timeout: 30) {
            TMTagLists.customKeys().contains("remote-key")
        }
        XCTAssertEqual(TMTagLists.customKeys(), ["remote-key", local])
        XCTAssertEqual(TMTagLists.name(for: "remote-key"), "Remote set")
        XCTAssertEqual(TMTagLists.name(for: local), "Renamed elsewhere")
        XCTAssertEqual(TMTagLists.ids(for: "remote-key"), [7, 8])
        XCTAssertEqual(TMTagLists.ids(for: local), [1809, 42])
        XCTAssertGreaterThan(posts, 0, "a remote change posts .userDataChanged")

        // And a remote deletion drops the list here too.
        awaitError("the other device to delete its list") {
            self.remoteDoc.setData(
                [
                    "lists": [local: [1809, 42]],
                    "listInfo": [local: ["name": "Renamed elsewhere", "order": 0]]
                ],
                completion: $0
            )
        }
        spinUntil("the remote deletion to reach this device", timeout: 30) {
            !TMTagLists.customKeys().contains("remote-key")
        }
        XCTAssertEqual(TMTagLists.customKeys(), [local])
        XCTAssertEqual(TMTagLists.ids(for: "remote-key"), [])
    }

    // MARK: - Reading the document

    private func ids(_ snapshot: DocumentSnapshot?) -> [String: [Int]] {
        guard let raw = snapshot?.get("lists") as? [String: Any] else { return [:] }
        return raw.compactMapValues { ($0 as? [Any])?.compactMap { ($0 as? NSNumber)?.intValue } }
    }

    private func info(_ snapshot: DocumentSnapshot?) -> [String: [String: Any]] {
        (snapshot?.get("listInfo") as? [String: [String: Any]]) ?? [:]
    }

    private func assertInfo(_ entry: [String: Any]?, name: String, order: Int,
                            file: StaticString = #filePath, line: UInt = #line) {
        guard let entry else {
            return XCTFail("expected a listInfo entry", file: file, line: line)
        }
        XCTAssertEqual(entry["name"] as? String, name, file: file, line: line)
        XCTAssertEqual((entry["order"] as? NSNumber)?.intValue, order, file: file, line: line)
        XCTAssertEqual(Set(entry.keys), ["name", "order"], file: file, line: line)
    }

    /// Re-reads the document from the second client, straight from the server, until it matches.
    @discardableResult
    private func awaitRemote(_ description: String,
                             timeout: TimeInterval = 30,
                             file: StaticString = #filePath,
                             line: UInt = #line,
                             _ matches: @escaping (DocumentSnapshot) -> Bool) -> DocumentSnapshot? {
        let deadline = Date(timeIntervalSinceNow: timeout)
        var latest: DocumentSnapshot?
        repeat {
            latest = awaitCallback("read \(description)", timeout: timeout) {
                self.remoteDoc.getDocument(source: .server, completion: $0)
            }
            if let latest, matches(latest) { return latest }
            RunLoop.current.run(mode: .default, before: Date(timeIntervalSinceNow: 0.05))
        } while Date() < deadline
        XCTFail("Timed out waiting for \(description); document was \(latest?.data() ?? [:])",
                file: file, line: line)
        return latest
    }

    // MARK: - Waiting

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
        let apps = (local: configure(named: "tagmasterEmulatorDevice"),
                    remote: configure(named: "tagmasterEmulatorOtherDevice"))
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
