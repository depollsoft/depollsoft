//
//  DPAppDelegate.m
//  tagmaster
//
//  Created by David Poll on 3/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPAppDelegate.h"
#import <limits.h>
#import "TMLogoBackgroundView.h"

@import FirebaseAuth;
@import FirebaseCore;

#if __has_include(<FBSDKCoreKit/FBSDKCoreKit.h>)
#import <FBSDKCoreKit/FBSDKCoreKit.h>
#define HAS_FBSDK 1
#else
#define HAS_FBSDK 0
#endif

#if __has_include(<GoogleSignIn/GoogleSignIn.h>)
#import <GoogleSignIn/GoogleSignIn.h>
#define HAS_GOOGLE_SIGN_IN 1
#else
#define HAS_GOOGLE_SIGN_IN 0
#endif

#import "DPBarbershop.h"
#import "DPHomeViewController.h"
#import "DPBrowseViewController.h"
#import "DPJsonSerializer.h"
#import "DPTagViewController.h"
#import "TMQuartetStaffView.h"
#import "TMQuartetArtwork.h"
#import "tagmaster-Swift.h"

NSNotificationName const TMTagListDidChangeNotification = @"TMTagListDidChangeNotification";
NSNotificationName const TMTagSelectionDidChangeNotification = @"TMTagSelectionDidChangeNotification";

/// Shown in the secondary column before a tag is chosen on iPad: the quartet
/// staff at rest, over the same barber-pole background as the rest of the app.
@interface TMTagPlaceholderController : UIViewController
@end

@implementation TMTagPlaceholderController
- (void)viewDidLoad {
    [super viewDidLoad];
    [DPAppDelegate setUpBackground:self.view];

    TMQuartetStaffView *staff = [TMQuartetStaffView new];
    staff.animationAllowed = NO;
    staff.translatesAutoresizingMaskIntoConstraints = NO;

    UILabel *heading = [UILabel new];
    heading.text = @"Pick a tag";
    UIFontDescriptor *descriptor = [[UIFont preferredFontForTextStyle:UIFontTextStyleTitle2].fontDescriptor
        fontDescriptorByAddingAttributes:@{UIFontDescriptorTraitsAttribute: @{UIFontWeightTrait: @(UIFontWeightSemibold)}}];
    heading.font = [UIFont fontWithDescriptor:descriptor size:0];
    heading.adjustsFontForContentSizeCategory = YES;
    heading.numberOfLines = 0;
    heading.textAlignment = NSTextAlignmentCenter;
    heading.textColor = UIColor.labelColor;
    heading.accessibilityTraits |= UIAccessibilityTraitHeader;

    UILabel *body = [UILabel new];
    body.text = @"Choose a tag from the list. Its summary, tracks, sheet music, and videos open here.";
    body.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
    body.adjustsFontForContentSizeCategory = YES;
    body.numberOfLines = 0;
    body.textAlignment = NSTextAlignmentCenter;
    body.textColor = UIColor.secondaryLabelColor;

    UIStackView *stack = [[UIStackView alloc] initWithArrangedSubviews:@[staff, heading, body]];
    stack.axis = UILayoutConstraintAxisVertical;
    stack.alignment = UIStackViewAlignmentCenter;
    stack.spacing = 8;
    [stack setCustomSpacing:24 afterView:staff];
    stack.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:stack];
    [NSLayoutConstraint activateConstraints:@[
        [stack.centerXAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.centerXAnchor],
        [stack.centerYAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.centerYAnchor],
        [stack.leadingAnchor constraintGreaterThanOrEqualToAnchor:self.view.safeAreaLayoutGuide.leadingAnchor constant:24],
        [stack.trailingAnchor constraintLessThanOrEqualToAnchor:self.view.safeAreaLayoutGuide.trailingAnchor constant:-24],
        [heading.widthAnchor constraintLessThanOrEqualToConstant:480],
        [body.widthAnchor constraintLessThanOrEqualToConstant:480],
        [staff.widthAnchor constraintEqualToConstant:TMQuartetWidth],
        [staff.heightAnchor constraintEqualToConstant:TMQuartetHeight]
    ]];
}
@end

@interface DPAppDelegate () <UISplitViewControllerDelegate>
@end

/// Records whichever responder a nil-targeted action lands on, i.e. the first responder.
static __weak UIResponder *TMRecordedFirstResponder;

@interface UIResponder (TMFirstResponder)
- (void)tm_recordFirstResponder:(id)sender;
@end

@implementation UIResponder (TMFirstResponder)
- (void)tm_recordFirstResponder:(id)sender {
    TMRecordedFirstResponder = self;
}
@end

@implementation DPAppDelegate

@synthesize window = _window;
@synthesize managedObjectContext = __managedObjectContext;
@synthesize managedObjectModel = __managedObjectModel;
@synthesize persistentStoreCoordinator = __persistentStoreCoordinator;
@synthesize navigationController;

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    if (NSClassFromString(@"XCTestCase") != nil) {
        self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
        self.window.rootViewController = [UIViewController new];
        self.window.hidden = YES;
        return YES;
    }

    [DPAppLog start];
    [FIRApp configure];
#if HAS_FBSDK
    [[FBSDKApplicationDelegate sharedInstance] application:application
                             didFinishLaunchingWithOptions:launchOptions];
#endif
    [application registerForRemoteNotifications];
        
    self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
    [DPJsonSerializer registerSerializer:^NSString *(NSURL *url) {
        return [url absoluteString];
    } deserializer:^NSURL *(NSString *input) {
        return [NSURL URLWithString:input];
    } forClass:[NSURL class]];
    
    NSNumberFormatter *numberFormatter = [[NSNumberFormatter alloc] init];
    [DPJsonSerializer registerSerializer:^NSString *(NSDate *date) {
        return [numberFormatter stringFromNumber:@([date timeIntervalSince1970])];
    } deserializer:^NSDate *(NSString *input) {
        return [NSDate dateWithTimeIntervalSince1970:[numberFormatter numberFromString:input].doubleValue];
    } forClass:[[NSDate date] class]];
    
    // Override point for customization after application launch.
    self.window.backgroundColor = [UIColor systemBackgroundColor];
    self.window.tintColor = [DPAppDelegate accentColor];
    UINavigationBarAppearance *navigationAppearance = [[UINavigationBarAppearance alloc] init];
    [navigationAppearance configureWithOpaqueBackground];
    navigationAppearance.backgroundColor = [UIColor colorWithWhite:55.0 / 255.0 alpha:1];
    navigationAppearance.titleTextAttributes = @{NSForegroundColorAttributeName: [UIColor whiteColor]};
    navigationAppearance.largeTitleTextAttributes = @{NSForegroundColorAttributeName: [UIColor whiteColor]};
    UINavigationController *navController = [[UINavigationController alloc] init];
    UINavigationBar *navigationBar = navController.navigationBar;
    navigationBar.standardAppearance = navigationAppearance;
    navigationBar.scrollEdgeAppearance = navigationAppearance;
    navigationBar.compactAppearance = navigationAppearance;
    navigationBar.compactScrollEdgeAppearance = navigationAppearance;
    navigationBar.tintColor = [UIColor whiteColor];
    navigationBar.overrideUserInterfaceStyle = UIUserInterfaceStyleDark;
    navigationBar.barStyle = UIBarStyleBlack;
    // With opaque chrome, keep UIKit's large-title host above the bar background.
    navigationBar.translucent = NO;
    [self.window makeKeyAndVisible];
    
    navController.navigationBar.prefersLargeTitles = YES;
    [navController pushViewController:[[DPHomeViewController alloc] init] animated:NO];
    navigationController = navController;

    if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad) {
        // List and detail side by side on iPad; the Home stack stays the primary column.
        UISplitViewController *split = [[UISplitViewController alloc] initWithStyle:UISplitViewControllerStyleDoubleColumn];
        split.delegate = self;
        // One watermark behind both columns, installed before any column loads its view.
        [DPAppDelegate installSharedBackgroundIn:split.view];
        split.preferredDisplayMode = UISplitViewControllerDisplayModeOneBesideSecondary;
        split.preferredSplitBehavior = UISplitViewControllerSplitBehaviorTile;
        // A comfortable list width on both 11- and 13-inch iPads, without
        // touching the bar appearance shared with the detail column.
        split.minimumPrimaryColumnWidth = 320;
        split.maximumPrimaryColumnWidth = 400;
        split.preferredPrimaryColumnWidthFraction = 0.36;
        [split setViewController:navController forColumn:UISplitViewControllerColumnPrimary];
        UINavigationController *detailNavigation = [[UINavigationController alloc] initWithRootViewController:[[TMTagPlaceholderController alloc] init]];
        detailNavigation.navigationBar.standardAppearance = navigationAppearance;
        detailNavigation.navigationBar.scrollEdgeAppearance = navigationAppearance;
        detailNavigation.navigationBar.compactAppearance = navigationAppearance;
        detailNavigation.navigationBar.compactScrollEdgeAppearance = navigationAppearance;
        detailNavigation.navigationBar.tintColor = [UIColor whiteColor];
        detailNavigation.navigationBar.overrideUserInterfaceStyle = UIUserInterfaceStyleDark;
        detailNavigation.navigationBar.barStyle = UIBarStyleBlack;
        detailNavigation.navigationBar.translucent = NO;
        [split setViewController:detailNavigation forColumn:UISplitViewControllerColumnSecondary];
        self.window.rootViewController = split;
    } else {
        self.window.rootViewController = navController;
    }
    [self.window makeKeyAndVisible];
    
    [self extraInit];

    return YES;
}

- (BOOL)application:(UIApplication *)app openURL:(NSURL *)url options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options {
#if HAS_GOOGLE_SIGN_IN
    if ([[GIDSignIn sharedInstance] handleURL:url]) {
        return YES;
    }
#endif
#if HAS_FBSDK
    if ([[FBSDKApplicationDelegate sharedInstance] application:app
                                                     openURL:url
                                                     options:options]) {
        return YES;
    }
#endif
    if ([[FIRAuth auth] canHandleURL:url]) {
        return YES;
    }
    // Auth callbacks above keep their provider-specific schemes and paths.
    if (url.scheme.length == 0 || [url.scheme caseInsensitiveCompare:@"tagmaster"] != NSOrderedSame ||
        url.user || url.password || url.port) return NO;
    // Split without normalizing away empty components or trailing slashes.
    NSString *path = [NSURLComponents componentsWithURL:url resolvingAgainstBaseURL:NO].path;
    NSArray<NSString *> *components = [path componentsSeparatedByString:@"/"];
    BOOL hostIsTag = [url.host isEqualToString:@"tag"] && components.count == 2;
    BOOL pathHasTag = (url.host.length == 0 || [url.host isEqualToString:@"open"]) &&
        components.count == 3 && [components[1] isEqualToString:@"tag"];
    if ((!hostIsTag && !pathHasTag) || ![components.firstObject isEqualToString:@""]) return NO;
    NSString *identifier = components.lastObject;
    if (identifier.length == 0) return NO;
    int tagId = 0;
    for (NSUInteger index = 0; index < identifier.length; index++) {
        unichar character = [identifier characterAtIndex:index];
        if (character < '0' || character > '9') return NO;
        int digit = character - '0';
        if (tagId > (INT_MAX - digit) / 10) return NO;
        tagId = tagId * 10 + digit;
    }
    if (tagId == 0) return NO;
    [DPAppDelegate showTagWithId:tagId from:self.navigationController.topViewController];
    return YES;
}

+ (void)showTagWithId:(int)tagId from:(UIViewController *)sender {
    id<TMTagListSource> source = [sender conformsToProtocol:@protocol(TMTagListSource)] ? (id<TMTagListSource>)sender : nil;
    if ([sender isKindOfClass:DPTagViewController.class]) source = ((DPTagViewController *)sender).source;
    UISplitViewController *split = sender.splitViewController;
    if (split && !split.isCollapsed) {
        UINavigationController *secondary = (UINavigationController *)[split viewControllerForColumn:UISplitViewControllerColumnSecondary];
        UIViewController *root = [secondary isKindOfClass:[UINavigationController class]] ? secondary.viewControllers.firstObject : nil;
        if ([root isKindOfClass:[DPTagViewController class]]) {
            // Reuse the existing detail so the page the user was on (Summary,
            // Details, Tracks, Videos) survives the tag change.
            DPTagViewController *existing = (DPTagViewController *)root;
            existing.source = source;
            existing.tagId = tagId;
        } else {
            DPTagViewController *controller = [[DPTagViewController alloc] init];
            controller.source = source;
            controller.tagId = tagId;
            if ([secondary isKindOfClass:UINavigationController.class]) {
                // Keep the placeholder's configured navigation bar and safe-area behavior.
                [secondary setViewControllers:@[controller] animated:NO];
            } else {
                [split setViewController:[[UINavigationController alloc] initWithRootViewController:controller]
                               forColumn:UISplitViewControllerColumnSecondary];
            }
        }
        [NSNotificationCenter.defaultCenter postNotificationName:TMTagSelectionDidChangeNotification object:split];
        [source tm_didStepToTagId:tagId];
        if (split.displayMode == UISplitViewControllerDisplayModeOneOverSecondary) {
            [split hideColumn:UISplitViewControllerColumnPrimary];
        }
        return;
    }
    DPTagViewController *controller = [[DPTagViewController alloc] init];
    controller.tagId = tagId;
    UINavigationController *navigation = sender.navigationController ?: [(DPAppDelegate *)UIApplication.sharedApplication.delegate navigationController];
    [navigation pushViewController:controller animated:YES];
}

#pragma mark - Keyboard stepping from either column

/// The detail showing beside a list, or nil when there is no expanded split or no tag yet.
- (DPTagViewController *)tm_expandedDetail {
    UISplitViewController *split = (UISplitViewController *)self.window.rootViewController;
    if (![split isKindOfClass:UISplitViewController.class] || split.isCollapsed) return nil;
    UINavigationController *secondary = (UINavigationController *)[split viewControllerForColumn:UISplitViewControllerColumnSecondary];
    UIViewController *root = [secondary isKindOfClass:UINavigationController.class] ? secondary.viewControllers.firstObject : nil;
    return [root isKindOfClass:DPTagViewController.class] ? (DPTagViewController *)root : nil;
}

/// Whether the first responder sits inside the detail column, where the detail's own key
/// commands already serve; asking the chain with a nil target reaches the first responder.
- (BOOL)tm_detailOwnsFocus:(DPTagViewController *)detail {
    TMRecordedFirstResponder = nil;
    [UIApplication.sharedApplication sendAction:@selector(tm_recordFirstResponder:) to:nil from:self forEvent:nil];
    for (UIResponder *responder = TMRecordedFirstResponder; responder; responder = responder.nextResponder) {
        if (responder == detail) return YES;
    }
    return NO;
}

/// The list column usually holds keyboard focus on iPad, and the detail is not in its
/// responder chain. The delegate is always in the chain, so it republishes the detail's
/// ⌘↑ / ⌘↓ commands and forwards their actions.
- (NSArray<UIKeyCommand *> *)keyCommands {
    DPTagViewController *detail = [self tm_expandedDetail];
    if (!detail || [self tm_detailOwnsFocus:detail]) return nil;
    return detail.keyCommands;
}

- (void)stepToPreviousTag {
    [[self tm_expandedDetail] stepToPreviousTag];
}

- (void)stepToNextTag {
    [[self tm_expandedDetail] stepToNextTag];
}

- (BOOL)canPerformAction:(SEL)action withSender:(id)sender {
    if (action == @selector(stepToPreviousTag) || action == @selector(stepToNextTag)) {
        DPTagViewController *detail = [self tm_expandedDetail];
        return detail != nil && [detail canPerformAction:action withSender:sender];
    }
    return [super canPerformAction:action withSender:sender];
}

+ (UIColor *)accentColor {
    static UIColor *accent;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        accent = [UIColor colorWithDynamicProvider:^UIColor *(UITraitCollection *traits) {
            return traits.userInterfaceStyle == UIUserInterfaceStyleDark
                ? [UIColor colorWithRed:0x5A / 255.0 green:0xC8 / 255.0 blue:0xFA / 255.0 alpha:1]
                : [UIColor colorWithRed:0x00 / 255.0 green:0x7A / 255.0 blue:0xA3 / 255.0 alpha:1];
        }];
    });
    return accent;
}

+ (NSNumber *)currentSplitTagIdFor:(UIViewController *)sender {
    UISplitViewController *split = sender.splitViewController;
    if (!split || split.isCollapsed) return nil;
    UINavigationController *secondary = (UINavigationController *)[split viewControllerForColumn:UISplitViewControllerColumnSecondary];
    UIViewController *root = [secondary isKindOfClass:[UINavigationController class]] ? secondary.viewControllers.firstObject : nil;
    if (![root isKindOfClass:[DPTagViewController class]]) return nil;
    return @(((DPTagViewController *)root).tagId);
}

- (void)splitViewControllerDidCollapse:(UISplitViewController *)splitViewController {
    [NSNotificationCenter.defaultCenter postNotificationName:TMTagSelectionDidChangeNotification object:splitViewController];
}

- (void)splitViewControllerDidExpand:(UISplitViewController *)splitViewController {
    [NSNotificationCenter.defaultCenter postNotificationName:TMTagSelectionDidChangeNotification object:splitViewController];
}

- (UISplitViewControllerColumn)splitViewController:(UISplitViewController *)svc topColumnForCollapsingToProposedTopColumn:(UISplitViewControllerColumn)proposedTopColumn {
    // Keep a chosen tag on top when the window narrows; never surface the placeholder.
    UINavigationController *secondary = (UINavigationController *)[svc viewControllerForColumn:UISplitViewControllerColumnSecondary];
    UIViewController *detail = [secondary isKindOfClass:[UINavigationController class]] ? secondary.topViewController : secondary;
    return [detail isKindOfClass:[DPTagViewController class]] ? UISplitViewControllerColumnSecondary : UISplitViewControllerColumnPrimary;
}

- (void)applicationWillResignActive:(UIApplication *)application
{
    /*
     Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
     Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
     */
}

- (void)applicationDidEnterBackground:(UIApplication *)application
{
    /*
     Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
     If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
     */
}

- (void)applicationWillEnterForeground:(UIApplication *)application
{
    /*
     Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
     */
}

- (void)applicationWillTerminate:(UIApplication *)application
{

}

+ (BOOL)containsFavorite:(int)tagId {
    return [self.favorites containsObject:@(tagId)];
}

+ (void)moveFavoriteAt:(NSUInteger)fromIndex to:(NSUInteger)toIndex {
    NSMutableArray *favorites = [NSMutableArray arrayWithArray:self.favorites];
    id toMove = self.favorites[fromIndex];
    [favorites removeObjectAtIndex:fromIndex];
    [favorites insertObject:toMove atIndex:toIndex];
    [self setFavorites:favorites];
}

+ (void)addFavorite:(int)tagId {
    if ([self.favorites containsObject:@(tagId)]) {
        return;
    }
    NSMutableArray *favorites = [NSMutableArray arrayWithArray:self.favorites];
    [favorites addObject:@(tagId)];
    [self setFavorites:favorites];
}

+ (void)removeFavorite:(int)tagId {
    NSMutableArray *favorites = [NSMutableArray arrayWithArray:self.favorites];
    [favorites removeObject:@(tagId)];
    [self setFavorites:favorites];
}

+ (BOOL)containsTeachable:(int)tagId {
    return [self.teachable containsObject:@(tagId)];
}

+ (void)moveTeachableAt:(NSUInteger)fromIndex to:(NSUInteger)toIndex {
    NSMutableArray *teachable = [NSMutableArray arrayWithArray:self.teachable];
    id toMove = teachable[fromIndex];
    [teachable removeObjectAtIndex:fromIndex];
    [teachable insertObject:toMove atIndex:toIndex];
    [self setTeachable:teachable];
}

+ (void)addTeachable:(int)tagId {
    if ([self.teachable containsObject:@(tagId)]) {
        return;
    }
    NSMutableArray *teachable = [NSMutableArray arrayWithArray:self.teachable];
    [teachable addObject:@(tagId)];
    [self setTeachable:teachable];
}

+ (void)removeTeachable:(int)tagId {
    NSMutableArray *teachable = [NSMutableArray arrayWithArray:self.teachable];
    [teachable removeObject:@(tagId)];
    [self setTeachable:teachable];
}

+ (UIBarButtonItem *)barButtonItemWithSystemName:(NSString *)systemName
                                          target:(id)target
                                          action:(SEL)action {
    UIImageSymbolConfiguration *configuration =
        [UIImageSymbolConfiguration configurationWithPointSize:17
                                                        weight:UIImageSymbolWeightRegular
                                                         scale:UIImageSymbolScaleMedium];
    UIImage *image = [UIImage systemImageNamed:systemName
                             withConfiguration:configuration];
    UIBarButtonItem *item = [[UIBarButtonItem alloc] initWithImage:image
                                            style:UIBarButtonItemStylePlain
                                           target:target
                                           action:action];
    item.accessibilityLabel = @{@"magnifyingglass": @"Search",
                                @"square.and.arrow.up": @"Share",
                                @"tag": @"Favorite and Teachable options",
                                @"arrow.clockwise": @"Refresh",
                                @"chevron.up": @"Previous tag",
                                @"chevron.down": @"Next tag"}[systemName];
    return item;
}

static TMLogoBackgroundView *TMSharedBackground;

+ (TMLogoBackgroundView *)addLogoBackgroundTo:(UIView *)view {
    TMLogoBackgroundView *backgroundImage = [[TMLogoBackgroundView alloc] initWithFrame:CGRectZero];
    backgroundImage.translatesAutoresizingMaskIntoConstraints = NO;
    [view addSubview:backgroundImage];
    [view sendSubviewToBack:backgroundImage];
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[backgroundImage]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(backgroundImage)]];
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|-60-[backgroundImage]-44-|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(backgroundImage)]];
    return backgroundImage;
}

+ (void)installSharedBackgroundIn:(UIView *)view {
    [TMSharedBackground removeFromSuperview];
    view.backgroundColor = [UIColor systemBackgroundColor];
    TMSharedBackground = [self addLogoBackgroundTo:view];
}

+ (void)removeSharedBackground {
    [TMSharedBackground removeFromSuperview];
    TMSharedBackground = nil;
}

+ (void)setUpBackground:(UIView *)view {
    if (TMSharedBackground) {
        // The split paints the watermark once behind both columns; screens stay clear.
        view.backgroundColor = [UIColor clearColor];
        if ([view isKindOfClass:[UITableView class]]) ((UITableView *)view).backgroundView = nil;
        return;
    }
    view.backgroundColor = [UIColor systemBackgroundColor];
    if ([view isKindOfClass:[UITableView class]]) {
        UITableView *tableView = (UITableView *)view;
        view = tableView.backgroundView = [[UIView alloc] init];
    }
    [self addLogoBackgroundTo:view];
}

@end
