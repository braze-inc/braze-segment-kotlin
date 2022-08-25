package com.segment.analytics.destinations.mydestination.testapp

import android.app.Application
import android.content.Context

class MainApplication : Application() {
    companion object {
        var ctx: Context? = null
        val LogTag = MainActivity::class.java.simpleName
    }

    override fun onCreate() {
        super.onCreate()
        ctx = applicationContext
    }

}
