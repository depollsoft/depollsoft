//
//  DPPitchPipeModel.h
//  pitchperfect
//
//  Created by David Poll on 6/12/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface DPPitchPipeModel : NSObject

@property (nonatomic, readonly, strong) NSArray *notes;
@property (nonatomic) BOOL isFromFToF; 

@end
