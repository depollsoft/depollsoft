//
//  HFTokenInformation.h
//  Hoomi
//
//  Created by David Poll on 12/5/14.
//  Copyright (c) 2014 Hoomi. All rights reserved.
//

#import <Foundation/Foundation.h>

@class HFAccessToken;

/*!
 The result of querying for more information about a token.
 */
@interface HFTokenInformation : NSObject

/*!
 The token (with updated known scopes and expiration).
 */
@property (nonatomic, readonly, strong) HFAccessToken *token;

/*!
 The application to which this token belongs.
 */
@property (nonatomic, readonly, copy) NSString *applicationId;

/*!
 The time at which this token was issued.
 */
@property (nonatomic, readonly, copy) NSDate *issued;

/*!
 The user id (scoped to the application) that this token is for, if any.
 */
@property (nonatomic, readonly, copy) NSString *userId;

/*!
 Creates an HFTokenInformation object with the given token, applicationId, issuance date, and user ID.
 */
+ (instancetype)tokenInformationWithToken:(HFAccessToken *)token
                            applicationId:(NSString *)applicationId
                                   issued:(NSDate *)issued
                                   userId:(NSString *)userId;

@end
