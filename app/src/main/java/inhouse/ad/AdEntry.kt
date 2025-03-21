package inhouse.ad

import com.google.gson.Gson
import jerry.gadgets.Logger
import java.util.regex.Pattern

private val logger = Logger("AD_ENTRY")

class AdEntry {
    var adItemId = ""
    var mediaType = AdMediaType.IMAGE
    var adTitle = ""
    var metaMapJson = "{}"
    var assetUrl = ""
    var adActionUrl = ""
    var adAction = AdActionType.StoreDeepLink
    var maxDisplay = 10
    var targetPkgId = ""

    var tvOnly = false

    var blacklistRule = BlacklistRule()

    override fun toString(): String {
        return Gson().toJson(this)
    }

    @Transient
    var debugMode = false
}

/**
 * All fields support  format.
 */
class BlacklistRule {
    var versionString = ""
    var versionCode = ""
    var deviceModel = ""
    var deviceName = ""
    var deviceProduct = ""
    var systemVersion = ""

    @Transient
    var versionStringPatterns = mutableListOf<Pattern>()
    @Transient
    var versionCodePatterns = mutableListOf<Pattern>()
    @Transient
    var deviceModelPatterns  = mutableListOf<Pattern>()
    @Transient
    var deviceNamePatterns  = mutableListOf<Pattern>()
    @Transient
    var deviceProductPatterns = mutableListOf<Pattern>()
    @Transient
    var systemVersionPatterns = mutableListOf<Pattern>()

    fun compilePatternMatchers() {
        val outputs = mapOf(
            versionStringPatterns to versionString,
            versionCodePatterns to versionCode,
            deviceModelPatterns to deviceModel,
            deviceNamePatterns to deviceName,
            deviceProductPatterns to deviceProduct,
            systemVersionPatterns to systemVersion,
        )

        outputs.forEach { (o, s) ->

            if (s.isEmpty() || s.isBlank()) {
                return@forEach
            }

            val csvArr = s.split(",")
            csvArr.forEach { ss ->
                try {
                    val p = Pattern.compile(ss.trim())
                    o.add(p)
                } catch (e:Throwable) {
                    logger.debug { "$ss ignored, pattern compile failure, $e" }
                }
            }
        }
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }
}

enum class AdMediaType {
    IMAGE, VIDEO, HTML,
}

enum class AdActionType {
    StoreDeepLink, WebLink,
}
