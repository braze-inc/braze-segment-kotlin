package com.segment.analytics.kotlin.brazesample.testapp

import android.app.Application
import com.segment.analytics.kotlin.android.Analytics
import com.segment.analytics.kotlin.core.Analytics
import com.segment.analytics.kotlin.destinations.braze.BrazeDestination

class MainApplication : Application() {
    companion object {
        const val WRITE_KEY = "YOUR_WRITE_KEY";
        lateinit var analytics: Analytics
    }

    override fun onCreate() {
        super.onCreate()
        analytics = Analytics(WRITE_KEY, applicationContext) {
            this.collectDeviceId = true
            this.trackApplicationLifecycleEvents = true
            this.trackDeepLinks = true
            this.flushAt = 1
            this.flushInterval = 0
        }

        analytics.add(plugin = BrazeDestination(applicationContext))
    }
}
