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
 
 @param applicationId the application ID that this client will use
 */
+ (instancetype)clientWithApplicationId:(NSString *)applicationId;

/*!
 Gets the default client to use for accessing Hoomi.
 */
+ (HFClient *)currentClient;

/*!
 Sets the default client to use for accessing Hoomi.
 
 @param client the new current HFClient
 */
+ (void)setCurrentClient:(HFClient *)client;

/*!
 Enables client authentication, which will only be effective when not running in the simulator
 and when an app receipt is available on the device (as will be the case when the app has been purchased
 through the app store).
 */
- (void)enableClientAuthentication;

/*!
 Enables client authentication, and will request an app receipt if none is
 available.  During testing, you won't be able to use the simulator, and may
 need to register a sandbox account through iTunes Connect (https://developer.apple.com/library/ios/documentation/LanguagesUtilities/Conceptual/iTunesConnect_Guide/Chapters/SettingUpUserAccounts.html)
 */
- (void)requireClientAuthentication;

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
 
 @param application the UIApplication
 @param url the URL being opened in the application
 @param sourceApplication the bundle ID of the app that is requesting your app to open the URL
 @param annotation a property list object supplied by the source app to communicate information to the receiving app
 
 @return true if and only if the link was the result of a Hoomi callback and was handled by Hoomi
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
