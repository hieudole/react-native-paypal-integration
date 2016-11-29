'use strict'

const { NativeModules } = require('react-native');
const { PayPal } = NativeModules;

module.exports = {
  ...PayPal,
  profileSharing: function profileSharing(payPalConfig, callback) {
    PayPal.profileSharing(payPalConfig, callback);
  }
}
