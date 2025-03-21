package js.injection

import android.content.Context
import java.util.*

class JSLoader constructor(context: Context) {
    private var jsQueryCache: Hashtable<String, String> = Hashtable<String, String>()

    companion object {
        var hidden = false;
    }


    fun js(domain: String): String {
        val sb = StringBuilder()
        if (jsQueryCache.containsKey("common")) {
            sb.append(jsQueryCache["common"])

            sb.append("_hh_jsEnv.hideAd = $hidden;\n")
        }

        if (jsQueryCache.containsKey(domain)) {
            sb.append(jsQueryCache.get(domain))
        }

        return sb.toString()
    }

    init {
        load(context, "common")
        load(context, "youtube")
        load(context, "facebook")
        load(context, "twitter")
    }

    private fun load(context: Context, key: String) {
        var assetName = "${key}.js"
        if (!jsQueryCache.containsKey(key)) {
            val inputStream = context.assets.open(assetName)
            val bytes = ByteArray(inputStream.available())
            inputStream.read(bytes)
            inputStream.close()
            // println(String(bytes))
            jsQueryCache[key] = String(bytes)
        }
    }
}