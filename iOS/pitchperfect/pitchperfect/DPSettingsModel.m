//
//  DPSettingsModel.m
//  pitchperfect
//
//  Created by David Poll on 6/22/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPSettingsModel.h"
#import <Parse/Parse.h>

#define WAKE_LOCK_KEY @"depollsoft.pitchperfect.WakeLock"
#define TOGGLE_NOTE_KEY @"depollsoft.pitchperfect.ToggleNote"

@implementation DPSettingsModel

+ (DPSettingsModel *)sharedInstance {
    static DPSettingsModel *instance;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[DPSettingsModel alloc] init];
    });
    return instance;
}

- (BOOL)wakeLock {
    return [[NSUserDefaults standardUserDefaults] boolForKey:WAKE_LOCK_KEY];
}

- (void)setWakeLock:(BOOL)wakeLock {
    [[NSUserDefaults standardUserDefaults] setBool:wakeLock forKey:WAKE_LOCK_KEY];
    [UIApplication sharedApplication].idleTimerDisabled = wakeLock;
    [self refreshUser];
}

- (BOOL)toggleNotes {
    return [[NSUserDefaults standardUserDefaults] boolForKey:TOGGLE_NOTE_KEY];
}

- (void)setToggleNotes:(BOOL)toggleNotes {
    [[NSUserDefaults standardUserDefaults] setBool:toggleNotes forKey:TOGGLE_NOTE_KEY];
    [self refreshUser];
}

- (void)refreshUser {
    if ([PFUser currentUser]) {
        @try {
            [[PFUser currentUser] setObject:[NSNumber numberWithBool:self.toggleNotes] forKey:@"ToggleNote"];
            [[PFUser currentUser] setObject:[NSNumber numberWithBool:self.wakeLock] forKey:@"WakeLock"];
            [[PFUser currentUser] saveInBackground];
        }
        @catch (NSException *exception) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self refreshUser];
            });
        }
    }
}

- (void)restoreUser {
    if ([PFUser currentUser]) {
        if ([[PFUser currentUser].allKeys containsObject:@"ToggleNote"]) {
            self.toggleNotes = [[[PFUser currentUser] objectForKey:@"ToggleNote"] boolValue];
        }
        if ([[PFUser currentUser].allKeys containsObject:@"WakeLock"]) {
            self.wakeLock = [[[PFUser currentUser] objectForKey:@"WakeLock"] boolValue];
        }
    }
}

- (id)init {
    if (self = [super init]) {
        self.wakeLock = self.wakeLock;
    }
    return self;
}

@end
