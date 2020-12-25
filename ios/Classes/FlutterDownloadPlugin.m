#import "FlutterDownloadPlugin.h"
#if __has_include(<flutter_download/flutter_download-Swift.h>)
#import <flutter_download/flutter_download-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "flutter_download-Swift.h"
#endif

@implementation FlutterDownloadPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterDownloadPlugin registerWithRegistrar:registrar];
}
@end
