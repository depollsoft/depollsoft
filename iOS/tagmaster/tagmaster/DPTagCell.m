//
//  DPTagCell.m
//  tagmaster
//
//  Created by David Poll on 8/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPTagCell.h"
#import "DPBusyIndicator.h"

@interface DPTagCell ()

@property (nonatomic, strong) UILabel *title;
@property (nonatomic, strong) UILabel *aka;
@property (nonatomic, strong) UILabel *details;
@property (nonatomic, strong) UIImageView *hasSheetMusic;
@property (nonatomic, strong) UIImageView *hasLearningTracks;
@property (nonatomic, strong) DPBusyIndicator *busyIndicator;

@end

@implementation DPTagCell

@synthesize tagInstance, title, aka, details, hasLearningTracks, hasSheetMusic, rootView, busyIndicator;

- (id)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier
{
    self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];
    if (self) {
        self.backgroundColor = [UIColor clearColor];
        self.rootView = [[UIView alloc] init];
        self.rootView.translatesAutoresizingMaskIntoConstraints = NO;
        
        self.busyIndicator = [[DPBusyIndicator alloc] init];
        self.busyIndicator.child = self.rootView;
        self.busyIndicator.translatesAutoresizingMaskIntoConstraints = NO;
        
        [self.contentView addSubview:self.busyIndicator];
        [self.contentView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[busyIndicator]|"
                                                                                 options:0
                                                                                 metrics:nil
                                                                                   views:NSDictionaryOfVariableBindings(busyIndicator)]];
        [self.contentView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[busyIndicator]|"
                                                                                 options:0
                                                                                 metrics:nil
                                                                                   views:NSDictionaryOfVariableBindings(busyIndicator)]];
        
        UILabel *hasSheetMusicLabel = [[UILabel alloc] init];
        hasSheetMusicLabel.translatesAutoresizingMaskIntoConstraints = NO;
        hasSheetMusicLabel.text = @"Sheet Music";
        hasSheetMusicLabel.font = [hasSheetMusicLabel.font fontWithSize:10];
        UILabel *hasLearningTracksLabel = [[UILabel alloc] init];
        hasLearningTracksLabel.translatesAutoresizingMaskIntoConstraints = NO;
        hasLearningTracksLabel.text = @"Learning Tracks";
        hasLearningTracksLabel.font = [hasLearningTracksLabel.font fontWithSize:10];
        self.title = [[UILabel alloc] init];
        self.title.translatesAutoresizingMaskIntoConstraints = NO;
        self.title.font = [self.title.font fontWithSize:15];
        self.aka = [[UILabel alloc] init];
        self.aka.translatesAutoresizingMaskIntoConstraints = NO;
        self.aka.font = [self.aka.font fontWithSize:12];
        self.details = [[UILabel alloc] init];
        self.details.translatesAutoresizingMaskIntoConstraints = NO;
        self.details.font = [self.aka.font fontWithSize:12];
        self.hasSheetMusic = [[UIImageView alloc] initWithImage:[DPTagCell offImage]];
        self.hasSheetMusic.translatesAutoresizingMaskIntoConstraints = NO;
        self.hasLearningTracks = [[UIImageView alloc] initWithImage:[DPTagCell offImage]];
        self.hasLearningTracks.translatesAutoresizingMaskIntoConstraints = NO;
        
        [self.rootView addSubview:self.title];
        [self.rootView addSubview:self.aka];
        [self.rootView addSubview:self.details];
        [self.rootView addSubview:self.hasSheetMusic];
        [self.rootView addSubview:self.hasLearningTracks];
        [self.rootView addSubview:hasLearningTracksLabel];
        [self.rootView addSubview:hasSheetMusicLabel];
        
        NSDictionary *bindings = NSDictionaryOfVariableBindings(title, aka, details, hasSheetMusic, hasLearningTracks, hasLearningTracksLabel, hasSheetMusicLabel);
        
        [self.rootView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|-15-[title][aka][details][hasSheetMusic]-15-|"
                                                                                 options:0
                                                                                 metrics:nil
                                                                                   views:bindings]];
        [self.rootView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|-4-[title]|"
                                                                                 options:0
                                                                                 metrics:nil
                                                                                   views:bindings]];
        [self.rootView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|-[aka]|"
                                                                                 options:0
                                                                                 metrics:nil
                                                                                   views:bindings]];
        [self.rootView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|-[details]|"
                                                                                 options:0
                                                                                 metrics:nil
                                                                                   views:bindings]];
        [self.rootView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|-[hasSheetMusic]-[hasSheetMusicLabel]->=40-[hasLearningTracks]-[hasLearningTracksLabel]->=0-|"
                                                                                 options:NSLayoutFormatAlignAllCenterY
                                                                                 metrics:nil
                                                                                   views:bindings]];
    }
    return self;
}

- (void)setSelected:(BOOL)selected animated:(BOOL)animated
{
    [super setSelected:selected animated:animated];

    // Configure the view for the selected state
}

- (void)setTagId:(int)tId {
    _tagId = tId;
    [self loadTag:NO];
}

- (void)loadTag:(BOOL)refresh {
    int tagId = self.tagId;
    if (!refresh) {
        DPTag *t = [DPTag loadFromCache:tagId];
        if (t) {
            self.tagInstance = t;
            return;
        }
    }
    self.tagInstance = nil;
    [self.busyIndicator incrementBusyCount];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        @try {
            DPTag *t = [DPTag loadTagById:tagId refresh:refresh];
            dispatch_async(dispatch_get_main_queue(), ^{
                if (t.tagId == self.tagId) {
                    self.tagInstance = t;
                    UITableView *tableView = (UITableView *)self.superview;
                    while (tableView && ![tableView isKindOfClass:[UITableView class]]) {
                        tableView = (UITableView *)tableView.superview;
                    }
                    NSIndexPath *indexPath = [tableView indexPathForCell:self];
                    if (indexPath) {
                        [tableView reloadRowsAtIndexPaths:@[indexPath]
                                         withRowAnimation:UITableViewRowAnimationAutomatic];
                    }
                }
                [self.busyIndicator decrementBusyCount];
            });
        }
        @catch (NSException *exception) {
            [self.busyIndicator decrementBusyCount];
        }
    });
}

+ (NSDateFormatter *)dateFormatter {
    static NSDateFormatter *formatter;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        formatter = [[NSDateFormatter alloc] init];
        formatter.dateFormat = @"MM/dd/yy";
    });
    return formatter;
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

- (void)setTagInstance:(DPTag *)newTag {
    NSDateFormatter *formatter = [DPTagCell dateFormatter];
    tagInstance = newTag;
    self.title.text = self.tagInstance.title ?: @"Tag";
    self.aka.text = self.tagInstance.alternativeTitle ? [@"a.k.a. " stringByAppendingString:self.tagInstance.alternativeTitle] : nil;
    NSString *detailsString = nil;
    if (tagInstance) {
        detailsString = [@"Posted: " stringByAppendingString:[formatter stringFromDate:tagInstance.posted]];
        if (tagInstance.rating != 0) {
            detailsString = [[NSString stringWithFormat:@"Rating: %1.2f ", tagInstance.rating] stringByAppendingString:detailsString];
        }
        if (tagInstance.downloadCount != 0) {
            detailsString = [detailsString stringByAppendingFormat:@" DLs: %d", tagInstance.downloadCount];
        }
    }
    self.details.text = detailsString ?: @"Posted: Rating: DLs:";
    self.hasLearningTracks.image = self.tagInstance.tracks.count > 0 ? [DPTagCell onImage] : [DPTagCell offImage];
    self.hasSheetMusic.image = self.tagInstance.sheetMusicUri ? [DPTagCell onImage] : [DPTagCell offImage];
}

- (CGFloat)calculatedHeight {
    if (tagInstance.alternativeTitle) {
        return [DPTagCell withAkaHeight];
    } else {
        return [DPTagCell withoutAkaHeight];
    }
}

+ (CGFloat)withAkaHeight {
    static CGFloat height;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        DPTag *tag = [[DPTag alloc] init];
        tag.title = @"A";
        tag.alternativeTitle = @"A";
        tag.posted = [NSDate date];
        DPTagCell *cell = [[DPTagCell alloc] init];
        cell.tagInstance = tag;
        height = [cell.rootView systemLayoutSizeFittingSize:CGSizeZero].height;
    });
    return height;
}

+ (CGFloat)withoutAkaHeight {
    static CGFloat height;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        DPTag *tag = [[DPTag alloc] init];
        tag.title = @"A";
        tag.posted = [NSDate date];
        DPTagCell *cell = [[DPTagCell alloc] init];
        cell.tagInstance = tag;
        height = [cell.rootView systemLayoutSizeFittingSize:CGSizeZero].height;
    });
    return height;
}

+ (CGFloat)tagHeight:(DPTag *)tag {
    if (tag.alternativeTitle) {
        return [self withAkaHeight];
    } else {
        return [self withoutAkaHeight];
    }
}

@end
