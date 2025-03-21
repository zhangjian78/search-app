package jerry.gadgets

import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log2
import kotlin.math.pow

private val logger = Logger("DATA_HELPER")

val Int.formatAsFileSize: String
    get() = toLong().formatAsFileSize

val Long.formatAsFileSize: String
    get() = log2(if (this != 0L) toDouble() else 1.0).toInt().div(10).let {
        val precision = when (it) {
            0 -> 0; 1 -> 1; else -> 2
        }
        val prefix = arrayOf("", "K", "M", "G", "T", "P", "E", "Z", "Y")
        String.format("%.${precision}f ${prefix[it]}B", toDouble() / 2.0.pow(it * 10.0))
    }
val Long.formatAsMonthAndDay: String
    get()  {
        val d = Date(this)
        val fmt = SimpleDateFormat("MM-dd")
        return fmt.format(d)
    }

val Long.formatAsDayAndHour: String
    get()  {
        val d = Date(this)
        val fmt = SimpleDateFormat("dd/HH:mm")
        return fmt.format(d)
    }


object DataHelper {

    var cacheBaseDirectory:File? = null
    var localCacheDirectory: File? = null

    var inited = false

    fun initWithCacheDirectory(dir:File) {
        if (!inited) {
            inited = true
        } else {
            throw Error("double init.")
        }
        logger.debug{"cache directory set to: ${dir.absolutePath}"}
        cacheBaseDirectory = dir
        localCacheDirectory = File(dir, "media_cache")
    }


}
