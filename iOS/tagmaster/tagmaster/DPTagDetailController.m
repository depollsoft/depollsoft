//
//  DPTagDetailController.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTagDetailController.h"
#import "DPGridLayout.h"

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
    lastRefreshedFormatter.dateFormat = @"MM/dd/yy hh:mm:ss a";
    self.lastRefreshedLabel.text = [lastRefreshedFormatter stringFromDate:self.tag.lastRefreshed];
    
    NSDateFormatter *otherDateFormatter = [[NSDateFormatter alloc] init];
    otherDateFormatter.dateFormat = @"EEEE, LLLL d, yyyy";
    
    self.downloadsLabel.text = [NSString stringWithFormat:@"%d", self.tag.downloadCount];
    
    self.linkButton.url = [NSURL URLWithString:[NSString stringWithFormat:@"http://www.barbershoptags.com/dbpage.php?pg=view&dbase=tags&id=%d", self.tag.tagId]];
    
    [self.postedByButton setTitle:self.tag.provider forState:UIControlStateNormal];
    self.postedByButton.url = self.tag.providerWebsite;
    [self.postedByButton setEnabled:self.tag.providerWebsite];
    [self.grid setView:self.postedByHeader hidden:!self.tag.provider];
    [self.grid setView:self.postedByButton hidden:!self.tag.provider];
    
    self.postedLabel.text = [otherDateFormatter stringFromDate:self.tag.posted];
    
    [self.arrangedByButton setTitle:self.tag.arranger forState:UIControlStateNormal];
    self.arrangedByButton.url = self.tag.arrangerWebsite;
    [self.arrangedByButton setEnabled:self.tag.arrangerWebsite];
    [self.grid setView:self.arrangedByHeader hidden:!self.tag.arranger];
    [self.grid setView:self.arrangedByButton hidden:!self.tag.arranger];
    
    self.yearArrangedLabel.text = [NSString stringWithFormat:@"%d", self.tag.yearArranged];
    [self.grid setView:self.yearArrangedHeader hidden:self.tag.yearArranged == 0];
    [self.grid setView:self.yearArrangedLabel hidden:self.tag.yearArranged == 0];
    
    [self.sungByButton setTitle:self.tag.sungBy forState:UIControlStateNormal];
    self.sungByButton.url = self.tag.sungByWebsite;
    [self.sungByButton setEnabled:self.tag.sungByWebsite];
    [self.grid setView:self.sungByHeader hidden:!self.tag.sungBy];
    [self.grid setView:self.sungByButton hidden:!self.tag.sungBy];
    
    self.yearSungLabel.text = [NSString stringWithFormat:@"%d", self.tag.sungYear];
    [self.grid setView:self.yearSungHeader hidden:self.tag.sungYear == 0];
    [self.grid setView:self.yearSungLabel hidden:self.tag.sungYear == 0];
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
    self.linkButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [self.linkButton setTitle:@"BarbershopTags.com" forState:UIControlStateNormal];
    self.postedByHeader = [self makeHeader:@"Posted By"];
    self.postedByButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    self.postedHeader = [self makeHeader:@"Posted"];
    self.postedLabel = [self makeBodyLabel];
    self.arrangedByHeader = [self makeHeader:@"Arranged By"];
    self.arrangedByButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    self.yearArrangedHeader = [self makeHeader:@"Year Arranged"];
    self.yearArrangedLabel = [self makeBodyLabel];
    self.sungByHeader = [self makeHeader:@"Sung By"];
    self.sungByButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    self.yearSungHeader = [self makeHeader:@"Year Sung"];
    self.yearSungLabel = [self makeBodyLabel];
    
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
    [self.grid addSubview:self.linkButton row:4 column:2];
    [self.grid addSubview:self.postedByButton row:5 column:2];
    [self.grid addSubview:self.postedLabel row:6 column:2];
    [self.grid addSubview:self.arrangedByButton row:7 column:2];
    [self.grid addSubview:self.yearArrangedLabel row:8 column:2];
    [self.grid addSubview:self.sungByButton row:9 column:2];
    [self.grid addSubview:self.yearSungLabel row:10 column:2];
    
    UIScrollView *scroller = [[UIScrollView alloc] init];
    
    [self setUpGrid:self.grid withScroller:scroller];
    
    [self refreshView];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
