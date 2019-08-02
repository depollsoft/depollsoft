//
//  DPTag.h
//  tagmaster
//
//  Created by David Poll on 3/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "DPRemoteLocation.h"
#import "DPConstants.h"
#import "DPNote.h"
#import "DPTagQueryResult.h"
#import "DPTrack.h"

@interface DPTag : NSObject

@property (nonatomic) int appVersion;
@property (nonatomic) int tagId;
@property (nonatomic, copy) NSString *title;
@property (nonatomic, copy) NSDate *lastRefreshed;
@property (nonatomic, copy) NSString *alternativeTitle;
@property (nonatomic, copy) NSString *version;
@property (nonatomic, copy) NSString *writtenKey;
@property (nonatomic) int parts;
@property (nonatomic, copy) NSString *tagType;
@property (nonatomic, copy) NSString *recordingMethod;
@property (nonatomic, copy) NSString *teachingVideo;
@property (nonatomic, copy) NSString *notes;
@property (nonatomic, copy) NSString *arranger;
@property (nonatomic, copy) NSURL *arrangerWebsite;
@property (nonatomic) int yearArranged;
@property (nonatomic, copy) NSString *sungBy;
@property (nonatomic, copy) NSURL *sungByWebsite;
@property (nonatomic) int sungYear;
@property (nonatomic, copy) NSString *learningTrackQuartet;
@property (nonatomic, copy) NSURL *learningTrackQuartetWebsite;
@property (nonatomic, copy) NSString *teacher;
@property (nonatomic, copy) NSURL *teacherWebsite;
@property (nonatomic, copy) NSString *provider;
@property (nonatomic, copy) NSURL *providerWebsite;
@property (nonatomic, copy) NSDate *posted;
@property (nonatomic) int classicTagNumber;
@property (nonatomic) double rating;
@property (nonatomic) int downloadCount;
@property (nonatomic, strong) DPRemoteLocation *sheetMusicUri;
@property (nonatomic, strong) DPRemoteLocation *notationUri;
@property (nonatomic, strong) DPRemoteLocation *allPartsTrackUri;
@property (nonatomic, strong) DPRemoteLocation *bassTrackUri;
@property (nonatomic, strong) DPRemoteLocation *baritoneTrackUri;
@property (nonatomic, strong) DPRemoteLocation *leadTrackUri;
@property (nonatomic, strong) DPRemoteLocation *tenorTrackUri;
@property (nonatomic, strong) DPRemoteLocation *other1TrackUri;
@property (nonatomic, strong) DPRemoteLocation *other2TrackUri;
@property (nonatomic, strong) DPRemoteLocation *other3TrackUri;
@property (nonatomic, strong) DPRemoteLocation *other4TrackUri;
@property (nonatomic, strong) NSArray *videos;
@property (nonatomic, copy) NSString *lyrics;

+ (void)clearCache;
+ (long)getCurrentCacheSize;
+ (DPTag *)loadFromCache:(int)tagId;
+ (DPTag *)loadTagById:(int)tagId;
+ (DPTag *)loadTagById:(int)tagId refresh:(BOOL)refresh;
+ (DPTagQueryResult *)query:(NSString *)query;
+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults;
+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start;
+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts;
+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts learningTracks:(NSNumber *)learningTracks;
+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts learningTracks:(NSNumber *)learningTracks sheetMusic:(NSNumber *)sheetMusic;
+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts learningTracks:(NSNumber *)learningTracks sheetMusic:(NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection;
+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts learningTracks:(NSNumber *)learningTracks sheetMusic:(NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection sortBy:(enum DPTagSortOptions)sortBy;
+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts learningTracks:(NSNumber *)learningTracks sheetMusic:(NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection sortBy:(enum DPTagSortOptions)sortBy minimumRating:(NSNumber *)minimumRating;
+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts learningTracks:(NSNumber *)learningTracks sheetMusic:(NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection sortBy:(enum DPTagSortOptions)sortBy minimumRating:(NSNumber *)minimumRating minimumDownloads:(NSNumber *)minimumDownloads;
+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts learningTracks:(NSNumber *)learningTracks sheetMusic:(NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection sortBy:(enum DPTagSortOptions)sortBy minimumRating:(NSNumber *)minimumRating minimumDownloads:(NSNumber *)minimumDownloads cache:(BOOL)cache;
+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts learningTracks:(NSNumber *)learningTracks sheetMusic:(NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection sortBy:(enum DPTagSortOptions)sortBy minimumRating:(NSNumber *)minimumRating minimumDownloads:(NSNumber *)minimumDownloads cache:(BOOL)cache fieldList:(NSString *)fieldList;
+ (DPTag *)queryById:(int)tagId;

- (NSURL *)tagUri;
- (void)cache;
- (DPNote *)keyNote;
- (void)rate:(NSUInteger)rating;

@property (readonly) NSArray<DPTrack *> *tracks;

@end
