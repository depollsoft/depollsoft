//
//  Analytics.swift
//  depolllib
//
//  Created by David Poll on 6/16/21.
//  Copyright © 2021 DepollSoft. All rights reserved.
//

import Foundation

public class Analytics {
    public static let appOpenEvent = "app_open"
    private static let endpoint = URL(string: "https://api.depollsoft.xyz/analytics")!
    private var prefix: String
    public init(prefix: String) {
        self.prefix = prefix
    }
    
    private func persistKey(_ key: String) -> String {
        return "\(prefix).\(key)"
    }
    
    private func getOSInfo() -> String {
        let os = ProcessInfo().operatingSystemVersion
        return String(os.majorVersion) + "." + String(os.minorVersion) + "." + String(os.patchVersion)
    }
    
    private lazy var modelName: String = {
        var systemInfo = utsname()
        uname(&systemInfo)
        let machineMirror = Mirror(reflecting: systemInfo.machine)
        let identifier = machineMirror.children.reduce("") { identifier, element in
            guard let value = element.value as? Int8, value != 0 else { return identifier }
            return identifier + String(UnicodeScalar(UInt8(value)))
        }
        return identifier
    }()
    
    private func startOfHour(_ date: Date) -> Date {
        let timestamp = date.timeIntervalSince1970
        let startOfHour = timestamp - timestamp.truncatingRemainder(dividingBy: 3600)
        return Date(timeIntervalSince1970: startOfHour)
    }
    
    private func updateTags(_ tags: Set<String>, forEvent event: String) -> Set<String> {
        var newTags = tags
        let dailyKey = persistKey("\(event).daily")
        let hourlyKey = persistKey("\(event).hourly")
        let now = Date()
        let lastDaily = UserDefaults.standard.object(forKey: dailyKey) as? Date
        let lastHourly = UserDefaults.standard.object(forKey: hourlyKey) as? Date
        let calendar = Calendar.current
        if lastDaily == nil || calendar.startOfDay(for: lastDaily!) != calendar.startOfDay(for: now) {
            newTags.insert("daily")
            UserDefaults.standard.set(now, forKey: dailyKey)
        }
        if lastHourly == nil || startOfHour(lastHourly!) != startOfHour(now) {
            newTags.insert("hourly")
            UserDefaults.standard.set(now, forKey: hourlyKey)
        }
        return newTags
    }
    
    private func formatDate(_ date: Date) -> String{
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "YYYY-MM-dd HH:mm:ss.SSSSSS"
        return formatter.string(from: date)
    }
    
    private func transformDictionary<T>(_ dict: [String: T], keyKey: String, valueKey: String) -> [[String: Any]] {
        var result: [[String: Any]] = []
        for (key, value) in dict {
            result.append([
                            keyKey: key,
                            valueKey:value
            ])
        }
        return result
    }
    
    public func logEvent(_ eventName: String,
                         tags: Set<String> = [],
                         fields: [String: String] = [:],
                         metrics: [String: Double] = [:]) {
        do{
            var request = URLRequest(url: Analytics.endpoint)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try JSONSerialization.data(withJSONObject: [
                "tags": Array(updateTags(tags, forEvent: eventName)),
                "app_id": Bundle.main.bundleIdentifier!,
                "app_version": Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion")!,
                "event_timestamp": formatDate(Date()),
                "event_timezone": TimeZone.current.identifier,
                "event_name": eventName,
                "fields": transformDictionary(fields, keyKey: "key", valueKey: "value"),
                "metrics": transformDictionary(metrics, keyKey: "name", valueKey: "value"),
                "device_model": modelName,
                "platform": "iOS",
                "platform_version": getOSInfo()
            ])
            URLSession.shared.dataTask(with: request).resume()
        } catch {
            print("Caught while logging: \(error)")
        }
    }
    
    public private(set) static var sharedInstance: Analytics = Analytics(prefix: "depolllib.analytics")
}
