package inhouse.ad

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object OurApps {

    fun openTabGmailApp(context: Context) {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("amzn://apps/android?p=com.ihanghai.gmail")
                )
            )
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.amazon.com/gp/product/B00D6B4PUG")
                )
            )
        }
    }

    fun hasTabGmailAppInstalled(context: Context) : Boolean {
        return checkAppInstalled(context, "com.ihanghai.gmail")
    }

    fun openGalaHDR(context: Context) {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("amzn://apps/android?p=team.apollo.vistapoint_tv")
                )
            )
        } catch (anfe: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.amazon.com/gp/product/B09D2JLWR4")
                )
            )
        }
    }

    fun hasGalaHDRInstalled(context: Context) : Boolean {
        return checkAppInstalled(context, "team.apollo.vistapoint_tv")
    }

    fun openDriveForGoogle(context: Context) {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("amzn://apps/android?p=com.voyagerapps.googledriver")
                )
            )
        } catch (anfe: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.amazon.com/gp/product/B00EJ9RIXA")
                )
            )
        }
    }

    fun open1Cast(context: Context) {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("amzn://apps/android?p=apollo.gadgets.atv_airplay")
                )
            )
        } catch (anfe: ActivityNotFoundException) {
            // context.startActivity(
            //     Intent(
            //         Intent.ACTION_VIEW,
            //         Uri.parse("https://www.amazon.com/gp/product/B00D6B4PUG")
            //     )
            // )
        }
    }

    fun openOneTube(context: Context) {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("amzn://apps/android?p=com.vtube.protube")
                )
            )
        } catch (anfe: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.amazon.com/gp/product/B00OCO0910")
                )
            )
        }
    }

    fun openSearchPlus(context: Context) {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("amzn://apps/android?p=com.ihanghai.googlesearchWithoutTextInput")
                )
            )
        } catch (anfe: ActivityNotFoundException) {
            anfe.printStackTrace()
        }
    }

    fun hasDriveForGoogleInstalled(context: Context) : Boolean {
        return checkAppInstalled(context, "com.voyagerapps.googledriver")
    }


    @Suppress("DEPRECATION")
    fun checkAppInstalled(context: Context, pkgName: String) : Boolean {
        return try {
            context.packageManager.getPackageInfo(pkgName, 0);
            true;
        } catch (e: PackageManager.NameNotFoundException) {
            false;
        }
    }

    fun parsePkgNameFromStoreUri(uri:String) : String? {
        val prefix = "amzn://apps/android?p="
        if (!uri.startsWith(prefix))
            return null

        return uri.substring(prefix.length).trim()
    }
}