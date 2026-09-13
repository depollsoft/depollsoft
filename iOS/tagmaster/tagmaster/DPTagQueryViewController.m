//
//  DPTagQueryViewController.m
//  tagmaster
//
//  Created by David Poll on 8/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPTagQueryViewController.h"
#import "DPTagCell.h"
#import "DPUtils+Subscripts.h"
#import "DPTagViewController.h"
#import "DPAppDelegate.h"
#import "DPTagPageControllerBase.h"

// A stationary barber-pole frame with moving stripes, only for tag queries.
@interface TMBarberPoleLoadingView : UIView
@property (nonatomic, strong) CALayer *cylinder;
@property (nonatomic, strong) CAShapeLayer *stripes;
@property (nonatomic, strong) CAShapeLayer *frameLayer;
@property (nonatomic) BOOL requested;
@property (nonatomic) BOOL controllerVisible;
@property (nonatomic) BOOL appActive;
- (void)startAnimating;
- (void)stopAnimating;
- (BOOL)isAnimating;
@end

@implementation TMBarberPoleLoadingView
- (instancetype)initWithFrame:(CGRect)frame {
    if ((self = [super initWithFrame:frame])) {
        self.isAccessibilityElement = YES;
        self.accessibilityLabel = @"Loading tags";
        self.accessibilityIdentifier = @"query.loading.barberpole";
        self.appActive = UIApplication.sharedApplication.applicationState == UIApplicationStateActive;
        self.cylinder = [CALayer layer];
        self.cylinder.backgroundColor = UIColor.whiteColor.CGColor;
        self.cylinder.cornerRadius = 3;
        self.cylinder.masksToBounds = YES;
        [self.layer addSublayer:self.cylinder];
        self.stripes = [CAShapeLayer layer];
        [self.cylinder addSublayer:self.stripes];
        for (NSUInteger color = 0; color < 2; color++) {
            CAShapeLayer *stripe = [CAShapeLayer layer];
            UIBezierPath *path = [UIBezierPath bezierPath];
            for (NSInteger y = -72; y < 108; y += 24) {
                CGFloat start = y + color * 12;
                [path moveToPoint:CGPointMake(-20, start + 20)];
                [path addLineToPoint:CGPointMake(40, start - 10)];
                [path addLineToPoint:CGPointMake(40, start - 4)];
                [path addLineToPoint:CGPointMake(-20, start + 26)];
                [path closePath];
            }
            stripe.path = path.CGPath;
            stripe.fillColor = (color == 0 ? UIColor.systemRedColor : UIColor.systemBlueColor).CGColor;
            [self.stripes addSublayer:stripe];
        }
        self.frameLayer = [CAShapeLayer layer];
        [self.layer addSublayer:self.frameLayer];
        NSNotificationCenter *center = NSNotificationCenter.defaultCenter;
        [center addObserver:self selector:@selector(resignActive) name:UIApplicationWillResignActiveNotification object:nil];
        [center addObserver:self selector:@selector(becomeActive) name:UIApplicationDidBecomeActiveNotification object:nil];
        [center addObserver:self selector:@selector(updateAnimation) name:UIAccessibilityReduceMotionStatusDidChangeNotification object:nil];
    }
    return self;
}
- (void)dealloc { [NSNotificationCenter.defaultCenter removeObserver:self]; }
- (CGSize)intrinsicContentSize { return CGSizeMake(28, 68); }
- (CGSize)sizeThatFits:(CGSize)size { return CGSizeMake(MAX(28, self.bounds.size.width), 68); }
- (BOOL)reduceMotionEnabled { return UIAccessibilityIsReduceMotionEnabled(); }
- (BOOL)isAnimating { return self.requested; }
- (void)startAnimating { self.requested = YES; self.hidden = NO; [self updateAnimation]; }
- (void)stopAnimating { self.requested = NO; self.hidden = YES; [self updateAnimation]; }
- (void)setControllerVisible:(BOOL)visible { _controllerVisible = visible; [self updateAnimation]; }
- (void)setHidden:(BOOL)hidden { [super setHidden:hidden]; [self updateAnimation]; }
- (void)didMoveToWindow { [super didMoveToWindow]; [self updateAnimation]; }
- (void)resignActive { self.appActive = NO; [self updateAnimation]; }
- (void)becomeActive { self.appActive = YES; [self updateAnimation]; }
- (void)updateAnimation {
    BOOL visible = self.window && self.controllerVisible && self.appActive && self.requested;
    for (UIView *view = self; view; view = view.superview) {
        visible &= !view.hidden && view.alpha > 0;
        if (view.clipsToBounds) visible &= CGRectIntersectsRect([self convertRect:self.bounds toView:view], view.bounds);
    }
    if (!visible || [self reduceMotionEnabled]) {
        [self.stripes removeAnimationForKey:@"rotationStripes"];
    } else if (![self.stripes animationForKey:@"rotationStripes"]) {
        CABasicAnimation *motion = [CABasicAnimation animationWithKeyPath:@"transform.translation.y"];
        motion.fromValue = @0; motion.toValue = @24;
        motion.duration = 2;
        motion.repeatCount = HUGE_VALF;
        motion.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionLinear];
        [self.stripes addAnimation:motion forKey:@"rotationStripes"];
    }
}
- (void)layoutSubviews {
    [super layoutSubviews];
    [CATransaction begin]; [CATransaction setDisableActions:YES];
    CGFloat x = floor((self.bounds.size.width - 28) / 2), y = 8;
    self.cylinder.frame = CGRectMake(x + 5, y + 8, 18, 36);
    // Keep one full repeat above the clip throughout the downward travel.
    // Core Animation snapshots sublayers at their bounds during composition.
    self.stripes.frame = CGRectMake(0, -24, self.cylinder.bounds.size.width, self.cylinder.bounds.size.height + 48);
    self.frameLayer.frame = CGRectMake(x, y, 28, 52);
    UIBezierPath *frame = [UIBezierPath bezierPathWithRoundedRect:CGRectMake(4.5, 7.5, 19, 37) cornerRadius:3];
    self.frameLayer.path = frame.CGPath;
    self.frameLayer.fillColor = UIColor.clearColor.CGColor;
    self.frameLayer.strokeColor = [UIColor.secondaryLabelColor resolvedColorWithTraitCollection:self.traitCollection].CGColor;
    self.frameLayer.lineWidth = 1;
    if (self.frameLayer.sublayers.count == 0) {
        for (NSNumber *top in @[@0, @46]) {
            CAShapeLayer *cap = [CAShapeLayer layer];
            cap.path = [UIBezierPath bezierPathWithRoundedRect:CGRectMake(1, top.doubleValue, 26, 6) cornerRadius:3].CGPath;
            [self.frameLayer addSublayer:cap];
        }
    }
    for (CAShapeLayer *cap in self.frameLayer.sublayers) cap.fillColor = self.frameLayer.strokeColor;
    [CATransaction commit];
    [self updateAnimation];
}
- (void)traitCollectionDidChange:(UITraitCollection *)previousTraitCollection {
    [super traitCollectionDidChange:previousTraitCollection];
    [self setNeedsLayout];
}
@end

@interface DPTagQueryViewController () <UITableViewDataSource, UITableViewDelegate>

@property (atomic, retain) DPTagQueryResult *mostRecentResult;
@property (nonatomic, retain) TMBarberPoleLoadingView *activity;
@property (nonatomic, retain) UILabel *statusLabel;
@property (nonatomic, retain) UIButton *retryButton;
@property (nonatomic) BOOL failed;
@property (nonatomic, retain) UITableView *tagTable;
@property (nonatomic, retain) UIRefreshControl *refreshControl;

@end

@implementation DPTagQueryViewController

@synthesize tagTable;

- (id)init {
    if (self = [super init]) {
        [self commonInit];
    }
    return self;
}

- (id)initWithCoder:(NSCoder *)aDecoder {
    if (self = [super initWithCoder:aDecoder]) {
        [self commonInit];
    }
    return self;
}

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        [self commonInit];
    }
    return self;
}

- (void)commonInit {
    self.mostRecentResult = [[DPTagQueryResult alloc] init];
    self.mostRecentResult.start = 0;
    self.mostRecentResult.count = 0;
    self.isLoading = NO;
    self.hasMoreResults = YES;
    self.resultSetSize = 20;
    self.maxResults = 1000;
    self.tags = [NSMutableArray array];
    
    //self.query = @"lover come back";
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    if (![self.parentViewController isKindOfClass:[TMPageViewController class]]) {
        [DPAppDelegate setUpBackground:self.view];
    }
    
	// Do any additional setup after loading the view.
    
    self.tagTable = [[UITableView alloc] init];
    self.tagTable.translatesAutoresizingMaskIntoConstraints = NO;
    self.tagTable.delegate = self;
    self.tagTable.dataSource = self;
    self.tagTable.backgroundColor = [UIColor clearColor];
    self.tagTable.rowHeight = UITableViewAutomaticDimension;
    self.tagTable.estimatedRowHeight = 100;
    [self.tagTable registerClass:[DPTagCell class] forCellReuseIdentifier:@"Tag"];
    
    self.activity = [[TMBarberPoleLoadingView alloc] initWithFrame:CGRectMake(0, 0, self.view.bounds.size.width, 68)];
    
    self.statusLabel = [[UILabel alloc] init];
    self.statusLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
    self.statusLabel.adjustsFontForContentSizeCategory = YES;
    self.statusLabel.numberOfLines = 0;
    self.statusLabel.textAlignment = NSTextAlignmentCenter;
    self.statusLabel.textColor = [UIColor secondaryLabelColor];
    self.retryButton = [UIButton buttonWithType:UIButtonTypeSystem];
    [self.retryButton setTitle:@"Retry" forState:UIControlStateNormal];
    [self.retryButton.heightAnchor constraintGreaterThanOrEqualToConstant:44].active = YES;
    [self.retryButton addTarget:self action:@selector(refresh) forControlEvents:UIControlEventTouchUpInside];
    
    self.tagTable.tableFooterView = self.activity;
    
    self.refreshControl = [[UIRefreshControl alloc] init];
    [self.refreshControl addTarget:self action:@selector(refresh) forControlEvents:UIControlEventValueChanged];
    [self.tagTable addSubview:self.refreshControl];
    
    [self.view addSubview:self.tagTable];
    
    NSMutableDictionary *bindings = [NSMutableDictionary dictionaryWithDictionary:NSDictionaryOfVariableBindings(tagTable)];
    
    if (![self.parentViewController isKindOfClass:[TMPageViewController class]]) {
        [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:[tagTable]|"
                                                                          options:0
                                                                          metrics:nil
                                                                            views:bindings]];
        [tagTable.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.topAnchor].active = YES;
    } else {
        [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[tagTable]|"
                                                                          options:0
                                                                          metrics:nil
                                                                            views:bindings]];
    }
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[tagTable]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:bindings]];
    
    [self fetchResults];
    [self refreshViews]; // A request may have started before the view mounted.
    
    self.navigationItem.largeTitleDisplayMode = UINavigationItemLargeTitleDisplayModeNever;
    self.navigationItem.title = !self.query || self.query.length == 0 ? @"Search Results" : self.query;
}

- (void)refresh {
    if (self.isLoading) return;
    self.hasMoreResults = YES;
    self.failed = NO;
    self.statusText = nil;
    self.mostRecentResult = [[DPTagQueryResult alloc] init];
    self.mostRecentResult.start = 0;
    self.mostRecentResult.count = 0;
    self.tags = [NSMutableArray array];
    [self fetchResults];
}

- (void)refreshViews {
    if (self.isLoading) {
        self.tagTable.tableFooterView = self.activity;
        [self.activity startAnimating];
    } else {
        [self.activity stopAnimating];
        self.tagTable.tableFooterView = nil;
    }
    
    if (self.statusText) {
        self.statusLabel.text = self.statusText;
        // Centered symbol, message and recovery, the way system empty states read.
        UIImageView *symbol = [[UIImageView alloc] initWithImage:[UIImage systemImageNamed:self.failed ? @"wifi.exclamationmark" : @"magnifyingglass"
                                                                            withConfiguration:[UIImageSymbolConfiguration configurationWithPointSize:44 weight:UIImageSymbolWeightLight]]];
        symbol.tintColor = [UIColor secondaryLabelColor];
        symbol.contentMode = UIViewContentModeScaleAspectFit;
        symbol.isAccessibilityElement = NO;
        UIStackView *header = [[UIStackView alloc] initWithArrangedSubviews:@[symbol, self.statusLabel, self.retryButton]];
        header.axis = UILayoutConstraintAxisVertical;
        header.alignment = UIStackViewAlignmentCenter;
        header.spacing = 12;
        header.layoutMarginsRelativeArrangement = YES;
        header.directionalLayoutMargins = NSDirectionalEdgeInsetsMake(48, 32, 24, 32);
        self.retryButton.hidden = !self.failed;
        self.tagTable.tableHeaderView = header;
        [self sizeStatusHeader];
    } else {
        self.tagTable.tableHeaderView = nil;
    }
    [self sizeStatusHeader];
    [self.activity sizeToFit];
    if ([self.refreshControl isRefreshing]) {
        [self.refreshControl endRefreshing];
    }
    
    [self.tagTable reloadData];
}

- (void)sizeStatusHeader {
    UIView *header = self.tagTable.tableHeaderView;
    if (!header) return;
    CGFloat width = self.tagTable.bounds.size.width;
    CGFloat height = [header systemLayoutSizeFittingSize:CGSizeMake(width, 0) withHorizontalFittingPriority:UILayoutPriorityRequired verticalFittingPriority:UILayoutPriorityFittingSizeLevel].height;
    if (header.frame.size.height != height || header.frame.size.width != width) {
        header.frame = CGRectMake(0, 0, width, height);
        self.tagTable.tableHeaderView = header;
    }
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    self.activity.controllerVisible = YES;
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    self.activity.controllerVisible = NO;
}

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    [self sizeStatusHeader];
    if (self.tagTable.tableFooterView == self.activity) {
        CGRect frame = CGRectMake(0, 0, self.tagTable.bounds.size.width, 68);
        if (!CGSizeEqualToSize(self.activity.frame.size, frame.size)) {
            self.activity.frame = frame;
            self.tagTable.tableFooterView = self.activity;
        }
    }
}

- (void)fetchResults {
    if (self.isLoading) {
        return;
    }
    if (!self.hasMoreResults) {
        return;
    }
    self.isLoading = YES;
    [self refreshViews];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        @try {
            DPTagQueryResult *result = [DPTag query:self.query
                                    numberOfResults:self.resultSetSize
                                              start:self.mostRecentResult.start + self.mostRecentResult.count
                                              parts:self.parts
                                     learningTracks:self.hasLearningTracks
                                         sheetMusic:self.hasSheetMusic
                                         collection:self.collection
                                             sortBy:self.sortBy];
            if (!result) {
                [NSException raise:NSInternalInconsistencyException format:@"Result should be non-nil"];
            };
            self.statusText = nil;
            self.mostRecentResult = result;
            [(NSMutableArray *)self.tags addObjectsFromArray:result.tags];
            self.hasMoreResults = result.start + result.count < MIN(result.available, self.maxResults);
            if (result.available == 0) {
                self.statusText = @"No tags could be found that matched your query.";
            }
        }
        @catch (NSException *exception) {
            self.statusText = @"Tags couldn't be loaded. Check your connection and try again.";
            self.failed = YES;
            self.hasMoreResults = NO;
        }
        @finally {
            self.isLoading = NO;
            [self performSelectorOnMainThread:@selector(refreshViews) withObject:nil waitUntilDone:NO];
        }
    });
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return self.tags.count;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    [DPAppDelegate showTagWithId:[self.tags[indexPath.row] tagId] from:self];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    DPTagCell *cell = [tableView dequeueReusableCellWithIdentifier:@"Tag" forIndexPath:indexPath];
    cell.tagInstance = self.tags[indexPath.row];
    return cell;
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
    return UITableViewAutomaticDimension;
}

- (void)scrollViewDidScroll:(UIScrollView *)scrollView {
    [self.activity updateAnimation];
    CGFloat currentOffset = scrollView.contentOffset.y;
    CGFloat threshold = scrollView.contentSize.height * 7 / 8 - scrollView.frame.size.height;
    if (currentOffset >= threshold) {
        [self fetchResults];
    }
}

@end
