//
//  DPKey.h
//  pitchperfectlib
//
//  Created by David Poll on 5/4/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>

@class DPNote;
@class DPKeyType;
@class DPAccidental;

@interface DPKey : NSObject

+ (NSArray *)majorKeys;
+ (NSArray *)minorKeys;

- (id)initWithNote:(DPNote *)note keyType:(DPKeyType *)type numAccidentals:(int)numAccidentals;
- (DPAccidental *)accidental;
- (NSString *)friendlyName;

@property (nonatomic, strong) DPNote *note;
@property (nonatomic, strong) DPKeyType *keyType;
@property (nonatomic) int numAccidentals;

@end
