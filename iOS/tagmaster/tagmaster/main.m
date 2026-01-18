//
//  main.m
//  tagmaster
//
//  Created by David Poll on 3/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>

#import "DPAppDelegate.h"

int main(int argc, char *argv[])
{
    @autoreleasepool {
        BOOL runningTests = NSClassFromString(@"XCTestCase") != nil;
        Class delegateClass = runningTests ? NSClassFromString(@"TMTestAppDelegate") : [DPAppDelegate class];
        return UIApplicationMain(argc, argv, nil, NSStringFromClass(delegateClass));
    }
}
