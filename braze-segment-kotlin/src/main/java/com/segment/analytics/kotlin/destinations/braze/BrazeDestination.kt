package com.segment.analytics.kotlin.destinations.braze

import android.app.Activity
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.braze.Braze
import com.braze.configuration.BrazeConfig
import com.braze.enums.BrazeSdkMetadata
import com.braze.enums.Gender
import com.braze.enums.Month
import com.braze.enums.SdkFlavor
import com.braze.models.outgoing.AttributionData
import com.braze.models.outgoing.BrazeProperties
import com.braze.ui.inappmessage.BrazeInAppMessageManager
import com.segment.analytics.kotlin.android.plugins.AndroidLifecycle
import com.segment.analytics.kotlin.core.*
import com.segment.analytics.kotlin.core.platform.DestinationPlugin
import com.segment.analytics.kotlin.core.platform.Plugin
import com.segment.analytics.kotlin.core.platform.plugins.logger.log
import com.segment.analytics.kotlin.core.utilities.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject
import java.math.BigDecimal
import java.util.*

@Serializable
data class BrazeSettings(
    // Values not configured here should be set
    // via your `braze.xml` configuration.
    val apiKey: String,
    val customEndpoint: String,
    @SerialName("automatic_in_app_message_registration_enabled")
    val automaticInAppMessageRegistrationEnabled: Boolean,
    val logPurchaseWhenRevenuePresent: Boolean
)

class BrazeDestination(
    private val context: Context,
) : DestinationPlugin(), AndroidLifecycle {

    @VisibleForTesting
    var brazeSettings: BrazeSettings? = null
    override val key: String = "Appboy"

    @VisibleForTesting
    internal var brazeTestingMock: Braze? = null
    val braze: Braze
        get() = brazeTestingMock ?: Braze.getInstance(context)

    private var isAutomaticInAppMessageRegistrationEnabled: Boolean = false
    private var shouldLogPurchaseWhenRevenuePresent: Boolean = true

    override fun update(settings: Settings, type: Plugin.UpdateType) {
        super.update(settings, type)
        if (settings.hasIntegrationSettings(this)) {
            analytics.log("Braze Destination is enabled")
            if (type == Plugin.UpdateType.Initial) {
                val brazeSettings: BrazeSettings? = settings.destinationSettings(key)
                if (brazeSettings == null) {
                    analytics.log("Braze Settings not available. Not loading Braze Destination.")
                    return
                }
                this.brazeSettings = brazeSettings

                val builder: BrazeConfig.Builder = BrazeConfig.Builder()
                    .setApiKey(brazeSettings.apiKey)
                    .setSdkFlavor(SdkFlavor.SEGMENT)
                    .setSdkMetadata(EnumSet.of(BrazeSdkMetadata.SEGMENT))

                if (brazeSettings.customEndpoint.isNotBlank()) {
                    builder.setCustomEndpoint(brazeSettings.customEndpoint)
                }
                Braze.configure(context, builder.build())
                isAutomaticInAppMessageRegistrationEnabled = brazeSettings.automaticInAppMessageRegistrationEnabled
                shouldLogPurchaseWhenRevenuePresent = brazeSettings.logPurchaseWhenRevenuePresent

                analytics.log("Braze Destination loaded")
            }
        } else {
            analytics.log("Braze destination is disabled via settings")
        }
    }

    @Suppress("LongMethod", "ComplexMethod", "NestedBlockDepth")
    override fun track(payload: TrackEvent): BaseEvent {
        val eventName = payload.event
        val properties: JsonObject = payload.properties
        val revenue: Double = properties.getDouble(REVENUE_KEY) ?: 0.0

        when {
            eventName == INSTALL_EVENT_NAME -> {
                try {
                    val campaignProps: JsonObject? = properties["campaign"]?.safeJsonObject
                    braze.getCurrentUser { currentUser ->
                        if (campaignProps != null) {
                            currentUser.setAttributionData(
                                AttributionData(
                                    campaignProps.getString("source").orEmpty(),
                                    campaignProps.getString("name").orEmpty(),
                                    campaignProps.getString("ad_group").orEmpty(),
                                    campaignProps.getString("ad_creative").orEmpty()
                                )
                            )
                        }
                    }
                    return payload
                } catch (exception: Exception) {
                    analytics.log(
                        "This Install Attributed event is not in the proper format and cannot be"
                            + " logged. The exception is $exception."
                    )
                }
            }
            shouldLogPurchaseWhenRevenuePresent && revenue != 0.0 || eventName == PURCHASE_EVENT_NAME_1 || eventName == PURCHASE_EVENT_NAME_2 -> {
                val currencyCode: String? =
                    if (properties.getString(CURRENCY_KEY).isNullOrBlank()) {
                        DEFAULT_CURRENCY_CODE
                    } else properties.getString(CURRENCY_KEY)

                if (properties.containsKey("products")) {
                    properties["products"]?.safeJsonArray?.let { array ->
                        for (product in array) {
                            logPurchaseForSingleItem(
                                product.jsonObject.getString("id"),
                                currencyCode,
                                BigDecimal.valueOf(
                                    product.jsonObject.getDouble("price") ?: 0.0
                                ),
                                properties
                            )
                        }
                    }
                } else {
                    logPurchaseForSingleItem(
                        eventName,
                        currencyCode,
                        BigDecimal.valueOf(revenue)!!,
                        properties
                    )
                }
            }
            else -> {
                if (properties.isEmpty()) {
                    analytics.log("Calling braze.logCustomEvent for event $eventName")
                    braze.logCustomEvent(eventName)
                } else {
                    analytics.log(
                        "Calling braze.logCustomEvent for event $eventName with properties ${properties.toContent()}."
                    )
                    val brazeProperties = properties.toBrazeProperties()
                    brazeProperties.removeProperty(REVENUE_KEY)
                    brazeProperties.removeProperty(CURRENCY_KEY)
                    braze.logCustomEvent(eventName, brazeProperties)
                }
            }
        }
        return payload
    }

    @Suppress("LongMethod", "ComplexMethod", "NestedBlockDepth")
    override fun identify(payload: IdentifyEvent): BaseEvent {
        val userId: String = payload.userId
        if (userId.isNotBlank()) {
            braze.changeUser(userId)
        }

        val traits: Traits = payload.traits
        analytics.log("Traits in BrazeDestination::identify = ${traits.toString()}")

        braze.getCurrentUser { currentUser ->
            if (traits.containsKey(SUBSCRIPTION_GROUP_KEY)) {
                val subscriptions = traits[SUBSCRIPTION_GROUP_KEY]?.safeJsonArray
                subscriptions?.forEach { subscriptionInfo ->
                    if (subscriptionInfo != null && subscriptionInfo is JsonObject) {
                        val groupId = subscriptionInfo.getString(SUBSCRIPTION_ID_KEY)
                        val groupState = subscriptionInfo.getString(SUBSCRIPTION_STATE_KEY)
                        if (!groupId.isNullOrBlank()) {
                            when (groupState) {
                                "subscribed" -> {
                                    currentUser.addToSubscriptionGroup(groupId)
                                }

                                "unsubscribed" -> {
                                    currentUser.removeFromSubscriptionGroup(groupId)
                                }

                                else -> {
                                    analytics.log(
                                        "Unrecognized Braze subscription state: $groupState."
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val birthdayString = traits.getString("birthday").orEmpty()
            if (birthdayString.isNotBlank()) {
                try {
                    val birthday = Date(birthdayString)
                    val birthdayCal = Calendar.getInstance(Locale.US)
                    val month = Month.getMonth(Calendar.MONTH)
                    birthdayCal.time = birthday
                    if (month != null) {
                        currentUser.setDateOfBirth(
                            birthdayCal[Calendar.YEAR],
                            month,
                            birthdayCal[Calendar.DAY_OF_MONTH]
                        )
                    } else {
                        analytics.log(
                            "Could not get birthday month from $birthdayString, skipping."
                        )
                    }
                } catch (exception: Exception) {
                    analytics.log(
                        "birthday was in an incorrect format, skipping. "
                                + "The exception is $exception."
                    )
                }
            }

            val email: String = traits.getString("email").orEmpty()
            if (email.isNotBlank()) {
                currentUser.setEmail(email)
            }

            val firstName: String = traits.getString("firstName").orEmpty()
            if (firstName.isNotBlank()) {
                currentUser.setFirstName(firstName)
            }

            val lastName: String = traits.getString("lastName").orEmpty()
            if (lastName.isNotBlank()) {
                currentUser.setLastName(lastName)
            }

            val gender: String = traits.getString("gender").orEmpty().uppercase(Locale.getDefault())
            if (gender.isNotBlank()) {
                if (MALE_TOKENS.contains(
                        gender.uppercase(
                            Locale.getDefault()
                        )
                    )
                ) {
                    currentUser.setGender(Gender.MALE)
                } else if (FEMALE_TOKENS.contains(
                        gender.uppercase(
                            Locale.getDefault()
                        )
                    )
                ) {
                    currentUser.setGender(Gender.FEMALE)
                }
            }

            val phone: String = traits.getString("phone").orEmpty()
            if (phone.isNotBlank()) {
                currentUser.setPhoneNumber(phone)
            }

            val address = traits["address"]
            if (address != null && address is JsonObject) {
                val city: String = address.getString("city").orEmpty()
                if (city.isNotBlank()) {
                    currentUser.setHomeCity(city)
                }
                val country: String = address.getString("country").orEmpty()
                if (country.isNotBlank()) {
                    currentUser.setCountry(country)
                }
            }

            for (key: String in traits.keys) {
                if (RESERVED_KEYS.contains(key)) {
                    analytics.log("Skipping reserved key $key")
                    continue
                }
                
                val value = traits[key]?.toContent()

                if (value is Boolean) {
                    currentUser.setCustomUserAttribute(key, value)
                } else if (value is Int) {
                    currentUser.setCustomUserAttribute(key, value)
                } else if (value is Float) {
                    currentUser.setCustomUserAttribute(key, value)
                } else if (value is Double) {
                    currentUser.setCustomUserAttribute(key, value)
                } else if (value is Long) {
                    currentUser.setCustomUserAttribute(key, value)
                } else if (value is String) {
                    try {
                        val dateTime = Date(value)
                        currentUser.setCustomUserAttributeToSecondsFromEpoch(key, dateTime.time)
                    } catch (_: Exception) {
                        currentUser.setCustomUserAttribute(key, value)
                    }
                } else if (value is List<*>) {
                    val stringValueList = mutableListOf<String>()
                    for (content in value) {
                        stringValueList.add(content.toString())
                    }
                    if (stringValueList.size > 0) {
                        val valueArray: Array<String?> = stringValueList.toTypedArray()
                        currentUser.setCustomAttributeArray(
                            key,
                            valueArray
                        )
                    }
                } else if (value is Map<*, *>) {
                    try {
                        currentUser.setCustomUserAttribute(key, JSONObject(value))
                    } catch (e: Exception) {
                        analytics.log(
                            "Error converting to JSONObject for key $key: ${e.message}"
                        )
                    }
                } else {
                    analytics.log(
                        "Braze can't map segment value for custom Braze user "
                                + "attribute with key $key and value $value"
                    )
                }
            }
        }

        return payload
    }

    override fun flush() {
        super.flush()
        analytics.log("Calling braze.requestImmediateDataFlush().")
        braze.requestImmediateDataFlush()
    }

    override fun onActivityStarted(activity: Activity?) {
        super.onActivityStarted(activity)
        braze.openSession(activity)
    }

    override fun onActivityStopped(activity: Activity?) {
        super.onActivityStopped(activity)
        braze.closeSession(activity)
    }

    override fun onActivityResumed(activity: Activity?) {
        super.onActivityResumed(activity)
        if (isAutomaticInAppMessageRegistrationEnabled) {
            BrazeInAppMessageManager.getInstance().registerInAppMessageManager(activity)
        }
    }

    override fun onActivityPaused(activity: Activity?) {
        super.onActivityPaused(activity)
        if (isAutomaticInAppMessageRegistrationEnabled) {
            BrazeInAppMessageManager.getInstance().unregisterInAppMessageManager(activity)
        }
    }

    private fun logPurchaseForSingleItem(
        productId: String?,
        currencyCode: String?,
        price: BigDecimal?,
        propertiesJson: JsonObject?
    ) {
        val brazeProperties = propertiesJson?.toBrazeProperties()
        brazeProperties?.removeProperty(REVENUE_KEY)
        brazeProperties?.removeProperty(CURRENCY_KEY)

        if (brazeProperties == null || brazeProperties.size == 0) {
            analytics.log(
                "Calling braze.logPurchase for purchase $productId for %.02f $currencyCode"
                    + " with no properties.".format(price)
            )
            braze.logPurchase(productId, currencyCode, price)
        } else {
            analytics.log(
                "Calling braze.logPurchase for purchase $productId for %.02f $currencyCode"
                    + " with properties $propertiesJson.".format(price)
            )
            braze.logPurchase(
                productId,
                currencyCode,
                price,
                brazeProperties
            )
        }
    }

    companion object {
        private const val INSTALL_EVENT_NAME = "Install Attributed"
        private const val PURCHASE_EVENT_NAME_1 = "Order Completed"
        private const val PURCHASE_EVENT_NAME_2 = "Completed Order"

        private const val DEFAULT_CURRENCY_CODE = "USD"
        private val MALE_TOKENS = setOf("M", "MALE")
        private val FEMALE_TOKENS = setOf("F", "FEMALE")

        private const val REVENUE_KEY = "revenue"
        private const val CURRENCY_KEY = "currency"

        private const val SUBSCRIPTION_GROUP_KEY = "braze_subscription_groups"
        private const val SUBSCRIPTION_ID_KEY = "subscription_group_id"
        private const val SUBSCRIPTION_STATE_KEY = "subscription_state_id"

        private val RESERVED_KEYS = listOf(
            "birthday",
            "email",
            "firstName",
            "lastName",
            "gender",
            "phone",
            "address",
            "anonymousId",
            "userId",
            SUBSCRIPTION_GROUP_KEY
        )

        private fun JsonObject.toBrazeProperties() =
            // This handles going from kotlinx.serialization.json.JsonObject to org.json.JSONObject
            BrazeProperties(JSONObject(this.toString()))
    }
}
