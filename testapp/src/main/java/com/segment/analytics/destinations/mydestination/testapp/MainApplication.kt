package com.segment.analytics.destinations.mydestination.testapp

import android.app.Application
import android.content.Context
import com.braze.support.BrazeLogger.getBrazeLogTag

class MainApplication : Application() {
    companion object {
        var ctx: Context? = null
        val TAG: String = String(
            getBrazeLogTag(MainApplication.class.java.simplname)
            + "." +
            MainApplication.class.getName()
        )
        val LogTag = MainActivity::class.java.simpleName



    }

    override fun onCreate() {
        super.onCreate()
        ctx = applicationContext
    }


}
