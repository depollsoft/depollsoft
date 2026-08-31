import Foundation

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
        queue.sync {
            (try? String(contentsOf: fileURL, encoding: .utf8)) ?? "No logs captured."
        }
    }

    private static func trimIfNeeded() {
        guard let data = try? Data(contentsOf: fileURL), data.count > maximumBytes else {
            return
        }
        let tail = data.suffix(maximumBytes / 2)
        try? Data(tail).write(to: fileURL, options: .atomic)
    }
}
