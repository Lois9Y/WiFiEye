# WiFiEye

A basic proof of concept: Android application using OCR for WiFi password scanning.

## Motivation

Small Project to familiarize myself with a few new concepts:

 * Anyline OCR SDK - <https://documentation.anyline.io/>
 * Android WifiManager - <https://developer.android.com/reference/android/net/wifi/WifiManager.html>
 * Espresso Instrumentation Tests - <https://developer.android.com/training/testing/ui-testing/espresso-testing.html>

## Installation

import the project to your IDE of choice - build - try it out.
Requires a device with a back-camera and WiFi capabilities.

## Anyline OCR SDK

API for Optical Character Recognition on mobile devices. Free to use for non-commercial purpose. 

#### Installation

In case you want to use the API in your own application simply follow the Quick Start Guide here: <https://documentation.anyline.io/#quick-start-guide> .

To get started, I relied heavily on the IBAN Example (provided here:<https://documentation.anyline.io/#anylineocr-examples>), albeit passwords are less restrictive in their format/whitelisted characters, meaning for passwords, anything goes really. 

#### config.json

The following Configuration.json worked out well for scanning passwords (for details see <https://documentation.anyline.io/#configuration>): 
[password_view_config.json](app/src/main/assets/password_view_config.json)

#### Training Data 

The Example <https://www.anyline.io/download/> comes ith a german and english set of training data. 
However, since the target application is not bound to any particular language, an additional set of training data has been generated (based on font Calibri via <http://ocr7.com/>)

#### License Key 

You can get your license key here: https://www.anyline.io/pricing/

#### Potential Development

There is plenty of potential for further optimization regarding the OCR settings. 
As this is a simple Proof of concept, the application goes for fast recognition in favor of high confidence, subsequently allowing the user to correct any mistakes in the scanned password.

## WifiManager

As per API Level 23
>Your apps can now change the state of WifiConfiguration objects only if you created these objects. You are not permitted to modify or delete WifiConfiguration objects created by the user or by other apps.

This seems to generate some issues when the application attempts to  in the case that a WiFi Network, already known to the device.

Changes in WifiConfigurations are sometimes discarded by the WifiManager, although the issue couldn't be generated consistently. Further investigation required. 
Check out the Debug-Log tagged "WifiReciever" for particulars of the situation. 

#### Potential Development

Deep Dive into WifiManager investigating the "verbose" error code: -1, on configuration changes.  

## Tests

As of right now, UI testing with Espresso, is being done for MainActivity Screen and related Intents.

#### Potential Development

Adding a Mocking Framework to mock out the WiFiManager, allowing for automated tests across the entire application 

## Next Steps

As this is a proof of concept, no encryption has been employed for storing scanned passwords, all persistence operations are employed in memory only. Discarding any scanned passwords on application end. 
 
 * Add Encryption/Key Store for previous scans, allowing for persistent user expierience. 

## Dependencies / Sources

 * Realm Database used. See <https://github.com/realm/realm-java>
 * SwipeRefreshListFragment: See `com.example.android.swiperefreshlistfragment`
 * SlidingTabLayout & SlidingTabStrip: See `com.google.samples.apps.iosched.ui.widget`

## License

A short snippet describing the license (MIT, Apache, etc.)