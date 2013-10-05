//
//  DPTagXMLParser.m
//  tagmaster
//
//  Created by David Poll on 3/17/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPConstants.h"
#import "DPTagXMLParser.h"
#import "DPTag.h"
#import "DPVideo.h"
#import "DPTagQueryResult.h"
#import "DPTrack.h"
#import "DPRemoteLocation.h"

@implementation DPTagXMLParser

- (id)init {
    if (self = [super init]) {
        dayNameDateFormatter = [[NSDateFormatter alloc] init];
        [dayNameDateFormatter setDateFormat:@"E, d MMM yyyy"];
    }
    return self;
}

- (void)parser:(NSXMLParser *)parser didStartElement:(NSString *)elementName namespaceURI:(NSString *)namespaceURI qualifiedName:(NSString *)qName attributes:(NSDictionary *)attributeDict {
    if ([result.lastObject isKindOfClass:[NSString class]]) {
        [result removeLastObject];
    }
    if ([elementName isEqualToString:@"tags"]) {
        DPTagQueryResult *queryResult = [[DPTagQueryResult alloc] init];
        queryResult.count = [[attributeDict objectForKey:@"count"] intValue];
        queryResult.available = [[attributeDict objectForKey:@"available"] intValue];
        queryResult.tags = [NSMutableArray arrayWithCapacity:queryResult.count];
        [result addObject:queryResult];
    } else if ([elementName isEqualToString:@"tag"]) {
        DPTag *tag = [[DPTag alloc] init];
        tag.lastRefreshed = [NSDate date];
        [result addObject:tag];
    } else if ([elementName isEqualToString:@"videos"]) {
        [result addObject:[NSMutableArray arrayWithCapacity:[[attributeDict objectForKey:@"count"] intValue]]];
    } else if ([elementName isEqualToString:@"video"]) {
        [result addObject:[[DPVideo alloc] init]];
    } else if ([attributeDict objectForKey:@"type"]) {
        DPRemoteLocation *remoteLocation = [[DPRemoteLocation alloc] init];
        remoteLocation.type = [attributeDict objectForKey:@"type"];
        [result addObject:remoteLocation];
    }
}

- (void)parser:(NSXMLParser *)parser foundCharacters:(NSString *)string {
    if ([result.lastObject isKindOfClass:[NSString class]]) {
        NSString *priorString = result.lastObject;
        NSString *builtString = [priorString stringByAppendingString:string];
        [result replaceObjectAtIndex:result.count - 1 withObject:builtString];
    } else {
        [result addObject:string];
    }
}

- (void)parser:(NSXMLParser *)parser didEndElement:(NSString *)elementName namespaceURI:(NSString *)namespaceURI qualifiedName:(NSString *)qName {
    if ([result.lastObject isKindOfClass:[NSString class]]) {
        NSString *data = result.lastObject;
        [result removeLastObject];
        data = [data stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
        if (data.length) {
            [result addObject:data];
        }
    }
    if ([result.lastObject isKindOfClass:[NSString class]]) {
        NSString *data = result.lastObject;
        NSString *noCommas = [data stringByReplacingOccurrencesOfString:@"," withString:@""];
        [result removeLastObject];
        if ([result.lastObject isKindOfClass:[DPTag class]]) {
            DPTag *tag = result.lastObject;
            if ([elementName isEqualToString:@"id"]) {
                tag.tagId = [noCommas intValue];
            } else if ([elementName isEqualToString:@"Title"]) {
                tag.title = data;
            } else if ([elementName isEqualToString:@"AltTitle"]) {
                tag.alternativeTitle = data;
            } else if ([elementName isEqualToString:@"Version"]) {
                tag.version = data;
            } else if ([elementName isEqualToString:@"WritKey"]) {
                tag.writtenKey = data;
            } else if ([elementName isEqualToString:@"Parts"]) {
                tag.parts = [noCommas intValue];
            } else if ([elementName isEqualToString:@"Type"]) {
                tag.tagType = data;
            } else if ([elementName isEqualToString:@"Recording"]) {
                tag.recordingMethod = data;
            } else if ([elementName isEqualToString:@"TeachVid"]) {
                tag.teachingVideo = data;
            } else if ([elementName isEqualToString:@"Lyrics"]) {
                tag.lyrics = data;
            } else if ([elementName isEqualToString:@"Notes"]) {
                tag.notes = data;
            } else if ([elementName isEqualToString:@"Arranger"]) {
                tag.arranger = data;
            } else if ([elementName isEqualToString:@"ArrWebsite"]) {
                tag.arrangerWebsite = [NSURL URLWithString:data];
            } else if ([elementName isEqualToString:@"Arranged"]) {
                tag.yearArranged = [noCommas intValue];
            } else if ([elementName isEqualToString:@"SungBy"]) {
                tag.sungBy = data;
            } else if ([elementName isEqualToString:@"SungWebsite"]) {
                tag.sungByWebsite = [NSURL URLWithString:data];
            } else if ([elementName isEqualToString:@"SungYear"]) {
                tag.sungYear = [noCommas intValue];
            } else if ([elementName isEqualToString:@"Quartet"]) {
                tag.learningTrackQuartet = data;
            } else if ([elementName isEqualToString:@"QWebsite"]) {
                tag.learningTrackQuartetWebsite = [NSURL URLWithString:data];
            } else if ([elementName isEqualToString:@"Teacher"]) {
                tag.teacher = data;
            } else if ([elementName isEqualToString:@"TWebsite"]) {
                tag.teacherWebsite = [NSURL URLWithString:data];
            } else if ([elementName isEqualToString:@"Provider"]) {
                tag.provider = data;
            } else if ([elementName isEqualToString:@"ProvWebsite"]) {
                tag.providerWebsite = [NSURL URLWithString:data];
            } else if ([elementName isEqualToString:@"Posted"]) {
                tag.posted = [dayNameDateFormatter dateFromString: data];
            } else if ([elementName isEqualToString:@"Classic"]) {
                tag.classicTagNumber = [noCommas intValue];
            } else if ([elementName isEqualToString:@"Rating"]) {
                tag.rating = [data doubleValue];
            } else if ([elementName isEqualToString:@"Downloaded"]) {
                tag.downloadCount = [noCommas intValue];
            }
        } else if ([result.lastObject isKindOfClass:[DPRemoteLocation class]]) {
            DPRemoteLocation *location = result.lastObject;
            location.uri = [NSURL URLWithString:data];
            [result removeLastObject];
            DPTag *tag = result.lastObject;
            if ([elementName isEqualToString:@"SheetMusic"]) {
                tag.sheetMusicUri = location;
            } else if ([elementName isEqualToString:@"AllParts"]) {
                tag.allPartsTrackUri = location;
            } else if ([elementName isEqualToString:@"Bass"]) {
                tag.bassTrackUri = location;
            } else if ([elementName isEqualToString:@"Bari"]) {
                tag.baritoneTrackUri = location;
            } else if ([elementName isEqualToString:@"Lead"]) {
                tag.leadTrackUri = location;
            } else if ([elementName isEqualToString:@"Tenor"]) {
                tag.tenorTrackUri = location;
            } else if ([elementName isEqualToString:@"Other1"]) {
                tag.other1TrackUri = location;
            } else if ([elementName isEqualToString:@"Other2"]) {
                tag.other2TrackUri = location;
            } else if ([elementName isEqualToString:@"Other3"]) {
                tag.other3TrackUri = location;
            } else if ([elementName isEqualToString:@"Other4"]) {
                tag.other4TrackUri = location;
            }
        } else if ([result.lastObject isKindOfClass:[DPVideo class]]) {
            DPVideo *video = result.lastObject;
            if ([elementName isEqualToString:@"id"]) {
                video.videoId = [noCommas intValue];
            } else if ([elementName isEqualToString:@"Desc"]) {
                video.description = data;
            } else if ([elementName isEqualToString:@"SungKey"]) {
                data = [data stringByReplacingOccurrencesOfString:@"&#9837;" withString:@"\u266D"];
                data = [data stringByReplacingOccurrencesOfString:@"&#9839;" withString:@"\u266F"];
                video.sungKey = data;
            } else if ([elementName isEqualToString:@"Multitrack"]) {
                video.isMultitrack = [data isEqualToString:@"Yes"];
            } else if ([elementName isEqualToString:@"Code"]) {
                video.youTubeCode = data;
            } else if ([elementName isEqualToString:@"SungBy"]) {
                video.sungBy = data;
            } else if ([elementName isEqualToString:@"SungWebsite"]) {
                video.sungWebsite = [NSURL URLWithString:data];
            } else if ([elementName isEqualToString:@"Posted"]) {
                video.posted = [dayNameDateFormatter dateFromString:data];
            }
        }
    } else if ([elementName isEqualToString:@"video"]) {
        DPVideo *video = result.lastObject;
        [result removeLastObject];
        NSMutableArray *videoList = result.lastObject;
        [videoList addObject:video];
    } else if ([elementName isEqualToString:@"videos"]) {
        NSMutableArray *videoList = result.lastObject;
        [result removeLastObject];
        DPTag *tag = result.lastObject;
        tag.videos = [NSArray arrayWithArray:videoList];
    } else if ([elementName isEqualToString:@"tag"]) {
        DPTag *tag = result.lastObject;
        [result removeLastObject];
        DPTagQueryResult *queryResult = result.lastObject;
        [(NSMutableArray *)queryResult.tags addObject:tag];
    } else if ([elementName isEqualToString:@"tags"]) {
        DPTagQueryResult *queryResult = result.lastObject;
        queryResult.tags = [NSArray arrayWithArray:queryResult.tags];
    }
}

- (NSArray *)parseWithData:(NSData *)xmlData {
    NSXMLParser *parser = [[NSXMLParser alloc] initWithData:xmlData];
    parser.delegate = self;
    result = [NSMutableArray arrayWithCapacity:3];
    if (![parser parse]) {
        result = nil;
        return nil;
    }
    NSArray *toReturn = result;
    result = nil;
    return toReturn;
}

- (NSArray *)parseWithUrl:(NSURL *)url {
    NSXMLParser *parser = [[NSXMLParser alloc] initWithContentsOfURL:url];
    parser.delegate = self;
    result = [NSMutableArray arrayWithCapacity:3];
    if (![parser parse]) {
        result = nil;
        return nil;
    }
    NSArray *toReturn = result;
    result = nil;
    return toReturn;
}

@end
