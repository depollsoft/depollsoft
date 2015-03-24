//
//  HFClient.h
//  Hoomi
//
//  Created by David Poll on 12/4/14.
//  Copyright (c) 2014 Hoomi. All rights reserved.
//

#import <Foundation/Foundation.h>

@class HFAccessToken;
@class HFAppData;
@class BFTask;

/*!
 The main entry point for working with Hoomi.
 */
@interface HFClient : NSObject

/*!
 Creates an HFClient with the given application ID from Hoomi.
 */
+ (instancetype)clientWithApplicationId:(NSString *)applicationId;

/*!
 Gets the default client to use for accessing Hoomi.
 */
+ (HFClient *)currentClient;

/*!
 Sets the default client to use for accessing Hoomi.
 */
+ (void)setCurrentClient:(HFClient *)client;

#pragma mark Token Management

@property (readwrite, nonatomic, strong) HFAccessToken *currentToken;

/*!
 Clears the current access token.
 */
- (void)logOut;

/*!
 Begins the process of authorizing with Hoomi using the given redirect url and scopes.
 
 @param redirectUrl the redirect URL to use to return to your app
 @param scopes the set of (NSString) scopes to request access to
 @return an HFAccessToken (asynchronously)
 */
- (BFTask *)authorizeAsyncWithRedirectUrl:(NSURL *)redirectUrl scopes:(NSArray *)scopes;

/*!
 Pass through for completing the login process.  Call this method from your AppDelegate's
 application:openURL:sourceApplication:annotation: selector implementation.
 */
- (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
  sourceApplication:(NSString *)sourceApplication
         annotation:(id)annotation;

/*!
 Gets token information for the given Hoomi access token.
 
 @param token the token to fetch information for
 @return HFTokenInformation (asynchronously)
 */
- (BFTask *)tokenInformationAsync:(HFAccessToken *)token;


#pragma mark App Data

/*!
 Gets the app data for a user.
 
 @param token the access token (which must have the user:app:data:read scope) for the user
 @return HFAppData (asynchronously)
 */
- (BFTask *)appDataAsyncWithToken:(HFAccessToken *)token;

/*!
 Gets the app data for the current user (the current token must have the user:app:data:read scope).
 
 @return HFAppData (asynchronously)
 */
- (BFTask *)appDataAsync;

/*!
 Sets the app data for the user with the given token.
 
 @param jsonData the new (JSON-serializable) data to associate with the user
 @param ETag an ETag to be used for optimistic concurrency control. Set to "*" to ignore the ETag
 @param token the token (which must have the user:app:data:write scope) for the user
 @return the new HFAppData (asynchronously)
 */
- (BFTask *)setAppDataAsync:(NSDictionary *)jsonData ETag:(NSString *)ETag token:(HFAccessToken *)token;

/*!
 Sets the app data for the current user (the current token must have the user:app:data:write scope).
 
 @param jsonData the new (JSON-serializable) data to associate with the user
 @param ETag an ETag to be used for optimistic concurrency control. Set to "*" to ignore the ETag
 @return the new HFAppData (asynchronously)
 */
- (BFTask *)setAppDataAsync:(NSDictionary *)jsonData ETag:(NSString *)ETag;

/*!
 Sets the app data for the user with the given token.
 
 @param jsonData the new (JSON-serializable) data to associate with the user
 @param token the token (which must have the user:app:data:write scope) for the user
 @return the new HFAppData (asynchronously)
 */
- (BFTask *)setAppDataAsync:(NSDictionary *)jsonData token:(HFAccessToken *)token;

/*!
 Sets the app data for the current user (the current token must have the user:app:data:write scope).
 
 @param jsonData the new (JSON-serializable) data to associate with the user
 @return the new HFAppData (asynchronously)
 */
- (BFTask *)setAppDataAsync:(NSDictionary *)jsonData;

@end
