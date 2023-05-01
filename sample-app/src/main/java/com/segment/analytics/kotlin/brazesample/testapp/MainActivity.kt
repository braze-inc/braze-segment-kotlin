package com.segment.analytics.kotlin.brazesample.testapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.braze.support.BrazeLogger
import com.segment.analytics.kotlin.brazesample.testapp.databinding.ActivityMainBinding
import kotlinx.serialization.json.*

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
            MainApplication.analytics.identify("bob", buildJsonObject {
                put("username", "BobBraze")
                put("email", "bob@test.com")
                put("plan", "premium")
                put("testArray", buildJsonArray {
                    add("test")
                    add(3)
                    add(true)
                }
                )
                put("jobInfo", buildJsonObject {
                    put("department", "G9D")
                    put("office", "030-2 E208")
                    put("skillInfo", buildJsonObject {
                        put("certified", true)
                        put("languages", buildJsonArray {
                            add("Kotlin")
                            add("Java")
                            add(1)
                            add(false)
                        })
                    })
                })
            });
        }
    }
}
