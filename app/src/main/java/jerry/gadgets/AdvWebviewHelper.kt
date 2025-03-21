package jerry.gadgets

import android.Manifest
import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.Message
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import im.delight.android.webview.AdvancedWebView


private val logger = Logger("ADV_WV_HELPER")

typealias WvNewWindowRequestHandler = (view: WebView?, isDialog: Boolean, isUserGesture: Boolean)->Boolean

class AdvWebviewHelper(private val activity: Activity,
                       private val webView: AdvancedWebView,
                       private val pageLoadingAware: WvPageLoadingAware?,
                       private val fullScreenVideoSupport: WvFullScreenVideoSupport?,
                       private val newWindowReqHdl:WvNewWindowRequestHandler?,
                       private val downloadReqHdl:WvDownloadRequestAware?,
                       private val microphonSupport:WvMicroPhoneAccessSupport? = null) {

    interface WvPageLoadingAware {
        fun onPageStarted(url: String?)
        fun onPageFinished(url: String?)
        fun onNewLoadRequest(url: String?): WebResourceResponse?
        fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean
    }

    interface WvFullScreenVideoSupport {
        fun onShowCustomView (view: View?,  callback: WebChromeClient.CustomViewCallback?)
        fun getVideoLoadingProgressView():View?
        fun onHideCustomView()
    }

    interface WvDownloadRequestAware {
        fun onDownloadRequested(url: String,
                                suggestedFilename: String,
                                mimeType: String,
                                contentLength: Long,
                                contentDisposition: String,
                                userAgent: String)
    }

    interface WvMicroPhoneAccessSupport {
        fun onMicroPhoneAudioAccessRequest(origin: WebView, hasAndroidPermission: Boolean) : Boolean
    }


    val wvId:String = "AdvWebView[${webView.hashCode()}]"

    var chromeClient:WebChromeClient? = null

    val isFirstItemInPageHistory:Boolean get() {
        val canGoBackMoreThan2 = webView.canGoBackOrForward(-2)
        if (canGoBackMoreThan2) {
            return false
        }

        val backForwardList = webView.copyBackForwardList()

        val c = backForwardList.size
        val pos = backForwardList.currentIndex

        if (c <= pos || pos <= 0) {
            return true
        }

        val originUrl = webView.originalUrl

        val item = backForwardList.getItemAtIndex(0)
        if (item.url == originUrl || item.originalUrl == originUrl) {
            return true
        }

        return true
    }

    fun enableBuildinZoom(on: Boolean) : AdvWebviewHelper {
        webView.settings.textZoom = 100
        // webView.settings.defaultFontSize = 16
        logger.info { "${webView.settings.textZoom} ${webView.settings.defaultFontSize}" }

        if (on) {
            webView.settings.setSupportZoom(true)
            webView.settings.builtInZoomControls = true
            //This will zoom out the WebView
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
            webView.setInitialScale(1)
        }

        return this
    }

    fun enableCookies() : AdvWebviewHelper {
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        logger.debug{"check cookie setting, accept cookie? ${cm.acceptCookie()}"}
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            logger.debug{"SDk version above android L so forcibly enabling ThirdPartyCookies"}
            cm.setAcceptThirdPartyCookies(webView, true)
        }
        cm.setCookie("https://a.b.c", "a=b")
        cm.flush()
        return this
    }

    fun checkCookies(url: String?): String? {
        if (url == null || url.isEmpty()) {
            return null
        }
        val cm = CookieManager.getInstance()
        return cm.getCookie(url)
    }

    fun flushCookies() {
        CookieManager.getInstance().flush()
    }


    fun setupListenersWithProgressBar(progressBar: ProgressBar, enablePopup: Boolean, closePopupOnUrlLoaded: PopWebviewWrapper.ClosePopupOnUrlLoaded?) : AdvWebviewHelper {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        settings.domStorageEnabled = true

        var ua = settings.userAgentString
        logger.debug{"original user agent: $ua"}
        ua = ua.replace("wv", "", ignoreCase = true)
        // ua = "Mozilla/5.0 (Linux; Android 9; KFONWI Build/PS7322; ) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/96.0.4664.92 Safari/537.36"
        // ua = "Mozilla/5.0 (X11; CrOS x86_64 10066.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.106 Safari/537.36"
        logger.debug{"modified user agent: $ua"}
        settings.userAgentString = ua

        webView.setMixedContentAllowed(true)
        if (enablePopup) {
            webView.settings.setSupportMultipleWindows(true)
        }

        chromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {

                if (request == null) {
                    return
                }

                for (permission in request.resources) {
                    logger.debug { "wv request permission of $permission" }

                    when (permission) {
                        "android.webkit.resource.AUDIO_CAPTURE" -> {
                            val hasPermission = activity.checkPermissions(
                                listOf(
                                    Manifest.permission.RECORD_AUDIO
                                )
                            )
                            logger.debug { "wv requested permission($permission) got? $hasPermission"  }
                            var grantOrigRequest: Boolean
                            if (!hasPermission) {
                                grantOrigRequest = microphonSupport?.onMicroPhoneAudioAccessRequest(webView, false)?:false
                            } else {
                                grantOrigRequest = microphonSupport?.onMicroPhoneAudioAccessRequest(webView, true)?:false
                            }
                            if (grantOrigRequest) {
                                request.grant(request.resources)
                            } else {
                                request.deny()
                            }
                            return
                        }
                    }
                }

                request.grant(request.resources)
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (newProgress >= 98) {
                    progressBar.visibility = View.INVISIBLE
                } else {
                    progressBar.visibility = View.VISIBLE
                }
                progressBar.progress = newProgress
            }

            override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
            ): Boolean {
                logger.debug{"create new popup window from hosting web-view: ${view}, url: ${view?.url}"}
                logger.debug{"is dialog? $isDialog, is user gesture? $isUserGesture"}

                val hdl = newWindowReqHdl

                if (hdl != null) {
                    return hdl(view, isDialog, isUserGesture)
                }

                return if (enablePopup) {
                    val wrapper = PopWebviewWrapper(mCtx = activity, mStartPage = null, mClosePage = closePopupOnUrlLoaded)
                    logger.debug { wrapper.mWebView.url }
                    val transport = resultMsg!!.obj as WebView.WebViewTransport
                    transport.webView = wrapper.mWebView
                    resultMsg.sendToTarget()
                    wrapper.showPopup()

                    true
                } else {
                    logger.debug{"block creating new popup window from hosting web-view: ${view},  url: ${view?.url}"}
                    false
                }
            }

            override fun onShowCustomView (view: View?,  callback: CustomViewCallback?) {
                super.onShowCustomView(view, callback) //not necessarily? empty in the parent.
                fullScreenVideoSupport?.onShowCustomView(view, callback)
            }
            override fun getVideoLoadingProgressView():View? {
                return fullScreenVideoSupport?.getVideoLoadingProgressView()
            }

            override fun onHideCustomView() {
                super.onHideCustomView()//not necessarily? empty in the parent.
                fullScreenVideoSupport?.onHideCustomView()
            }
        }
        webView.webChromeClient = chromeClient
        webView.webViewClient = object: WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val resp = pageLoadingAware?.onNewLoadRequest(request?.url.toString())
                if (null != resp) {
                    return resp
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                logger.debug{"do update visited history of wv: ${view},  url: ${url}, is reload? $isReload"}

                val backForwardList = view?.copyBackForwardList() ?: return

                logger.debug { "current item pos: ${backForwardList.currentIndex}" }

                val c = backForwardList.size
                for (i in 0 until c) {
                    val item = backForwardList.getItemAtIndex(i)
                    logger.debug { "item $i, url: ${item.url}" }
                    logger.debug { "item $i, original url: ${item.originalUrl}" }
                    logger.debug { "item $i, title: ${item.title}" }
                    logger.debug { "can go back ${i+1}, ${view.canGoBackOrForward(-(i+1))}" }
                }
            }
        }

        webView.setListener(activity, object : AdvancedWebView.Listener {
            override fun onPageStarted(url: String?, favicon: Bitmap?) {
                pageLoadingAware?.onPageStarted(url)

                val cookie = checkCookies(url)
                if (BuildConfig.DEBUG) {
                    logger.debug{"loading started: $url"}
                    logger.debug{"pre-loading, will use cookie? $cookie"}
                }
            }


            override fun onPageFinished(url: String?) {
                pageLoadingAware?.onPageFinished(url)
                flushCookies()

                val cookie = checkCookies(url)
                if (BuildConfig.DEBUG) {
                    logger.debug{"loading finished: $url"}
                    logger.debug{"post-loading, check cookie for url: $cookie"}
                }
            }

            override fun onPageError(errorCode: Int, description: String, failingUrl: String) {
                logger.debug{"loading error: $errorCode, $description, $failingUrl"}
            }
            override fun onDownloadRequested(
                    url: String,
                    suggestedFilename: String,
                    mimeType: String,
                    contentLength: Long,
                    contentDisposition: String,
                    userAgent: String
            ) {
                downloadReqHdl?.onDownloadRequested(url,
                    suggestedFilename,
                    mimeType,
                    contentLength,
                    contentDisposition,
                    userAgent
                )
            }

            override fun onExternalPageRequest(url: String) {
                logger.debug{"loading external page on url: $url"}
            }
        })

        return this
    }

}
