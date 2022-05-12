# Analytics-Kotlin Braze

Add Braze device mode support to your applications via this plugin for
    [Analytics-Kotlin](https://github.com/segmentio/analytics-kotlin)

To install the Segment-Braze integration, simply add this line to your analytics-kotlin gradle file:
    samples/kotlin-android-app-destinations/build.gradle
    Replacd <latest_version> with built number. For example 1.5.2.

```
implementation 'com.segment.analytics.kotlin.destinations:braze:<latest_version>'
```

Or the following for Kotlin DSL

```
implementation("com.segment.analytics.kotlin.destinations:braze:<latest_version>")
```

## Using the Plugin in your App

Open the file where you setup and configure the Analytics-Kotlin library
    samples/kotlin-android-app-destinations/src/main/java/com/segment/analytics/kotlin/destinations/MainApplication.kt
    Add this plugin to the list of imports.

```
import com.segment.analytics.kotlin.destinations.braze.BrazeSession
```

Just under your Analytics-Kotlin library setup, call `analytics.add(plugin = ...)`
to add an instance of the plugin to the Analytics timeline.

```
    analytics = Analytics("<YOUR WRITE KEY>", applicationContext) {
        this.collectDeviceId = true
        this.trackApplicationLifecycleEvents = true
        this.trackDeepLinks = true
        this.flushAt = 1
        this.flushInterval = 0
    }
    analytics.add(plugin = BrazeSession())
```

Your events will now begin to flow to Braze in device mode.


## Support

Please use Github issues, Pull Requests, or feel free to reach out to our [support team](https://segment.com/help/).

## Integrating with Segment

Interested in integrating your service with us? Check out our [Partners page](https://segment.com/partners/) for more details.

## License
```
MIT License

Copyright (c) 2021 Segment

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
