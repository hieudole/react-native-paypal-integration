# react-native-paypal-integration

## Installation

``` bash
npm install --save react-native-paypal-integration
```
## Android

1. Edit `android/app/build.gradle`:

```diff
dependencies {
    ...
    compile "com.facebook.react:react-native:+"  // From node_modules
+   compile project(':react-native-paypal-integration')
}
```

2. Edit `android/settings.gradle`:

```diff
...
include ':app'
+ include ':react-native-paypal-integration'
+ project(':react-native-paypal-integration').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-paypal-integration/android')
```

3. Edit `MainApplication.java`:

```diff
+ import com.hieudole.paypal.PayPalPackage;

  public class MainApplication extends Application implements ReactApplication {
    //......
    
    @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
+         new PayPalPackage(),
          new MainReactPackage()
      );
    }
    
    ......
  }
```

## Usage

### Import 

```javascript
import PayPal from 'react-native-paypal-integration';
```
### Profile Sharing

```javascript
PayPal.profileSharing({
    clientId: 'Your client ID',
    environment: PayPal.SANDBOX,
    merchantName: 'Your merchant name',
    merchantPrivacyPolicyUri: 'http://your-url.com/policy',
    merchantUserAgreementUri: 'http://your-url.com/legal',
    scopes: [
        PayPal.SCOPE_PROFILE, // Full Name, Birth Date, Time Zone, Locale, Language
        PayPal.SCOPE_PAYPAL_ATTRIBUTES, // Age Range, Account Status, Account Type, Account Creation Date
        PayPal.SCOPE_EMAIL, // Email
        PayPal.SCOPE_ADDRESS, // Address
        PayPal.SCOPE_PHONE // Telephone
    ]
  }, 
  function (r) {
    console.log(r);
  }
);
```
