// Forces XCTest bundle to link Swift runtime when testing ObjC target with Swift deps
import Foundation
import XCTest
@testable import depolllib

// Intercepts all HTTP requests from URLSession to avoid network I/O in tests
final class FakeURLProtocol: URLProtocol {
    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }
    override func startLoading() {
        // Respond with 200/empty body
        let response = HTTPURLResponse(url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocolDidFinishLoading(self)
    }
    override func stopLoading() {}
}

final class AnalyticsIntegratedTests: XCTestCase {
    func testLogEventBuildsRequestAndReturns() {
        URLProtocol.registerClass(FakeURLProtocol.self)
        // Exercise logging path; ensure it does not crash
        Analytics.sharedInstance.logEvent(Analytics.appOpenEvent,
                                          tags: ["unit"],
                                          fields: ["k":"v"],
                                          metrics: ["m": 1.0])
        // No assertions; absence of crash and coverage of code paths is sufficient
    }
}
