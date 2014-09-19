//
//  DPTagVideoController.m
//  tagmaster
//
//  Created by David Poll on 10/5/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTagVideoController.h"
#import "UIView+DPUtils.h"
#import "DPFileCache.h"

@interface DPTagVideoController () <UITableViewDelegate, UITableViewDataSource>

@property (nonatomic, strong) UITableView *tableView;

@end

@implementation DPTagVideoController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
	self.tableView = [[UITableView alloc] initWithFrame:CGRectNull style:UITableViewStyleGrouped];
    self.tableView.dataSource = self;
    self.tableView.delegate = self;
    self.tableView.translatesAutoresizingMaskIntoConstraints = NO;
    self.tableView.backgroundColor = [UIColor clearColor];
    
    [self.view addSubview:self.tableView];
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[_tableView]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(_tableView)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[_tableView]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(_tableView)]];
    
    [self refreshView];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)refreshView {
    [self.tableView reloadData];
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section {
    if (self.tag.teachingVideo && section == 0) {
        return @"Teaching Video";
    }
    return @"User Submissions";
}

- (NSString *)tableView:(UITableView *)tableView titleForFooterInSection:(NSInteger)section {
    if (self.tag.teachingVideo && section == 0) {
        return  nil;
    }
    if (self.tag.videos.count > 0) {
        return nil;
    }
    return @"Sorry, this tag does not have any videos associated with it.";
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    if (self.tag.teachingVideo) {
        return 2;
    }
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    if (self.tag.teachingVideo && section == 0) {
        return 1;
    }
    return self.tag.videos.count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    DPGridLayout *grid = [[DPGridLayout alloc] init];
    grid.rowDimensions = @[
                           [DPGridDimension dimensionWithStars:1],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimensionWithStars:1]
                           ];
    grid.columnDimensions = @[
                              [DPGridDimension dimensionWithSize:88],
                              [DPGridDimension dimension],
                              [DPGridDimension dimensionWithSize:8],
                              [DPGridDimension dimensionWithStars:1]
                              ];
    grid.translatesAutoresizingMaskIntoConstraints = NO;
    
    UIImageView *thumb = [[UIImageView alloc] init];
    thumb.backgroundColor = [UIColor darkGrayColor];
    [grid addSubview:[thumb pad:4] row:0 column:0 rowSpan:6 colSpan:1];
    
    NSURL *thumbnail = nil;
    
    if (self.tag.teachingVideo && indexPath.section == 0) {
        [grid addSubview:[self makeHeader:@"Teacher"] row:1 column:1];
        UILabel *teacherLabel = [self makeBodyLabel];
        teacherLabel.text = self.tag.teacher;
        [grid addSubview:teacherLabel row:1 column:3];
        thumbnail = [NSURL URLWithString:[NSString stringWithFormat:@"http://img.youtube.com/vi/%@/2.jpg", self.tag.teachingVideo]];
    } else {
        DPVideo *video = self.tag.videos[indexPath.row];
        thumbnail = [NSURL URLWithString:[NSString stringWithFormat:@"http://img.youtube.com/vi/%@/2.jpg", video.youTubeCode]];
        
        if (video.sungBy) {
            [grid addSubview:[self makeHeader:@"Sung By"] row:1 column:1];
            UILabel *sungByLabel = [self makeBodyLabel];
            sungByLabel.text = video.sungBy;
            [grid addSubview:sungByLabel row:1 column:3];
        }
        
        if (video.sungKey) {
            [grid addSubview:[self makeHeader:@"Key"] row:2 column:1];
            UILabel *keyLabel = [self makeBodyLabel];
            keyLabel.text = video.sungKey;
            [grid addSubview:keyLabel row:2 column:3];
        }
        
        [grid addSubview:[self makeHeader:@"Posted"] row:3 column:1];
        UILabel *postedLabel = [self makeBodyLabel];
        NSDateFormatter *otherDateFormatter = [[NSDateFormatter alloc] init];
        otherDateFormatter.dateFormat = @"EEEE, LLLL d, yyyy";
        postedLabel.text = [otherDateFormatter stringFromDate:video.posted];
        [grid addSubview:postedLabel row:3 column:3];
        
        DPGridLayout *multitrackGrid = [[DPGridLayout alloc] init];
        multitrackGrid.columnDimensions = @[
                                            [DPGridDimension dimension],
                                            [DPGridDimension dimensionWithSize:8],
                                            [DPGridDimension dimension]
                                            ];
        UILabel *multitrackLabel = [self makeBodyLabel];
        multitrackLabel.text = @"Multitrack";
        [multitrackGrid addSubview:multitrackLabel row:0 column:2];
        UIImageView *multitrackImage = [[UIImageView alloc] initWithImage:video.isMultitrack ? [DPTagVideoController onImage] : [DPTagVideoController offImage]];
        [multitrackGrid addSubview:multitrackImage row:0 column:0];
        
        [grid addSubview:[multitrackGrid centeredHorizontally] row:4 column:1 rowSpan:1 colSpan:3];
    }
    
    NSString *thumbnailKey = [DPFileCache keyForURL:thumbnail];
    if ([[NSFileManager defaultManager] fileExistsAtPath:[DPFileCache pathForKey:thumbnailKey]]) {
        thumb.image = [UIImage imageWithData:[DPFileCache readDataForKey:thumbnailKey]];
    } else {
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            NSData *data = [NSData dataWithContentsOfURL:thumbnail];
            [DPFileCache writeData:data forKey:thumbnailKey];
            dispatch_async(dispatch_get_main_queue(), ^{
                thumb.image = [UIImage imageWithData:data];
            });
        });
    }
    
    UITableViewCell *cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:nil];
    cell.backgroundColor = [UIColor clearColor];
    [cell.contentView addSubview:grid];
    [cell.contentView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[grid]|"
                                                                             options:0
                                                                             metrics:nil
                                                                               views:NSDictionaryOfVariableBindings(grid)]];
    [cell.contentView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[grid]|"
                                                                             options:0
                                                                             metrics:nil
                                                                               views:NSDictionaryOfVariableBindings(grid)]];

    return cell;
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
    return 68;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    NSString *youTubeCode = nil;
    if (self.tag.teachingVideo && indexPath.section == 0) {
        youTubeCode = self.tag.teachingVideo;
    } else {
        DPVideo *video = self.tag.videos[indexPath.row];
        youTubeCode = video.youTubeCode;
    }
    NSURL *youTubeURL = [NSURL URLWithString:[NSString stringWithFormat:@"http://www.youtube.com/watch?v=%@", youTubeCode]];
    [[UIApplication sharedApplication] openURL:youTubeURL];
}

+ (UIImage *)onImage {
    static UIImage *image;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        image = [UIImage imageNamed:@"ic_check_yes.png"];
    });
    return image;
}

+ (UIImage *)offImage {
    static UIImage *image;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        image = [UIImage imageNamed:@"ic_check_no.png"];
    });
    return image;
}

@end
