package jerry.gadgets.gsearch


import amz_iap.DRMLicensingCallback
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.amazon.device.drm.LicensingService
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxSegment
import com.applovin.mediation.MaxSegmentCollection
import com.applovin.mediation.ads.MaxAdView
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.interfaces.ItemClickListener
import com.denzcoskun.imageslider.models.SlideModel
import com.github.ybq.android.spinkit.style.Wave
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.FirebaseDatabase
import inhouse.ad.OurApps
import jerry.gadgets.AdvWebviewHelper
import jerry.gadgets.AppSettings
import jerry.gadgets.BaseActivity
import jerry.gadgets.Logger
import jerry.gadgets.PopWebviewActionBeginLoadingPage
import jerry.gadgets.R
import jerry.gadgets.WvNewWindowRequestHandler
import jerry.gadgets.ad.Admob
import jerry.gadgets.isLikelyTelevision
import java.io.ByteArrayInputStream
import java.util.concurrent.Executors


class MainActivity : BaseActivity(), MaxAdViewAdListener {

    override val logger = Logger("TabGS")
    private var versionName = "1.0"
    private val ivHome: ImageView? by lazy { findViewById(R.id.iv_icon) }
    private var mBottomBannerAdView: View? = null
    private var popKeyboard = false

    private var cursor: ImageView? = null
    private var cursorXY = Point(0, 0)
    private val minCursorStepLength = 12
    private val maxCursorStepLength = 72
    private var cursorStepLength = minCursorStepLength
    private var keyDownTimes = 0 //通过按下的次数来判断光标移动的速度
    private var admob: Admob? = null
    private var lastKeyword = ""

    private var adView: MaxAdView? = null
    val applovin_sdk_key = "Pid3ekmJNfQQPIKSapToz0kOUF2tXbJb5TCF67O5kltyVs83GSq7tNkTVE550ZstU9S66VqLimI2v272JB2eLl"

    var mService: SearchService? = null
    var mBound = false
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            // We've bound to MyService, cast the IBinder and get MyService instance
            val binder: SearchService.LocalBinder = service as SearchService.LocalBinder
            mService = binder.getService()
            mBound = true
            bringServiceToForeground()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    /**
     * End of Vosk Integration
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        val isTv = isLikelyTelevision()
        // mEnableSimpleAd = !isTv
        mEnableSimpleAd = false

        logger.debug { "enable AD $mEnableSimpleAd, is TV? $isTv" }

        if (isTv) {
            AppSettings.startPage = "file:///android_asset/start/index_tv.html"
        } else {
            AppSettings.startPage = "file:///android_asset/start/index.html"
        }
        intent.getStringExtra("url")?.let {
            AppSettings.startPage = it
        }

        super.onCreate(savedInstanceState)

        val info: PackageInfo = packageManager.getPackageInfo(this.packageName, 0)
        versionName = info.versionName!!

        mWebView.requestFocus(View.FOCUS_DOWN)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        // AndroidBug5497Workaround.assistActivity(this)

        logger.info { "Device Model: " + Build.MODEL + "/" + Build.VERSION.SDK_INT }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // mWebView.settings.userAgentString = AppSettings.userAgentString
        }

        LicensingService.verifyLicense(this.applicationContext, DRMLicensingCallback())

        ivHome?.setOnClickListener {
            mWebView.loadUrl(AppSettings.startPage)
            mWebView.clearHistory()
        }

        // startService()

        popKeyboard = intent.getBooleanExtra("notification", false)

        // voice input related UI & logic
        val voiceInput = findViewById<EditText>(R.id.voice_input_text_field)
        val waveDrawable = Wave()
        waveDrawable.setBounds(0, 0, 100, 100)
        //noinspection deprecation
        waveDrawable.setColor(resources.getColor(R.color.amzYellow_D1))
        voiceInput.setCompoundDrawables(waveDrawable, null, null, null)

        cursor = layoutInflater.inflate(R.layout.cursor, null) as ImageView?
        val lp = FrameLayout.LayoutParams(26.dpToPx, 26.dpToPx)
        mRoot.addView(cursor, lp)

        FirebaseAnalytics.getInstance(this)
        // applovin()
        if (isTv) {
            cursor?.visibility = View.VISIBLE
            popKeyboard = true
        }
    }

    fun saveSearchKeywordRealtime(userId: String, keyword: String) {
        val database = FirebaseDatabase.getInstance().reference
        val searchId = database.child("user_searches").push().key ?: return

        val searchEntry = mapOf(
            "userId" to userId,
            "keyword" to keyword,
            "timestamp" to System.currentTimeMillis()
        )

        database.child("user_searches").child(searchId)
            .setValue(searchEntry)
            .addOnSuccessListener {
                println("搜索记录已存入 Realtime Database")
            }
            .addOnFailureListener { e ->
                println("存储搜索记录失败: $e")
            }
    }

    override fun onResume() {
        super.onResume()

        val metrics: DisplayMetrics = resources.displayMetrics
        cursorXY.x = metrics.widthPixels / 2
        cursorXY.y = metrics.heightPixels / 2
        updateCursor()

        // performClick(cursorXY.x.toFloat(), cursorXY.y.toFloat() - 42.dpToPx)
    }

    override fun onPause() {
        super.onPause()
        if (mBound) {
            unbindService(connection)
            mBound = false
        }
    }


    private fun startService() {
        val intent = Intent(this, SearchService::class.java)
        intent.action = SearchService.FOREGROUND_SERVICE
        ContextCompat.startForegroundService(
            this,
            intent
        )
    }

    private fun stopService() {
        val intent = Intent(this, SearchService::class.java)
        intent.action = SearchService.STOP_SERVICE
        startService(intent)
    }

    private fun bringServiceToForeground() {
        mService?.let {
            if (!it.isForeGroundService) {
                val intent = Intent(this, SearchService::class.java)
                intent.action = SearchService.FOREGROUND_SERVICE
                ContextCompat.startForegroundService(this, intent)
                mService!!.doForegroundThings()
            }
        }

    }

    private fun setupBottomBannerAdView() {

        mBottomBannerAdView = findViewById(R.id.bottom_ad_banner)

        val slider = mBottomBannerAdView?.findViewById<ImageSlider>(R.id.image_slider)
        val imageList = ArrayList<SlideModel>()
        imageList.add(SlideModel(R.drawable.ad1, "", ScaleTypes.CENTER_INSIDE))
        imageList.add(SlideModel(R.drawable.ad2, "", ScaleTypes.CENTER_INSIDE))
        imageList.add(SlideModel(R.drawable.ad3, "", ScaleTypes.CENTER_INSIDE))
        imageList.add(SlideModel(R.drawable.ad4, "", ScaleTypes.CENTER_INSIDE))
        slider?.setImageList(imageList)
        slider?.startSliding(3000)
        slider?.setItemClickListener(object : ItemClickListener {
            override fun onItemSelected(position: Int) {
                OurApps.openGalaHDR(this@MainActivity)
            }
        })
        val ivGmail = mBottomBannerAdView?.findViewById<ImageView>(R.id.iv_gmail)
        ivGmail?.setOnClickListener {
            OurApps.openTabGmailApp(this@MainActivity)
        }
        val ivGoogDrive = mBottomBannerAdView?.findViewById<ImageView>(R.id.iv_drive)
        ivGoogDrive?.setOnClickListener {
            OurApps.openDriveForGoogle(this@MainActivity)
        }

        if (hasPackage("team.apollo.vistapoint_tv")) {
            slider?.visibility = View.GONE
        }

        if (hasPackage("com.voyagerapps.googledriver")) {
            ivGoogDrive?.visibility = View.GONE
        }

        if (hasPackage("com.ihanghai.gmail")) {
            ivGmail?.visibility = View.GONE
        }

        if (isLikelyTelevision()) {
            hideBottomBannerAdView()
        }
    }

    private fun showBottomBannerAdView() {
        if (isLikelyTelevision()) {
            return
        }
        mBottomBannerAdView?.visibility = View.VISIBLE // 2025.01.09 不显示本地广告
    }

    private fun hideBottomBannerAdView() {
        val upgradeView = findViewById<LinearLayout>(R.id.ll_upgrade)
        upgradeView.visibility = View.GONE

        // val adBanner = findViewById<FrameLayout>(R.id.fl_admob)
        // adBanner?.removeAllViews()
        // adBanner?.visibility = View.GONE
        // admob?.hiddenBanner()
        mBottomBannerAdView?.visibility = View.GONE
    }

    override fun onKeyboardActive() {
        hideBottomBannerAdView()
    }

    override fun onKeyboardInactive() {
        showBottomBannerAdView()
    }

    override fun wvEnablePopupWindow(): Boolean {
        return false
    }

    override fun wvPopupWindowRequestHandler(): WvNewWindowRequestHandler? {
        return null
    }

    override fun wvDownloadRequestHandler(): AdvWebviewHelper.WvDownloadRequestAware? {
        return object : AdvWebviewHelper.WvDownloadRequestAware {
            override fun onDownloadRequested(
                url: String,
                suggestedFilename: String,
                mimeType: String,
                contentLength: Long,
                contentDisposition: String,
                userAgent: String
            ) {
                logger.debug("down request got, url: $url")
                logger.debug("down request got, suggest file name: $suggestedFilename")
                logger.debug("down request got, mime: $mimeType")
                logger.debug("down request got, content length: $contentLength")
                logger.debug("down request got, content disposition: $contentDisposition")
                logger.debug("down request got, userAgent: $userAgent")
                val uri = Uri.parse(url)
                logger.debug("open downloadable content with system browser: $uri")
                val browserIntent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(browserIntent)
            }
        }
    }

    override fun wvLoadingAnimationCloseChecker(): (String?) -> Boolean {
        return {
            true
        }
    }

    override fun wvPopupAutoCloseChecker(): (url: String?, actionType: String) -> Boolean {
        return { url, actionType ->
            if (actionType == PopWebviewActionBeginLoadingPage && url != null) {
                wvJsOpenSystemBrowser(url)
            }

            url != null
        }
    }

    override fun onWvShouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url?.contains("accounts.google.com") == true) {
            println("onWvShouldOverrideUrlLoading: $url")
            return true
        }
        return false
    }

    override fun onWvPageStarted(url: String?) {
        logger.info { "page load start: $url" }

        handler.postDelayed({
            if (url == null) {
                return@postDelayed
            }
            if (url.indexOf("file://") < 0) {
                hideBottomBannerAdView()
            }
        }, 100L)
    }

    @JavascriptInterface
    fun saveSearch(keyword: String) {
        if (lastKeyword == keyword) {
            return
        }
        kotlin.runCatching {
            saveSearchKeywordRealtime(
                Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID),
                keyword
            )
            lastKeyword = keyword
        }
    }


    @JavascriptInterface
    fun toggleNotification(b: Boolean) {
        if (b) {
            startService()
        } else {
            stopService()
        }
    }

    @JavascriptInterface
    fun openOurApps(name: String) {
        println("openOurApps -> $name")
        when (name) {
            "OneTube" -> {
                OurApps.openOneTube(this)
            }

            "1Cast" -> {
                OurApps.open1Cast(this)
            }

            "Search" -> {
                OurApps.openSearchPlus(this)
            }
        }
    }

    @JavascriptInterface
    fun enableDownload(b: String) {
        logger.debug { "enable download exec..." }
        handler.post {

        }
    }

    fun doQuery(q: String) {
        if (mWebView.url?.startsWith("file://") == true) {
            logger.debug { "set query string on home page." }

            val js = """
                   setSearchQuery('$q');
                   doSearch();
            """.trimIndent()
            evaluateJsCodeInWebview(js) {
                logger.debug { "set query string done: $it" }
            }
        } else {
            handler.post{
                val qEncoded = java.net.URLEncoder.encode(q, "utf-8")
                val url = "https://www.google.com/search?q=$qEncoded"
                mWebView.loadUrl(url)
            }
        }
    }

    override fun onWvPageFinished(url: String?) {
        logger.info { "page load finished: $url" }

        // if (isLikelyTelevision() && (url?.indexOf("file://") ?: -1) >= 0) { // 本地
        //     handler.postDelayed({
        //         val metrics: DisplayMetrics = resources.displayMetrics
        //         val x = metrics.widthPixels / 2f
        //         val y = metrics.heightPixels / 2f
        //
        //         performClick(mWebView, x, y - 42.dpToPx)
        //     }, 500L)
        // }

        val js = """
            var div = document.querySelector('div[class="Q3DXx Efnghe"]');
            if (div) {
                div.style = "display: none !important;"
            }
            
            div = document.getElementById("footcnt");
            if (div) {
                div.style = "display: none !important;"
            }
            
            div = document.getElementById("EOlPnc");
            if (div) {
                div.style = "display: none !important;"
            }
            
            div = document.getElementById("gb");
            if (div) {
                if (div.textContent.indexOf("Sign in") > -1) {
                    div.style = "display: none !important;"
                }
            }
            
        """.trimIndent()
        evaluateJsCodeInWebview(js) {}

        Uri.parse(url).getQueryParameter("q")?.let {
            if (it.isNotEmpty()) {
                saveSearch(it)
            }
        }
    }

    private fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain", "utf-8",
            ByteArrayInputStream("Sorry we don't support this feature yet".toByteArray())
        )
    }

    override fun onWvNewLoadRequest(url: String?): WebResourceResponse? {
        println("onWvNewLoadRequest $url")
        if (url?.contains("accounts.google.com") == true) {
            return createEmptyResponse()
        }
        return null
    }

    override fun onWvStartPageShown(url: String?) {
        handler.postDelayed({
            if (url == null) {
                return@postDelayed
            }
            if (url.indexOf("file://") >= 0) {
                // if (mBottomBannerAdView == null) {
                //     setupBottomBannerAdView()
                // }
                // showBottomBannerAdView()
            }

        }, 1500L)


        if (url == null || !url.startsWith("file://")) {
            return
        }

        if (popKeyboard) {
            val js = """
                    function _hh_simulateClick(skipButton) {
                    if (!window.androidCtx) {
                        return;
                    }

                    var rect = skipButton.getBoundingClientRect();
                    window.scrollTo(0, rect.top);
                    rect = skipButton.getBoundingClientRect();
                    var bounds = {
                        "x": Math.round(rect.left),
                        "y": Math.round(rect.top),
                        "width": Math.round(rect.width),
                        "height": Math.round(rect.height)
                    };
                    if (bounds.width > 0) {
                        window.androidCtx.simulateClick(JSON.stringify(bounds));
                    }
                }
                
                var area = document.querySelector("input");
                if (area) {
                    _hh_simulateClick(area)
                }
            """.trimIndent()
            evaluateJsCodeInWebview(js) {}
        }
    }

    override fun onWvFullScreenVideoViewVisibilityChange(visible: Boolean) {

    }

    override fun wvMicrophoneAccessSupportHandler(): AdvWebviewHelper.WvMicroPhoneAccessSupport {
        return object : AdvWebviewHelper.WvMicroPhoneAccessSupport {
            override fun onMicroPhoneAudioAccessRequest(
                origin: WebView,
                hasAndroidPermission: Boolean
            ): Boolean {
                return false
            }

        }
    }

    override fun attachBaseContext(newBase: Context?) {
        val newOverride = Configuration(newBase?.resources?.configuration)
        newOverride.fontScale = 1.0f
        applyOverrideConfiguration(newOverride)

        super.attachBaseContext(newBase)
    }

    private fun adjustFontScale(context: Context, configuration: Configuration) {
        if (configuration.fontScale != 1f) {
            configuration.fontScale = 1f
            val metrics: DisplayMetrics = context.resources.displayMetrics
            val wm: WindowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
            wm.getDefaultDisplay().getMetrics(metrics)
            metrics.scaledDensity = configuration.fontScale * metrics.density
            context.resources.updateConfiguration(configuration, metrics)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action

        // 返回键由上层处理
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return super.dispatchKeyEvent(event)
        }

        if (isMenu(keyCode)) {
            if (action == KeyEvent.ACTION_UP) {
                if (cursor?.visibility == View.GONE) {
                    cursor?.visibility = View.VISIBLE
                } else {
                    cursor?.visibility = View.GONE
                }
            }
            return true
        }

        if (isEnter(keyCode)) {
            if (action == KeyEvent.ACTION_UP) {
                mWvLoadingAniAndAdViewMgr.mLoadingOverlayAdTitleActionBtn.performClick()
            }
        }

        if (cursor?.visibility == View.GONE) {
            return super.dispatchKeyEvent(event)
        }

        mouseEvent(action, keyCode)
        return true
    }

    private fun mouseEvent(action: Int, keycode: Int) {
        val metrics: DisplayMetrics = resources.displayMetrics
        var maxWidth = metrics.widthPixels
        var maxHeight = metrics.heightPixels

        if (action == KeyEvent.ACTION_UP) {
            keyDownTimes = 0
            cursorStepLength = minCursorStepLength
        } else {
            keyDownTimes++
            if (keyDownTimes > 2) {
                cursorStepLength = minCursorStepLength * 2
            }
            // cursorStepLength += (keyDownTimes / 3) * minCursorStepLength * 2
            cursorStepLength = cursorStepLength.coerceIn(minCursorStepLength, maxCursorStepLength)
        }

        if (isUp(keycode)) {
            if (action == KeyEvent.ACTION_DOWN) {
                cursorXY.y -= cursorStepLength
                if (cursorXY.y < 42.dpToPx) {
                    scrollWebUp(true)
                }
                cursorXY.y = cursorXY.y.coerceAtLeast(42.dpToPx)
                updateCursor()
            }
        } else if (isDown((keycode))) {
            if (action == KeyEvent.ACTION_DOWN) {
                cursorXY.y += cursorStepLength
                if (cursorXY.y > maxHeight - 26.dpToPx) {
                    scrollWebDown(true)
                }
                cursorXY.y = cursorXY.y.coerceAtMost(maxHeight - 26.dpToPx)
                updateCursor()
            }
        } else if (isLeft(keycode)) {
            if (action == KeyEvent.ACTION_DOWN) {
                cursorXY.x -= cursorStepLength
                cursorXY.x = cursorXY.x.coerceAtLeast(0)
                updateCursor()
            }
        } else if (isRight((keycode))) {
            if (action == KeyEvent.ACTION_DOWN) {
                cursorXY.x += cursorStepLength
                cursorXY.x = cursorXY.x.coerceAtMost(maxWidth - 1)
                updateCursor()
            }
        } else if (isEnter(keycode)) {
            if (action == KeyEvent.ACTION_DOWN) {
                return
            }


            val view = findViewById<LinearLayout>(R.id.ll_upgrade)
            val location = IntArray(2)

            // 获取 View 在屏幕上的坐标
            view.getLocationOnScreen(location)

            // println("${view.x}, ${view.y}, ${view.width}, ${view.height} - ${location[0]} ${location[1]} - ${cursorXY}")
            val rect = Rect(location[0], location[1], location[0] + view.width, location[1] + view.height)
            // println(rect)
            if (view.visibility == View.VISIBLE && rect.contains(cursorXY.x, cursorXY.y)) {
                // view.requestFocus()
                // performClick(view, cursorXY.x.toFloat(), (cursorXY.y - view.y))
            } else {
                performClick(if (null == mCustomView) mWebView else mCustomView!!, cursorXY.x.toFloat(), cursorXY.y.toFloat() - 42.dpToPx)
            }
        } else if (isRewind(keycode)) {
            if (action == KeyEvent.ACTION_DOWN) {
                return
            }
            scrollWebUp()
        } else if (isForward(keycode)) {
            if (action == KeyEvent.ACTION_DOWN) {
                return
            }
            scrollWebDown()
        }
    }

    private fun updateCursor() {
        runOnUiThread {
            val lp = FrameLayout.LayoutParams(26.dpToPx, 26.dpToPx)
            lp.setMargins(cursorXY.x, cursorXY.y, 0, 0)
            cursor?.layoutParams = lp
        }
    }

    protected fun isUp(keycode: Int): Boolean {
        return keycode == KeyEvent.KEYCODE_DPAD_UP ||
                keycode == KeyEvent.KEYCODE_W
    }

    protected fun isDown(keycode: Int): Boolean {
        return keycode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keycode == KeyEvent.KEYCODE_S
    }

    protected fun isLeft(keycode: Int): Boolean {
        return keycode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keycode == KeyEvent.KEYCODE_A
    }

    protected fun isRight(keycode: Int): Boolean {
        return keycode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keycode == KeyEvent.KEYCODE_D
    }

    protected fun isEnter(keycode: Int): Boolean {
        return keycode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keycode == KeyEvent.KEYCODE_ENTER ||
                keycode == KeyEvent.KEYCODE_Z
    }

    protected fun isMenu(keycode: Int): Boolean {
        return keycode == KeyEvent.KEYCODE_MENU ||
                keycode == KeyEvent.KEYCODE_Q
    }

    protected fun isRewind(keycode: Int): Boolean {
        return keycode == KeyEvent.KEYCODE_MEDIA_REWIND
    }

    protected fun isForward(keycode: Int): Boolean {
        return keycode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
    }

    private fun scrollWebUp(tiny: Boolean = false) {
        val js = """
            var _top = parseInt(window.scrollY);
            if (_top > 0) {
                var step = parseInt(innerHeight * ${if (tiny) 2 else 4} / 5);
                _top -= step;
                _top = Math.max(0, _top);
                window.scrollTo({
                    top:_top,
                    left:0,
                    behavior: "smooth"
                });
            }
        """.trimIndent()
        evaluateJsCodeInWebview(js) {}
    }

    private fun scrollWebDown(tiny: Boolean = false) {
        val js = """
            if (document.body.clientHeight > innerHeight) {
                var step = parseInt(innerHeight * ${if (tiny) 2 else 4} / 5);
                var _top = parseInt(window.scrollY);
                if (_top < document.body.clientHeight - step) {
                    _top += step;
                    _top = Math.min(document.body.clientHeight - step, _top);
                    window.scrollTo({
                        top:_top,
                        left:0,
                        behavior: "smooth"
                    });
                }
            }
        """.trimIndent()
        evaluateJsCodeInWebview(js) {}
    }

    fun performClick(view: View, x: Float, y: Float) {
        Handler(mainLooper).post {
            var downTime: Long = SystemClock.uptimeMillis()
            var eventTime: Long = SystemClock.uptimeMillis() + 100
            val metaState = 0

            var me = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_MOVE,
                x,
                y,
                metaState
            )
            // view.dispatchTouchEvent(me)
            me.recycle()

            downTime = SystemClock.uptimeMillis()
            eventTime = SystemClock.uptimeMillis() + 100
            me = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                metaState
            )
            view.dispatchTouchEvent(me)
            me.recycle()

            downTime = SystemClock.uptimeMillis()
            eventTime = SystemClock.uptimeMillis() + 100
            me = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_UP,
                x,
                y,
                metaState
            )
            view.dispatchTouchEvent(me)
            me.recycle()
        }
    }

    private fun hasPackage(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_GIDS)
            true
        } catch (ex: java.lang.Exception) {
            false
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val configuration = Configuration(newConfig)
        adjustFontScale(applicationContext, configuration)
    }

    override fun onBackPressed() {
        if (mLoadingOverlay.visibility == View.VISIBLE) {
            if (mWvLoadingAniAndAdViewMgr.mLoadingOverlayCloseBtn.visibility == View.VISIBLE) {
                mWvLoadingAniAndAdViewMgr.hideViewContainer()
            }
            return
        }
        if (mWebView.canGoBack()) {
            mWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun applovin() {
        val executor = Executors.newSingleThreadExecutor();
        executor.execute {
            val initConfig = AppLovinSdkInitializationConfiguration.builder(applovin_sdk_key, this)
            initConfig.mediationProvider = AppLovinMediationProvider.MAX
            initConfig.segmentCollection = MaxSegmentCollection.builder().addSegment(MaxSegment(849, listOf(1, 3))).build()

            // Enable test mode by default for the current device. Cannot be run on the main thread.
            // val currentGaid = AdvertisingIdClient.getAdvertisingIdInfo(this).id
            // if (currentGaid != null) {
            //     initConfig.testDeviceAdvertisingIds = Collections.singletonList(currentGaid)
            // }

            // Initialize the SDK with the configuration
            AppLovinSdk.getInstance(this).initialize(initConfig.build()) { configuration ->
                println("AppLovin SDK initialized $configuration")
                createBannerAd()
            }

            executor.shutdown()
        }
    }

    private fun createBannerAd() {
        adView = findViewById(R.id.bannerAdView)
        adView?.loadAd()
    }

    // MAX Ad Listener
    override fun onAdLoaded(maxAd: MaxAd) {
        Log.d("AppLovin", "Interstitial loaded")
        adView?.visibility = View.VISIBLE
    }

    override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
        Log.d("AppLovin", "Interstitial failed to load with error: $error")
        adView?.visibility = View.GONE
    }

    override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
        Log.d("AppLovin", "Interstitial failed to display with error: $error")
        adView?.visibility = View.GONE
    }

    override fun onAdExpanded(p0: MaxAd) {
    }

    override fun onAdCollapsed(p0: MaxAd) {
    }

    override fun onAdDisplayed(maxAd: MaxAd) {
        Log.d("AppLovin", "Interstitial displayed")
        adView?.visibility = View.VISIBLE
    }

    override fun onAdClicked(maxAd: MaxAd) {}

    override fun onAdHidden(maxAd: MaxAd) {

    }
}
