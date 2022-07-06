package com.segment.analytics.kotlin.destinations.braze


import android.app.Activity
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.appboy.Appboy
import com.appboy.IAppboy
import com.appboy.enums.Gender
import com.appboy.enums.Month
import com.appboy.models.outgoing.AttributionData
import com.braze.Braze
import com.braze.BrazeUser
import com.braze.models.outgoing.BrazeProperties
import com.braze.support.isNullOrBlank
import com.braze.ui.inappmessage.BrazeInAppMessageManager
import com.segment.analytics.kotlin.android.plugins.AndroidLifecycle
import com.segment.analytics.kotlin.android.utilities.toJSONArray
import com.segment.analytics.kotlin.core.*
import com.segment.analytics.kotlin.core.platform.DestinationPlugin
import com.segment.analytics.kotlin.core.platform.Plugin
import com.segment.analytics.kotlin.core.platform.plugins.logger.log
import com.segment.analytics.kotlin.core.utilities.*
import com.segment.analytics.kotlin.core.utilities.getDouble
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.math.BigDecimal
import java.time.*
import java.util.*


private const val mAutomaticInAppMessageRegistrationEnabled = false
private val mContext: Context? = null

// Only used for testing
private val mBraze: IAppboy? = null

private const val DEFAULT_CURRENCY_CODE = "USD"
private const val APPBOY_KEY = "Appboy"
private val MALE_TOKENS: Set<String> = HashSet(
    Arrays.asList(
        "M",
        "MALE"
    )
)
private val FEMALE_TOKENS: Set<String> = HashSet(
    Arrays.asList(
        "F",
        "FEMALE"
    )
)

private const val API_KEY_KEY = "apiKey"
private const val REVENUE_KEY = "revenue"
private const val CURRENCY_KEY = "currency"

private val RESERVED_KEYS = Arrays.asList(
    "birthday", "email", "firstName",
    "lastName", "gender", "phone", "address", "anonymousId", "userId"
)

@Serializable
data class BrazeSettings(
    var brazeDevKey: String,
    var limitedDataUse: Boolean = false,
    var trackScreenEvents: Boolean = false,
    var trackPageEvents: Boolean = false,
    var trackAttributionData: Boolean = false
)

class BrazeDestination(
    private val context: Context,
) : DestinationPlugin(), AndroidLifecycle {

    internal var settings: BrazeSettings? = null

    override val key: String = "Braze Events"

    override val braze: Braze = null

    override fun update(settings: Settings, type: Plugin.UpdateType) {
        super.update(settings, type)
        if (settings.hasIntegrationSettings(this)) {

            analytics.log("Braze Destination is enabled")
            this.settings = settings.destinationSettings(key)


            if (type == Plugin.UpdateType.Initial) {
                braze = Braze.getInstance(context)
                analytics.log("Braze Destination loaded")
            }
        } else {
            analytics.log("Braze destination is disabled via settings")
        }
    }


    override fun track(payload: TrackEvent): BaseEvent? {
        super.track(payload)
        if (payload == null) {
            return payload
        }

        val userId = payload.userId

        val eventName = payload.event

        val settings = settings ?: return payload

        val properties: JsonArray = payload.properties

        try {
            if (eventName == "Install Attributed") {
                val campaignProps: JsonObject? = properties["campaign"] as JsonObject?
                val currentUser: BrazeUser? = getInternalInstance()?.getCurrentUser()
                if (campaignProps != null && currentUser != null) {
                    currentUser.setAttributionData(
                        AttributionData(
                            campaignProps?.getString("source"),
                            campaignProps?.getString("name"),
                            campaignProps?.getString("ad_group"),
                            campaignProps?.getString("ad_creative")
                        )
                    )
                }
                return payload
            }
        } catch (exception: Exception) {
            analytics.log(
                "This Install Attributed event is not in the proper format and cannot be"
                        + " logged. The exception is " + exception + "."
            )
        }

        val revenueObject: JsonObject? = properties.toContent(REVENUE_KEY)
        val revenue: Double? = revenueObject.getDouble(REVENUE_KEY)
        if (revenue != 0.0 || eventName == "Order Completed" || eventName == "Completed Order") {
            val currencyCode: String? =
                if (isNullOrBlank(properties.toString(CURRENCY_KEY)))
                    DEFAULT_CURRENCY_CODE
                else properties.toString(CURRENCY_KEY)

            if (properties.toString("products") != null) {
                for (product in properties!!.toString("products")) {
                    logPurchaseForSingleItem(
                        product.toString("id"),
                        currencyCode,
                        BigDecimal.valueOf(product.toString("price"),
                        properties
                    )
                }
            } else {
                logPurchaseForSingleItem(
                    eventName,
                    currencyCode,
                    BigDecimal.valueOf(revenue!!),
                    properties
                )
            }
        } else {
            if (properties == null || properties.size == 0) {
                analytics.log(String.format("Calling braze.logCustomEvent for event %s", eventName))
                getInternalInstance()?.logCustomEvent(eventName)
            } else {
                analytics.log(
                    String.format("Calling braze.logCustomEvent for event %s with properties %s.",
                    eventName, properties.toString()
                ))
                getInternalInstance()?.logCustomEvent(eventName, BrazeProperties(properties))
            }
        }
        return payload
    }


    override fun identify(payload: IdentifyEvent): BaseEvent? {

        val userId: String = payload.userId
        if (!isNullOrBlank(userId)) {
            getInternalInstance()?.changeUser(userId)
        }

        val traits: JsonArray = payload.traits

        val name: String = traits.toString("name")

        val currentUser: BrazeUser? = getInternalInstance()?.getCurrentUser()
        if (currentUser == null) {
            analytics.log("Braze.getCurrentUser() was null, aborting identify")
            return payload
        }


        val birthdayString: String = traits.toString("birthday")
        val birthday: ZonedDateTime = ZonedDateTime(birthdayString)

        if (birthday != null) {
            val birthdayCal = Calendar.getInstance(Locale.US)
            birthdayCal.time = birthday
            currentUser.setDateOfBirth(
                birthdayCal[Calendar.YEAR],
                Month.values()[birthdayCal[Calendar.MONTH]],
                birthdayCal[Calendar.DAY_OF_MONTH]
            )
        }


        val email: String? = traits.toString("email")
        if (!isNullOrBlank(email)) {
            currentUser.setEmail(email)
        }

        val firstName: String? = traits.toString("firstName")
        if (!isNullOrBlank(firstName)) {
            currentUser.setFirstName(firstName)
        }

        val lastName: String? = traits.toString("lastName")
       if (!isNullOrBlank(lastName)) {
            currentUser.setLastName(lastName)
        }

        val gender: String? = traits.toString("gender")
        if (!isNullOrBlank(gender)) {
            if (MALE_TOKENS.contains(
                    gender?.uppercase(
                        Locale.getDefault()
                    )
                )
            ) {
                currentUser.setGender(Gender.MALE)
            } else if (FEMALE_TOKENS.contains(
                    gender?.uppercase(
                        Locale.getDefault()
                    )
                )
            ) {
                currentUser.setGender(Gender.FEMALE)
            }
        }

        val phone: String? = traits.toString("phone")
        if (!isNullOrBlank(phone)) {
            currentUser.setPhoneNumber(phone)
        }


        val address: JsonArray? = traits.toJSONArray("address")
        if (address != null) {
            val city: String? = address.toString("city")
            if (!isNullOrBlank(city)) {
                currentUser.setHomeCity(city)
            }
            val country: String? = address.toString("country")
            if (!isNullOrBlank(country)) {
                currentUser.setCountry(country)
            }
        }

        for (key: String? in traits.toString("keySet")) {
            if (RESERVED_KEYS.contains( key )) {
                analytics.log(String.format("Skipping reserved key %s", key))
                continue
            }
            val value: Any? = traits[key]
            if (value is Boolean) {
                currentUser.setCustomUserAttribute(key!!, (value as Boolean?)!!)
            } else if (value is Int) {
                currentUser.setCustomUserAttribute(key!!, (value as Int?)!!)
            } else if (value is Double) {
                currentUser.setCustomUserAttribute(key!!, (value as Double?)!!)
            } else if (value is Float) {
                currentUser.setCustomUserAttribute(key!!, (value as Float?)!!)
            } else if (value is Long) {
                currentUser.setCustomUserAttribute(key!!, (value as Long?)!!)
            } else if (value is Date) {
                val secondsFromEpoch = value.time / 1000L
                currentUser.setCustomUserAttributeToSecondsFromEpoch(key!!, secondsFromEpoch)
            } else if (value is String) {
                currentUser.setCustomUserAttribute(key!!, (value as String?)!!)
            } else if (value is Array<*>) {
                currentUser.setCustomAttributeArray(key!!, (value as Array<String?>?)!!)
            } else if (value is List<*>) {
                val valueArrayList = ArrayList(value as Collection<Any>?)
                val stringValueList: MutableList<String> = ArrayList()
                for (objectValue: Any? in valueArrayList) {
                    if (objectValue is String) {
                        stringValueList.add(objectValue)
                    }
                }
                if (stringValueList.size > 0) {
                    val arrayValue = arrayOfNulls<String>(stringValueList.size)
                    currentUser.setCustomAttributeArray(
                        key!!,
                        arrayValue
                    )
               }
            } else {
                analytics.log(
                    String.format("Braze can't map segment value for custom Braze user "
                            + "attribute with key %s and value %s", key, value
                ))
            }
        }

        return payload
    }


    override fun group(payload: GroupEvent): BaseEvent? {

        val type = payload.type

        val userId: String = payload.userId
        if (!isNullOrBlank(userId)) {
            getInternalInstance()?.changeUser(userId)
        }

        val groupIdKey: String = 'groupId' + payload.groupId
        if (!isNullOrBlank(groupIdKey)) {
            getInternalInstance()?.changeUser(groupIdKey)
        }

        val currentUser: BrazeUser? = getInternalInstance()?.getCurrentUser()
        if (currentUser == null) {
            analytics.log("Braze.getCurrentUser() was null, aborting identify")
            return payload
        }


        val messageId = payload.messageId

        val timeStamp : String? = payload.timestamp
        if (!isNullOrBlank(timeStamp)) {
        }
        val timeStamp: Date = payload.timestamp
        if (timeStamp != null) {
            val timeStampCal = Calendar.getInstance(Locale.US)
            timeStampCal.time = timeStamp
            currentUser.setDateOfBirth(
                timeStampCal[Calendar.YEAR],
                Month.values()[timeStampCal[Calendar.MONTH]],
                timeStampCal[Calendar.DAY_OF_MONTH]
            )
        }

        val email : String? = payload.toString("email")
        if (!isNullOrBlank(email)) {
            currentUser.setEmail(email)
        }

        val projectId: String = payload.toString("projectId")

        val traits: JsonObject = payload.traits
        for( trait in traits) {

        }

        return payload
    }



    companion object {
        // Rules for transforming a track event name
        private val TRAIT_MAPPER = mapOf(
            "firstName" to "first_name",
            "lastName" to "last_name",
            "birthday" to "dob",
            "avatar" to "image_url",
            "address.city" to "home_city",
            "address.country" to "home_country",
            "gender" to "gender"
        )
    }

    fun getUnderlyingInstance(): Appboy? {
        return if (mContext != null) {
            Braze.getInstance(mContext) as Appboy
        } else {
            mBraze as Appboy?
        }
    }

    fun getInternalInstance(): IAppboy? {
        return if (mContext != null) {
            Braze.getInstance(mContext)
        } else {
            mBraze
        }
    }

    override fun flush() {
        super.flush()
        analytics.log("Calling braze.requestImmediateDataFlush().")
        getInternalInstance()!!.requestImmediateDataFlush()
    }


    override fun onActivityStarted(activity: Activity?) {
        super.onActivityStarted(activity)
        getInternalInstance()!!.openSession(activity)
    }

    override fun onActivityStopped(activity: Activity?) {
        super.onActivityStopped(activity)
        getInternalInstance()!!.closeSession(activity)
    }

    override fun onActivityResumed(activity: Activity?) {
        super.onActivityResumed(activity)
        if (mAutomaticInAppMessageRegistrationEnabled) {
            BrazeInAppMessageManager.getInstance().registerInAppMessageManager(activity)
        }
    }

    override fun onActivityPaused(activity: Activity?) {
        super.onActivityPaused(activity)
        if (mAutomaticInAppMessageRegistrationEnabled) {
            BrazeInAppMessageManager.getInstance().unregisterInAppMessageManager(activity)
        }
    }


    @VisibleForTesting
    fun logPurchaseForSingleItem(
        productId: String?,
        currencyCode: String?,
        price: BigDecimal?,
        propertiesJson: JsonObject?
    ) {
        if (propertiesJson == null || propertiesJson.size == 0) {
            analytics.log(
                String.format("Calling braze.logPurchase for purchase %s for %.02f %s"
                    + " with no properties.", productId, price, currencyCode))
            getInternalInstance()!!.logPurchase(productId, currencyCode, price)
        } else {
            analytics.log(
                String.format("Calling braze.logPurchase for purchase %s for %.02f %s"
                        + " with properties %s.",
                    productId, price, currencyCode, propertiesJson.toString()))
            getInternalInstance()!!.logPurchase(
                productId,
                currencyCode,
                price,
                BrazeProperties(propertiesJson)
            )
        }
    }
}

