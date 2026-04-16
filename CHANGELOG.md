## 5.0.0

#### Breaking
- Updated Braze Android SDK [from 36.0.0 to 42.0.0](https://github.com/braze-inc/braze-android-sdk/compare/v36.0.0...v42.0.0#diff-06572a96a58dc510037d5efa622f9bec8519bc1beab13c9f251e97e657a9d4ed).
- Updated the source and target build compatibility from Java 8 to Java 11.

#### Added
- Updated Analytics-Kotlin [from 1.19.1 to 1.20.0](https://github.com/segmentio/analytics-kotlin/compare/1.19.1...1.20.0).
- Added the ability to unset the following user attributes by setting them to `JsonNull` in the `identify` call:
  - First name
  - Last name
  - Phone number
  - Email
  - Gender
  - Home city
  - Country
- Added support for SDK Authentication.
  - To enable SDK Authentication in your app, add `<bool name="com_braze_sdk_authentication_enabled">true</bool>` to the `braze.xml` file.
  - In order to set the SDK authentication signature, include the key "braze_sdk_auth_signature" in the `identify` call.
- Added the ability to conditionally import `com.braze:android-sdk-location` by setting `importBrazeLocationLibrary=true` in your `gradle.properties`. This removes the need to explicitly add the location dependency in your `build.gradle`.

## 4.0.1

> [!IMPORTANT]
> This release reverts the increase to the minimum Android SDK version of the Braze Android SDK from API 21 to API 25 introduced in 34.0.0. This allows the SDK to once again be compiled into apps supporting as early as API 21. However, we are not reintroducing formal support for < API 25. Read more [here](https://github.com/braze-inc/braze-android-sdk/blob/master/CHANGELOG.md#3600).

#### Fixed
- The `minSdk` enforced by the Braze Segment plugin is now downgraded from `25` to `21`, matching the `minSdk` in the underlying Braze Android SDK.

## 4.0.0

#### Breaking
* Updated Braze Android SDK [from 35.0.0 to 36.0.0](https://github.com/braze-inc/braze-android-sdk/compare/v35.0.0...v36.0.0#diff-06572a96a58dc510037d5efa622f9bec8519bc1beab13c9f251e97e657a9d4ed).

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
