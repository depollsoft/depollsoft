//
//  DPSongsModel.h
//  pitchperfect
//
//  Created by David Poll on 6/22/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface DPSongsModel : NSObject

+ (DPSongsModel *)sharedInstance;
- (void)saveAllToParse;
- (void)saveAllToParse:(BOOL)immediately;
- (void)refreshFromParse;
- (void)storeValue;

@property (nonatomic, strong) NSMutableArray *songs;
@property (nonatomic, weak) id delegate;

@end
