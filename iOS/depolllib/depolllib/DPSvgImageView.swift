//
//  DPSvgImage.swift
//  depolllib
//
//  Created by David Poll on 6/22/21.
//  Copyright © 2021 DepollSoft. All rights reserved.
//

import Foundation
import UIKit
import WebKit

import UIKit

@objc public class DPSvgImageView: UIView {
    private let webView = WKWebView()

    @objc public init() {
        super.init(frame: .zero)
        webView.navigationDelegate = self
        webView.scrollView.isScrollEnabled = false
        webView.contentMode = .scaleAspectFit
        webView.backgroundColor = .clear
        webView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        addSubview(webView)
    }

    @objc required public init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    deinit {
        webView.stopLoading()
    }

    @objc public func load(fullUrl: String) {
        webView.stopLoading()
        if let url = URL(string: fullUrl) {
            webView.load(URLRequest(url: url))
        }
    }
}

extension DPSvgImageView: WKNavigationDelegate {
    public func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        let scaleFactor = webView.bounds.size.width / webView.scrollView.contentSize.width
        if scaleFactor <= 0 {
            return
        }

        webView.scrollView.minimumZoomScale = scaleFactor
        webView.scrollView.maximumZoomScale = scaleFactor
        webView.scrollView.zoomScale = scaleFactor
    }
}
