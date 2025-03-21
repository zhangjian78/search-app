package jerry.gadgets

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.core.view.children
import im.delight.android.webview.AdvancedWebView
import inhouse.ad.*
import java.io.File
import kotlin.concurrent.fixedRateTimer

private val logger = Logger("SIMPLE_AD")

class WvLoadingAniAndAdViewStateManager(private val mActivity: BaseActivity, private val mAdService: AdStoreService) {

    private val PREF_KEY_ENABLE_AD_MODE = "PREF_KEY_ENABLE_AD_MODE"

    private val sharedPref: SharedPreferences = mActivity.getPreferences(Context.MODE_PRIVATE)

    val mLoadingOverlay : ViewGroup by lazy {mActivity.findViewById(R.id.wv_loading_overlay)}
    val mLoadingOverlayVideo : VideoView by lazy {mActivity.findViewById(R.id.wv_loading_overlay_video)}
    val mLoadingOverlayImage : ImageView by lazy {mActivity.findViewById(R.id.wv_loading_overlay_image)}
    val mLoadingOverlayWebview : AdvancedWebView by lazy {mActivity.findViewById(R.id.wv_loading_overlay_html)}

    val mLoadingOverlayAdTitleText : TextView by lazy {mActivity.findViewById(R.id.wv_loading_ad_title_text)}
    val mLoadingOverlayAdTitleActionBtn : Button by lazy {mActivity.findViewById(R.id.wv_loading_ad_title_action_btn)}
    val mLoadingOverlayCloseBtnParent : ViewGroup by lazy {mActivity.findViewById(R.id.wv_loading_ad_close_btn_parent)}
    val mLoadingOverlayCloseBtn : Button by lazy {mActivity.findViewById(R.id.wv_loading_ad_close_btn)}
    val mLoadingOverlayCloseBtnBg : ImageView by lazy {mActivity.findViewById(R.id.wv_loading_ad_close_btn_bg)}
    val mLoadingOverlayCloseBtnCounterText : TextView by lazy {mActivity.findViewById(R.id.wv_loading_ad_close_btn_counter)}

    var mEnableAdMode = false
    var mCanContinueToWebviewWithRealContentToShow = false
    private var mLoadingVideoAnimationPlayed = false

    private val TIMER_SETTING_FOR_AD_MODE = 4
    private val TIMER_SETTING_FOR_NON_AD_MODE = 2

    private var mTimeLeftForDisplay = 0

    var onShownListener:()->Unit = {}
    var onHiddenListener:()->Unit = {}

    var mAdEntry:AdEntry? = null
    private var mAdEntryRendered = false

    fun onResume() {
        if (mLoadingOverlay.visibility == View.VISIBLE ) {
            // comment by zhj, 2025/2/13
            // mLoadingOverlayVideo.start()
        }

    }

    fun onPause() {
        // comment by zhj, 2025/2/13
        // mLoadingOverlayVideo.stopPlayback()
    }

    init {
        mEnableAdMode = sharedPref.getBoolean(PREF_KEY_ENABLE_AD_MODE, false)
       // mEnableAdMode = true
        mTimeLeftForDisplay = if (mEnableAdMode) {
            TIMER_SETTING_FOR_AD_MODE
        } else {
            TIMER_SETTING_FOR_NON_AD_MODE
        }
        logger.debug { "init, enable ad mode with loading animation? $mEnableAdMode, timer setting: $mTimeLeftForDisplay" }

        mLoadingOverlayCloseBtn.setOnClickListener {
            hideViewContainer()
        }
    }


    fun enableAdModeForNextBoot(adModeOn:Boolean) {
        val e  = sharedPref.edit()
        e.putBoolean(PREF_KEY_ENABLE_AD_MODE, adModeOn)
        e.apply()

        logger.debug { "enable AD mode for next boot? $adModeOn" }
    }



    fun prepareContent() {
        var ad = mAdEntry

        if (BuildConfig.DEBUG && mActivity.checkReadExternalStoragePermission()) {
            mEnableAdMode = (ad != null)
        } else {
            logger.debug {"not a debug build, will not activate preview AD logic."}
        }

        if (!mEnableAdMode) {
            logger.debug {"ad mode disabled, will not load any ads."}
            prepareLoadingAnimationVideo()
            return
        }

        if (ad == null) {
            prepareLoadingAnimationVideo()
            return
        }

        mAdService.appendImprHistoryNowOf(ad.adItemId)
        NamedExecutor.singleThreadExecutor.execute {
            mAdService.saveToCache()
        }

        when (ad.mediaType) {
            AdMediaType.VIDEO -> {

                val cachedAdAsset = mAdService.cachedFileOfAdRes(ad)
                if (cachedAdAsset == null || !cachedAdAsset.exists()) {
                    prepareLoadingAnimationVideo()
                    return
                }
                prepareAdVideo(ad, cachedAdAsset)
                mAdEntryRendered = true
            }
            AdMediaType.IMAGE -> {

                val cachedAdAsset = mAdService.cachedFileOfAdRes(ad)
                if (cachedAdAsset == null || !cachedAdAsset.exists()) {
                    prepareLoadingAnimationVideo()
                    return
                }

                prepareAdImage(ad, cachedAdAsset)
                mLoadingVideoAnimationPlayed = true
                mAdEntryRendered = true
            }
            AdMediaType.HTML -> {
                prepareAdWebpage(ad)
                mLoadingVideoAnimationPlayed = true
                mAdEntryRendered = true
            }
        }
    }

    private var autoCloseTimer = fixedRateTimer(name="loading_ani", daemon = true, initialDelay = 1000L, period = 1000L) {
        logger.debug { "timer working, current state: " +
                "ad mode? $mEnableAdMode, " +
                "web-view's main content ready? $mCanContinueToWebviewWithRealContentToShow, " +
                "animation video(if any) played? $mLoadingVideoAnimationPlayed, " +
                "time left for display? $mTimeLeftForDisplay, " +
                "has ad entry(json)? ${mAdEntry !=null}"
        }

        if (mTimeLeftForDisplay > 0 && mCanContinueToWebviewWithRealContentToShow) {
            mTimeLeftForDisplay--
        }

        Handler(Looper.getMainLooper()).post {
            when {
                canShowCountDownTimer() -> {
                    updateCountTimerVisibility(true)
                }
                canShowCloseBtn() -> {
                    updateCloseBtnVisibility(true)
                }
                else -> {
                    updateCloseBtnVisibility(false)
                    updateCountTimerVisibility(false)
                }
            }
        }

    }

    private fun updateCloseBtnVisibility(shown: Boolean) {
        mLoadingOverlayCloseBtnParent.children.forEach { v ->
            v.visibility = View.GONE
        }

        listOf(mLoadingOverlayCloseBtnParent, mLoadingOverlayCloseBtnBg, mLoadingOverlayCloseBtn).forEach { v ->
            if (shown) {
                v.visibility = View.VISIBLE
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateCountTimerVisibility(shown: Boolean) {
        mLoadingOverlayCloseBtnParent.children.forEach { v ->
            v.visibility = View.GONE
        }

        listOf(mLoadingOverlayCloseBtnParent, mLoadingOverlayCloseBtnCounterText).forEach { v ->
            if (shown) {
                v.visibility = View.VISIBLE
            }
        }

        mLoadingOverlayCloseBtnCounterText.text = "${mTimeLeftForDisplay}s"
    }

    private fun updateAndShowAdTitleIfNotBlank(title: String, actionUrl: String) {
        if (title.isBlank() || title.isEmpty()) {
            mLoadingOverlayAdTitleText.visibility = View.GONE
        } else {
            mLoadingOverlayAdTitleText.visibility = View.VISIBLE
            mLoadingOverlayAdTitleText.text = title
        }

        if (actionUrl.isBlank() || actionUrl.isEmpty()) {
            return
        }

        mLoadingOverlayAdTitleActionBtn.setOnClickListener {
            try {
                mLoadingVideoAnimationPlayed = true

                logger.debug { "using intent URL: $actionUrl" }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(actionUrl))
                mActivity.startActivity(intent)
            } catch (e:Throwable) {
                logger.debug { "can not execute AD action url: $actionUrl, error: $e" }
            } finally {

            }
        }
    }

    fun hideViewContainer() {
        if (mLoadingOverlay.visibility != View.VISIBLE) {
            return
        }

        // comment by zhj, 2025/2/13
        // mLoadingOverlayVideo.stopPlayback()
        mLoadingOverlay.visibility = View.GONE
        mLoadingOverlayAdTitleActionBtn.setOnClickListener { null }

        autoCloseTimer.cancel()
        autoCloseTimer.purge()

        logger.debug { "view group hidden, timer purged." }

        this.onHiddenListener()
    }

    fun showViewContainer() {

        mLoadingOverlay.visibility = View.VISIBLE
        // comment by zhj, 2025/2/13
        // mLoadingOverlayVideo.start()
        logger.debug { "view group shown." }

        this.onShownListener()
    }

    fun hasActiveUI() : Boolean {
        return mLoadingOverlay.visibility == View.VISIBLE
    }

    private fun prepareAdVideo(adEntry:AdEntry, cachedAssetVideo:File) {

        // modify by zhj, 2025/2/13 let mLoadingOverlayVideo gone
        mLoadingOverlayVideo.visibility = View.GONE
        mLoadingOverlayImage.visibility = View.GONE
        mLoadingOverlayWebview.visibility = View.GONE

        // comment by zhj, 2025/2/13
        // mLoadingOverlayVideo.setVideoURI(Uri.fromFile(cachedAssetVideo))
        // mLoadingOverlayVideo.setOnCompletionListener {
        //     mLoadingVideoAnimationPlayed = true
        //     logger.debug { "AD video asset media playback completed once." }
        //     if (mLoadingOverlay.visibility == View.VISIBLE) {
        //         mLoadingOverlayVideo.start()
        //     }
        // }

        updateAndShowAdTitleIfNotBlank(adEntry.adTitle, adEntry.adActionUrl)
    }


    private fun prepareAdWebpage(adEntry:AdEntry) {

        mLoadingOverlayVideo.visibility = View.GONE
        mLoadingOverlayImage.visibility = View.GONE
        mLoadingOverlayWebview.visibility = View.VISIBLE

        mLoadingOverlayWebview.loadUrl(adEntry.assetUrl, false)

        updateAndShowAdTitleIfNotBlank(adEntry.adTitle, adEntry.adActionUrl)
    }



    private fun prepareAdImage(adEntry:AdEntry, cachedAssetImage:File) {

        mLoadingOverlayVideo.visibility = View.GONE
        mLoadingOverlayImage.visibility = View.VISIBLE
        mLoadingOverlayWebview.visibility = View.GONE

        mLoadingOverlayImage.setImageURI(Uri.fromFile(cachedAssetImage))

        updateAndShowAdTitleIfNotBlank(adEntry.adTitle, adEntry.adActionUrl)
    }

    private fun prepareLoadingAnimationVideo() {
        val uri: Uri = Uri.parse("android.resource://" + mActivity.packageName + "/" + R.raw.animate)

        // modify by zhj, 2025/2/13 let mLoadingOverlayVideo gone
        mLoadingOverlayVideo.visibility = View.GONE
        mLoadingOverlayImage.visibility = View.GONE
        mLoadingOverlayWebview.visibility = View.GONE

        // comment by zhj, 2025/2/13
        // mLoadingOverlayVideo.setVideoURI(uri)
        // mLoadingOverlayVideo.setOnCompletionListener {
        //     mLoadingVideoAnimationPlayed = true
        //     logger.debug { "embedded media playback completed once." }
        //     mLoadingOverlayVideo.start()
        // }
    }

    private fun canShowCloseBtn() : Boolean {
        if (!mEnableAdMode) {
            return mCanContinueToWebviewWithRealContentToShow
        } else {
            if (!mCanContinueToWebviewWithRealContentToShow) {
                return false
            }

            if (mLoadingVideoAnimationPlayed && mTimeLeftForDisplay <= 0) {
                return true
            }

            return false
        }
    }

    private fun canShowCountDownTimer() : Boolean {
        if (canShowCloseBtn()) {
            return false
        }

        if (mCanContinueToWebviewWithRealContentToShow && mTimeLeftForDisplay > 0) {
            return true
        }

        return false
    }

    fun canAutoClose() : Boolean {
        return if (!mEnableAdMode) {
            mCanContinueToWebviewWithRealContentToShow
        } else {
            false
        }
    }

}

const val REMOTE_UPD_URL = "https://i.stwvoyager.app/sa/v1/${BuildConfig.APPLICATION_ID}.db?vn=${BuildConfig.VERSION_NAME}&vc=${BuildConfig.VERSION_CODE}"
const val REMOTE_UPD_URL_DEBUG = "https://i.stwvoyager.app/sa/v1/debug/${BuildConfig.APPLICATION_ID}.db?vn=${BuildConfig.VERSION_NAME}&vc=${BuildConfig.VERSION_CODE}"

fun BaseActivity.simpleAdDbInit(async:Boolean) {
    val f = {
        logger.debug { "init ad db." }

        val f = File(this.cacheDir, simpleAdDbRemoteConfigureKey())
        if (!f.exists()) {
            f.mkdirs()
            logger.debug { "directory created on $f" }
        } else {
            logger.debug { "directory existed already: $f"}
        }

        var dbURL = REMOTE_UPD_URL
        if (BuildConfig.DEBUG) {
            dbURL = REMOTE_UPD_URL_DEBUG
        }
        // adDbAndImprHistory = AdStoreService(f, this, dbURL)
        adDbAndImprHistory = AdStoreService(cacheBaseDir = f,
            context = this.applicationContext,
            isTv = isLikelyTelevision(),
            disabledAdTypes = setOf(AdMediaType.HTML, AdMediaType.VIDEO),
            remoteJsonUpdURL = dbURL)

        if (mEnableSimpleAd && adDbAndImprHistory.init() == AdDbOrigin.Asset) {
            logger.debug { "asset loaded from asset, enable AD mode."}
            mWvLoadingAniAndAdViewMgr.mEnableAdMode = true
        }

        mWvLoadingAniAndAdViewMgr.mEnableAdMode = mEnableSimpleAd
    }

    if (async) {
        NamedExecutor.singleThreadExecutor.execute {
            f()
        }
    } else {
        f()
    }
}

/**
 *
 * Key for retrieving AD data stored in remote config service and also
 * used as the cache name.
 *
 *
 */
fun BaseActivity.simpleAdDbRemoteConfigureKey() : String {
    val k = "simple_ad_v1"
    logger.debug { "AD db conf key for $this: $k" }
    return k
}
