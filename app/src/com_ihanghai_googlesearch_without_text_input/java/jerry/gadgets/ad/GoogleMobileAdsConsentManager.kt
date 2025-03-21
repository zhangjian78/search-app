package jerry.gadgets.ad

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm.OnConsentFormDismissedListener
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import jerry.gadgets.BuildConfig

class GoogleMobileAdsConsentManager private constructor(context: Context) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    // 用于在同意收集完成时触发回调
    fun interface OnConsentGatheringCompleteListener {
        fun consentGatheringComplete(error: FormError?)
    }

    // 用于确定应用是否可以请求广告
    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    // 用于确定是否需要显示隐私选项表单
    val isPrivacyOptionsRequired: Boolean
        get() =
            consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * 用于调用 UMP SDK 的方法，请求同意信息并在必要时加载/显示同意表单。
     */
    fun gatherConsent(
        activity: Activity,
        onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener,
    ) {
        val debugSettings =
            ConsentDebugSettings.Builder(activity)
                // 出于测试目的，可以强制设置调试地理位置为 EEA 或 NOT_EEA。
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId(Admob.TEST_DEVICE_HASHED_IDS[0])
                .build()

        val params = ConsentRequestParameters.Builder().setConsentDebugSettings(debugSettings).build()

        // 每次应用启动时，都应请求更新同意信息
        // consentInformation.reset()
        if (BuildConfig.DEBUG) {
            consentInformation.reset()
        }
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    // 已经获取了用户同意
                    onConsentGatheringCompleteListener.consentGatheringComplete(formError)
                }
            },
            { requestConsentError ->
                onConsentGatheringCompleteListener.consentGatheringComplete(requestConsentError)
            },
        )
    }

    // 调用 UMP SDK 的方法显示隐私选项表单
    fun showPrivacyOptionsForm(
        activity: Activity,
        onConsentFormDismissedListener: OnConsentFormDismissedListener,
    ) {
        // [START present_privacy_options_form]
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onConsentFormDismissedListener)
        // [END present_privacy_options_form]
    }

    companion object {
        @Volatile private var instance: GoogleMobileAdsConsentManager? = null

        fun getInstance(context: Context) =
            instance
                ?: synchronized(this) {
                    instance ?: GoogleMobileAdsConsentManager(context).also { instance = it }
                }
    }
}