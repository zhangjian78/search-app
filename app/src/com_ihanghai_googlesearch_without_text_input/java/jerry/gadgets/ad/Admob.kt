package jerry.gadgets.ad

import android.os.Build
import android.util.Log
import android.view.WindowMetrics
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import jerry.gadgets.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class Admob(val activity: AppCompatActivity) {
    companion object {
        private const val TAG = "Admob"

        val BANNER_AD = 0
        val OPEN_AD = 1
        val INTERSTITIAL__AD = 2

        val AD_UNIT_INTERSTITIAL_ID = "ca-app-pub-2810561603769985/5889468575" // Interstitial
        val AD_UNIT_INTERSTITIAL_ID_TEST = "ca-app-pub-3940256099942544/1033173712" // Interstitial test
        val AD_UNIT_BANNER_ID = "ca-app-pub-2810561603769985/6090780563"
        val AD_UNIT_BANNER_ID_TEST = "ca-app-pub-3940256099942544/9214589741"
        val AD_UNIT_OPEN_ID = "ca-app-pub-2810561603769985~2465923419" // OPEN
        val AD_UNIT_OPEN_ID_TEST = "ca-app-pub-3940256099942544/9257395921" // OPEN

        // Check your logcat output for the test device hashed ID e.g.
        // "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("ABCDEF012345"))
        // to get test ads on this device" or
        // "Use new ConsentDebugSettings.Builder().addTestDeviceHashedId("ABCDEF012345") to set this as
        // a debug device".
        val TEST_DEVICE_HASHED_IDS = arrayOf(
            "FA4A36AE825E84BEB8E3FFC255A93CAF",
        )
    }

    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private val gatherConsentFinished = AtomicBoolean(false)
    private var mInterstitialAd: InterstitialAd? = null
    private var adView: AdView? = null

    private val adSize: AdSize
        get() {
            val displayMetrics = activity.resources.displayMetrics
            val adWidthPixels =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val windowMetrics: WindowMetrics = activity.windowManager.currentWindowMetrics
                    windowMetrics.bounds.width()
                } else {
                    displayMetrics.widthPixels
                }
            val density = displayMetrics.density
            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
        }

    private fun loadAdmob(callback: () -> Unit) {
        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(activity)
        // 插页广告
        googleMobileAdsConsentManager.gatherConsent(activity) { consentError ->
            if (consentError != null) {
                // Consent not obtained in current session.
                Log.w(TAG, "${consentError.errorCode}: ${consentError.message}")
            }

            if (googleMobileAdsConsentManager.canRequestAds) {
                _initializeMobileAdsSdk(callback)
            }
        }

        // This sample attempts to load ads using consent obtained in the previous session.
        if (googleMobileAdsConsentManager.canRequestAds) {
            _initializeMobileAdsSdk(callback)
        }
    }

    fun loadBanner(adContainer: FrameLayout, callback: () -> Unit) {
        loadAdmob {
            _loadBanner(adContainer)
            callback.invoke()
        }
    }

    fun hiddenBanner() {
        adView?.removeAllViews()
    }

    private fun loadInterstitialAd() {
        loadAdmob {
            _loadInterstitialAd()
        }
    }

    private fun _loadBanner(adContainer: FrameLayout) {
        val adView = AdView(activity)
        adView.adUnitId = if (BuildConfig.DEBUG) AD_UNIT_BANNER_ID_TEST else AD_UNIT_BANNER_ID
        adView.setAdSize(adSize)
        this.adView = adView

        adContainer.removeAllViews()
        adContainer.addView(adView)

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun _initializeMobileAdsSdk(callback: () -> Unit) {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        // Set your test devices.
        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(
                listOf(
                    AdRequest.DEVICE_ID_EMULATOR,
                    TEST_DEVICE_HASHED_IDS[0],
                )
            ) // 这里会输出实际设备的哈希 ID
            .build()

        MobileAds.setRequestConfiguration(requestConfiguration)

        CoroutineScope(Dispatchers.IO).launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(activity) {}
            activity.runOnUiThread {
                // Load an ad on the main thread.
                // loadInterstitialAd()
                // loadOpenAd()
                callback.invoke()
            }
        }
    }

    private fun _loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        // val adUnitID =
        //     if (BuildConfig.DEBUG) "ca-app-pub-2810561603769985/8451969616" else "ca-app-pub-3940256099942544/1033173712"
        val adUnitID = if (BuildConfig.DEBUG) {
            AD_UNIT_INTERSTITIAL_ID_TEST
        } else {
            AD_UNIT_INTERSTITIAL_ID
        }

        InterstitialAd.load(
            activity,
            adUnitID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { Log.d(TAG, it) }
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "InterstitialAd was loaded.")
                    mInterstitialAd = interstitialAd
                    // pause()
                    mInterstitialAd?.show(activity)
                }
            })

        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "InterstitialAd was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                Log.d(TAG, "InterstitialAd dismissed fullscreen content.")
                mInterstitialAd = null
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                super.onAdFailedToShowFullScreenContent(adError)
                Log.e(TAG, "InterstitialAd failed to show fullscreen content.")
                mInterstitialAd = null
            }


            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "InterstitialAd recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "InterstitialAd showed fullscreen content.")
            }
        }
    }
}