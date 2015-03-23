//
//  HFAccessToken.h
//  Hoomi
//
//  Created by David Poll on 12/4/14.
//  Copyright (c) 2014 Hoomi. All rights reserved.
//

#import <Foundation/Foundation.h>

/*!
 Represents a Hoomi access token.
 */
@interface HFAccessToken : NSObject

/*!
 The token string that will be used for requests with this token.
 */
@property (nonatomic, readonly, copy) NSString *tokenString;

/*!
 The set of scopes known to be provided by this token. Users may revoke access
 to these scopes.
 */
@property (nonatomic, readonly, copy) NSArray *knownScopes;

/*!
 The expiration known for this token. The token may becoem invalid before its
 expiration date.
 */
@property (nonatomic, readonly, copy) NSDate *knownExpiration;

/*!
 Creates an HFAccessToken given just the token string, in case you are restoring
 a serialized token without additional information or retrieving a token from your
 server.
 */
+ (instancetype)tokenWithString:(NSString *)token;

/*!
 Creates an HFAccessToken with its known scopes and expiration.
 */
+ (instancetype)tokenWithString:(NSString *)token
                    knownScopes:(NSArray *)knownScopes
                knownExpiration:(NSDate *)knownExpiration;

@end
