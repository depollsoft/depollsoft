//
//  DPLoginViewController.h
//  pitchperfect
//
//  Created by David Poll on 3/23/15.
//  Copyright (c) 2015 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>
@import Bolts;
#import "DPToolbarViewController.h"

@interface DPLoginViewController : DPToolbarViewController

@property (nonatomic, readonly) BFTask *loginTask;
@property (nonatomic, readwrite) BOOL isHoomiLogout;

- (void)completeLogIn:(BOOL)isNew NS_SWIFT_NAME(completeLogIn(_:));

@end
