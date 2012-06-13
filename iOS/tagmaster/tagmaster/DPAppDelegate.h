//
//  DPAppDelegate.h
//  tagmaster
//
//  Created by David Poll on 3/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface DPAppDelegate : UIResponder <UIApplicationDelegate>

@property (strong, nonatomic) UIWindow *window;

@property (readonly, strong, nonatomic) NSManagedObjectContext *managedObjectContext;
@property (readonly, strong, nonatomic) NSManagedObjectModel *managedObjectModel;
@property (readonly, strong, nonatomic) NSPersistentStoreCoordinator *persistentStoreCoordinator;

- (void)saveContext;
- (NSURL *)applicationDocumentsDirectory;

@end
