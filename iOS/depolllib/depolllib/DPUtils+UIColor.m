//
//  DPUtils+UIColor.m
//  depolllib
//
//  Created by David Poll on 6/21/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPUtils+UIColor.h"

@implementation UIColor (DPUtils)

- (UIColor *)invert {
    CGColorRef oldCGColor = self.CGColor;
    int numberOfComponents = CGColorGetNumberOfComponents(oldCGColor);
    
    // can not invert - the only component is the alpha
    // e.g. self == [UIColor groupTableViewBackgroundColor]
    if (numberOfComponents <= 1) {
        return [UIColor colorWithCGColor:oldCGColor];
    }
    
    const CGFloat *oldComponentColors = CGColorGetComponents(oldCGColor);
    CGFloat newComponentColors[numberOfComponents];
    int i = - 1;
    while (++i < numberOfComponents - 1) {
        newComponentColors[i] = 1 - oldComponentColors[i];
    }
    newComponentColors[i] = oldComponentColors[i]; // alpha
    
    CGColorRef newCGColor = CGColorCreate(CGColorGetColorSpace(oldCGColor), newComponentColors);
    UIColor *newColor = [UIColor colorWithCGColor:newCGColor];
    CGColorRelease(newCGColor);
    
    return newColor;
}

- (UIColor *)withAlpha:(CGFloat)alpha {
    CGColorRef oldCGColor = self.CGColor;
    int numberOfComponents = CGColorGetNumberOfComponents(oldCGColor);
    
    const CGFloat *oldComponentColors = CGColorGetComponents(oldCGColor);
    CGFloat newComponentColors[numberOfComponents];
    int i = - 1;
    while (++i < numberOfComponents - 1) {
        newComponentColors[i] = oldComponentColors[i];
    }
    newComponentColors[i] = alpha; // alpha
    
    CGColorRef newCGColor = CGColorCreate(CGColorGetColorSpace(oldCGColor), newComponentColors);
    UIColor *newColor = [UIColor colorWithCGColor:newCGColor];
    CGColorRelease(newCGColor);
    
    return newColor;
}

@end
