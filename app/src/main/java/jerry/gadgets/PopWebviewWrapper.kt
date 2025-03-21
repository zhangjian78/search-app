package jerry.gadgets

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import im.delight.android.webview.AdvancedWebView


private val logger = Logger("WV_POPUP_WRAPPER")

val PopWebviewActionBeginLoadingPage = "PopWebviewAction_BeginLoadingPage"
val PopWebviewActionEndOfLoadingPage = "PopWebviewAction_EndOfLoadingPage"
val PopWebviewActionNewUrlLoadRequest = "PopWebviewAction_NewUrlLoadRequest"

enum class PopWebviewWindowState {
    Inited,
    Shown,
    Dismissed,
    Unknown,
}

class PopWebviewWrapper(
    private val mCtx: Activity,
    private val mStartPage: String?,
    var mClosePage: ClosePopupOnUrlLoaded?
) {


    fun interface ClosePopupOnUrlLoaded {
        fun closePopupOnUrl(u: String, loadingType: String): Boolean
    }

    private val mPopupRoot: View
    private val mPopupCloseBtn: ImageButton
    val mWebView: AdvancedWebView
    private val mWebViewProgressBar: ProgressBar

    private val mPopupWindow: PopupWindow

    var mWindowState = PopWebviewWindowState.Unknown

    init {
        // Initialize a new instance of LayoutInflater service
        val inflater =
            mCtx.getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Inflate the custom layout/view
        mPopupRoot = inflater.inflate(R.layout.popup_webview, null)
        mPopupCloseBtn = mPopupRoot.findViewById(R.id.popup_close)
        mWebView = mPopupRoot.findViewById(R.id.pop_webview)
        mWebViewProgressBar = mPopupRoot.findViewById(R.id.pop_webview_progress)

        // Initialize a new instance of popup window
        mPopupWindow = PopupWindow(mCtx)
        mPopupWindow.isFocusable = true
        mPopupWindow.contentView = mPopupRoot
        mPopupWindow.width = ViewGroup.LayoutParams.MATCH_PARENT
        mPopupWindow.height = ViewGroup.LayoutParams.MATCH_PARENT
        mPopupWindow.setBackgroundDrawable(
            ColorDrawable(
                Color.TRANSPARENT
            )
        )

        mWindowState = PopWebviewWindowState.Inited

        // Set an elevation value for popup window
        // Call requires API level 21
//        if(Build.VERSION.SDK_INT>=21){
//            mPopupWindow.setElevation(5.0f);
//        }

        // Set a click listener for the popup window close button
        mPopupCloseBtn.setOnClickListener {
            mPopupWindow.contentView.post {
                mPopupWindow.dismiss()
                mWindowState = PopWebviewWindowState.Dismissed
            }
        }


        val pageLoadingAware = object : AdvWebviewHelper.WvPageLoadingAware {
            override fun onPageStarted(url: String?) {
                logger.debug { "popup window page loading started: $url" }
                if (mClosePage == null || url == null)
                    return

                if (mClosePage?.closePopupOnUrl(url, PopWebviewActionBeginLoadingPage) == true) {

                    mPopupWindow.contentView.post {
                        mPopupWindow.dismiss()
                        mWindowState = PopWebviewWindowState.Dismissed
                    }
                }
            }

            override fun onPageFinished(url: String?) {
                logger.debug { "popup window page loading ended: $url" }
                if (mClosePage == null || url == null)
                    return

                if (mClosePage?.closePopupOnUrl(url, PopWebviewActionEndOfLoadingPage) == true) {

                    mPopupWindow.contentView.post {
                        dismiss()
                    }
                }
            }

            override fun onNewLoadRequest(url: String?): WebResourceResponse? {
                logger.debug { "popup window new load request to: $url" }


                if (mClosePage == null || url == null)
                    return null

                if (mClosePage?.closePopupOnUrl(url, PopWebviewActionNewUrlLoadRequest) == true) {

                    mPopupWindow.contentView.post {
                        dismiss()
                    }
                }
                return null
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false
            }
        }
        AdvWebviewHelper(
            mCtx,
            mWebView,
            pageLoadingAware,
            null,
            null,
            null
        ).setupListenersWithProgressBar(mWebViewProgressBar, false, null)
    }

    fun openPage(u: String) {
        mPopupRoot.postDelayed({
            mWebView.loadUrl(u)
        }, 200L)
    }

    fun showPopup() {
        logger.debug { "show popup window: $this" }

        /*
            public void showAtLocation (View parent, int gravity, int x, int y)
                Display the content view in a popup window at the specified location. If the
                popup window cannot fit on screen, it will be clipped.
                Learn WindowManager.LayoutParams for more information on how gravity and the x
                and y parameters are related. Specifying a gravity of NO_GRAVITY is similar
                to specifying Gravity.LEFT | Gravity.TOP.

            Parameters
                parent : a parent view to get the getWindowToken() token from
                gravity : the gravity which controls the placement of the popup window
                x : the popup's x location offset
                y : the popup's y location offset
        */
        // Finally, show the popup window at the center location of root relative layout
        mPopupWindow.showAtLocation(mCtx.findViewById(android.R.id.content), Gravity.CENTER, 0, 0)
        if (mStartPage != null) {
           openPage(mStartPage)
        }

        mWindowState = PopWebviewWindowState.Shown
    }

    fun dismiss() {
        mPopupWindow.dismiss()
        mWindowState = PopWebviewWindowState.Dismissed
    }

}
