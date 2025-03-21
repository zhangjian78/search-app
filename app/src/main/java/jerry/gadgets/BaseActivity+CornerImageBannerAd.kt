package jerry.gadgets

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.interfaces.ItemClickListener
import com.denzcoskun.imageslider.models.SlideModel
import java.util.*


fun BaseActivity.cornerImageBannerAdClickCntKey() : String {
    //this will make the counter reset on every release.
    return "CLICK_CNT_${BuildConfig.VERSION_CODE}"
}

fun BaseActivity.cornerImageBannerAdSaveClickCnt(newValue:Int) : Int {
    val k = cornerImageBannerAdClickCntKey()
    val p = getSharedPreferences(k, Context.MODE_PRIVATE).edit()
    p.putInt(k, newValue)
    p.apply()
    return newValue
}

fun BaseActivity.cornerImageBannerAdReadClickCnt() : Int {
    val k = cornerImageBannerAdClickCntKey()
    return getSharedPreferences(k, Context.MODE_PRIVATE).getInt(k, 0)
}

fun BaseActivity.cornerImageBannerAdShowAd(imageResIds: List<Int>, autoCloseDelay: Int, maxClicks: Int, clickHdl: () -> Unit) {
    if (imageResIds.isEmpty()) {
        return
    }

    logger.debug("version key: ${cornerImageBannerAdClickCntKey()} show corner AD, display density: ${this.checkScreenDPI()}")
    val clicks = cornerImageBannerAdReadClickCnt()
    if (!BuildConfig.DEBUG && clicks >= maxClicks) {
        logger.debug("corner AD clicked $clicks times, no action.")
        return
    }

    runOnUiThread {
        val ad = layoutInflater.inflate(R.layout.banner_ad, null)
        val frameLayoutParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        frameLayoutParams.apply {
            gravity = Gravity.END + Gravity.BOTTOM
        }

        val slider = ad.findViewById<ImageSlider>(R.id.image_slider)
        val ivClose = ad.findViewById<ImageView>(R.id.iv_close)
        ivClose?.setOnClickListener {
            cornerImageBannerAdHideAd(ad, clicks)
        }

        val imageList = ArrayList<SlideModel>()

        imageResIds.forEach {
            imageList.add(SlideModel(it, "", ScaleTypes.CENTER_CROP))
        }

        slider.setImageList(imageList)
        slider.startSliding(3000)
        slider.setItemClickListener(object : ItemClickListener {
            override fun onItemSelected(position: Int) {
                cornerImageBannerAdSaveClickCnt(clicks+1)
               clickHdl()
            }
        })

        if (autoCloseDelay <= 0) {
            return@runOnUiThread
        }

        mCornerImageBannerAdDeactivateTimer = Timer()
        mCornerImageBannerAdDeactivateTimer?.schedule(object : TimerTask() {
            override fun run() {
                logger.debug( "corner AD hidden by timer.")
                cornerImageBannerAdHideAd(ad, clicks)
            }
        }, autoCloseDelay * 1000L)

        mRoot.addView(ad, frameLayoutParams)
    }
}

fun BaseActivity.cornerImageBannerAdHideAd(ad: View, clicks: Int) {
    cornerImageBannerAdSaveClickCnt(clicks + 1)

    runOnUiThread{
        mCornerImageBannerAdDeactivateTimer?.cancel()
        mCornerImageBannerAdDeactivateTimer?.purge()
        mCornerImageBannerAdDeactivateTimer = null

        ad.visibility = View.GONE
        mRoot.removeView(ad)
    }
}
