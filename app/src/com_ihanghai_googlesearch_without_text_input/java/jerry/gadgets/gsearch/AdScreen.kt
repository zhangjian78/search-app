package jerry.gadgets.gsearch

import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import inhouse.ad.OurApps
import jerry.gadgets.R
import jerry.gadgets.databinding.ActivityAdScreenBinding


class AdScreen : AppCompatActivity() {
    private val binding: ActivityAdScreenBinding by lazy {
        ActivityAdScreenBinding.inflate(layoutInflater)
    }
    private var times = 4


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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
            binding.root.windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        }

        object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.wvLoadingAdCloseBtnCounter.text = "${times--}"
            }

            override fun onFinish() {
                binding.wvLoadingAdCloseBtnCounter.visibility = View.GONE
                binding.wvLoadingAdCloseBtnBg.visibility = View.VISIBLE
            }
        }.start()

        binding.wvLoadingAdCloseBtnBg.setOnClickListener {
            finish()
        }

        binding.ivAd.setOnClickListener {
            OurApps.openOneTube(this)
        }

        binding.tvDownload.setOnClickListener {
            OurApps.openOneTube(this)
        }

        binding.tvRemind.setOnClickListener {
            if (times <= 0) {
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        binding.tvDownload.requestFocus()
    }

    override fun onBackPressed() {
        println("times = $times")
        if (times > 0) {
        } else {
            super.onBackPressed()
        }
    }

    // override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    //     val keyCode = event.keyCode
    //     val action = event.action
    //
    //     if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
    //         keyCode == KeyEvent.KEYCODE_ENTER ||
    //         keyCode == KeyEvent.KEYCODE_Z) {
    //         if (action == KeyEvent.ACTION_UP) {
    //             binding.ivAd.performClick()
    //             return true
    //         }
    //     }
    //
    //
    //     return super.dispatchKeyEvent(event)
    // }
}