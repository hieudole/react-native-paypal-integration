package com.hieudole.paypal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import com.paypal.android.sdk.payments.PayPalAuthorization;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;
import com.paypal.android.sdk.payments.PayPalItem;
import com.paypal.android.sdk.payments.PayPalOAuthScopes;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalPaymentDetails;
import com.paypal.android.sdk.payments.PayPalProfileSharingActivity;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;
import com.paypal.android.sdk.payments.ShippingAddress;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import java.math.BigDecimal;

import org.json.JSONException;

import android.net.Uri;

public class PayPal extends ReactContextBaseJavaModule implements ActivityEventListener {
  
  private static final int REQUEST_CODE_PAYMENT = 1;
  private static final int REQUEST_CODE_FUTURE_PAYMENT = 2;
  private static final int REQUEST_CODE_PROFILE_SHARING = 3;
  
  private static final String ENVIRONMENT_NO_NETWORK = "NO_NETWORK";
  private static final String ENVIRONMENT_SANDBOX = "SANDBOX";
  private static final String ENVIRONMENT_PRODUCTION = "PRODUCTION";

  private Callback successCallback;

  private Callback errorCallback;
  
  private Callback callback;

  public PayPal(ReactApplicationContext reactContext) {
    super(reactContext);

    reactContext.addActivityEventListener(this);
  }

  @Override
  public String getName() {
    return "PayPal";
  }

  @Override public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();

    constants.put(ENVIRONMENT_NO_NETWORK, PayPalConfiguration.ENVIRONMENT_NO_NETWORK);
    constants.put(ENVIRONMENT_SANDBOX, PayPalConfiguration.ENVIRONMENT_SANDBOX);
    constants.put(ENVIRONMENT_PRODUCTION, PayPalConfiguration.ENVIRONMENT_PRODUCTION);

    return constants;
  }

  private PayPalOAuthScopes getOauthScopes() {
        Set<String> scopes = new HashSet<String>(Arrays.asList(PayPalOAuthScopes.PAYPAL_SCOPE_EMAIL, PayPalOAuthScopes.PAYPAL_SCOPE_ADDRESS));
        return new PayPalOAuthScopes(scopes);
    }
  
  private PayPalConfiguration CreatePayPalConfiguration(final ReadableMap config) {
    final String environment = config.getString("environment");
    final String clientId = config.getString("clientId");
    final String merchantName = config.getString("merchantName");
    final String merchantPrivacyPolicyUri = config.getString("merchantPrivacyPolicyUri");
    final String merchantUserAgreementUri = config.getString("merchantUserAgreementUri");

     return new PayPalConfiguration()
      .environment(environment)
      .clientId(clientId)
      .merchantName(merchantName)
      .merchantPrivacyPolicyUri(Uri.parse(merchantPrivacyPolicyUri))
      .merchantUserAgreementUri(Uri.parse(merchantUserAgreementUri));
  }

  @ReactMethod
  public void profileSharing(final ReadableMap payPalConfig, Callback callback)  {
    Activity currentActivity = this.getCurrentActivity();
    if (currentActivity == null) {
      return;
    }

    this.callback = callback;

    PayPalConfiguration config = this.CreatePayPalConfiguration(payPalConfig);
   
    this.startPayPalService(config, currentActivity);
  
    Intent intent = new Intent(currentActivity, PayPalProfileSharingActivity.class);
    intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
    intent.putExtra(PayPalProfileSharingActivity.EXTRA_REQUESTED_SCOPES, getOauthScopes());
    
    currentActivity.startActivityForResult(intent, REQUEST_CODE_PROFILE_SHARING);
  }

  private void startPayPalService(PayPalConfiguration config, Activity currentActivity) {
    if (currentActivity == null) {
      return;
    }
    
    Intent intent = new Intent(currentActivity, PayPalService.class);
    intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
    currentActivity.startService(intent);
  }

  private void stopPayPalService() {
    Activity currentActivity = this.getCurrentActivity();
    if (currentActivity == null) {
      return;
    }

    currentActivity.stopService(new Intent(currentActivity, PayPalService.class));
  }
  
  public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    Log.i("PayPalModule", "onActivityResult: requestCode = " + requestCode);

    if (requestCode != REQUEST_CODE_PAYMENT && 
        requestCode != REQUEST_CODE_FUTURE_PAYMENT &&
        requestCode != REQUEST_CODE_PROFILE_SHARING) {
        
        return;
    }

    if (requestCode == REQUEST_CODE_PROFILE_SHARING) {
      if (resultCode == Activity.RESULT_OK) {
        PayPalAuthorization auth = data.getParcelableExtra(PayPalProfileSharingActivity.EXTRA_RESULT_AUTHORIZATION);
            if (auth != null) {
               if (callback != null) {
                      callback.invoke(auth.toJSONObject().toString());
               }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            if (callback != null) {
              WritableMap response = Arguments.createMap();
              response.putString("cancelled", "The user cancelled");
              callback.invoke(response);
            }
        } else if (resultCode == PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID) {
            if (callback != null) {
              WritableMap response = Arguments.createMap();
              response.putString("error", "Invalid config");
              callback.invoke(response);
            }
        }
    }

    this.stopPayPalService();
  }

  public void onNewIntent(Intent intent) { }
}
