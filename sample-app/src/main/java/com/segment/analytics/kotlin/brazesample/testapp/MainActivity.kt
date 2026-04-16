package com.segment.analytics.kotlin.brazesample.testapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.braze.enums.BrazeDateFormat
import com.braze.support.BrazeLogger
import com.braze.support.formatDate
import com.segment.analytics.kotlin.brazesample.testapp.databinding.ActivityMainBinding
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BrazeLogger.logLevel = Log.VERBOSE
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        findViewById<Button>(R.id.trackButton).setOnClickListener {
            MainApplication.analytics.track("TrackTest")
        }

        findViewById<Button>(R.id.identifyButton).setOnClickListener {
            val userId = "segmentKotlin-${Date().formatDate(BrazeDateFormat.SHORT)}"
            MainApplication.analytics.identify(userId, buildIdentifyTraits(includeSdkAuthSignature = false))
        }

        findViewById<Button>(R.id.identifyWithSdkAuthButton).setOnClickListener {
            val signature = Random.nextInt(100).toString()
            val userId = "segmentKotlin-${Date().formatDate(BrazeDateFormat.SHORT)}"
            MainApplication.analytics.identify(userId, buildIdentifyTraits(includeSdkAuthSignature = true, sdkAuthSignature = signature))
        }
    }

    private fun buildIdentifyTraits(
        includeSdkAuthSignature: Boolean,
        sdkAuthSignature: String = "",
    ): JsonObject = buildJsonObject {
        if (includeSdkAuthSignature && sdkAuthSignature.isNotBlank()) {
            put("braze_sdk_auth_signature", sdkAuthSignature)
        }
        put("username", "BobBraze")
        put("email", "bob@test.com")
        put("plan", "premium")
        put("testArray", buildJsonArray {
            add(JsonPrimitive("test"))
            add(JsonPrimitive(3))
            add(JsonPrimitive(true))
        })
        put("jobInfo", buildJsonObject {
            put("department", "G9D")
            put("office", "030-2 E208")
            put("skillInfo", buildJsonObject {
                put("certified", true)
                put("languages", buildJsonArray {
                    add(JsonPrimitive("Kotlin"))
                    add(JsonPrimitive("Java"))
                    add(JsonPrimitive(1))
                    add(JsonPrimitive(false))
                })
            })
        })
    }
}
