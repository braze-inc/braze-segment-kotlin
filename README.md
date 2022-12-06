# Analytics-Kotlin-Braze

Add Braze device mode support to your applications via this plugin for
[Analytics-Kotlin](https://github.com/segmentio/analytics-kotlin)

## Adding the dependency

To install the Segment-Braze integration, simply add this line to your app's build.gradle file, replacing
`<latest_version>` with the latest version number.

```
implementation 'com.braze:braze-segment-kotlin:<latest_version>'
```

Or the following for Kotlin DSL

```
implementation("com.braze:braze-segment-kotlin:<latest_version>")
```

Also add the following lines to the build.gradle file

```
repositories {
  maven { url "https://appboy.github.io/appboy-android-sdk/sdk" }
}  
```  

## Using the Plugin in your App

Open the file where you setup and configure the Analytics-Kotlin library
which will usually be MainApplication.kt
Add this plugin to the list of imports.

```
import com.segment.analytics.kotlin.destinations.braze.BrazeDestination
```

Just under your Analytics-Kotlin library setup, call `analytics.add(plugin = ...)`
to add an instance of the plugin to the Analytics timeline.

```
    analytics = Analytics("<YOUR WRITE KEY>", applicationContext) {
        this.collectDeviceId = true
        this.trackApplicationLifecycleEvents = true
        this.trackDeepLinks = true
        this.flushAt = 3
        this.flushInterval = 0
    }
    analytics.add(plugin = BrazeDestination(applicationContext))
```

Your events will now begin to flow to Braze in device mode.
