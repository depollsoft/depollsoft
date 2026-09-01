import XCTest

func firstList(in app: XCUIApplication) -> XCUIElement {
    let table = app.tables.firstMatch
    if table.exists { return table }
    return app.collectionViews.firstMatch
}
