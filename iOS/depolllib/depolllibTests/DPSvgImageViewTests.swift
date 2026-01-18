
import XCTest
@testable import depolllib
import WebKit

class DPSvgImageViewTests: XCTestCase {
    
    func testLoadUrl() {
        let svg = DPSvgImageView()
        // We can't easily verify the webview loaded the URL without async waiting or mocking WKWebView (which is hard).
        // But we can call the method to ensure no crash and coverage of the method body.
        svg.load(fullUrl: "https://example.com/image.svg")
        
        // Verify initialization properties
        let subviews = svg.subviews
        XCTAssertTrue(subviews.count > 0)
        if let webView = subviews.first as? WKWebView {
            XCTAssertFalse(webView.scrollView.isScrollEnabled)
            XCTAssertEqual(webView.contentMode, .scaleAspectFit)
        }
    }
    
    func testNavigationDelegate() {
        let svg = DPSvgImageView()
        let webView = WKWebView()
        // Mock content size
        webView.scrollView.contentSize = CGSize(width: 1000, height: 1000)
        webView.bounds = CGRect(x: 0, y: 0, width: 100, height: 100)
        
        svg.webView(webView, didFinish: nil)
        
        // Scale factor should be 100/1000 = 0.1
        XCTAssertEqual(webView.scrollView.minimumZoomScale, 0.1, accuracy: 0.001)
        XCTAssertEqual(webView.scrollView.maximumZoomScale, 0.1, accuracy: 0.001)
        XCTAssertEqual(webView.scrollView.zoomScale, 0.1, accuracy: 0.001)
    }
}
