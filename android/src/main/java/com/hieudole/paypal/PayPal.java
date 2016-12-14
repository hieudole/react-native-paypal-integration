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
import com.facebook.react.bridge.ReadableArray;
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

  private static final String SCOPE_FUTURE_PAYMENTS = "SCOPE_FUTURE_PAYMENTS";
  private static final String SCOPE_PROFILE = "SCOPE_PROFILE";
  private static final String SCOPE_PAYPAL_ATTRIBUTES = "SCOPE_PAYPAL_ATTRIBUTES";
  private static final String SCOPE_EMAIL = "SCOPE_EMAIL";
  private static final String SCOPE_ADDRESS = "SCOPE_ADDRESS";
  private static final String SCOPE_PHONE = "SCOPE_PHONE";
  private static final String SCOPE_OPENID = "SCOPE_OPENID";

  private Callback successCallback;

  private Callback errorCallback;
  
  private Callback callback;

  public PayPal(ReactApplicationContext reactContext) {
    super(reactContext);

    reactContext.addActivityEventListener(this);
  }

  private PayPalOAuthScopes getOauthScopes(final ReadableMap config) {
    Set<String> scopes = new HashSet<String>();

    ReadableArray readableArray = config.getArray("scopes");
    for (int index = 0; index < readableArray.size(); index++) {
      String scope = readableArray.getString(index);
      Log.i("PayPalModule", scope);
      if (!scopes.contains(scope)) {
        scopes.add(scope);
      }
    }
    
    return new PayPalOAuthScopes(scopes);
  }
  
  private PayPalConfiguration createPayPalConfiguration(final ReadableMap config) {
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

   private void startPayPalService(PayPalConfiguration config, Activity currentActivity) {
    if (currentActivity == null) {
      return;
    }
    
    Intent intent = new Intent(currentActivity, PayPalService.class);
    intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
    currentActivity.startService(intent);
  }

  private void stopPayPalService(Activity currentActivity) {
    if (currentActivity == null) {
      return;
    }

    currentActivity.stopService(new Intent(currentActivity, PayPalService.class));
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

    constants.put(SCOPE_FUTURE_PAYMENTS, PayPalOAuthScopes.PAYPAL_SCOPE_FUTURE_PAYMENTS);
    constants.put(SCOPE_PROFILE, PayPalOAuthScopes.PAYPAL_SCOPE_PROFILE);
    constants.put(SCOPE_PAYPAL_ATTRIBUTES, PayPalOAuthScopes.PAYPAL_SCOPE_PAYPAL_ATTRIBUTES);
    constants.put(SCOPE_EMAIL, PayPalOAuthScopes.PAYPAL_SCOPE_EMAIL);
    constants.put(SCOPE_ADDRESS, PayPalOAuthScopes.PAYPAL_SCOPE_ADDRESS);
    constants.put(SCOPE_ADDRESS, PayPalOAuthScopes.PAYPAL_SCOPE_ADDRESS);
    constants.put(SCOPE_PHONE, PayPalOAuthScopes.PAYPAL_SCOPE_PHONE);
    constants.put(SCOPE_OPENID, PayPalOAuthScopes.PAYPAL_SCOPE_OPENID);
    
    return constants;
  }

  @ReactMethod
  public void profileSharing(final ReadableMap payPalConfig, Callback callback)  {
    Activity currentActivity = this.getCurrentActivity();
    if (currentActivity == null) {
      return;
    }

    this.callback = callback;

    PayPalConfiguration config = this.createPayPalConfiguration(payPalConfig);
  
    this.startPayPalService(config, currentActivity);
  
    Intent intent = new Intent(currentActivity, PayPalProfileSharingActivity.class);
    intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
    intent.putExtra(PayPalProfileSharingActivity.EXTRA_REQUESTED_SCOPES, this.getOauthScopes(payPalConfig));
    
    currentActivity.startActivityForResult(intent, REQUEST_CODE_PROFILE_SHARING);
  }
  
  public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    Activity activity = this.getCurrentActivity();
    this.processOnActivityResult(activity, requestCode, resultCode, data);
  }

  public void onActivityResult(Activity activity, final int requestCode, final int resultCode, final Intent data) {
    this.processOnActivityResult(activity, requestCode, resultCode, data);
  }

  private void processOnActivityResult(Activity activity, final int requestCode, final int resultCode, final Intent data) {
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

    this.stopPayPalService(activity);
  }

  public void onNewIntent(Intent intent) { }
}
