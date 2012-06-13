//
//  DPTagXMLParser.h
//  tagmaster
//
//  Created by David Poll on 3/17/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface DPTagXMLParser : NSObject<NSXMLParserDelegate> {
    NSMutableArray *result;
    NSDateFormatter *dayNameDateFormatter;
}

- (NSArray *)parseWithData:(NSData *)xmlData;
- (NSArray *)parseWithUrl:(NSURL *)url;

@end
