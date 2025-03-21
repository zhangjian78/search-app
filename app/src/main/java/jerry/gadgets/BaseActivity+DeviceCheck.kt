package jerry.gadgets

import android.util.DisplayMetrics

fun BaseActivity.checkScreenDPI(): String {
    val ds = resources.displayMetrics.densityDpi
    return if (ds < DisplayMetrics.DENSITY_MEDIUM) {
        "LDPI"
    } else if (ds < DisplayMetrics.DENSITY_HIGH) {
        "MDPI"
    } else if (ds < DisplayMetrics.DENSITY_XHIGH) {
        "HDPI"
    } else if (ds < DisplayMetrics.DENSITY_XXHIGH) {
        "XHDPI"
    } else if (ds < DisplayMetrics.DENSITY_XXXHIGH) {
        "XXHDPI"
    } else {
        "XXXHDPI"
    }
}
