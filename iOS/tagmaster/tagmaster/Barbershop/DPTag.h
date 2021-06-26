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
@property (nonatomic, copy, nullable) NSString *title;
@property (nonatomic, copy, nullable) NSDate *lastRefreshed;
@property (nonatomic, copy, nullable) NSString *alternativeTitle;
@property (nonatomic, copy, nullable) NSString *version;
@property (nonatomic, copy, nullable) NSString *writtenKey;
@property (nonatomic) int parts;
@property (nonatomic, copy, nullable) NSString *tagType;
@property (nonatomic, copy, nullable) NSString *recordingMethod;
@property (nonatomic, copy, nullable) NSString *teachingVideo;
@property (nonatomic, copy, nullable) NSString *notes;
@property (nonatomic, copy, nullable) NSString *arranger;
@property (nonatomic, copy, nullable) NSURL *arrangerWebsite;
@property (nonatomic) int yearArranged;
@property (nonatomic, copy, nullable) NSString *sungBy;
@property (nonatomic, copy, nullable) NSURL *sungByWebsite;
@property (nonatomic) int sungYear;
@property (nonatomic, copy, nullable) NSString *learningTrackQuartet;
@property (nonatomic, copy, nullable) NSURL *learningTrackQuartetWebsite;
@property (nonatomic, copy, nullable) NSString *teacher;
@property (nonatomic, copy, nullable) NSURL *teacherWebsite;
@property (nonatomic, copy, nullable) NSString *provider;
@property (nonatomic, copy, nullable) NSURL *providerWebsite;
@property (nonatomic, copy, nullable) NSDate *posted;
@property (nonatomic) int classicTagNumber;
@property (nonatomic) double rating;
@property (nonatomic) int downloadCount;
@property (nonatomic, strong, nullable) DPRemoteLocation *sheetMusicUri;
@property (nonatomic, strong, nullable) DPRemoteLocation *notationUri;
@property (nonatomic, strong, nullable) DPRemoteLocation *allPartsTrackUri;
@property (nonatomic, strong, nullable) DPRemoteLocation *bassTrackUri;
@property (nonatomic, strong, nullable) DPRemoteLocation *baritoneTrackUri;
@property (nonatomic, strong, nullable) DPRemoteLocation *leadTrackUri;
@property (nonatomic, strong, nullable) DPRemoteLocation *tenorTrackUri;
@property (nonatomic, strong, nullable) DPRemoteLocation *other1TrackUri;
@property (nonatomic, strong, nullable) DPRemoteLocation *other2TrackUri;
@property (nonatomic, strong, nullable) DPRemoteLocation *other3TrackUri;
@property (nonatomic, strong, nullable) DPRemoteLocation *other4TrackUri;
@property (nonatomic, strong, nullable) NSArray *videos;
@property (nonatomic, copy, nullable) NSString *lyrics;

+ (void)clearCache;
+ (long)getCurrentCacheSize;
+ (nullable DPTag *)loadFromCache:(int)tagId;
+ (nullable DPTag *)loadTagById:(int)tagId;
+ (nullable DPTag *)loadTagById:(int)tagId refresh:(BOOL)refresh;
+ (nonnull DPTagQueryResult *)query:(nullable NSString *)query;
+ (nonnull DPTagQueryResult *)query:(nullable NSString *)query numberOfResults:(int)numberOfResults;
+ (nonnull DPTagQueryResult *)query:(nullable NSString *)query numberOfResults:(int)numberOfResults start:(int)start;
+ (nonnull DPTagQueryResult *)query:(nullable NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(nullable NSNumber *)parts;
+ (nonnull DPTagQueryResult *)query:(nullable NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(nullable NSNumber *)parts learningTracks:(nullable NSNumber *)learningTracks;
+ (nonnull DPTagQueryResult *)query:(nullable NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(nullable NSNumber *)parts learningTracks:(nullable NSNumber *)learningTracks sheetMusic:(nullable NSNumber *)sheetMusic;
+ (nonnull DPTagQueryResult *)query:(nullable NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(nullable NSNumber *)parts learningTracks:(nullable NSNumber *)learningTracks sheetMusic:(nullable NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection;
+ (nonnull DPTagQueryResult *)query:(nullable NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(nullable NSNumber *)parts learningTracks:(nullable NSNumber *)learningTracks sheetMusic:(nullable NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection sortBy:(enum DPTagSortOptions)sortBy;
+ (nonnull DPTagQueryResult *)query:(nullable NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(nullable NSNumber *)parts learningTracks:(nullable NSNumber *)learningTracks sheetMusic:(nullable NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection sortBy:(enum DPTagSortOptions)sortBy minimumRating:(nullable NSNumber *)minimumRating;
+ (nonnull DPTagQueryResult *)query:(nullable NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(nullable NSNumber *)parts learningTracks:(nullable NSNumber *)learningTracks sheetMusic:(nullable NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection sortBy:(enum DPTagSortOptions)sortBy minimumRating:(nullable NSNumber *)minimumRating minimumDownloads:(nullable NSNumber *)minimumDownloads;
+ (nonnull DPTagQueryResult *)query:(nullable NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(nullable NSNumber *)parts learningTracks:(nullable NSNumber *)learningTracks sheetMusic:(nullable NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection sortBy:(enum DPTagSortOptions)sortBy minimumRating:(nullable NSNumber *)minimumRating minimumDownloads:(nullable NSNumber *)minimumDownloads cache:(BOOL)cache;
+ (nonnull DPTagQueryResult *)query:(nullable NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(nullable NSNumber *)parts learningTracks:(nullable NSNumber *)learningTracks sheetMusic:(nullable NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection sortBy:(enum DPTagSortOptions)sortBy minimumRating:(nullable NSNumber *)minimumRating minimumDownloads:(nullable NSNumber *)minimumDownloads cache:(BOOL)cache fieldList:(nullable NSString *)fieldList;
+ (DPTag *_Nullable)queryById:(int)tagId;
+ (nonnull NSArray<DPTag *> *)queryByIds:(nonnull NSArray<NSNumber *> *)tagIds;
+ (nonnull NSArray<DPTag *> *)queryByIds:(nonnull NSArray<NSNumber *> *)tagIds cache:(BOOL)cache;

- (nullable NSURL *)tagUri;
- (void)cache;
- (nullable DPNote *)keyNote;
- (void)rate:(NSUInteger)rating;

@property (readonly, nonnull) NSArray<DPTrack *> *tracks;

@end
