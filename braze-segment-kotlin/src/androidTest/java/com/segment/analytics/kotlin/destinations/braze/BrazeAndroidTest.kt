package com.segment.analytics.kotlin.destinations.braze

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.segment.analytics.kotlin.android.Analytics
import com.segment.analytics.kotlin.core.IdentifyEvent
import com.segment.analytics.kotlin.core.Settings
import com.segment.analytics.kotlin.core.Traits
import com.segment.analytics.kotlin.core.platform.Plugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@RunWith(AndroidJUnit4ClassRunner::class)
class BrazeAndroidTest {
    private lateinit var brazeDestination: BrazeDestination

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        brazeDestination = BrazeDestination(context)
        brazeDestination.analytics = Analytics("testkey", context) {
            this.collectDeviceId = true
            this.trackApplicationLifecycleEvents = true
            this.trackDeepLinks = true
            this.flushAt = 1
            this.flushInterval = 0
        }
        val settings = getMockSettings()
        brazeDestination.update(settings, Plugin.UpdateType.Initial)
    }

    @Test
    fun testIdentifyCallsChangeUser() {
        val userId = "bob"

        val braze = brazeDestination.braze
        ktAssertNotNull(braze)
        braze.getCurrentUser { user ->
            assertNull(user)
        }
        val identifyEvent = IdentifyEvent(userId, getMockTraits())
        brazeDestination.identify(identifyEvent)
        braze.getCurrentUser { user ->
            ktAssertNotNull(user)
            assertEquals(userId, user.userId)
        }
    }

    private fun getMockSettings(
        apiKey: String = API_KEY,
        customEndpoint: String = CUSTOM_ENDPOINT,
        autoInAppRegistration: Boolean = true
    ): Settings {
        val brazeJsonSettings = JsonObject(
            content = mapOf(
                "apiKey" to JsonPrimitive(apiKey),
                "customEndpoint" to JsonPrimitive(customEndpoint),
                "automatic_in_app_message_registration_enabled" to JsonPrimitive(autoInAppRegistration),
                "logPurchaseWhenRevenuePresent" to JsonPrimitive(false)
            )
        )
        val integrations = JsonObject(mapOf("Appboy" to brazeJsonSettings))

        return Settings(integrations)
    }

    private fun getMockTraits(): Traits {
        val address = JsonObject(
            mapOf(
                "street" to JsonPrimitive("123 Main St"),
                "city" to JsonPrimitive(CITY),
                "state" to JsonPrimitive("NY"),
                "country" to JsonPrimitive(COUNTRY),
            )
        )
        return JsonObject(
            content = mapOf(
                "firstName" to JsonPrimitive(FIRST_NAME),
                "lastName" to JsonPrimitive(LAST_NAME),
                "birthday" to JsonPrimitive(BIRTHDAY_STRING),
                "email" to JsonPrimitive(EMAIL),
                "gender" to JsonPrimitive(GENDER),
                "phone" to JsonPrimitive(PHONE),
                "address" to address,
                CUSTOM_STRING_KEY to JsonPrimitive(CUSTOM_STRING),
                CUSTOM_INT_KEY to JsonPrimitive(CUSTOM_INT),
                CUSTOM_DOUBLE_KEY to JsonPrimitive(CUSTOM_DOUBLE),
                CUSTOM_FLOAT_KEY to JsonPrimitive(CUSTOM_FLOAT),
                CUSTOM_BOOLEAN_KEY to JsonPrimitive(CUSTOM_BOOLEAN),
                CUSTOM_LONG_KEY to JsonPrimitive(CUSTOM_LONG)
            )
        )
    }

    @OptIn(ExperimentalContracts::class)
    fun ktAssertNotNull(thing: Any?, failureMessage: String? = null) {
        contract {
            returns() implies (thing != null)
        }
        assertNotNull(failureMessage, thing)
    }

    companion object {
        private const val API_KEY = "MY_API_KEY"
        private const val CUSTOM_ENDPOINT = "https://api.braze.com/v3/"
        private const val FIRST_NAME = "Robert"
        private const val LAST_NAME = "Doe"
        private const val BIRTHDAY_STRING = "Tue, 16 Mar 2010 13:30:00 GMT"
        private const val EMAIL = "foo@bar.com"
        private const val GENDER = "male"
        private const val PHONE = "507-555-1234"
        private const val CITY = "Funkytown"
        private const val COUNTRY = "US"
        private const val CUSTOM_STRING_KEY = "mystring"
        private const val CUSTOM_STRING = "lalala"
        private const val CUSTOM_INT_KEY = "myint"
        private const val CUSTOM_INT = 1234

        private const val CUSTOM_BOOLEAN_KEY = "myboolean"
        private const val CUSTOM_BOOLEAN = true
        private const val CUSTOM_DOUBLE_KEY = "mydouble"
        private const val CUSTOM_DOUBLE = 1.23

        private const val CUSTOM_FLOAT_KEY = "myfloat"
        private const val CUSTOM_FLOAT = 4.56F

        private const val CUSTOM_LONG_KEY = "mylong"
        private const val CUSTOM_LONG = 9000000000000000000L
    }
}
