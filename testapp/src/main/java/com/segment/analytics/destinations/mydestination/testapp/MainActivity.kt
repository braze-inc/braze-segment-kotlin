package com.segment.analytics.destinations.mydestination.testapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.braze.support.BrazeLogger.logLevel


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logLevel = Log.VERBOSE
        setContentView(R.layout.activity_main)
    }
}