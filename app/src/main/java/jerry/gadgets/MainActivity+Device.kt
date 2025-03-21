package jerry.gadgets

import android.content.pm.PackageManager
import android.content.res.Configuration.*
import android.os.Build

enum class UiModeType {
    NORMAL,
    DESK,
    CAR,
    TV,
    APPLIANCE,
    WATCH,
    VR
}

fun BaseActivity.getUiModeType(): UiModeType {

    val modeType = mUiModeManager.currentModeType
    return when {
        modeType == UI_MODE_TYPE_APPLIANCE -> UiModeType.APPLIANCE
        modeType == UI_MODE_TYPE_CAR -> UiModeType.CAR
        modeType == UI_MODE_TYPE_DESK -> UiModeType.DESK
        modeType == UI_MODE_TYPE_TELEVISION -> UiModeType.TV
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && modeType == UI_MODE_TYPE_WATCH -> UiModeType.WATCH
        Build.VERSION.SDK_INT  >= Build.VERSION_CODES.O && modeType == UI_MODE_TYPE_VR_HEADSET -> UiModeType.VR
        isLikelyTelevision() -> UiModeType.TV
        modeType == UI_MODE_TYPE_NORMAL -> UiModeType.NORMAL
        else -> UiModeType.NORMAL
    }
}

fun BaseActivity.isReportedAsTelevision(): Boolean {
    return this.getUiModeType() == UiModeType.TV
}

fun BaseActivity.isLikelyTelevision(): Boolean {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) -> true
        Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP &&
                @Suppress("DEPRECATION")
                packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) -> true
        packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST) &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_ETHERNET) &&
                !packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) -> true
        else -> false
    }
}

fun BaseActivity.getDeviceIapDeviceTypeDef() : String {
    return if (this.isLikelyTelevision()) {
        "tv"
    } else {
        "tablet"
    }
}