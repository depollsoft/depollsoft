//
//  DPSettingsModel.swift
//  pitchperfect
//
//  Created by David Poll on 6/20/21.
//  Copyright © 2021 DepollSoft. All rights reserved.
//

import Foundation
import Firebase

let WAKE_LOCK_KEY = "depollsoft.pitchperfect.WakeLock"
let TOGGLE_NOTE_KEY = "depollsoft.pitchperfect.ToggleNote"

@objc public class DPSettingsModel: NSObject {
    private var listenerRegistration: ListenerRegistration?
    private var userRef: DocumentReference?
    
    @objc public func attachToFirestore() {
        let user = Auth.auth().currentUser
        userRef = Firestore.firestore().document("users/\(user!.uid)")
        listenerRegistration = userRef?.addSnapshotListener { snapshot, error in
            if error == nil {
                return
            }
            self.wakeLock = snapshot?.get("wakeLock") as? Bool ?? self.wakeLock
            self.toggleNotes = snapshot?.get("toggleNotes") as? Bool ?? self.toggleNotes
        }
    }
    
    @objc public func detachFromFirestore() {
        if listenerRegistration != nil {
            listenerRegistration?.remove()
        }
        listenerRegistration = nil
        userRef = nil
    }
    
    @objc public var wakeLock: Bool {
        get {
            UserDefaults.standard.bool(forKey: WAKE_LOCK_KEY)
        }
        set {
            UserDefaults.standard.set(newValue, forKey: WAKE_LOCK_KEY)
            if userRef != nil {
                userRef?.setValue(newValue, forKey: "wakeLock")
            }
        }
    }
    
    @objc public var toggleNotes: Bool {
        get {
            UserDefaults.standard.bool(forKey: TOGGLE_NOTE_KEY)
        }
        set {
            UserDefaults.standard.set(newValue, forKey: TOGGLE_NOTE_KEY)
            if userRef != nil {
                userRef?.setValue(newValue, forKey: "toggleNotes")
            }
        }
    }
    
    @objc public static let sharedInstance: DPSettingsModel = DPSettingsModel()
    
    private override init() {
        super.init()
        self.wakeLock = false
        self.toggleNotes = false
    }
}
/*
#import "DPSettingsModel.h"
#import <Parse/Parse.h>

#define WAKE_LOCK_KEY @"depollsoft.pitchperfect.WakeLock"
#define TOGGLE_NOTE_KEY @"depollsoft.pitchperfect.ToggleNote"

@interface DPSettingsModel ()

@property (nonatomic) BOOL shouldRefresh;

@end

@implementation DPSettingsModel

@synthesize shouldRefresh;

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
    if ([PFUser currentUser] && shouldRefresh) {
        @try {
            [[PFUser currentUser] setObject:[NSNumber numberWithBool:self.toggleNotes] forKey:@"ToggleNote"];
            [[PFUser currentUser] setObject:[NSNumber numberWithBool:self.wakeLock] forKey:@"WakeLock"];
            [[PFUser currentUser] saveEventually];
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
            shouldRefresh = NO;
            self.toggleNotes = [[[PFUser currentUser] objectForKey:@"ToggleNote"] boolValue];
            shouldRefresh = YES;
        }
        if ([[PFUser currentUser].allKeys containsObject:@"WakeLock"]) {
            shouldRefresh = NO;
            self.wakeLock = [[[PFUser currentUser] objectForKey:@"WakeLock"] boolValue];
            shouldRefresh = YES;
        }
    }
}

- (id)init {
    if (self = [super init]) {
        self.wakeLock = self.wakeLock;
        shouldRefresh = YES;
    }
    return self;
}

@end
*/
