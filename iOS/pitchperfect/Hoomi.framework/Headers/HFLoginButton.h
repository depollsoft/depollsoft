//
//  HFLoginButton.h
//  Hoomi
//
//  Created by David Poll on 12/8/14.
//  Copyright (c) 2014 Hoomi. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <Hoomi/HFLoginButtonDelegate.h>

/*!
 Defines login button styles for the HFLoginButton.
 */
typedef NS_ENUM(NSInteger, HFLoginButtonStyle) {
  /*!
   Makes the login button use a green background with a white text/image overlay.
   */
  HFLoginButtonStyleWhiteOnGreen = 0,
  /*!
   Makes the login button use a white background with a green text/image overlay.
   */
  HFLoginButtonStyleGreenOnWhite = 1
};

@class HFClient;

/*!
 Provides a button that can be used to initiate login with Hoomi.
 */
IB_DESIGNABLE
@interface HFLoginButton : UIView

/*!
 The HFClient to use for login. The default client will be used if none is set.
 */
@property (nonatomic, readwrite, strong) HFClient *client;

/*!
 A list of scopes (strings) to request from the user when logging in.
 */
@property (nonatomic, readwrite, copy) NSArray *scopes;

/*!
 The redirect URI for this app. This value must be set before attempting login.
 */
@property (nonatomic, readwrite, copy) NSURL *redirectUri;

/*!
 The button style (green on white or white on green) for the button.
 */
@property (nonatomic, readwrite, assign) HFLoginButtonStyle buttonStyle;

/*!
 A delegate to receive messages when the button is clicked and when authorization completes.
 */
@property (nonatomic, weak) id<HFLoginButtonDelegate> delegate;

@end
