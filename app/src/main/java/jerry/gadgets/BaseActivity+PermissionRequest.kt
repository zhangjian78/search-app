package jerry.gadgets

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

var PERMISSION_REQ_CODE = 1111

fun Activity.checkReadExternalStoragePermission(): Boolean {
    return this.checkPermissions(
        listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    )
}

fun Activity.checkReadAndWriteExternalStoragePermission(): Boolean {
    return this.checkPermissions(
        listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    )
}

fun Activity.checkPermissions(permissions: List<String>) : Boolean {
    val notGranted = mutableListOf<String>()

    for (p in permissions) {
        if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
            notGranted.add(p)
        }
    }

    if (notGranted.isEmpty()) {
        return true
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        ActivityCompat.requestPermissions(
            this,
            notGranted.toTypedArray(),
            PERMISSION_REQ_CODE
        )
    }

    return false

}
