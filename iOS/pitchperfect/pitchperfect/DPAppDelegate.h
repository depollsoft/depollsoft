//
//  DPAppDelegate.h
//  pitchperfect
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <GoogleMobileAds/GoogleMobileAds.h>

@class DPNote;

@interface DPAppDelegate : UIResponder <UIApplicationDelegate>

+ (void)startupRefreshFromParse;
+ (void)noteTouchStarted:(DPNote *)note forCell:(UITableViewCell *)cell;
+ (void)noteTouchEnded:(DPNote *)note forCell:(UITableViewCell *)cell;
+ (BOOL)testAds;
+ (GADRequest *)adRequest;

@property (strong, nonatomic) UIWindow *window;

@end
