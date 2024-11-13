package com.reactnativekalapaekyc;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import java.util.Objects;

import vn.kalapa.ekyc.KalapaHandler;
import vn.kalapa.ekyc.KalapaSDK;
import vn.kalapa.ekyc.KalapaSDKConfig;
import vn.kalapa.ekyc.KalapaSDKResultCode;
import vn.kalapa.ekyc.models.KalapaResult;

@ReactModule(name = KalapaEkycModule.NAME)
public class KalapaEkycModule extends ReactContextBaseJavaModule {
  public static final String NAME = "KalapaEkyc";

  public KalapaEkycModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }


  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  @ReactMethod
  public void multiply(int a, int b, Promise promise) {
    promise.resolve(a * b);
  }
  

  @ReactMethod
  public void start(String session, String flow, ReadableMap data, Promise promise) {

      String domain = data.getString("domain");
      String mainColor = data.getString("main_color") != null ? data.getString("main_color") : "#1F69E6";
      String background = data.getString("background_color") != null ? data.getString("background_color") : "#FFFFFF";
      String mainTextColor = data.getString("main_text_color") != null ? data.getString("main_text_color") : "#000000";
      String btnTextColor = data.getString("btn_text_color") != null ? data.getString("btn_text_color") : "#FFFFFF";
      String language = data.getString("language") != null ? data.getString("language") : "en";
      int livenessVersion = data.getInt("liveness_version");

      String faceData = data.getString("face_data") != null ? data.getString("face_data") : "";
      String mrzData = data.getString("mrz") != null ? data.getString("mrz") : "";
      String sessionId = data.getString("session_id") != null ? data.getString("session_id") : "";

      Context context = getReactApplicationContext();
      KalapaSDKConfig klpConfig = new KalapaSDKConfig.KalapaSDKConfigBuilder(context)
              .withBaseURL(domain)
              .withMainColor(mainColor)
              .withBackgroundColor(background)
              .withMainTextColor(mainTextColor)
              .withBtnTextColor(btnTextColor)
              .withLanguage(language)
              .withLivenessVersion(livenessVersion).build();

      if (getCurrentActivity() != null) {
          KalapaSDK.KalapaSDKBuilder builder = new KalapaSDK.KalapaSDKBuilder(getCurrentActivity(), klpConfig);
          if (faceData != null && !faceData.isEmpty()) builder.withFaceData(faceData);
          if (mrzData != null && !mrzData.isEmpty()) builder.withMrz(mrzData);
          if (sessionId != null && !sessionId.isEmpty()) builder.withLeftoverSession(sessionId);

          builder.build().start(session, flow, new KalapaHandler() {
              @Override
              public void onExpired() {
                  promise.reject("EXPIRED", "Session expired");
//                    promise.reject("401", "EXPIRED");
              }

              @Override
              public void onComplete(@NonNull KalapaResult kalapaResult) {
                  super.onComplete(kalapaResult);
                  String resultMap = kalapaResult.toJson();
                  WritableMap res = new WritableNativeMap();
                  res.putString("kalapa_result", resultMap);
//                    callback.invoke(res);
                  promise.resolve(res);
              }

              @Override
              public void onError(@NonNull KalapaSDKResultCode kalapaSDKResultCode) {
                  super.onError(kalapaSDKResultCode);
                  switch (kalapaSDKResultCode) {
                      case USER_LEAVE:
                          promise.reject("CANCELED", kalapaSDKResultCode.getEn());
                      case DEVICE_NOT_SUPPORTED:
                          promise.reject("UNSUPPORTED", kalapaSDKResultCode.getEn());
                      case CONFIGURATION_NOT_ACCEPTABLE:
                          promise.reject("CONFIG_ERROR", kalapaSDKResultCode.getEn());
                      default:
                          promise.reject("OTHER", kalapaSDKResultCode.getEn());
                  }
              }
          });
      } else Toast.makeText(getReactApplicationContext(), "Activity is null", Toast.LENGTH_LONG).show();
  }

  public static native int nativeMultiply(int a, int b);
}
