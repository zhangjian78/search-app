package jerry.gadgets

import amz_adm.DeviceMessageManager
import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.*
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebChromeClient.CustomViewCallback
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import im.delight.android.webview.AdvancedWebView
import inhouse.ad.AdStoreService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import java.util.*



abstract class BaseActivity : AppCompatActivity() {
    open val logger = Logger("WV_BASE")

    val handler = Handler(Looper.getMainLooper())

    val ioScope = CoroutineScope(Dispatchers.IO)


    protected var mWebStartPageLoaded = false

    protected val mWebView: AdvancedWebView by lazy {findViewById(R.id.adv_webview)}
    protected val mWebViewProgressBar: ProgressBar by lazy {findViewById(R.id.adv_webview_progress)}

    val mUiModeManager by lazy { getSystemService(UI_MODE_SERVICE) as UiModeManager }
    /**
     * for full screen video.
     */
    protected var mCustomView: View? = null
    protected val mCustomViewHolder: FrameLayout by lazy {findViewById(R.id.wv_fullscreen_video_view)}
    protected val mUpgradeView: LinearLayout by lazy {findViewById(R.id.ll_upgrade)}
    protected var mCustomViewCallback: CustomViewCallback? = null

    protected var mWvHelper : AdvWebviewHelper? = null
    protected var mWebChromeClient: WebChromeClient? = null

    protected var mKbDetector: KeyboardStatusDetector? = null

    val mLoadingOverlay : ViewGroup by lazy {findViewById(R.id.wv_loading_overlay)}

    val mWvLoadingAniAndAdViewMgr : WvLoadingAniAndAdViewStateManager by lazy {
        val m = WvLoadingAniAndAdViewStateManager(this, this.adDbAndImprHistory)
        m.onShownListener = {
            //if the first loaded page has <input> tag,
            //switch between apps will cause keyboard to popup,
            //which is not desired.
            //here we hide the web-view if loading animation or AD
            //is active.
            mWebView.visibility = View.INVISIBLE
        }
        m.onHiddenListener = {
            mWebView.visibility = View.VISIBLE
            onWvStartPageShown(mWebView.url.toString())
        }
        m
    }

    protected var mVideoProgressView: View? = null

    protected var mOriginalOrientation = 0
    protected var mOriginalSystemUiVisibility = 0

    protected var mDisableFullScreenVideoRotationFix = false

    lateinit var adDbAndImprHistory: AdStoreService

    /**
     * for image based banner AD.
     */
    var mCornerImageBannerAdDeactivateTimer: Timer? = null
    val mRoot: FrameLayout by lazy { findViewById(R.id.main_root) }

    private var mDefaultWithFullScreenMode = false


    /**
     * for Amazon Device Messaging.
     */
    protected val mAmzMsgMgr : DeviceMessageManager by lazy {
        DeviceMessageManager(this)
    }

    /**
     * click simulation.
     */
    protected var simClickTime: Long = -1

    @JavascriptInterface
    open fun simulateClick(simulateClickData: String) {
        handler.post {
            logger.debug( simulateClickData )
            if (System.currentTimeMillis() - simClickTime < 0.2) {
                return@post
            }
            simClickTime = System.currentTimeMillis()

            var rect = JSONObject(simulateClickData)
            var x = rect.optInt("x", 0).dpToPx + 0f
            var y = rect.optInt("y", 0).dpToPx + 0f
            var width = rect.optInt("width", 0).dpToPx + 0f
            var height = rect.optInt("height", 0).dpToPx + 0f
            x += width / 2
            y += height / 2

            val downTime: Long = SystemClock.uptimeMillis()
            val eventTime: Long = SystemClock.uptimeMillis() + 100
            val metaState = 0
            var me: MotionEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                metaState
            )
            mWebView.dispatchTouchEvent(me)
            me = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_UP,
                x,
                y,
                metaState
            )
            mWebView.dispatchTouchEvent(me)
            logger.debug( "simulate clicking done." )
        }
    }

    protected val (Int).dpToPx: Int
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            mWebView.resources.displayMetrics
        ).toInt()

    @Suppress("DEPRECATION")
    private fun requestFullScreenMode() {
        val flags: Int = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        window.decorView.systemUiVisibility = flags

        val decorView: View = window.decorView
        decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                decorView.systemUiVisibility = flags
            }
        }

        if (Build.VERSION.SDK_INT >= 30) { // removal of status and navigation bar
            mWebView.windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        }
    }


    @Suppress("DEPRECATION")
    protected fun exitFullScreenMode() {
        val decorView: View = window.decorView
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    var mEnableSimpleAd = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.simpleAdDbInit(false)

        val adItem = if (mEnableSimpleAd) {
            this.adDbAndImprHistory.getNextAvailableAdItem()
        } else {
            null
        }
        logger.debug { "have ad item to render? $adItem" }
        if (adItem != null) {
            mWvLoadingAniAndAdViewMgr.mAdEntry = adItem
        } else {
            mWvLoadingAniAndAdViewMgr.mEnableAdMode = false
        }

        this.mWvLoadingAniAndAdViewMgr.prepareContent()

        if (this.mDefaultWithFullScreenMode) {
            this.requestFullScreenMode()
        }

        logger.debug("init wv-helper object.")
        this.initWebviewHelper()

        logger.debug("keyboard detector added.")
        mKbDetector = KeyboardStatusDetector()
        mKbDetector?.registerActivity(this)
        mKbDetector?.setVisibilityListener { kbVisible ->
            if (kbVisible) {
                logger.debug("keyboard on")

                onKeyboardActive()
            } else {
                logger.debug("keyboard off")
                mWebView.animate()?.translationY(0f)

                onKeyboardInactive()
            }
        }

        CiperUtil.applicationContext = this

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        // https://stackoverflow.com/questions/31433246/classcastexception-org-slf4j-impl-log4jloggeradapter-cannot-be-cast-to-ch-qos-l

        logger.info( "Device Model: " + Build.MODEL + "/" + Build.VERSION.SDK_INT )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && AppSettings.userAgentString.isNotBlank()) {
            mWebView.settings.userAgentString = AppSettings.userAgentString
        }

        if (this.mAmzMsgMgr.doCheckSetup()) {
            this.mAmzMsgMgr.doDeviceRegister();
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        logger.warn( "permission request code: $requestCode, got permissions: $permissions, results: $grantResults" )
    }

    open fun onKeyboardActive() {}

    open fun onKeyboardInactive() {}

    open fun wvCanLoadStartPage() : Boolean {
        return true
    }

    abstract fun wvEnablePopupWindow():Boolean

    /**
     * If null, the web-view helper will use a default handle
     * which is only capable of handling same-page-popup window.
     */
    abstract fun wvPopupWindowRequestHandler():WvNewWindowRequestHandler?

    /**
     * If null, nothing will happen if the hyperlink is pointed to downloadable content.
     */
    abstract fun wvDownloadRequestHandler(): AdvWebviewHelper.WvDownloadRequestAware?

    open fun wvMicrophoneAccessSupportHandler(): AdvWebviewHelper.WvMicroPhoneAccessSupport? {
        return null
    }

    /**
     * Return a lambda which can test if the loading animation can be closed,
     * by checking against loaded URL.
     */
    abstract fun wvLoadingAnimationCloseChecker():(String?)->Boolean

    /**
     * Return a lambda which can test the same-page-popup window can be closed automatically,
     * by checking against loaded URL.
     */
    abstract fun wvPopupAutoCloseChecker() : (String?, String)->Boolean

    abstract fun onWvPageStarted(url: String?)

    abstract fun onWvPageFinished(url: String?)

    open fun onWvNewLoadRequest(url: String?): WebResourceResponse? {
        logger.debug( "new load request to: $url" )
        return null
    }

    open fun onWvShouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        return false
    }

    abstract fun onWvStartPageShown(url: String?)

    abstract fun onWvFullScreenVideoViewVisibilityChange(visible:Boolean)

    fun markWvContentReadyToShow() {
        mWvLoadingAniAndAdViewMgr.mCanContinueToWebviewWithRealContentToShow = true
        if (mWvLoadingAniAndAdViewMgr.canAutoClose()) {
            mWvLoadingAniAndAdViewMgr.hideViewContainer()
        }
    }

    private fun createWvPageLoadingAwareCallback() : AdvWebviewHelper.WvPageLoadingAware {
        return object : AdvWebviewHelper.WvPageLoadingAware {
            override fun onPageStarted(url: String?) {
                onWvPageStarted(url)
            }

            override fun onPageFinished(url: String?) {
                onWvPageFinished(url)

                if (wvLoadingAnimationCloseChecker()(url)) {
                    handler.postDelayed({
                        markWvContentReadyToShow()
                    }, 1000L)
                }
            }

            override fun onNewLoadRequest(url: String?): WebResourceResponse? {
                return onWvNewLoadRequest(url)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return onWvShouldOverrideUrlLoading(view, url)
            }
        }
    }

    private fun createWvFullScreenSupport() : AdvWebviewHelper.WvFullScreenVideoSupport {
        return object : AdvWebviewHelper.WvFullScreenVideoSupport {

            @SuppressLint("SourceLockedOrientationActivity")
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                // if a view already exists then immediately terminate the new one
                if (mCustomView != null) {
                    callback?.onCustomViewHidden()
                    return
                }

                mCustomView = view
                mCustomView?.keepScreenOn = true
                mWebView.visibility = View.GONE
                mCustomViewHolder.visibility = View.VISIBLE
                mCustomViewHolder.addView(view)
                mCustomViewCallback = callback

                mOriginalSystemUiVisibility = window.decorView.systemUiVisibility
                mOriginalOrientation = requestedOrientation

                logger.debug("showing custom(video) view on top of web-view: $view, orientation: $mOriginalOrientation")

                onWvFullScreenVideoViewVisibilityChange(true)
                if (!mDisableFullScreenVideoRotationFix) {
                    //workaround to fix video player not scaled properly issue.
                    //the delay needs to be big enough between calls, or it will
                    //be ignored by ongoing operation.
                    Handler(Looper.getMainLooper()).postDelayed({
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }, 200)

                    Handler(Looper.getMainLooper()).postDelayed({
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }, 1200)
                }
            }

            override fun getVideoLoadingProgressView(): View? {
                if (mVideoProgressView == null) {
                    val inflater = LayoutInflater.from(applicationContext)
                    mVideoProgressView = inflater.inflate(R.layout.video_loading, null)
                }
                return mVideoProgressView
            }

            override fun onHideCustomView() {
                if (mCustomView == null)
                    return

                mWebView.visibility = View.VISIBLE
                mCustomViewHolder.visibility = View.GONE

                // Hide the custom view.
                mCustomView?.visibility = View.GONE
                mCustomView?.keepScreenOn = false

                // Remove the custom view from its container.
                mCustomViewHolder.removeView(mCustomView)
                mCustomViewCallback?.onCustomViewHidden()

                logger.debug("removing custom(video) view on top of web-view: $mCustomView")

                mCustomView = null
                mCustomViewCallback = null

                window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
                requestedOrientation = mOriginalOrientation

                onWvFullScreenVideoViewVisibilityChange(false)
            }

//                    override fun getDefaultVideoPoster(): Bitmap? {
//                        return BitmapFactory.decodeResource(resources, R.drawable.video_poster)
//                    }

        }
    }

    private fun initWebviewHelper() {

        this.mWvHelper = AdvWebviewHelper(
            activity = this,
            webView = mWebView,
            pageLoadingAware = this.createWvPageLoadingAwareCallback(),
            fullScreenVideoSupport = this.createWvFullScreenSupport(),
            newWindowReqHdl = this.wvPopupWindowRequestHandler(),
            downloadReqHdl = this.wvDownloadRequestHandler(),
            microphonSupport =  this.wvMicrophoneAccessSupportHandler(),
        ).setupListenersWithProgressBar(
            progressBar = mWebViewProgressBar,
            enablePopup = this.wvEnablePopupWindow(),
            closePopupOnUrlLoaded = this.wvPopupAutoCloseChecker(),

        ).enableBuildinZoom(true)

        this.mWebChromeClient = this.mWvHelper?.chromeClient
        this.mWebView.addJavascriptInterface(this, WV_JS_OBJNAME)
        // this.mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        this.mWebView.isFocusableInTouchMode = true
        this.mWvHelper?.enableCookies()
    }

    val WV_JS_OBJNAME = "androidCtx"

    @JavascriptInterface
    fun wvJsHello() {
        logger.debug("hello, invoked by javascript in the web-view context.")
    }

    @JavascriptInterface
    fun wvJsLog(msg: String) {
        if (msg.length > 1024) {
            // logger.warn("message too long, truncated below. actual length: ${msg.length}")
            // logger.warn(msg.substring(0, 1024))
            Log.d("WV_JS_LOG", msg.substring(0, 1024))
        } else {
            // logger.warn(msg)
            Log.d("WV_JS_LOG", msg)
            println("WV_JS_LOG $msg")
        }
    }

    @JavascriptInterface
    fun wvJsOpenSystemBrowser(msg: String) {
        handler.post {
            try {
                val intent = Intent(Intent.ACTION_VIEW).setData(Uri.parse(msg))
                startActivity(intent)
            } catch (e:Throwable) {

            } finally {

            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (mLoadingOverlay.visibility == View.VISIBLE) {
            mWebView.onResume()
            mWvLoadingAniAndAdViewMgr.onResume()

            if (!wvCanLoadStartPage()) {
                return
            }

            loadStartPageIfNeeded()
        }
    }

    open fun loadStartPageIfNeeded() {
        if (mWebStartPageLoaded) {
            return
        }

        // mWebView.loadUrl("about:blank")
        mWebStartPageLoaded = true
        handler.postDelayed({
            //enable again, work around over randomly cookie disabled bug
            mWvHelper?.enableCookies()
            val cookie = mWvHelper?.checkCookies(AppSettings.startPage)
            logger.debug("cookie: $cookie")

            mWebView.loadUrl(AppSettings.startPage)
            mWvLoadingAniAndAdViewMgr.showViewContainer()
        }, 200L)
    }


    @SuppressLint("NewApi")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (this.mDefaultWithFullScreenMode) {
            requestFullScreenMode()
        }
    }

    fun showPopupWebview(popupLink: String?, closePopup: PopWebviewWrapper.ClosePopupOnUrlLoaded?) : PopWebviewWrapper? {
        val link = popupLink ?: return null

        if (link.isBlank()) {
            return null
        }
        val pop =  PopWebviewWrapper(this, link, closePopup)
        pop.showPopup()
        return pop
    }


    override fun onPause() {
        if (mLoadingOverlay.visibility == View.VISIBLE) {
            mWebView.onPause()
            mWvLoadingAniAndAdViewMgr.onPause()
        }

        super.onPause()
    }

    override fun onStop() {
        if (mLoadingOverlay.visibility == View.VISIBLE) {
            if (mCustomView != null) {
                this.mWebChromeClient?.onHideCustomView()
            }
        }
        super.onStop()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (mCustomView != null) {
                this.mWebChromeClient?.onHideCustomView()
                return true
            }

            if ((mCustomView == null) && mWebView.canGoBack()) {
                mWebView.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }


    fun evaluateJsCodeInWebview(js:String, callback:(String)->Unit) {
        this.mWebView.post {
            this.mWebView.evaluateJavascript(js) {
                callback(it)
            }
        }
    }

    protected fun testHelloJsInject(callback:(String)->Unit) {
        val s = """
            (function(){
                $WV_JS_OBJNAME.wvJsHello();
                $WV_JS_OBJNAME.wvJsLog("can you see me?");
                return "hello world from js";
            })()
        """.trimIndent()

        this.evaluateJsCodeInWebview(s) {
            logger.debug( it )
            callback(it)
        }
    }


    protected fun testOpenNewWindowJsInject(callback:(String)->Unit) {
        val s = """
            (function(){
                var win = window.open("https://m.google.com", "_blank")
                return win + "_opened";
            })()
        """.trimIndent()

        this.evaluateJsCodeInWebview(s) {
            logger.debug( it )
            callback(it)
        }
    }

}
