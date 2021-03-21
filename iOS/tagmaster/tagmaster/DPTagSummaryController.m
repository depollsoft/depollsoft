//
//  DPTagSummaryController.m
//  tagmaster
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTagSummaryController.h"
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"
#import "DPTextView.h"
#import "DPFileCache.h"
#import "DPPitchPipeButton.h"
#import <QuickLook/QuickLook.h>

@interface DPSheetMusicPreview : NSObject <QLPreviewItem>

@property (nonatomic, strong) NSURL *previewItemURL;
@property (nonatomic, strong) NSString *previewItemTitle;

@end

@implementation DPSheetMusicPreview

@end

@interface DPTagSummaryController () <QLPreviewControllerDataSource, UIActionSheetDelegate>

@property (nonatomic, strong) UILabel *titleLabel;
@property (nonatomic, strong) UILabel *akaLabel;
@property (nonatomic, strong) UIProgressView *ratingBar;
@property (nonatomic, strong) UILabel *ratingLabel;
@property (nonatomic, strong) UIButton *ratingButton;
@property (nonatomic, strong) UILabel *partsLabel;
@property (nonatomic, strong) UILabel *typeLabel;
@property (nonatomic, strong) DPPitchPipeButton *keyButton;
@property (nonatomic, strong) UILabel *classicTagNumberLabel;
@property (nonatomic, strong) UIButton *sheetMusicButton;
@property (nonatomic, strong) UILabel *lyricsLabel;
@property (nonatomic, strong) UILabel *notesLabel;
@property (nonatomic, strong) UILabel *ratingHeader;
@property (nonatomic, strong) UILabel *partsHeader;
@property (nonatomic, strong) UILabel *typeHeader;
@property (nonatomic, strong) UILabel *keyHeader;
@property (nonatomic, strong) UILabel *notesHeader;
@property (nonatomic, strong) UILabel *lyricsHeader;
@property (nonatomic, strong) UILabel *classicTagNumberHeader;

@property (nonatomic, strong) DPGridLayout *grid;

@end

@implementation DPTagSummaryController

@synthesize titleLabel, akaLabel, ratingBar, ratingLabel, partsLabel, typeLabel, keyButton, classicTagNumberLabel, sheetMusicButton, ratingButton, lyricsLabel, notesLabel, ratingHeader, partsHeader, typeHeader, keyHeader, notesHeader, lyricsHeader, classicTagNumberHeader, grid;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)refreshView {
    titleLabel.text = self.tag.title;
    
    akaLabel.text = [NSString stringWithFormat:@"a.k.a. %@", self.tag.alternativeTitle];
    [grid setView:akaLabel hidden:!self.tag.alternativeTitle];
    
    ratingLabel.text = [NSString stringWithFormat:@"%1.2f", self.tag.rating];
    ratingBar.progress = self.tag.rating / 5;
    
    partsLabel.text = [NSString stringWithFormat:@"%d", self.tag.parts];
    
    typeLabel.text = self.tag.tagType;
    
    keyButton.note = [self.tag keyNote];
    [keyButton.button setTitle:self.tag.writtenKey forState:UIControlStateNormal];
    [grid setView:keyButton hidden:!self.tag.writtenKey];
    [grid setView:keyHeader hidden:!self.tag.writtenKey];
    
    classicTagNumberLabel.text = [NSString stringWithFormat:@"%d", self.tag.classicTagNumber];
    [grid setView:classicTagNumberLabel hidden:self.tag.classicTagNumber == 0];
    [grid setView:classicTagNumberHeader hidden:self.tag.classicTagNumber == 0];
    
    [grid setView:sheetMusicButton hidden:!self.tag.sheetMusicUri];
    
    lyricsLabel.text = self.tag.lyrics;
    [grid setView:lyricsLabel hidden:!self.tag.lyrics];
    [grid setView:lyricsHeader hidden:!self.tag.lyrics];
    
    notesLabel.text = self.tag.notes;
    [grid setView:notesLabel hidden:!self.tag.notes];
    [grid setView:notesHeader hidden:!self.tag.notes];
    
    [self.ratingButton setEnabled:YES];
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    UIScrollView *scroller = [[UIScrollView alloc] init];
    
    titleLabel = [self makeTitleLabel];
    akaLabel = [[UILabel alloc] init];
    akaLabel.font = [akaLabel.font fontWithSize:18];
    akaLabel.numberOfLines = 0;
    ratingBar = [[UIProgressView alloc] initWithProgressViewStyle:UIProgressViewStyleBar];
    ratingBar.backgroundColor = [UIColor colorWithWhite:0 alpha:0.1];
    ratingButton = [[UIButton alloc] init];
    [ratingButton setTitle:@"Rate" forState:UIControlStateNormal];
    ratingLabel = [self makeBodyLabel];
    ratingButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [ratingButton setTitle:@"Rate" forState:UIControlStateNormal];
    partsLabel = [self makeBodyLabel];
    typeLabel = [self makeBodyLabel];
    keyButton = [[DPPitchPipeButton alloc] init];
    [keyButton.button setTitleColor:[UIColor blackColor] forState:UIControlStateNormal];
    keyButton.button.titleLabel.font = [UIFont systemFontOfSize:12];
    keyButton.button.contentEdgeInsets = UIEdgeInsetsMake(4, 0, 4, 0);
    classicTagNumberLabel = [self makeBodyLabel];
    sheetMusicButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [sheetMusicButton setTitle:@"Sheet Music" forState:UIControlStateNormal];
    [sheetMusicButton addTarget:self action:@selector(openSheetMusic) forControlEvents:UIControlEventTouchUpInside];
    lyricsLabel = [self makeBodyLabel];
    lyricsLabel.numberOfLines = 0;
    notesLabel = [self makeBodyLabel];
    notesLabel.numberOfLines = 0;
    
    ratingHeader = [self makeHeader:@"Rating"];
    partsHeader = [self makeHeader:@"Parts"];
    typeHeader = [self makeHeader:@"Type"];
    keyHeader = [self makeHeader:@"Key"];
    notesHeader = [self makeHeader:@"Notes"];
    lyricsHeader = [self makeHeader:@"Lyrics"];
    classicTagNumberHeader = [self makeHeader:@"Classic Tag"];
    
    grid = [[DPGridLayout alloc] init];
    grid.columnDimensions = @[
                              [DPGridDimension dimension],
                              [DPGridDimension dimensionWithSize:8],
                              [DPGridDimension dimensionWithStars:1],
                              ];
    grid.rowDimensions = @[
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
    
    // Add titles
    [grid addSubview:titleLabel row:0 column:0 rowSpan:1 colSpan:3];
    [grid addSubview:[akaLabel padLeft:20 top:0 right:0 bottom:8]
                 row:1
              column:0
             rowSpan:1
             colSpan:3];
    
    // Add headers
    [grid addSubview:[ratingHeader centeredVertically] row:2 column:0];
    [grid addSubview:partsHeader row:3 column:0];
    [grid addSubview:typeHeader row:4 column:0];
    [grid addSubview:keyHeader row:5 column:0];
    [grid addSubview:classicTagNumberHeader row:6 column:0];
    [grid addSubview:sheetMusicButton row:7 column:0 rowSpan:1 colSpan:3];
    [grid addSubview:[lyricsHeader alignTop] row:8 column:0];
    [grid addSubview:[notesHeader alignTop] row:9 column:0];
    
    // Add content
    [grid addSubview:partsLabel row:3 column:2];
    [grid addSubview:typeLabel row:4 column:2];
    [grid addSubview:keyButton row:5 column:2];
    [grid addSubview:classicTagNumberLabel row:6 column:2];
    [grid addSubview:[lyricsLabel padLeft:0 top:0 right:0 bottom:8] row:8 column:2];
    [grid addSubview:notesLabel row:9 column:2];
    
    // Build rating UI
    DPGridLayout *ratingGrid = [[DPGridLayout alloc] init];
    ratingGrid.columnDimensions = @[
                                    [DPGridDimension dimensionWithStars:1],
                                    [DPGridDimension dimension]
                                    ];
    ratingGrid.rowDimensions = @[
                                 [DPGridDimension dimension],
                                 [DPGridDimension dimension]
                                 ];
    
    [ratingGrid addSubview:[ratingLabel centeredHorizontally] row:0 column:0];
    [ratingGrid addSubview:[ratingBar alignTop] row:1 column:0];
    [ratingGrid addSubview:[ratingButton padHorizontal:8 vertical:0] row:0 column:1 rowSpan:2 colSpan:1];
    [grid addSubview:[ratingGrid padHorizontal:0 vertical:4] row:2 column:2];
    
    [ratingButton addTarget:self action:@selector(rate) forControlEvents:UIControlEventTouchUpInside];
    
    [self setUpRootView:grid withScroller:scroller];
    
    [self refreshView];
}

- (void)rate {
    UIActionSheet *actionSheet = [[UIActionSheet alloc] initWithTitle:nil
                                                             delegate:self
                                                    cancelButtonTitle:@"Cancel"
                                               destructiveButtonTitle:nil
                                                    otherButtonTitles:@"★★★★★", @"★★★★", @"★★★", @"★★", @"★", nil];
    [actionSheet showFromRect:ratingButton.frame inView:ratingButton animated:YES];
}

- (void)actionSheet:(UIActionSheet *)actionSheet clickedButtonAtIndex:(NSInteger)buttonIndex {
    NSInteger rating = 5 - buttonIndex;
    if (rating == 0) {
        return;
    }
    [self.busyIndicator incrementBusyCount];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        @try {
            [self.tag rate:rating];
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.busyIndicator decrementBusyCount];
                [self.ratingButton setEnabled:NO];
            });
        }
        @catch (NSException *exception) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.busyIndicator decrementBusyCount];
            });
        }
    });
}

- (void)openSheetMusic {
    [self.busyIndicator incrementBusyCount];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        @try {
            NSString *key = self.tag.sheetMusicUri.cacheKey;
            if (![[NSFileManager defaultManager] fileExistsAtPath:[DPFileCache pathForKey:key]]) {
                [DPFileCache writeData:[NSData dataWithContentsOfURL:self.tag.sheetMusicUri.uri] forKey:key];
            }
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.busyIndicator decrementBusyCount];
                QLPreviewController *previewer = [[QLPreviewController alloc] init];
                previewer.dataSource = self;
                [self presentViewController:previewer animated:YES completion:NULL];
            });
        } @catch (NSException *exception) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.busyIndicator decrementBusyCount];
            });
        }
    });
}

- (id<QLPreviewItem>)previewController:(QLPreviewController *)controller previewItemAtIndex:(NSInteger)index {
    NSURL *url = [NSURL fileURLWithPath:[DPFileCache pathForKey:self.tag.sheetMusicUri.cacheKey]];
    DPSheetMusicPreview *preview = [[DPSheetMusicPreview alloc] init];
    preview.previewItemURL = url;
    preview.previewItemTitle = self.tag.title;
    return preview;
}

- (NSInteger)numberOfPreviewItemsInPreviewController:(QLPreviewController *)controller {
    return 1;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
