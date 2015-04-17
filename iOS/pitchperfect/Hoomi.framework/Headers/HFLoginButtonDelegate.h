//
//  HFLoginButtonDelegate.h
//  Hoomi
//
//  Created by David Poll on 12/8/14.
//  Copyright (c) 2014 Hoomi. All rights reserved.
//

#import <Hoomi/HFAccessToken.h>

@class HFLoginButton;

/*!
 Delegate definition for handling events from HFLoginButton.
 */
@protocol HFLoginButtonDelegate <NSObject>

@optional

/*!
 Called by the HFLoginButton before initiating authorization. Use this to show a progress indicator.
 
 @param button the button that was the source of the event
 */
- (void)buttonWillPerformHoomiAuthorization:(HFLoginButton *)button;

/*!
 Called by the HFLoginButton after authorization completes with either the new token or the error that
 occurred during the login process. This may not be called if the application was closed in the background
 during authorization.
 
 @param button the button that was the source of the event
 @param token the token (if any) that resulted from the authorization
 @param error the error that occurred during authorization (if any)
 */
- (void)button:(HFLoginButton *)button didPerformHoomiAuthorizationWithResult:(HFAccessToken *)token error:(NSError *)error;

@end