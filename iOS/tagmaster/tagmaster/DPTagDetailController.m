//
//  DPTagDetailController.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTagDetailController.h"
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"
#import "DPTagPageControllerBase.h"

@interface DPTagDetailController ()

@property (nonatomic, strong) UILabel *titleLabel;
@property (nonatomic, strong) UILabel *tagIdHeader;
@property (nonatomic, strong) UILabel *tagIdLabel;
@property (nonatomic, strong) UILabel *lastRefreshedHeader;
@property (nonatomic, strong) UILabel *lastRefreshedLabel;
@property (nonatomic, strong) UILabel *downloadsHeader;
@property (nonatomic, strong) UILabel *downloadsLabel;
@property (nonatomic, strong) UILabel *linkHeader;
@property (nonatomic, strong) UIButton *linkButton;
@property (nonatomic, strong) UILabel *postedByHeader;
@property (nonatomic, strong) UIButton *postedByButton;
@property (nonatomic, strong) UILabel *postedHeader;
@property (nonatomic, strong) UILabel *postedLabel;
@property (nonatomic, strong) UILabel *arrangedByHeader;
@property (nonatomic, strong) UIButton *arrangedByButton;
@property (nonatomic, strong) UILabel *yearArrangedHeader;
@property (nonatomic, strong) UILabel *yearArrangedLabel;
@property (nonatomic, strong) UILabel *sungByHeader;
@property (nonatomic, strong) UIButton *sungByButton;
@property (nonatomic, strong) UILabel *yearSungHeader;
@property (nonatomic, strong) UILabel *yearSungLabel;

@property (nonatomic, strong) DPGridLayout *grid;

@end

@implementation DPTagDetailController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)refreshView {
    // Update UI
    self.titleLabel.text = self.tag.title;
    
    self.tagIdLabel.text = [NSString stringWithFormat:@"%d", self.tag.tagId];
    
    NSDateFormatter *lastRefreshedFormatter = [[NSDateFormatter alloc] init];
    lastRefreshedFormatter.dateStyle = NSDateFormatterMediumStyle;
    lastRefreshedFormatter.timeStyle = NSDateFormatterShortStyle;
    self.lastRefreshedLabel.text = [lastRefreshedFormatter stringFromDate:self.tag.lastRefreshed];
    
    NSDateFormatter *otherDateFormatter = [[NSDateFormatter alloc] init];
    otherDateFormatter.dateStyle = NSDateFormatterFullStyle;
    
    self.downloadsLabel.text = [NSString stringWithFormat:@"%d", self.tag.downloadCount];
    
    self.linkButton.url = [NSURL URLWithString:[NSString stringWithFormat:@"https://www.barbershoptags.com/dbpage.php?pg=view&dbase=tags&id=%d", self.tag.tagId]];
    
    [self.postedByButton setTitle:self.tag.provider forState:UIControlStateNormal];
    self.postedByButton.url = self.tag.providerWebsite;
    [self setButton:self.postedByButton linked:!!self.tag.providerWebsite];
    [self.grid setView:self.postedByHeader hidden:!self.tag.provider];
    [self.grid setView:self.postedByButton hidden:!self.tag.provider];
    
    self.postedLabel.text = [otherDateFormatter stringFromDate:self.tag.posted];
    
    [self.arrangedByButton setTitle:self.tag.arranger forState:UIControlStateNormal];
    self.arrangedByButton.url = self.tag.arrangerWebsite;
    [self setButton:self.arrangedByButton linked:!!self.tag.arrangerWebsite];
    [self.grid setView:self.arrangedByHeader hidden:!self.tag.arranger];
    [self.grid setView:self.arrangedByButton hidden:!self.tag.arranger];
    
    self.yearArrangedLabel.text = [NSString stringWithFormat:@"%d", self.tag.yearArranged];
    [self.grid setView:self.yearArrangedHeader hidden:self.tag.yearArranged == 0];
    [self.grid setView:self.yearArrangedLabel hidden:self.tag.yearArranged == 0];
    
    [self.sungByButton setTitle:self.tag.sungBy forState:UIControlStateNormal];
    self.sungByButton.url = self.tag.sungByWebsite;
    [self setButton:self.sungByButton linked:!!self.tag.sungByWebsite];
    [self.grid setView:self.sungByHeader hidden:!self.tag.sungBy];
    [self.grid setView:self.sungByButton hidden:!self.tag.sungBy];
    
    self.yearSungLabel.text = [NSString stringWithFormat:@"%d", self.tag.sungYear];
    [self.grid setView:self.yearSungHeader hidden:self.tag.sungYear == 0];
    [self.grid setView:self.yearSungLabel hidden:self.tag.sungYear == 0];
}

/// A name without a website is plain information, not a disabled control.
- (void)setButton:(UIButton *)button linked:(BOOL)linked {
    UIButtonConfiguration *configuration = button.configuration;
    configuration.baseForegroundColor = linked ? nil : [UIColor labelColor];
    button.configuration = configuration;
    button.enabled = YES;
    button.userInteractionEnabled = linked;
    button.accessibilityTraits = linked ? UIAccessibilityTraitLink : UIAccessibilityTraitStaticText;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    self.titleLabel = [self makeTitleLabel];
    self.tagIdHeader = [self makeHeader:@"Tag ID"];
    self.tagIdLabel = [self makeBodyLabel];
    self.lastRefreshedHeader = [self makeHeader:@"Last Refreshed"];
    self.lastRefreshedLabel = [self makeBodyLabel];
    self.downloadsHeader = [self makeHeader:@"Downloads"];
    self.downloadsLabel = [self makeBodyLabel];
    self.linkHeader = [self makeHeader:@"Link"];
    self.linkButton = [TMWrappingButton buttonWithType:UIButtonTypeSystem];
    [self.linkButton setTitle:@"BarbershopTags.com" forState:UIControlStateNormal];
    self.postedByHeader = [self makeHeader:@"Posted By"];
    self.postedByButton = [TMWrappingButton buttonWithType:UIButtonTypeSystem];
    self.postedHeader = [self makeHeader:@"Posted"];
    self.postedLabel = [self makeBodyLabel];
    self.arrangedByHeader = [self makeHeader:@"Arranged By"];
    self.arrangedByButton = [TMWrappingButton buttonWithType:UIButtonTypeSystem];
    self.yearArrangedHeader = [self makeHeader:@"Year Arranged"];
    self.yearArrangedLabel = [self makeBodyLabel];
    self.sungByHeader = [self makeHeader:@"Sung By"];
    self.sungByButton = [TMWrappingButton buttonWithType:UIButtonTypeSystem];
    self.yearSungHeader = [self makeHeader:@"Year Sung"];
    self.yearSungLabel = [self makeBodyLabel];
    // Headings keep a single-line intrinsic width so the heading column is sized from them;
    // a multi-line heading reports no width and the value column swallows it.
    for (UILabel *header in @[self.tagIdHeader, self.lastRefreshedHeader, self.downloadsHeader, self.linkHeader, self.postedByHeader,
                              self.postedHeader, self.arrangedByHeader, self.yearArrangedHeader, self.sungByHeader, self.yearSungHeader]) {
        header.numberOfLines = 1;
        [header setContentCompressionResistancePriority:UILayoutPriorityRequired forAxis:UILayoutConstraintAxisHorizontal];
    }
    
    for (UIButton *button in @[self.linkButton, self.postedByButton, self.arrangedByButton, self.sungByButton]) {
        // Let the configuration own wrapping; a multi-line titleLabel inside a configured
        // button collapses to its minimum width and wraps one character per line.
        UIButtonConfiguration *configuration = [UIButtonConfiguration plainButtonConfiguration];
        configuration.titleLineBreakMode = NSLineBreakByWordWrapping;
        // A hair of leading inset keeps the first glyph of a wrapped title from clipping.
        configuration.contentInsets = NSDirectionalEdgeInsetsMake(0, 2, 0, 2);
        configuration.titleTextAttributesTransformer = ^NSDictionary<NSAttributedStringKey, id> *(NSDictionary<NSAttributedStringKey, id> *attributes) {
            NSMutableDictionary *updated = [attributes mutableCopy];
            updated[NSFontAttributeName] = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
            return updated;
        };
        button.configuration = configuration;
        button.titleLabel.adjustsFontForContentSizeCategory = YES;
        button.contentHorizontalAlignment = UIControlContentHorizontalAlignmentLeading;
        [button.heightAnchor constraintGreaterThanOrEqualToConstant:44].active = YES;
    }
    self.grid = [[DPGridLayout alloc] init];
    self.grid.rowDimensions = @[
                                [DPGridDimension dimension],
                                [DPGridDimension dimension],
                                [DPGridDimension dimension],
                                [DPGridDimension dimension],
                                [DPGridDimension dimension],
                                [DPGridDimension dimension],
                                [DPGridDimension dimension],
                                [DPGridDimension dimension],
                                [DPGridDimension dimension],
                                [DPGridDimension dimension],
                                [DPGridDimension dimension]
                                ];
    self.grid.columnDimensions = @[
                                   [DPGridDimension dimension],
                                   [DPGridDimension dimensionWithSize:8],
                                   [DPGridDimension dimensionWithStars:1]
                                   ];
    
    // Set up headers
    [self.grid addSubview:self.titleLabel row:0 column:0 rowSpan:1 colSpan:3];
    [self.grid addSubview:self.tagIdHeader row:1 column:0];
    [self.grid addSubview:self.lastRefreshedHeader row:2 column:0];
    [self.grid addSubview:self.downloadsHeader row:3 column:0];
    [self.grid addSubview:self.linkHeader row:4 column:0];
    [self.grid addSubview:self.postedByHeader row:5 column:0];
    [self.grid addSubview:self.postedHeader row:6 column:0];
    [self.grid addSubview:self.arrangedByHeader row:7 column:0];
    [self.grid addSubview:self.yearArrangedHeader row:8 column:0];
    [self.grid addSubview:self.sungByHeader row:9 column:0];
    [self.grid addSubview:self.yearSungHeader row:10 column:0];
    
    // Set up bodies
    [self.grid addSubview:self.tagIdLabel row:1 column:2];
    [self.grid addSubview:self.lastRefreshedLabel row:2 column:2];
    [self.grid addSubview:self.downloadsLabel row:3 column:2];
    // Buttons span the value column and lead-align their titles, so they wrap like the labels.
    [self.grid addSubview:self.linkButton row:4 column:2];
    [self.grid addSubview:self.postedByButton row:5 column:2];
    [self.grid addSubview:self.postedLabel row:6 column:2];
    [self.grid addSubview:self.arrangedByButton row:7 column:2];
    [self.grid addSubview:self.yearArrangedLabel row:8 column:2];
    [self.grid addSubview:self.sungByButton row:9 column:2];
    [self.grid addSubview:self.yearSungLabel row:10 column:2];
    
    UIScrollView *scroller = [[UIScrollView alloc] init];
    
    [self setUpRootView:self.grid withScroller:scroller];
    
    [self refreshView];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
