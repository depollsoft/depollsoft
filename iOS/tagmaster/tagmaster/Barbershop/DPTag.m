//
//  DPTag.m
//  tagmaster
//
//  Created by David Poll on 3/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPTag.h"
#import "DPTagXMLParser.h"
#import "DPFileCache.h"
#import "DPUtils+Subscripts.h"
#import "DPUtils+NSString.h"
#import "DPTrack.h"
#import <objc/runtime.h>

const int APP_VERSION = 1;
NSString *const API_URI_STRING = @"http://www.barbershoptags.com/api.php?client=TagMaster&";

@implementation DPTag

@synthesize allPartsTrackUri;
@synthesize alternativeTitle;
@synthesize appVersion;
@synthesize arranger;
@synthesize arrangerWebsite;
@synthesize baritoneTrackUri;
@synthesize bassTrackUri;
@synthesize notes;
@synthesize parts;
@synthesize tagId;
@synthesize title;
@synthesize lyrics;
@synthesize posted;
@synthesize rating;
@synthesize sungBy;
@synthesize videos;
@synthesize tagType;
@synthesize teacher;
@synthesize version;
@synthesize provider;
@synthesize sungYear;
@synthesize writtenKey;
@synthesize notationUri;
@synthesize leadTrackUri;
@synthesize yearArranged;
@synthesize downloadCount;
@synthesize lastRefreshed;
@synthesize sheetMusicUri;
@synthesize sungByWebsite;
@synthesize teachingVideo;
@synthesize tenorTrackUri;
@synthesize other1TrackUri;
@synthesize other2TrackUri;
@synthesize other3TrackUri;
@synthesize other4TrackUri;
@synthesize teacherWebsite;
@synthesize providerWebsite;
@synthesize recordingMethod;
@synthesize classicTagNumber;
@synthesize learningTrackQuartet;
@synthesize learningTrackQuartetWebsite;

- (id)init {
    if (self = [super init]) {
        self.appVersion = APP_VERSION;
    }
    return self;
}

+ (NSString *)cacheKeyForId:(int)tagId {
    return [NSString stringWithFormat:@"DPTag.%d", tagId];
}

- (NSString *)cacheKey {
    return [DPTag cacheKeyForId:self.tagId];
}

+ (NSMutableDictionary *)tagCache {
    static NSMutableDictionary *tagCache = nil;
    if (!tagCache) {
        tagCache = [NSMutableDictionary dictionary];
    }
    return tagCache;
}

+ (DPTag *)queryById:(int)tagId {
    NSMutableString *builtString = [NSMutableString stringWithString:API_URI_STRING];
    [builtString appendFormat:@"id=%d", tagId];
    NSURL *url = [NSURL URLWithString:builtString];
    DPTagXMLParser *parser = [[DPTagXMLParser alloc] init];
    NSArray *parseResult = [parser parseWithUrl:url];
    DPTagQueryResult *queryResult = [parseResult objectAtIndex:0];
    
    return queryResult.tags.count > 0 ? queryResult.tags[0] : nil;
}

- (NSString *)description {
    return [NSString stringWithFormat:@"{Tag id: %d Title: %@}", self.tagId, self.title];
}

+ (void)clearCache {
    // TODO: implement
}

+ (long)getCurrentCacheSize {
    // TODO: implement
    return 0;
}

+ (DPTag *)loadTagById:(int)tagId {
    return [DPTag loadTagById:tagId refresh:NO];
}

+ (DPTag *)loadTagById:(int)tagId refresh:(BOOL)refresh {
    if (!refresh) {
        DPTag *cached = [self loadFromCache:tagId];
        if (cached) {
            return cached;
        }
    }
    
    DPTag *foundTag = [self queryById:tagId];
    [foundTag cache];
    return foundTag;
}

+ (DPTag *)loadFromCache:(int)tagId {
    if (self.tagCache[@(tagId)]) {
        return self.tagCache[@(tagId)];
    }
    DPTag *cachedTag = [DPFileCache readObjectForKey:[self cacheKeyForId:tagId]];
    if (cachedTag && cachedTag.appVersion == APP_VERSION) {
        self.tagCache[@(tagId)] = cachedTag;
        return cachedTag;
    }
    return nil;
}

+ (DPTagQueryResult *)query:(NSString *)query {
    return [DPTag query:query numberOfResults:10];
}

+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults {
    return [DPTag query:query numberOfResults:numberOfResults start:0];
}

+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start {
    return [DPTag query:query numberOfResults:numberOfResults start:start parts:nil];
}

+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts {
    return [DPTag query:query numberOfResults:numberOfResults start:start parts:parts learningTracks:nil];
}

+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts learningTracks:(NSNumber *)learningTracks {
    return [DPTag query:query numberOfResults:numberOfResults start:start parts:parts learningTracks:learningTracks sheetMusic:nil];
}

+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts learningTracks:(NSNumber *)learningTracks sheetMusic:(NSNumber *)sheetMusic {
    return [DPTag query:query numberOfResults:numberOfResults start:start parts:parts learningTracks:learningTracks sheetMusic:sheetMusic collection:DPTagCollectionNone];
}

+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts learningTracks:(NSNumber *)learningTracks sheetMusic:(NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection {
    return [DPTag query:query numberOfResults:numberOfResults start:start parts:parts learningTracks:learningTracks sheetMusic:sheetMusic collection:tagCollection sortBy:DPTagSortNone];
}

+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts learningTracks:(NSNumber *)learningTracks sheetMusic:(NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection sortBy:(enum DPTagSortOptions)sortBy {
    return [DPTag query:query numberOfResults:numberOfResults start:start parts:parts learningTracks:learningTracks sheetMusic:sheetMusic collection:tagCollection sortBy:sortBy minimumRating:nil];
}

+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts learningTracks:(NSNumber *)learningTracks sheetMusic:(NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection sortBy:(enum DPTagSortOptions)sortBy minimumRating:(NSNumber *)minimumRating {
    return [DPTag query:query numberOfResults:numberOfResults start:start parts:parts learningTracks:learningTracks sheetMusic:sheetMusic collection:tagCollection sortBy:sortBy minimumRating:minimumRating minimumDownloads:nil];
}

+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts learningTracks:(NSNumber *)learningTracks sheetMusic:(NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection sortBy:(enum DPTagSortOptions)sortBy minimumRating:(NSNumber *)minimumRating minimumDownloads:(NSNumber *)minimumDownloads {
    return [DPTag query:query numberOfResults:numberOfResults start:start parts:parts learningTracks:learningTracks sheetMusic:sheetMusic collection:tagCollection sortBy:sortBy minimumRating:minimumRating minimumDownloads:minimumDownloads cache:NO];
}

+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts learningTracks:(NSNumber *)learningTracks sheetMusic:(NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection sortBy:(enum DPTagSortOptions)sortBy minimumRating:(NSNumber *)minimumRating minimumDownloads:(NSNumber *)minimumDownloads cache:(BOOL)cache {
    return [DPTag query:query numberOfResults:numberOfResults start:start parts:parts learningTracks:learningTracks sheetMusic:sheetMusic collection:tagCollection sortBy:sortBy minimumRating:minimumRating minimumDownloads:minimumDownloads cache:cache fieldList:@"id,Title,AltTitle,Rating,Posted,Downloaded,SheetMusic,AllParts,Bass,Bari,Lead,Tenor,Other1,Other2,Other3,Other4"];
}
+ (DPTagQueryResult *)query:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts learningTracks:(NSNumber *)learningTracks sheetMusic:(NSNumber *)sheetMusic collection:(enum DPTagCollection)tagCollection sortBy:(enum DPTagSortOptions)sortBy minimumRating:(NSNumber *)minimumRating minimumDownloads:(NSNumber *)minimumDownloads cache:(BOOL)cache fieldList:(NSString *)fieldList {
    NSMutableString *builtString = [NSMutableString stringWithString:API_URI_STRING];
    [builtString appendFormat:@"n=%d", numberOfResults];
    if (fieldList) {
        [builtString appendFormat:@"&fldlist=%@", fieldList];
    }
    [builtString appendFormat:@"&start=%d", start + 1];
    if (query && [[query stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]] length]) {
        [builtString appendFormat:@"&q=%@", [query stringByURLEncoding]];
    }
    if (parts) {
        [builtString appendFormat:@"&Parts=%@", parts];
    }
    if (learningTracks) {
        [builtString appendFormat:@"&Learning=%@", [learningTracks boolValue] ? @"Yes" : @"No"];
    }
    if (sheetMusic) {
        [builtString appendFormat:@"&SheetMusic=%@", [sheetMusic boolValue] ? @"Yes" : @"No"];
    }
    if (sortBy) {
        [builtString appendString:@"&Sortby="];
        switch (sortBy) {
            case DPTagSortNone:
                break;
            case DPTagSortClassic:
                [builtString appendString:@"Classic"];
                tagCollection = DPTagCollectionClassicTags;
                break;
            case DPTagSortDownloaded:
                [builtString appendString:@"Downloaded"];
                break;
            case DPTagSortPosted:
                [builtString appendString:@"Posted"];
                break;
            case DPTagSortRating:
                [builtString appendString:@"Rating"];
                break;
            case DPTagSortTitle:
                [builtString appendString:@"Title"];
                break;
        }
    }
    if (tagCollection) {
        [builtString appendString:@"&Collection="];
        switch (tagCollection) {
            case DPTagCollectionNone:
                break;
            case DPTagCollectionClassicTags:
                [builtString appendString:@"classic"];
                break;
            case DPTagCollectionEasyTags:
                [builtString appendString:@"easy"];
                break;
        }
    }
    if (minimumRating) {
        [builtString appendFormat:@"&MinRating=%@", minimumRating];
    }
    if (minimumDownloads) {
        [builtString appendFormat:@"&MinDownloaded=%@", minimumDownloads];
    }
    NSURL *url = [NSURL URLWithString:builtString];
    DPTagXMLParser *parser = [[DPTagXMLParser alloc] init];
    NSArray *parseResult = [parser parseWithUrl:url];
    DPTagQueryResult *queryResult = [parseResult objectAtIndex:0];
    queryResult.start = start;
    
    if (cache) {
        for (DPTag *tag in queryResult.tags) {
            [tag cache];
        }
    }
    
    return queryResult;
}

- (void)cache {
    DPTag.tagCache[@(self.tagId)] = self;
    [DPFileCache writeObject:self forKey:self.cacheKey];
    return;
}

- (NSArray *)tracks {
    static char key;
    NSMutableArray *arr = objc_getAssociatedObject(self, &key);
    if (!arr) {
        arr = [NSMutableArray array];
        if (self.allPartsTrackUri) {
            [arr addObject:[DPTrack trackWithTitle:@"All Parts" source:self.allPartsTrackUri]];
        }
        if (self.tenorTrackUri) {
            [arr addObject:[DPTrack trackWithTitle:@"Tenor" source:self.tenorTrackUri]];
        }
        if (self.leadTrackUri) {
            [arr addObject:[DPTrack trackWithTitle:@"Lead" source:self.leadTrackUri]];
        }
        if (self.baritoneTrackUri) {
            [arr addObject:[DPTrack trackWithTitle:@"Baritone" source:self.baritoneTrackUri]];
        }
        if (self.bassTrackUri) {
            [arr addObject:[DPTrack trackWithTitle:@"Bass" source:self.bassTrackUri]];
        }
        if (self.other1TrackUri) {
            [arr addObject:[DPTrack trackWithTitle:@"Other 1" source:self.other1TrackUri]];
        }
        if (self.other2TrackUri) {
            [arr addObject:[DPTrack trackWithTitle:@"Other 2" source:self.other2TrackUri]];
        }
        if (self.other3TrackUri) {
            [arr addObject:[DPTrack trackWithTitle:@"Other 3" source:self.other3TrackUri]];
        }
        if (self.other4TrackUri) {
            [arr addObject:[DPTrack trackWithTitle:@"Other 4" source:self.other4TrackUri]];
        }
        objc_setAssociatedObject(self, &key, arr, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    }
    return arr;
}

- (NSURL *)tagUri {
    return [NSURL URLWithString:[NSString stringWithFormat:@"http://tags.depoll.com/tag.php?id=%d", self.tagId]];
}

@end
