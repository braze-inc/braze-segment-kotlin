## 3.0.0

#### Breaking
* Updated Braze Android SDK [from 32.1.0 to 35.0.0](https://github.com/braze-inc/braze-android-sdk/compare/v32.1.0...v35.0.0#diff-06572a96a58dc510037d5efa622f9bec8519bc1beab13c9f251e97e657a9d4ed).
  * The minimum required Android SDK version is 25. See more details [here](https://github.com/braze-inc/braze-android-sdk?tab=readme-ov-file#version-information).

#### Fixed
* Fixes the internal `logPurchaseForSingleItem` method call to check for product IDs using the key `"product_id"` instead of `"id"`.
  * This change is backwards compatible, but in the event that both keys are provided, `"product_id"` will take precedence over `"id"`.
  * This aligns the Braze Segment plugin with the [V2 Ecommerce Events Spec](https://segment.com/docs/connections/spec/ecommerce/v2/).

#### Added
* Updated Analytics-Kotlin [from 1.16.3 to 1.19.1](https://github.com/segmentio/analytics-kotlin/compare/1.16.3...1.19.1).

## 2.0.0

##### Changed
* Updated to use Braze Android SDK 32.1.0 and analytics-kotlin 1.16.3.
* Updated to allow you to use direct Braze SDK calls without additional entries in your build.gradle file.

## 1.4.2

#### Added
* Adds the key `subscription_group_state` for setting the subscribed/unsubscribed status when using `braze_subscription_groups` in the Identify call.
    - Favor this value instead of `subscription_state_id`.

## 1.4.1

##### Changed
* Updated to use Braze Android SDK 26.1.1

## 1.4.0

##### Added
* Added support to parse `braze_subscription_groups` in the Identity traits to subscribe and unsubscribe from Braze subscription groups.
* Updated to use Braze Android SDK 26.1.0 and analytics-kotlin 1.13.0.

## 1.3.0

#### Breaking
* If the JsonObject sent through Segment's identify() has values that are JsonObjects, those values will be sent to Braze as a Nested Custom Attribute.
* If the JsonObject sent through Segment's identify() has values that are JsonArrays, all values will be converted to Strings and sent to Braze.

##### Changed
* Updated to use Braze Android SDK 25.0.0 and analytics-kotlin 1.10.5.

## 1.2.0

##### Changed
* Update version to not conflict with older versions.

## 1.0.0

##### Changed
* (Supports analytics-kotlin 1.6.2 and Braze Android SDK 23.2.1)
* Initial Release
