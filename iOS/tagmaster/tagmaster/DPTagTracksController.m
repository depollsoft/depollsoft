//
//  DPTagTracksController.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTagTracksController.h"
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"
#import <MediaPlayer/MediaPlayer.h>

@interface DPTagTracksController () <UITableViewDataSource, UITableViewDelegate>

@property (nonatomic, strong) UILabel *apology;
@property (nonatomic, strong) UILabel *recordingNotesHeader;
@property (nonatomic, strong) UILabel *recordingNotesLabel;
@property (nonatomic, strong) UITableView *partsTable;
@property (nonatomic, strong) DPGridLayout *grid;

@end

@implementation DPTagTracksController

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
	
    self.apology = [self makeBodyLabel];
    self.apology.text = @"Sorry, no tracks could be found for this tag.";
    self.apology.numberOfLines = 0;
    self.recordingNotesHeader = [self makeHeader:@"Recording Notes"];
    self.recordingNotesLabel = [self makeBodyLabel];
    self.recordingNotesLabel.numberOfLines = 0;
    
    self.partsTable = [[UITableView alloc] initWithFrame:CGRectNull style:UITableViewStyleGrouped];
    self.partsTable.delegate = self;
    self.partsTable.dataSource = self;
    self.partsTable.backgroundColor = [UIColor clearColor];
    
    self.grid = [[DPGridLayout alloc] init];
    self.grid.translatesAutoresizingMaskIntoConstraints = NO;
    self.grid.rowDimensions = @[
                                [DPGridDimension dimension],
                                [DPGridDimension dimension],
                                [DPGridDimension dimensionWithStars:1]
                                ];
    self.grid.columnDimensions = @[
                                   [DPGridDimension dimension],
                                   [DPGridDimension dimensionWithSize:8],
                                   [DPGridDimension dimensionWithStars:1]
                                   ];
    
    [self.grid addSubview:[self.apology padLeft:4 top:0 right:0 bottom:0] row:0 column:0 rowSpan:1 colSpan:3];
    [self.grid addSubview:[self.recordingNotesHeader padLeft:4 top:0 right:0 bottom:0] row:1 column:0];
    [self.grid addSubview:self.recordingNotesLabel row:1 column:2];
    [self.grid addSubview:self.partsTable row:2 column:0 rowSpan:1 colSpan:3];
    
    [self.view addSubview:self.grid];
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[_grid]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(_grid)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|-4-[_grid]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(_grid)]];
    [self refreshView];
}

- (void)refreshView {
    [self.grid setView:self.apology hidden:self.tag.tracks.count > 0];
    
    self.recordingNotesLabel.text = self.tag.recordingMethod;
    [self.grid setView:self.recordingNotesLabel hidden:!self.tag.recordingMethod];
    [self.grid setView:self.recordingNotesHeader hidden:!self.tag.recordingMethod];
    
    [self.partsTable reloadData];
    [self.grid setView:self.partsTable hidden:self.tag.tracks.count == 0];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    UITableViewCell *cell = [[UITableViewCell alloc] init];
    cell.textLabel.text = [self.tag.tracks[indexPath.row] title];
    cell.backgroundColor = [UIColor clearColor];
    return cell;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return self.tag.tracks.count;
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section {
    return @"Tracks";
}

@end
