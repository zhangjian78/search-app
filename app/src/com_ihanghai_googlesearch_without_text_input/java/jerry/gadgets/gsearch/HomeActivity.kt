package jerry.gadgets.gsearch

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import inhouse.ad.OurApps
import jerry.gadgets.R
import jerry.gadgets.databinding.ActivityAdScreenBinding
import jerry.gadgets.databinding.ActivityHomeBinding
import jerry.gadgets.isLikelyTelevision
import java.net.URLEncoder

class HomeActivity : AppCompatActivity() {
    private val binding: ActivityHomeBinding by lazy {
        ActivityHomeBinding.inflate(layoutInflater)
    }
    private val mainLooper: Looper = Looper.getMainLooper()
    private fun buildMainHandler(): Handler {
        return if (Build.VERSION.SDK_INT >= 28) Handler.createAsync(mainLooper) else try {
            Handler::class.java.getDeclaredConstructor(
                Looper::class.java,
                Handler.Callback::class.java,
                Boolean::class.javaPrimitiveType // async
            ).newInstance(mainLooper, null, true)
        } catch (ignored: NoSuchMethodException) {
            Handler(mainLooper)
        }
    }
    private var isBackPressedOnce = false

    private val mainHandler by lazy { buildMainHandler() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                hideKeyboardIfShown(binding.etSearch)
                val keyword = binding.etSearch.text.toString()
                if (keyword.isEmpty()) {
                    return@setOnEditorActionListener true
                }

                binding.etSearch.setSelection(binding.etSearch.text.length)
                startSearch(keyword)
                return@setOnEditorActionListener true
            }
            false
        }

        val sharedPreferences = getSharedPreferences("ADS", Context.MODE_PRIVATE)
        var times = sharedPreferences.getInt("POP_AD", 4)

        if (times > 0 && !OurApps.checkAppInstalled(this, "com.vtube.protube")) {
            times--
            val p = getSharedPreferences("ADS", Context.MODE_PRIVATE).edit()
            p.putInt("POP_AD", times)
            p.apply()
            startActivity(Intent(this, AdScreen::class.java))
        } else {
            if (isLikelyTelevision()) {
                mainHandler.postDelayed({
                    val view = binding.etSearch
                    val location = IntArray(2)
                    view.getLocationOnScreen(location)
                    performClick(view, location[0] + view.width / 2f, location[1] + view.height / 2f)
                }, 500L)
            }
        }
    }

    private fun startSearch(keyword: String) {
        val url = "https://www.google.com/search?q=${URLEncoder.encode(keyword, "UTF-8")}"
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("url", url)
        startActivity(intent)
    }

    private fun hideKeyboardIfShown(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // 判断输入法是否弹出
        if (imm.isActive) {
            // 隐藏输入法
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onResume() {
        super.onResume()

        // if (isLikelyTelevision()) {
        //     mainHandler.postDelayed({
        //         val view = binding.etSearch
        //         val location = IntArray(2)
        //         view.getLocationOnScreen(location)
        //         performClick(view, location[0] + view.width / 2f, location[1] + view.height / 2f)
        //     }, 500L)
        // }
    }

    private fun performClick(view: View, x: Float, y: Float) {
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

    override fun onBackPressed() {
        if (isBackPressedOnce) {
            super.onBackPressed()
        } else {
            isBackPressedOnce = true
            Toast.makeText(this, getString(R.string.back_hint), Toast.LENGTH_SHORT).show()
            mainHandler.postDelayed({ isBackPressedOnce = false }, 2000L)
        }
    }

    private fun isLikelyTelevision(): Boolean {
        return when {
            packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) -> true
            packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST) &&
                    packageManager.hasSystemFeature(PackageManager.FEATURE_ETHERNET) &&
                    !packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) -> true
            else -> false
        }
    }
}