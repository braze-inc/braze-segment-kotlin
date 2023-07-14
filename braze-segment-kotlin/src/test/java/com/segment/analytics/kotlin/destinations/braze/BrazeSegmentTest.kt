package com.segment.analytics.kotlin.destinations.braze

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.braze.Braze
import com.braze.BrazeUser
import com.braze.enums.Gender
import com.braze.enums.Month
import com.braze.events.IValueCallback
import com.braze.models.outgoing.AttributionData
import com.braze.models.outgoing.BrazeProperties
import com.braze.support.getOptionalString
import com.segment.analytics.kotlin.core.*
import com.segment.analytics.kotlin.core.platform.Plugin
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(RobolectricTestRunner::class)
class BrazeSegmentTest {
    private lateinit var brazeDestination: BrazeDestination
    private var mockedAnalytics: Analytics = mock()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        brazeDestination = BrazeDestination(context)
        brazeDestination.analytics = mockedAnalytics
        val settings = getMockSettings()
        brazeDestination.update(settings, Plugin.UpdateType.Initial)
    }

    @After
    fun tearDown() {
        brazeDestination.brazeTestingMock = null
    }

    @Test
    fun whenCalledWithInitial_update_brazeSettingsIsCorrect() {
        assertEquals(API_KEY, brazeDestination.brazeSettings?.apiKey)
        assertEquals(CUSTOM_ENDPOINT, brazeDestination.brazeSettings?.customEndpoint)
        assertEquals(true, brazeDestination.brazeSettings?.automaticInAppMessageRegistrationEnabled)
    }

    @Test
    fun testActivityStartedCallsOpenSession() {
        val activity = mock<Activity>()
        val brazeMock = mock<Braze>()
        brazeDestination.brazeTestingMock = brazeMock

        brazeDestination.onActivityStarted(activity)
        verify(brazeMock).openSession(activity)
    }

    @Test
    fun testActivityStoppedCallsCloseSession() {
        val activity = mock<Activity>()
        val brazeMock = mock<Braze>()
        brazeDestination.brazeTestingMock = brazeMock

        brazeDestination.onActivityStopped(activity)
        verify(brazeMock).closeSession(activity)
    }

    @Test
    fun whenCalledWithRefresh_update_valuesDoNotChange() {
        val anotherApiKey = "ANOTHER_API_KEY"
        val anotherEndpoint = "https://another.endpoint/v3/"

        assertEquals(API_KEY, brazeDestination.brazeSettings?.apiKey)
        assertEquals(CUSTOM_ENDPOINT, brazeDestination.brazeSettings?.customEndpoint)
        assertEquals(true, brazeDestination.brazeSettings?.automaticInAppMessageRegistrationEnabled)

        val settings2 = getMockSettings(anotherApiKey, anotherEndpoint, false)
        brazeDestination.update(settings2, Plugin.UpdateType.Refresh)
        assertEquals(API_KEY, brazeDestination.brazeSettings?.apiKey)
        assertEquals(CUSTOM_ENDPOINT, brazeDestination.brazeSettings?.customEndpoint)
        assertEquals(true, brazeDestination.brazeSettings?.automaticInAppMessageRegistrationEnabled)
    }

    @Test
    fun whenCalledWithInitial_update_brazeUserIsNull() {
        val braze = brazeDestination.braze
        ktAssertNotNull(braze)
        braze.getCurrentUser { user ->
            assertNull(user)
        }
    }

    @Test
    fun whenCalled_identify_brazeUserIdIsNotEmpty() {
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
            assertEquals("bob", user.userId)
        }
    }

    @Test
    fun whenCalledWithFullTraits_identify_brazeUserSetFunctionsAreCalled() {
        val userId = "bob"

        val jsonCapture = argumentCaptor<JSONObject>()
        val arrayCapture = argumentCaptor<Array<String?>>()
        val brazeUserMock: BrazeUser = mock() {
            on { setCustomUserAttribute(any(), jsonCapture.capture(), any()) }.thenReturn(true)
            on { setCustomAttributeArray(any(), arrayCapture.capture()) }.thenReturn(true)
        }
        val brazeMock = mock<Braze>() {
            on { currentUser } doReturn brazeUserMock
            on { getCurrentUser(any()) }.doAnswer { invocation ->
                (invocation.arguments[0] as IValueCallback<BrazeUser>).onSuccess(brazeUserMock)
            }
        }
        brazeDestination.brazeTestingMock = brazeMock

        val identifyEvent = IdentifyEvent(userId, getMockTraits())
        brazeDestination.identify(identifyEvent)
        verify(brazeUserMock, times(1)).setCountry(COUNTRY)
        verify(brazeUserMock, times(1)).setDateOfBirth(BIRTHDAY_YEAR, Month.valueOf(BIRTHDAY_MONTH), BIRTHDAY_DAY)
        verify(brazeUserMock, times(1)).setEmail(EMAIL)
        verify(brazeUserMock, times(1)).setFirstName(FIRST_NAME)
        verify(brazeUserMock, times(1)).setGender(Gender.MALE)
        verify(brazeUserMock, times(1)).setHomeCity(CITY)
        verify(brazeUserMock, times(1)).setLastName(LAST_NAME)
        verify(brazeUserMock, times(1)).setPhoneNumber(PHONE)

        verify(brazeUserMock, times(1)).setCustomUserAttribute(CUSTOM_STRING_KEY, CUSTOM_STRING)
        verify(brazeUserMock, times(1)).setCustomUserAttribute(CUSTOM_INT_KEY, CUSTOM_INT)
        verify(brazeUserMock, times(1)).setCustomUserAttribute(CUSTOM_BOOLEAN_KEY, CUSTOM_BOOLEAN)

        // Because of how JsonPrimitive works, floats are always processed as doubles
        verify(brazeUserMock, times(1)).setCustomUserAttribute(CUSTOM_FLOAT_KEY, 4.56)
        verify(brazeUserMock, times(0)).setCustomUserAttribute(any(), any() as Float)
        verify(brazeUserMock, times(1)).setCustomUserAttribute(CUSTOM_DOUBLE_KEY, CUSTOM_DOUBLE)
        verify(brazeUserMock, times(1)).setCustomUserAttribute(CUSTOM_LONG_KEY, CUSTOM_LONG)
        verify(brazeUserMock, times(1)).setCustomUserAttribute(any(), any(), any())
        verify(brazeUserMock, times(1)).setCustomAttributeArray(any(), any())
        verifyNoMoreInteractions(brazeUserMock)

        assertEquals(1, jsonCapture.allValues.size)
        val capturedValue = jsonCapture.firstValue
        assertEquals(2, capturedValue.length())
        assertEquals(JSON_LOCATION_VALUE, capturedValue.getString(JSON_LOCATION_KEY))
        assertEquals(JSON_DEPT_VALUE, capturedValue.getString(JSON_DEPT_KEY))

        assertEquals(1, arrayCapture.allValues.size)
        val capturedArray = arrayCapture.firstValue
        assertEquals(3, capturedArray.size)
        assertTrue(capturedArray.any { it.equals("one") })
        assertTrue(capturedArray.any { it.equals("2") })
        assertTrue(capturedArray.any { it.equals("true") })
    }

    @Test
    fun whenCalledWithArray_identify_brazeUserSetFunctionsAreCalled() {
        val userId = "bob"
        val brazeUserMock: BrazeUser = mock()
        val brazeMock = mock<Braze>() {
            on { currentUser } doReturn brazeUserMock
            on { getCurrentUser(any()) }.doAnswer { invocation ->
                (invocation.arguments[0] as IValueCallback<BrazeUser>).onSuccess(brazeUserMock)
            }
        }
        brazeDestination.brazeTestingMock = brazeMock

        val testArrayList = listOf(JsonPrimitive("alice"), JsonPrimitive("bob"))

        val identifyEvent = IdentifyEvent(
            userId,
            JsonObject(
                content = mapOf(
                    "testArrayList" to JsonArray(testArrayList)
                )
            )
        )

        brazeDestination.identify(identifyEvent)

        verify(brazeUserMock, times(1)).setCustomAttributeArray(
            eq("testArrayList"),
            eq(listOf("alice", "bob").toTypedArray())
        )
        verifyNoMoreInteractions(brazeUserMock)
    }

    @Test
    fun whenCalledWithEmptyTraits_identify_brazeUserSetFunctionsAreNotCalled() {
        val userId = "bob"

        val brazeUserMock: BrazeUser = mock()
        val brazeMock = mock<Braze>() {
            on { currentUser } doReturn brazeUserMock
        }
        brazeDestination.brazeTestingMock = brazeMock

        val identifyEvent = IdentifyEvent(userId, JsonObject(mapOf()))
        brazeDestination.identify(identifyEvent)
        verify(brazeUserMock, times(0)).setCountry(any())
        verify(brazeUserMock, times(0)).setDateOfBirth(any(), any(), any())
        verify(brazeUserMock, times(0)).setEmail(any())
        verify(brazeUserMock, times(0)).setFirstName(any())
        verify(brazeUserMock, times(0)).setGender(any())
        verify(brazeUserMock, times(0)).setHomeCity(any())
        verify(brazeUserMock, times(0)).setLastName(any())
        verify(brazeUserMock, times(0)).setPhoneNumber(any())
        verifyNoMoreInteractions(brazeUserMock)
    }

    @Test
    fun whenCalledWithGarbageBirthday_identify_doesntCrash() {
        val userId = "bob"

        val brazeUserMock: BrazeUser = mock()
        val brazeSpy = spy(brazeDestination.braze) {
            on { currentUser } doReturn brazeUserMock
        }
        brazeDestination.brazeTestingMock = brazeSpy

        val identifyEvent = IdentifyEvent(
            userId,
            JsonObject(
                content = mapOf(
                    "birthday" to JsonPrimitive(GARBAGE_DATE)
                )
            )
        )
        brazeDestination.identify(identifyEvent)
        verify(brazeUserMock, times(0)).setDateOfBirth(any(), any(), any())
        verifyNoMoreInteractions(brazeUserMock)
    }

    @Test
    fun whenCalled_track_callsLogCustomEvent() {
        val brazeMock = mock<Braze>()
        brazeDestination.brazeTestingMock = brazeMock

        val trackEvent = TrackEvent(
            JsonObject(emptyMap()),
            "My Event"
        )
        val payload = brazeDestination.track(trackEvent)
        assertEquals(payload, trackEvent)
        verify(brazeMock, times(1)).logCustomEvent("My Event")
    }

    @Test
    fun whenCalledWithProperties_track_callsLogCustomEventWithProperties() {
        val brazePropertiesCaptor = argumentCaptor<BrazeProperties>()
        val brazeMock = mock<Braze>() {
            on { logCustomEvent(any(), brazePropertiesCaptor.capture()) }.then { }
        }
        brazeDestination.brazeTestingMock = brazeMock

        val trackEvent = TrackEvent(
            JsonObject(
                mapOf(
                    "property1" to JsonPrimitive("A")
                )
            ),
            "My Event"
        )
        val payload = brazeDestination.track(trackEvent)
        assertEquals(payload, trackEvent)
        verify(brazeMock, times(1)).logCustomEvent(eq("My Event"), any())
        assertEquals(1, brazePropertiesCaptor.allValues.size)
        val brazeProperties = brazePropertiesCaptor.firstValue
        assertEquals(1, brazeProperties.size)
        assertEquals("A", brazeProperties.get("property1"))
    }

    @Test
    fun whenCalledWithEventNameOrderCompleted_track_callsLogPurchase() {
        val brazePropertiesCaptor = argumentCaptor<BrazeProperties>()
        val brazeMock = mock<Braze>() {
            on { logPurchase(any(), any(), any(), brazePropertiesCaptor.capture()) }.then { }
        }
        brazeDestination.brazeTestingMock = brazeMock

        val trackEvent = TrackEvent(
            JsonObject(
                mapOf(
                    "revenue" to JsonPrimitive(5.99),
                    "currency" to JsonPrimitive("CAD")
                )
            ),
            "Order Completed"
        )
        val payload = brazeDestination.track(trackEvent)
        assertEquals(payload, trackEvent)
        verify(brazeMock, times(1)).logPurchase(eq("Order Completed"), any(), any())
        assertEquals(0, brazePropertiesCaptor.allValues.size)
    }

    @Test
    fun whenCalledWithRevenueButSettingIsFalse_track_DoesNotCallLogPurchaseWithProperties() {
        val brazePropertiesCaptor = argumentCaptor<BrazeProperties>()
        val brazeMock = mock<Braze> {
            on { logPurchase(any(), any(), any(), brazePropertiesCaptor.capture()) }.then { }
            on { logCustomEvent(any(), brazePropertiesCaptor.capture()) }.then { }
        }
        val settings = getMockSettings(logPurchaseWhenRevenuePresent = false)
        brazeDestination.update(settings, Plugin.UpdateType.Initial)
        brazeDestination.brazeTestingMock = brazeMock

        val trackEvent = TrackEvent(
            JsonObject(
                mapOf(
                    "revenue" to JsonPrimitive(5.99),
                    "currency" to JsonPrimitive("CAD"),
                    "color" to JsonPrimitive("red")

                )
            ),
            "Random Event Name"
        )
        val payload = brazeDestination.track(trackEvent)
        assertEquals(payload, trackEvent)
        verify(brazeMock, times(0)).logPurchase(any(), any(), any(), any() as BrazeProperties?)

        verify(brazeMock, times(1)).logCustomEvent(eq("Random Event Name"), any() as BrazeProperties?)

        assertEquals(1, brazePropertiesCaptor.allValues.size)
        val brazeProperties = brazePropertiesCaptor.firstValue
        assertEquals(1, brazeProperties.size)
        assertEquals("red", brazeProperties["color"])
    }

    @Test
    fun whenCalledWithRevenueButSettingIsFalseButNameIsOrderCompleted_track_DoesCallLogPurchaseWithProperties() {
        val brazePropertiesCaptor = argumentCaptor<BrazeProperties>()
        val brazeMock = mock<Braze> {
            on { logPurchase(any(), any(), any(), brazePropertiesCaptor.capture()) }.then { }
        }
        val settings = getMockSettings(logPurchaseWhenRevenuePresent = false)
        brazeDestination.update(settings, Plugin.UpdateType.Initial)
        brazeDestination.brazeTestingMock = brazeMock

        val trackEvent = TrackEvent(
            JsonObject(
                mapOf(
                    "revenue" to JsonPrimitive(5.99),
                    "currency" to JsonPrimitive("CAD"),
                    "color" to JsonPrimitive("red")

                )
            ),
            "Order Completed"
        )
        val payload = brazeDestination.track(trackEvent)
        assertEquals(payload, trackEvent)
        verify(brazeMock, times(1)).logPurchase(eq("Order Completed"), any(), any(), any() as BrazeProperties?)

        assertEquals(1, brazePropertiesCaptor.allValues.size)
        val brazeProperties = brazePropertiesCaptor.firstValue
        assertEquals(1, brazeProperties.size)
        assertEquals("red", brazeProperties["color"])
    }

    @Test
    fun whenCalledWithRevenueAndSettingIsTrueButNameIsNotOrderCompleted_track_DoesCallLogPurchaseWithProperties() {
        val brazePropertiesCaptor = argumentCaptor<BrazeProperties>()
        val brazeMock = mock<Braze> {
            on { logPurchase(any(), any(), any(), brazePropertiesCaptor.capture()) }.then { }
        }
        val settings = getMockSettings(logPurchaseWhenRevenuePresent = true)
        brazeDestination.update(settings, Plugin.UpdateType.Initial)
        brazeDestination.brazeTestingMock = brazeMock

        val trackEvent = TrackEvent(
            JsonObject(
                mapOf(
                    "revenue" to JsonPrimitive(5.99),
                    "currency" to JsonPrimitive("CAD"),
                    "color" to JsonPrimitive("red")

                )
            ),
            "Random Event Name"
        )
        val payload = brazeDestination.track(trackEvent)
        assertEquals(payload, trackEvent)
        verify(brazeMock, times(1)).logPurchase(eq("Random Event Name"), any(), any(), any() as BrazeProperties?)

        assertEquals(1, brazePropertiesCaptor.allValues.size)
        val brazeProperties = brazePropertiesCaptor.firstValue
        assertEquals(1, brazeProperties.size)
        assertEquals("red", brazeProperties["color"])
    }

    @Test
    fun whenEventNameIsInstallAttributed_track_callsSetAttributionData() {
        val attributionDataCaptor = argumentCaptor<AttributionData>()

        val brazeUserMock: BrazeUser = mock() {
            on { setAttributionData(attributionDataCaptor.capture()) }.then { }
        }
        val brazeMock = mock<Braze>() {
            on { currentUser } doReturn brazeUserMock
            on { getCurrentUser(any()) }.doAnswer { invocation ->
                (invocation.arguments[0] as IValueCallback<BrazeUser>).onSuccess(brazeUserMock)
            }
        }
        brazeDestination.brazeTestingMock = brazeMock

        val trackEvent = TrackEvent(
            JsonObject(
                content = mapOf(
                    "campaign" to JsonObject(
                        mapOf(
                            "source" to JsonPrimitive(CAMPAIGN_SOURCE),
                            "name" to JsonPrimitive(CAMPAIGN_NAME),
                            "ad_group" to JsonPrimitive(CAMPAIGN_GROUP),
                            "ad_creative" to JsonPrimitive(CAMPAIGN_CREATIVE)
                        )
                    )
                )
            ),
            "Install Attributed"
        )
        val payload = brazeDestination.track(trackEvent)
        assertEquals(payload, trackEvent)
        verify(brazeUserMock, times(1)).setAttributionData(any())
        assertEquals(1, attributionDataCaptor.allValues.size)
        val attributionData = attributionDataCaptor.firstValue
        assertEquals(CAMPAIGN_SOURCE, attributionData.forJsonPut().getOptionalString("source"))
        assertEquals(CAMPAIGN_NAME, attributionData.forJsonPut().getOptionalString("campaign"))
        assertEquals(CAMPAIGN_GROUP, attributionData.forJsonPut().getOptionalString("adgroup"))
        assertEquals(CAMPAIGN_CREATIVE, attributionData.forJsonPut().getOptionalString("ad"))
        verifyNoMoreInteractions(brazeUserMock)
    }

    @Test
    fun whenCalledWithSubscriptionData_track_callsAddToSubscriptionGroup() {
        val groupIdCaptor = argumentCaptor<String>()
        val brazeUserMock: BrazeUser = mock() {
            on { addToSubscriptionGroup(groupIdCaptor.capture()) }.thenReturn(true)
        }

        val brazeMock = mock<Braze>() {
            on { currentUser } doReturn brazeUserMock
            on { getCurrentUser(any()) }.doAnswer { invocation ->
                (invocation.arguments[0] as IValueCallback<BrazeUser>).onSuccess(brazeUserMock)
            }
        }
        brazeDestination.brazeTestingMock = brazeMock

        val identifyEvent = IdentifyEvent(
            "myUser",
            getMockSubscriptionTraits(true)
        )
        val payload = brazeDestination.identify(identifyEvent)
        assertEquals(payload, identifyEvent)
        verify(brazeUserMock, times(1)).addToSubscriptionGroup(any())
        assertEquals("123-456-789", groupIdCaptor.firstValue)

        // Verify the other data is handled
        verify(brazeUserMock, times(1)).setFirstName(FIRST_NAME)
        verify(brazeUserMock, times(1)).setLastName(LAST_NAME)

        // Verify that nothing else was called (subscription data didn't turn into the custom attributes)
        verifyNoMoreInteractions(brazeUserMock)
    }

    @Test
    fun whenCalledWithUnSubscriptionData_identify_callsRemoveFromSubscriptionGroup() {
        val groupIdCaptor = argumentCaptor<String>()
        val brazeUserMock: BrazeUser = mock() {
            on { removeFromSubscriptionGroup(groupIdCaptor.capture()) }.thenReturn(true)
        }

        val brazeMock = mock<Braze>() {
            on { currentUser } doReturn brazeUserMock
            on { getCurrentUser(any()) }.doAnswer { invocation ->
                (invocation.arguments[0] as IValueCallback<BrazeUser>).onSuccess(brazeUserMock)
            }
        }
        brazeDestination.brazeTestingMock = brazeMock

        val identifyEvent = IdentifyEvent(
            "myUser",
            getMockSubscriptionTraits(false)
        )
        val payload = brazeDestination.identify(identifyEvent)
        assertEquals(payload, identifyEvent)
        verify(brazeUserMock, times(1)).removeFromSubscriptionGroup(any())
        assertEquals("123-456-789", groupIdCaptor.firstValue)

        // Verify the other data is handled
        verify(brazeUserMock, times(1)).setFirstName(FIRST_NAME)
        verify(brazeUserMock, times(1)).setLastName(LAST_NAME)

        // Verify that nothing else was called (subscription data didn't turn into the custom attributes)
        verifyNoMoreInteractions(brazeUserMock)
    }

    private fun getMockSettings(
        apiKey: String = API_KEY,
        customEndpoint: String = CUSTOM_ENDPOINT,
        autoInAppRegistration: Boolean = true,
        logPurchaseWhenRevenuePresent: Boolean = true
    ): Settings {
        val brazeJsonSettings = JsonObject(
            content = mapOf(
                "apiKey" to JsonPrimitive(apiKey),
                "customEndpoint" to JsonPrimitive(customEndpoint),
                "automatic_in_app_message_registration_enabled" to JsonPrimitive(autoInAppRegistration),
                "logPurchaseWhenRevenuePresent" to JsonPrimitive(logPurchaseWhenRevenuePresent)
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

        val nestedObject = JsonObject(
            mapOf(
                JSON_DEPT_KEY to JsonPrimitive(JSON_DEPT_VALUE),
                JSON_LOCATION_KEY to JsonPrimitive(JSON_LOCATION_VALUE)
            )
        )

        val jsonArray = JsonArray(
            listOf(
                JsonPrimitive("one"),
                JsonPrimitive(2),
                JsonPrimitive(true)
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
                CUSTOM_LONG_KEY to JsonPrimitive(CUSTOM_LONG),
                "jobInfo" to nestedObject,
                "jsonArray" to jsonArray
            )
        )
    }

    private fun getMockSubscriptionTraits(subscription: Boolean = true): Traits {
        val groupId = "123-456-789"
        val groupStatus = if (subscription) {
            "subscribed"
        } else {
            "unsubscribed"
        }

        val subscriptionData = JsonArray(
            listOf(
                JsonObject(
                    mapOf(
                        "subscription_group_id" to JsonPrimitive(groupId),
                        "subscription_state_id" to JsonPrimitive(groupStatus)
                    )
                )
            )
        )

        val contentMap = mutableMapOf(
            "firstName" to JsonPrimitive(FIRST_NAME),
            "lastName" to JsonPrimitive(LAST_NAME),
            "braze_subscription_groups" to subscriptionData
        )

        return JsonObject(
            content = contentMap
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
        private const val BIRTHDAY_YEAR = 2010
        private const val BIRTHDAY_MONTH = "MARCH"
        private const val BIRTHDAY_DAY = 16
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

        private const val JSON_DEPT_KEY = "department"
        private const val JSON_DEPT_VALUE = "G9D"
        private const val JSON_LOCATION_KEY = "location"
        private const val JSON_LOCATION_VALUE = "030-2 E208"

        private const val GARBAGE_DATE = "2010-03-16"

        private const val CAMPAIGN_SOURCE = "campsource"
        private const val CAMPAIGN_NAME = "campname"
        private const val CAMPAIGN_GROUP = "campgroup"
        private const val CAMPAIGN_CREATIVE = "campcreative"
    }
}
