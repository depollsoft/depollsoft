//
//  DPLabel.m
//  depolllib
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPLabel.h"

@implementation DPLabel

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code
    }
    return self;
}

- (CGSize)intrinsicContentSize {
    if (self.numberOfLines != 1) {
        return CGSizeMake(UIViewNoIntrinsicMetric, [super intrinsicContentSize].height);
    }
    return [super intrinsicContentSize];
}

- (void)setNumberOfLines:(NSInteger)numberOfLines {
    [super setNumberOfLines:numberOfLines];
    [self invalidateIntrinsicContentSize];
}

@end
