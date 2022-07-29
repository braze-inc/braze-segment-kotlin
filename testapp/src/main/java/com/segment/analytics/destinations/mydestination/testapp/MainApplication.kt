package com.segment.analytics.destinations.mydestination.testapp

import android.app.Application
import android.util.Log
import com.braze.support.BrazeLogger
import com.braze.support.BrazeLogger.getBrazeLogTag

class MainApplication : Application() {
    companion object {
    }

    override fun onCreate() {
        super.onCreate()
    }

    val TAG: String = String(
        getBrazeLogTag(
            MainApplication.class) + "." +
                    MainApplication.class. getName ()
        )

}
