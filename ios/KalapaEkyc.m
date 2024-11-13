#import "KalapaEkyc.h"
#import <UIKit/UIKit.h>
#import <KalapaSDK/KalapaSDK.h>

@implementation KalapaEkyc

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(start:(NSString *)session
                  flow:(NSString *)flow
                  data:(NSDictionary*)data
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        if (@available(iOS 13, *)) {
            NSString *domain = data[@"domain"] ?: @"https://ekyc-api.kalapa.vn";
            NSString *language = data[@"language"] ?: @"vi";
            
            NSString *mainColor = data[@"main_color"] ?: @"3270EA";
            NSString *backgroundColor = data[@"background_color"] ?: @"ffffff";
            NSString *mainTextColor = data[@"main_text_color"] ?: @"000000";
            NSString *btnTextColor = data[@"btn_text_color"] ?: @"ffffff";
            NSInteger livenessVersion = [data[@"liveness_version"] integerValue] ?: 0;
            
            NSString *faceData = data[@"face_data"];
            NSString *mrz = data[@"mrz"];
            NSString *sessionID = data[@"session_id"];
            
            KLPAppearance *klpAppearance = [[[[[[KLPAppearance Builder]
                                                withLanguage:language]
                                               withMainColor:mainColor]
                                              withBackgroundColor:backgroundColor]
                                             withMainTextColor:mainTextColor]
                                            withBtnTextColor:btnTextColor];
            
            KLPConfig *klpConfig = [self configureKLPWithSession:session
                                                          domain:domain
                                                 livenessVersion:livenessVersion
                                                      appearance:klpAppearance
                                                             mrz:mrz
                                                        faceData:faceData];
            
            if (sessionID != nil) {
                [klpConfig withSession:sessionID];
            }
            
            [klpConfig withExpiredHandler:^{
                reject(@"EXPIRED", @"Session expired", nil);
            }];
            
            [klpConfig withCancelSessionHandler:^{
                reject(@"CANCELED", @"Session was canceled by the user", nil);
            }];
            
            [klpConfig withResultHandler:^(KalapaResult * _Nullable result) {
                NSDictionary *jsonResult = [self handleResult:result];
                resolve(jsonResult);
            }];
            
            [klpConfig buildWithCompletionHandler:^(KLPConfig * _Nullable config, NSError * _Nullable error) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    if (error == nil) {
                        [[Kalapa shared] runWithFlow:flow withConfig:config];
                    } else {
                        reject(@"CONFIG_ERROR", @"Failed to build configuration", error);
                    }
                });
            }];
            
        } else {
            reject(@"UNSUPPORTED", @"Kalapa doesn't support iOS less than 11", nil);
        }
    });
}

#pragma mark - Helper Methods

- (KLPConfig *)configureKLPWithSession:(NSString *)session
                                 domain:(NSString *)domain
                        livenessVersion:(NSInteger)livenessVersion
                             appearance:(KLPAppearance *)appearance
                                    mrz:(NSString *)mrz
                               faceData:(NSString *)faceData {
  KLPConfig *config = [[[[[[KLPConfig BuilderWithSession:session]
                           withBaseUrl:domain]
                          withLivenessVersion:livenessVersion]
                         withAppearance:appearance]
                        withMRZ:mrz]
                       withFaceDataBase64:faceData];
  return config;
}

- (NSDictionary *)handleResult:(KalapaResult *)result {
  NSDictionary<NSString *, id> *resultJson = [result toDictionary];
  NSDictionary<NSString *, id> *rawJson = [result rawJson];
  NSDictionary *fields = rawJson[@"ocr_data"][@"data"][@"fields"];
  
  NSMutableDictionary *json = [NSMutableDictionary dictionaryWithDictionary:fields];
  json[@"session"] = rawJson[@"session"] ?: @"";
  json[@"decision"] = resultJson[@"decision"] ?: @"";
  json[@"nfc_data"] = rawJson[@"nfc_data"];
  
  NSDictionary *selfieData = rawJson[@"selfie_data"][@"data"];
  if (selfieData && selfieData != (id)[NSNull null]) {
    json[@"selfie_data"] = @{
      @"is_matched": selfieData[@"is_matched"],
      @"matching_score": selfieData[@"matching_score"]
    };
  }
  
  json[@"mrz_data"] = rawJson[@"ocr_data"][@"data"][@"mrz_data"];
  
  NSDictionary *qrCodeData = rawJson[@"ocr_data"][@"data"][@"qr_code"];
  if (qrCodeData && qrCodeData != (id)[NSNull null] && qrCodeData[@"data"]) {
      json[@"qr_code"] = @{@"decoded_text": qrCodeData[@"data"][@"decoded_text"] ?: @""};
  }
  
  if (resultJson[@"decision_detail"] && (resultJson[@"decision_detail"] != [NSNull null])) {
    json[@"decision_detail"] = resultJson[@"decision_detail"];
  }
  json[@"resident_entities"] = fields[@"resident_entities"];
  json[@"home_entities"] = fields[@"home_entities"];
  
  NSError *error;
  NSData *jsonData = [NSJSONSerialization dataWithJSONObject:json options:NSJSONWritingPrettyPrinted error:&error];
  NSString *jsonString = jsonData ? [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding] : @"";
  
  return @{@"kalapa_result": jsonString};
}


@end
