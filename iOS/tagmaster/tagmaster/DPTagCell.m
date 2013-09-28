//
//  DPTagCell.m
//  tagmaster
//
//  Created by David Poll on 8/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPTagCell.h"

@interface DPTagCell ()

@property (nonatomic, strong) UILabel *title;
@property (nonatomic, strong) UILabel *aka;
@property (nonatomic, strong) UILabel *details;
@property (nonatomic, strong) UISwitch *hasSheetMusic;
@property (nonatomic, strong) UISwitch *hasLearningTracks;

@end

@implementation DPTagCell

@synthesize tag, title, aka, details, hasLearningTracks, hasSheetMusic, rootView;

- (id)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier
{
    self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];
    if (self) {
        self.backgroundColor = [UIColor clearColor];
        self.rootView = [[UIView alloc] init];
        self.rootView.translatesAutoresizingMaskIntoConstraints = NO;
        [self.contentView addSubview:self.rootView];
        [self.contentView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[rootView]|"
                                                                                 options:0
                                                                                 metrics:nil
                                                                                   views:NSDictionaryOfVariableBindings(rootView)]];
        [self.contentView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[rootView]|"
                                                                                 options:0
                                                                                 metrics:nil
                                                                                   views:NSDictionaryOfVariableBindings(rootView)]];
        
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
        self.hasSheetMusic = [[UISwitch alloc] init];
        self.hasSheetMusic.translatesAutoresizingMaskIntoConstraints = NO;
        self.hasSheetMusic.transform = CGAffineTransformMakeScale(0.5, 0.5);
        [self.hasSheetMusic setEnabled:NO];
        self.hasLearningTracks = [[UISwitch alloc] init];
        self.hasLearningTracks.translatesAutoresizingMaskIntoConstraints = NO;
        self.hasLearningTracks.transform = CGAffineTransformMakeScale(0.5, 0.5);
        [self.hasLearningTracks setEnabled:NO];
        
        [self.rootView addSubview:self.title];
        [self.rootView addSubview:self.aka];
        [self.rootView addSubview:self.details];
        [self.rootView addSubview:self.hasSheetMusic];
        [self.rootView addSubview:self.hasLearningTracks];
        [self.rootView addSubview:hasLearningTracksLabel];
        [self.rootView addSubview:hasSheetMusicLabel];
        
        NSDictionary *bindings = NSDictionaryOfVariableBindings(title, aka, details, hasSheetMusic, hasLearningTracks, hasLearningTracksLabel, hasSheetMusicLabel);
        
        [self.rootView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|->=15-[title][aka][details][hasSheetMusic]->=15-|"
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
        [self.rootView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|-[hasSheetMusicLabel]-[hasSheetMusic]-[hasLearningTracksLabel]-[hasLearningTracks]->=0-|"
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

- (void)setTag:(DPTag *)newTag {
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    formatter.dateFormat = @"MM/dd/yy";
    tag = newTag;
    self.title.text = tag.title;
    self.aka.text = tag.alternativeTitle ? [@"a.k.a. " stringByAppendingString:tag.alternativeTitle] : nil;
    self.details.text = [NSString stringWithFormat:@"Rating: %1.2f  Posted: %@  DLs: %d", tag.rating, [formatter stringFromDate:tag.posted], tag.downloadCount];
    [self.hasLearningTracks setOn:tag.tracks.count > 0];
    [self.hasSheetMusic setOn:tag.sheetMusicUri];
}

- (CGFloat)calculatedHeight {
    if (tag.alternativeTitle) {
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
        DPTagCell *cell = [[DPTagCell alloc] init];
        cell.tag = tag;
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
        DPTagCell *cell = [[DPTagCell alloc] init];
        cell.tag = tag;
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
