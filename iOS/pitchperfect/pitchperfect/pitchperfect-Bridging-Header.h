//
//  Objective-C models and iOS lifecycle exposed to the SwiftUI application.
//

#include <TargetConditionals.h>
#if TARGET_OS_IPHONE
#import "DPAppDelegate.h"
#import "DPBannerAdView.h"
#endif
#import "../pitchperfectlib/pitchperfectlib/DPAccidental.h"
#import "../pitchperfectlib/pitchperfectlib/DPKey.h"
#import "../pitchperfectlib/pitchperfectlib/DPKeyType.h"
#import "../pitchperfectlib/pitchperfectlib/DPNote.h"
#import "DPPitchPipeModel.h"
#import "../pitchperfectlib/pitchperfectlib/DPPitchedSong.h"
#import "../../depolllib/depolllib/json/DPJsonSerializer.h"
