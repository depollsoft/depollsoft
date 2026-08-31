import Foundation
import UIKit

@objcMembers
public final class DPAppLog: NSObject {
    private static let queue = DispatchQueue(label: "com.depollsoft.app-log")
    private static let maximumBytes = 256 * 1024
    private static var fileURL: URL {
        let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
        return caches.appendingPathComponent("app.log")
    }

    public static func start() {
        log("Application started")
    }

    public static func log(_ message: String) {
        NSLog("%@", message)
        queue.sync {
            let formatter = ISO8601DateFormatter()
            let line = "\(formatter.string(from: Date())) \(message)\n"
            let data = Data(line.utf8)
            if !FileManager.default.fileExists(atPath: fileURL.path) {
                FileManager.default.createFile(atPath: fileURL.path, contents: data)
            } else if let handle = try? FileHandle(forWritingTo: fileURL) {
                defer { try? handle.close() }
                try? handle.seekToEnd()
                try? handle.write(contentsOf: data)
            }
            trimIfNeeded()
        }
    }

    public static func contents() -> String {
        let value = queue.sync {
            (try? String(contentsOf: fileURL, encoding: .utf8)) ?? "No logs captured."
        }
        DispatchQueue.main.async {
            showCopyConfirmation()
        }
        return value
    }

    private static func showCopyConfirmation() {
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)
        var presenter = window?.rootViewController
        while let presented = presenter?.presentedViewController {
            presenter = presented
        }
        guard presenter?.presentedViewController == nil else { return }
        let alert = UIAlertController(
            title: "Logs Copied",
            message: "Build details and app logs are ready to paste.",
            preferredStyle: .alert
        )
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        presenter?.present(alert, animated: true)
    }

    private static func trimIfNeeded() {
        guard let data = try? Data(contentsOf: fileURL), data.count > maximumBytes else {
            return
        }
        var start = data.count - maximumBytes / 2
        while start < data.count, data[start] & 0xC0 == 0x80 {
            start += 1
        }
        try? data.subdata(in: start..<data.count).write(to: fileURL, options: .atomic)
    }
}
