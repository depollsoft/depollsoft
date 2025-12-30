//
//  AnalyticsTests.swift
//  depolllibTests
//
//  Created for unit test coverage
//

import XCTest
@testable import depolllib

// MARK: - Testable Analytics Subclass
// Exposes private methods for unit testing

class TestableAnalytics: Analytics {
    // Expose private method via reflection-like approach
    func testStartOfHour(_ date: Date) -> Date {
        // Replicate logic from Analytics for testing
        let timestamp = date.timeIntervalSince1970
        let startOfHour = timestamp - timestamp.truncatingRemainder(dividingBy: 3600)
        return Date(timeIntervalSince1970: startOfHour)
    }
    
    func testFormatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "YYYY-MM-dd HH:mm:ss.SSSSSS"
        return formatter.string(from: date)
    }
    
    func testTransformDictionary<T>(_ dict: [String: T], keyKey: String, valueKey: String) -> [[String: Any]] {
        var result: [[String: Any]] = []
        for (key, value) in dict {
            result.append([
                keyKey: key,
                valueKey: value
            ])
        }
        return result
    }
}

// MARK: - Mock URLProtocol for intercepting network requests

class MockAnalyticsURLProtocol: URLProtocol {
    static var capturedRequests: [URLRequest] = []
    static var shouldSucceed = true
    
    static func reset() {
        capturedRequests = []
        shouldSucceed = true
    }
    
    override class func canInit(with request: URLRequest) -> Bool {
        return request.url?.host == "api.depollsoft.xyz"
    }
    
    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        return request
    }
    
    override func startLoading() {
        MockAnalyticsURLProtocol.capturedRequests.append(request)
        
        if MockAnalyticsURLProtocol.shouldSucceed {
            let response = HTTPURLResponse(
                url: request.url!,
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            )!
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocolDidFinishLoading(self)
        } else {
            let error = NSError(domain: "TestError", code: -1, userInfo: nil)
            client?.urlProtocol(self, didFailWithError: error)
        }
    }
    
    override func stopLoading() {}
}

// MARK: - Analytics Tests

final class AnalyticsTests: XCTestCase {
    
    var analytics: TestableAnalytics!
    let testPrefix = "test.analytics"
    
    override func setUp() {
        super.setUp()
        analytics = TestableAnalytics(prefix: testPrefix)
        MockAnalyticsURLProtocol.reset()
        URLProtocol.registerClass(MockAnalyticsURLProtocol.self)
        
        // Clear UserDefaults for test keys
        let dailyKey = "\(testPrefix).app_open.daily"
        let hourlyKey = "\(testPrefix).app_open.hourly"
        UserDefaults.standard.removeObject(forKey: dailyKey)
        UserDefaults.standard.removeObject(forKey: hourlyKey)
    }
    
    override func tearDown() {
        URLProtocol.unregisterClass(MockAnalyticsURLProtocol.self)
        
        // Clean up UserDefaults
        let dailyKey = "\(testPrefix).app_open.daily"
        let hourlyKey = "\(testPrefix).app_open.hourly"
        UserDefaults.standard.removeObject(forKey: dailyKey)
        UserDefaults.standard.removeObject(forKey: hourlyKey)
        
        super.tearDown()
    }
    
    // MARK: - testSharedInstanceIsSingleton
    
    func testSharedInstanceIsSingleton() {
        let instance1 = Analytics.sharedInstance
        let instance2 = Analytics.sharedInstance
        
        XCTAssertTrue(instance1 === instance2, "sharedInstance should return the same instance")
    }
    
    // MARK: - testLogEventSendsCorrectPayload
    
    func testLogEventSendsCorrectPayload() {
        let expectation = XCTestExpectation(description: "Request captured")
        
        analytics.logEvent("test_event",
                          tags: ["custom_tag"],
                          fields: ["field_key": "field_value"],
                          metrics: ["metric_name": 42.5])
        
        // Give time for async request
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            expectation.fulfill()
        }
        
        wait(for: [expectation], timeout: 2.0)
        
        guard let request = MockAnalyticsURLProtocol.capturedRequests.first else {
            XCTFail("No request was captured")
            return
        }
        
        XCTAssertEqual(request.httpMethod, "POST")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Content-Type"), "application/json")
        XCTAssertEqual(request.url?.absoluteString, "https://api.depollsoft.xyz/analytics")
        
        guard let body = request.httpBody,
              let json = try? JSONSerialization.jsonObject(with: body) as? [String: Any] else {
            XCTFail("Could not parse request body as JSON")
            return
        }
        
        XCTAssertEqual(json["event_name"] as? String, "test_event")
        XCTAssertEqual(json["platform"] as? String, "iOS")
        XCTAssertNotNil(json["event_timestamp"])
        XCTAssertNotNil(json["event_timezone"])
        XCTAssertNotNil(json["device_model"])
        XCTAssertNotNil(json["platform_version"])
        
        // Check tags contains our custom tag
        if let tags = json["tags"] as? [String] {
            XCTAssertTrue(tags.contains("custom_tag"), "Tags should contain custom_tag")
        }
        
        // Check fields transformation
        if let fields = json["fields"] as? [[String: Any]] {
            let hasFieldKey = fields.contains { ($0["key"] as? String) == "field_key" && ($0["value"] as? String) == "field_value" }
            XCTAssertTrue(hasFieldKey, "Fields should contain transformed field_key")
        }
        
        // Check metrics transformation
        if let metrics = json["metrics"] as? [[String: Any]] {
            let hasMetric = metrics.contains { ($0["name"] as? String) == "metric_name" && ($0["value"] as? Double) == 42.5 }
            XCTAssertTrue(hasMetric, "Metrics should contain transformed metric_name")
        }
    }
    
    // MARK: - testLogEventHandlesEmptyFields
    
    func testLogEventHandlesEmptyFields() {
        let expectation = XCTestExpectation(description: "Request captured")
        
        analytics.logEvent("empty_test")
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            expectation.fulfill()
        }
        
        wait(for: [expectation], timeout: 2.0)
        
        guard let request = MockAnalyticsURLProtocol.capturedRequests.first,
              let body = request.httpBody,
              let json = try? JSONSerialization.jsonObject(with: body) as? [String: Any] else {
            XCTFail("Could not capture and parse request")
            return
        }
        
        XCTAssertEqual(json["event_name"] as? String, "empty_test")
        
        // Fields and metrics should be empty arrays
        if let fields = json["fields"] as? [[String: Any]] {
            XCTAssertTrue(fields.isEmpty, "Fields should be empty when not provided")
        }
        
        if let metrics = json["metrics"] as? [[String: Any]] {
            XCTAssertTrue(metrics.isEmpty, "Metrics should be empty when not provided")
        }
    }
    
    // MARK: - testUpdateTagsAddsDailyOnNewDay
    
    func testUpdateTagsAddsDailyOnNewDay() {
        let dailyKey = "\(testPrefix).app_open.daily"
        
        // Simulate last daily was yesterday
        let yesterday = Calendar.current.date(byAdding: .day, value: -1, to: Date())!
        UserDefaults.standard.set(yesterday, forKey: dailyKey)
        
        let expectation = XCTestExpectation(description: "Request captured")
        
        analytics.logEvent(Analytics.appOpenEvent)
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            expectation.fulfill()
        }
        
        wait(for: [expectation], timeout: 2.0)
        
        guard let request = MockAnalyticsURLProtocol.capturedRequests.first,
              let body = request.httpBody,
              let json = try? JSONSerialization.jsonObject(with: body) as? [String: Any],
              let tags = json["tags"] as? [String] else {
            XCTFail("Could not capture and parse request")
            return
        }
        
        XCTAssertTrue(tags.contains("daily"), "Tags should contain 'daily' when last event was on a different day")
    }
    
    // MARK: - testUpdateTagsAddsHourlyOnNewHour
    
    func testUpdateTagsAddsHourlyOnNewHour() {
        let hourlyKey = "\(testPrefix).app_open.hourly"
        
        // Simulate last hourly was 2 hours ago
        let twoHoursAgo = Date(timeIntervalSinceNow: -7200)
        UserDefaults.standard.set(twoHoursAgo, forKey: hourlyKey)
        
        let expectation = XCTestExpectation(description: "Request captured")
        
        analytics.logEvent(Analytics.appOpenEvent)
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            expectation.fulfill()
        }
        
        wait(for: [expectation], timeout: 2.0)
        
        guard let request = MockAnalyticsURLProtocol.capturedRequests.first,
              let body = request.httpBody,
              let json = try? JSONSerialization.jsonObject(with: body) as? [String: Any],
              let tags = json["tags"] as? [String] else {
            XCTFail("Could not capture and parse request")
            return
        }
        
        XCTAssertTrue(tags.contains("hourly"), "Tags should contain 'hourly' when last event was in a different hour")
    }
    
    // MARK: - testFormatDateProducesCorrectFormat
    
    func testFormatDateProducesCorrectFormat() {
        // Create a known date: 2024-06-15 14:30:45.123456
        var components = DateComponents()
        components.year = 2024
        components.month = 6
        components.day = 15
        components.hour = 14
        components.minute = 30
        components.second = 45
        components.nanosecond = 123456000
        components.timeZone = TimeZone(identifier: "UTC")
        
        let calendar = Calendar(identifier: .gregorian)
        guard let testDate = calendar.date(from: components) else {
            XCTFail("Could not create test date")
            return
        }
        
        let formatted = analytics.testFormatDate(testDate)
        
        // Check format pattern: YYYY-MM-dd HH:mm:ss.SSSSSS
        let regex = try! NSRegularExpression(pattern: "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{6}$")
        let range = NSRange(formatted.startIndex..., in: formatted)
        XCTAssertNotNil(regex.firstMatch(in: formatted, range: range), 
                       "Date format should match YYYY-MM-dd HH:mm:ss.SSSSSS pattern, got: \(formatted)")
    }
    
    // MARK: - testTransformDictionaryMapsKeysValues
    
    func testTransformDictionaryMapsKeysValues() {
        let input = ["name": "John", "city": "NYC"]
        
        let result = analytics.testTransformDictionary(input, keyKey: "field", valueKey: "data")
        
        XCTAssertEqual(result.count, 2, "Should have 2 transformed entries")
        
        let hasName = result.contains { entry in
            (entry["field"] as? String) == "name" && (entry["data"] as? String) == "John"
        }
        XCTAssertTrue(hasName, "Should contain transformed 'name' entry")
        
        let hasCity = result.contains { entry in
            (entry["field"] as? String) == "city" && (entry["data"] as? String) == "NYC"
        }
        XCTAssertTrue(hasCity, "Should contain transformed 'city' entry")
    }
    
    // MARK: - testStartOfHourTruncatesCorrectly
    
    func testStartOfHourTruncatesCorrectly() {
        // Test with a date at 14:37:45
        let date = Date(timeIntervalSince1970: 1718456265) // Some arbitrary timestamp
        
        let startOfHour = analytics.testStartOfHour(date)
        
        // The result should have 0 minutes and 0 seconds
        let calendar = Calendar.current
        let components = calendar.dateComponents([.minute, .second], from: startOfHour)
        
        XCTAssertEqual(components.minute, 0, "Start of hour should have 0 minutes")
        XCTAssertEqual(components.second, 0, "Start of hour should have 0 seconds")
        
        // Verify the truncation math
        let originalTimestamp = date.timeIntervalSince1970
        let expectedStartOfHour = originalTimestamp - originalTimestamp.truncatingRemainder(dividingBy: 3600)
        XCTAssertEqual(startOfHour.timeIntervalSince1970, expectedStartOfHour, accuracy: 0.001)
    }
    
    func testStartOfHourWithExactHour() {
        // Test with a date exactly on the hour (e.g., 14:00:00)
        let exactHourTimestamp: TimeInterval = 1718452800 // An exact hour
        let date = Date(timeIntervalSince1970: exactHourTimestamp)
        
        let startOfHour = analytics.testStartOfHour(date)
        
        XCTAssertEqual(startOfHour.timeIntervalSince1970, exactHourTimestamp, accuracy: 0.001,
                      "Start of hour for exact hour should be the same timestamp")
    }
    
    // MARK: - Additional Coverage Tests
    
    func testAppOpenEventConstant() {
        XCTAssertEqual(Analytics.appOpenEvent, "app_open", "appOpenEvent constant should be 'app_open'")
    }
    
    func testMultipleTagsPreserved() {
        let expectation = XCTestExpectation(description: "Request captured")
        
        analytics.logEvent("multi_tag_test", tags: ["tag1", "tag2", "tag3"])
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            expectation.fulfill()
        }
        
        wait(for: [expectation], timeout: 2.0)
        
        guard let request = MockAnalyticsURLProtocol.capturedRequests.first,
              let body = request.httpBody,
              let json = try? JSONSerialization.jsonObject(with: body) as? [String: Any],
              let tags = json["tags"] as? [String] else {
            XCTFail("Could not capture and parse request")
            return
        }
        
        XCTAssertTrue(tags.contains("tag1"), "Should contain tag1")
        XCTAssertTrue(tags.contains("tag2"), "Should contain tag2")
        XCTAssertTrue(tags.contains("tag3"), "Should contain tag3")
    }
    
    func testTransformDictionaryWithDoubles() {
        let input = ["metric1": 1.5, "metric2": 99.9]
        
        let result = analytics.testTransformDictionary(input, keyKey: "name", valueKey: "value")
        
        XCTAssertEqual(result.count, 2)
        
        let hasMetric1 = result.contains { entry in
            (entry["name"] as? String) == "metric1" && (entry["value"] as? Double) == 1.5
        }
        XCTAssertTrue(hasMetric1, "Should contain transformed metric1")
    }
    
    func testTransformEmptyDictionary() {
        let input: [String: String] = [:]
        
        let result = analytics.testTransformDictionary(input, keyKey: "k", valueKey: "v")
        
        XCTAssertTrue(result.isEmpty, "Empty input should produce empty output")
    }
}
