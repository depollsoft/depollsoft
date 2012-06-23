//
//  DPSongsModel.m
//  pitchperfect
//
//  Created by David Poll on 6/22/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPSongsModel.h"
#import "DPJsonSerializer.h"
#import <Parse/Parse.h>

#define SONGS_KEY @"depollsoft.pitchperfect.Songs"
#define SONGS_CHANGED_KEY @"depollsoft.pitchperfect.SongsChanged"

@interface DPSongsModel ()

@property (nonatomic) BOOL suspendTimestamp;
@property (nonatomic) BOOL saving;
@property (nonatomic) BOOL refreshing;
@property (nonatomic) BOOL postponedSave;
@property (nonatomic) BOOL refreshPostponedSave;
@property (nonatomic, strong) PFObject *serialized;

- (PFObject *)toParseObject;

@end

@implementation DPSongsModel

@synthesize songs, serialized, suspendTimestamp, saving, refreshing, postponedSave, refreshPostponedSave, delegate;

- (id)init {
    if (self = [super init]) {
        self.suspendTimestamp = YES;
        NSDictionary *serializedSongs = [[NSUserDefaults standardUserDefaults] dictionaryForKey:SONGS_KEY];
        if (serializedSongs) {
            songs = [NSMutableArray arrayWithArray:[DPJsonSerializer deserializeDictionary:serializedSongs]];
        } else {
            songs = [NSMutableArray array];
        }
        self.suspendTimestamp = YES;
    }
    return self;
}

- (void)fromParseObject:(PFObject *)object {
    suspendTimestamp = YES;
    self.songs = [DPJsonSerializer deserializeDictionary:[object objectForKey:@"songs"]];
    suspendTimestamp = NO;
    serialized = object;
}

- (void)refreshFromParse {
    refreshing = YES;
    PFQuery *query = [PFQuery queryWithClassName:@"SongList"];
    @try {
        [query getFirstObjectInBackgroundWithBlock:^(PFObject *object, NSError *error) {
            refreshing = NO;
            if (error) {
                return;
            }
            if (![self lastChangedTime] || [object.updatedAt compare:[self lastChangedTime]] > 0) {
                [self fromParseObject:object];
            } else if (refreshPostponedSave) {
                [self saveAllToParse];
            }
            refreshPostponedSave = NO;
        }];
    }
    @catch (NSException *exception) {
        refreshing = NO;
    }
}

- (void)setLastChangedTime:(NSDate *)date {
    if (suspendTimestamp) {
        return;
    }
    if (serialized) {
        [[NSUserDefaults standardUserDefaults] setObject:date forKey:SONGS_CHANGED_KEY];
    } else {
        [[NSUserDefaults standardUserDefaults] setObject:nil forKey:SONGS_CHANGED_KEY];
    }
}

- (NSDate *)lastChangedTime {
    return [[NSUserDefaults standardUserDefaults] objectForKey:SONGS_CHANGED_KEY];
}

- (void)saveAllToParse {
    [self saveAllToParse:NO];
}

- (void)saveAllToParse:(BOOL)immediately {
    if(![PFUser currentUser]) {
        return;
    }
    if (refreshing) {
        refreshPostponedSave = YES;
        return;
    }
    if (saving) {
        postponedSave = YES;
        return;
    }
    if (immediately) {
        saving = YES;
        [[self toParseObject] saveInBackgroundWithBlock:^(BOOL succeeded, NSError *error) {
            saving = false;
            if (postponedSave) {
                [self saveAllToParse:NO];
            }
            postponedSave = NO;
        }];
    } else if (!serialized || serialized.updatedAt || [[self lastChangedTime] compare:serialized.updatedAt] > 0) {
        [[self toParseObject] saveEventually];
    }
}

- (void)storeValue {
    NSDictionary *toSave = [DPJsonSerializer serialize:songs];
    NSLog(@"Saving...\n%@", toSave);
    [[NSUserDefaults standardUserDefaults] setObject:toSave forKey:SONGS_KEY];
    if (!suspendTimestamp) {
        [self saveAllToParse];
    }
    [self setLastChangedTime:[NSDate date]];
}

- (void)setSongs:(NSMutableArray *)newSongs {
    songs = newSongs;
    [self storeValue];
    if ([delegate respondsToSelector:@selector(songsChanged)]) {
        [delegate songsChanged];
    }
}

- (void)songsChanged {
    
}

- (PFObject *)toParseObject {
    if (!serialized) {
        serialized = [PFObject objectWithClassName:@"SongList"];
        serialized.ACL = [PFACL ACLWithUser:[PFUser currentUser]];
        [serialized setObject:@"*default" forKey:@"name"];
    }
    [serialized setObject:[DPJsonSerializer serialize:songs] forKey:@"songs"];
    [serialized setObject:[PFUser currentUser] forKey:@"owner"];
    return serialized;
}

+ (DPSongsModel *)sharedInstance {
    static DPSongsModel *songs;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        songs = [[DPSongsModel alloc] init];
    });
    return songs;
}

@end
