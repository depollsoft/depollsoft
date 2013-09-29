//
//  UIButton+Hyperlink.m
//  depolllib
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "UIButton+Hyperlink.h"
#import <objc/runtime.h>

static int URL_KEY;

@implementation UIButton (Hyperlink)

- (NSURL *)url {
    return objc_getAssociatedObject(self, &URL_KEY);
}

- (void)setUrl:(NSURL *)url {
    objc_setAssociatedObject(self, &URL_KEY, url, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    [self removeTarget:self action:@selector(openHyperlink) forControlEvents:UIControlEventTouchUpInside];
    [self addTarget:self action:@selector(openHyperlink) forControlEvents:UIControlEventTouchUpInside];
}

- (void)openHyperlink {
    if (self.url) {
        [[UIApplication sharedApplication] openURL:self.url];
    }
}

@end
