package inhouse.ad


import android.content.Context
import android.os.Build
import com.google.gson.Gson
import jerry.gadgets.BuildConfig
import jerry.gadgets.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.math.BigInteger
import java.net.URL
import java.security.MessageDigest


private val logger = Logger(tag = "SAv1")

const val MAX_IMPR_HISTORY = 256
const val DB_ASSET_NAME = "adstore_asset_db.json"
const val DB_AND_IMPR_CACHE_NAME = "adstore_and_impr_db.json"
const val DB_AND_IMPR_CACHE_NAME_UPD = "adstore_and_impr_db.json.upd"

val ioScope = CoroutineScope(Dispatchers.IO)
val uiScope = CoroutineScope(Dispatchers.Main)

class SimpleAdDb {
    var ts = 0L
    var adEntries = mutableListOf<AdEntry>()

    fun entryOfItemId(itemId:String) : AdEntry? {
        return adEntries.find {
            it.adItemId == itemId
        }
    }
}

class ImprHistory {
    var adItemId = ""
    var presentedAt = 0L
}

class AdsAndHistory {
    var ads = SimpleAdDb()
    var history = mutableListOf<ImprHistory>()
    @Transient var imprCounters = mutableMapOf<String, Int>()

    fun updateCounters() {
        val counters = mutableMapOf<String, Int>()
        val copy = ArrayList(history)
        copy.forEach {
            counters[it.adItemId] = 0
        }

        copy.forEach {
            val itemId = it.adItemId
            val c = counters[itemId] ?: return@forEach
            counters[itemId] = c+1
        }

        imprCounters.clear()
        imprCounters.putAll(counters)
    }
}

enum class AdDbOrigin {
    Asset,
    Remote,
}

/**
 * if remoteJsonUpdURL is not empty, it will auto load it after initialization.
 */
class AdStoreService(private var cacheBaseDir: File = File("."),
                     private var context:Context,
                     private var isTv:Boolean = false,
                     private var disabledAdTypes:Set<AdMediaType> = setOf(),
                     private var remoteJsonUpdURL: String = "") {

    private var dbAndHistory = AdsAndHistory()

    fun init() : AdDbOrigin {
        var mode = AdDbOrigin.Remote
        if (!this.loadFromCache() && this.loadFromAsset()) {
            mode = AdDbOrigin.Asset
        }

        ioScope.launch {
            requestUpdate()
        }
        return mode
    }

    private fun download(link: String, path: String) {
        URL(link).openStream().use { input ->
            FileOutputStream(File(path)).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun requestUpdate() : Boolean {
        if (this.remoteJsonUpdURL.isBlank()) {
            return false
        }

        val downloadTo = File(this.cacheBaseDir, DB_AND_IMPR_CACHE_NAME_UPD)
        if (downloadTo.exists()) {
            downloadTo.delete()
        }

        return try {
            logger.info ( "enqueued db-json update request using: ${this.remoteJsonUpdURL}")
            download(this.remoteJsonUpdURL, downloadTo.absolutePath)

            val jsonText = downloadTo.readText()
            logger.info( "json db text:  $jsonText")
            this.updateDb(jsonText)
            this.saveToCache()

            true
        } catch (e:Exception) {
            logger.warn("error in update data: $e")
            false
        }
    }

    private fun loadFromAsset() : Boolean {
        try {
            val f = context.assets.open(DB_ASSET_NAME)
            logger.info ( "load ad db from asset ..." )

            val gson = Gson()
            val jsonStr = InputStreamReader(f).readText()
            logger.info ( "dump cached content: ")
            logger.info ( jsonStr )
            val assetDb = gson.fromJson(jsonStr, SimpleAdDb::class.java)
            this.dbAndHistory.ads = assetDb
            for (adEntry in dbAndHistory.ads.adEntries) {
                adEntry.blacklistRule.compilePatternMatchers()
            }
            this.updateDb(jsonStr)
            logger.info ( "restored entries from asset: ${dbAndHistory.ads.adEntries.size}" )

            //
            // copy asset resource into cache folder, so later it can
            // be used as normal files.
            //
            // keep the resource as small as possible, this will block main thread on
            // first boot.

            var hasCachedItem = false

            for (adEntry in dbAndHistory.ads.adEntries) {
                val cacheName = cachedFileNameOfAdRes(adEntry.assetUrl, adEntry.mediaType)
                val cachedCopy = File(this.cacheBaseDir, cacheName)
                if (cachedCopy.exists() && cachedCopy.length() > 0) {
                    hasCachedItem = true
                    continue
                }

                try {
                    val assetInputStream = context.assets.open(adEntry.assetUrl)
                    val cacheOutputStream = cachedCopy.outputStream()
                    IOUtil.copyStream(assetInputStream, cacheOutputStream)
                    hasCachedItem = true
                } catch (e:Throwable) {
                    logger.warn ( "failed to copy asset ad resource: $e" )
                }
            }

            return hasCachedItem
        } catch (e:Throwable) {
            logger.warn ( "failed to load from asset: $e" )
            return false
        }
    }

    private fun loadFromCache() : Boolean {
        try {
            val f = File(cacheBaseDir, DB_AND_IMPR_CACHE_NAME)
            logger.info ( "load ad db from $f ..." )
            if (!f.exists() || !f.canRead()) {
                logger.info ( "cache not existed, abort." )
                return false
            }

            val gson = Gson()
            val jsonStr = f.readText()
            logger.info ( "dump cached content: " )
            logger.info ( jsonStr )
            val cached = gson.fromJson(jsonStr, AdsAndHistory::class.java)
            this.dbAndHistory = cached
            this.dbAndHistory.ads.adEntries.forEach { e->
                e.blacklistRule.compilePatternMatchers()
            }
            logger.info ( "restored entries from cache: ${dbAndHistory.ads.adEntries.size}" )
            return true
        } catch (e:Throwable) {
            logger.warn ( "failed to load from cache: $e" )
            return false
        }
    }

    fun saveToCache() {
        try {
            val f = File(cacheBaseDir, DB_AND_IMPR_CACHE_NAME)
            logger.info ( "save ad db to $f ..." )
            val gson = Gson()
            val jsonStr = gson.toJson( this.dbAndHistory)
            logger.info ( "dump content: " )
            logger.info ( jsonStr )
            f.writeText(jsonStr)
            logger.info ( "saved, size: ${f.length()}" )
        } catch (e:Throwable) {
            logger.warn ( "failed to save to cache: $e" )
        }
    }

    private fun clearInstalledAppAdEntries(entries: List<AdEntry>) : MutableList<AdEntry> {
        val out = mutableListOf<AdEntry>()

        entries.forEach loop1@{  e ->
            var pkgId = e.targetPkgId

            if (pkgId.isBlank()) {
                pkgId = OurApps.parsePkgNameFromStoreUri(e.adActionUrl) ?: ""
            }

            logger.info ( "target package id set? $pkgId" )

            if (pkgId.isBlank()) {
                out.add(e)
                return@loop1
            }

            if (OurApps.checkAppInstalled(context, pkgId)) {
                logger.info ( "$pkgId installed, drop this item." )
                return@loop1
            }

            out.add(e)
        }

        return out
    }

    private fun clearBlacklistedAdEntries(entries:List<AdEntry>) : MutableList<AdEntry> {
        val out = mutableListOf<AdEntry>()

        val systemVersion = Build.VERSION.RELEASE

        val versionCode = BuildConfig.VERSION_CODE.toString()
        val versionName = BuildConfig.VERSION_NAME

        val deviceModel = Build.MODEL
        val productInfo = Build.PRODUCT
        val deviceName = Build.DEVICE

        entries.forEach loop1@{  e ->
            if (this.disabledAdTypes.contains(e.mediaType)) {
                return@loop1
            }

            val r = e.blacklistRule
            val patternsToTargetMap = mapOf(
                r.versionCodePatterns to versionCode,
                r.versionStringPatterns to versionName,
                r.deviceModelPatterns to deviceModel,
                r.deviceProductPatterns to productInfo,
                r.deviceNamePatterns to deviceName,
                r.systemVersionPatterns to systemVersion,
            )

            patternsToTargetMap.forEach loop2@{ (patterns, s) ->
                patterns.forEach loop3@{ p ->
                    if (p.matcher(s).matches()) {
                        return@loop1
                    }
                }
            }

            out.add(e)
        }

        return out
    }

    private fun clearDeviceTypeMismatchedAdEntries(entries:List<AdEntry>) : MutableList<AdEntry> {
        val out = mutableListOf<AdEntry>()

        entries.forEach loop1@{  e ->
            if (e.tvOnly == this.isTv) {
                out.add(e)
            }
        }

        return out
    }

    private fun updateDb(json:String) : Boolean {
        val gson = Gson()
        try {
            val db = gson.fromJson(json, SimpleAdDb::class.java)
            logger.info ( "remote version: ${db.ts}, local version: ${this.dbAndHistory.ads.ts}." )
            db.adEntries.forEach { e ->
                e.blacklistRule.compilePatternMatchers()
            }
            logger.info ( "ad db updated, downloaded total: ${db.adEntries.size}" )
            db.adEntries = clearDeviceTypeMismatchedAdEntries(db.adEntries)
            logger.info ( "ad db updated, device type filter applied, current size: ${db.adEntries.size}" )
            db.adEntries = clearBlacklistedAdEntries(db.adEntries)
            logger.info ( "ad db updated, blacklist filter applied, current size: ${db.adEntries.size}" )
            if (!BuildConfig.DEBUG) {
                db.adEntries = clearInstalledAppAdEntries(db.adEntries)
                logger.info("ad db updated, installed-app filter applied, current size: ${db.adEntries.size}")
            }
            // db.adEntries.forEach { entry->
            //     entry.maxDisplay = 10000
            // }
            this.dbAndHistory.ads = db

            this.beginPrefetchAdResource()

            return true
        } catch (e:Throwable) {
            logger.warn ( "failed to decode ad db: $e" )
            return false
        }
    }

    fun appendImprHistoryNowOf(adId:String) : Int {
        val record = ImprHistory()
        record.adItemId = adId
        record.presentedAt = System.currentTimeMillis()
        while (dbAndHistory.history.size > MAX_IMPR_HISTORY) {
            dbAndHistory.history.removeAt(0)
        }
        dbAndHistory.history.add(record)

        return dbAndHistory.history.size
    }

    private fun getUnCachedAdResource() : Map<String, String> {
        val out = mutableMapOf<String, String>()

        this.dbAndHistory.ads.adEntries.forEach { entry ->

            if (entry.mediaType != AdMediaType.VIDEO
                && entry.mediaType != AdMediaType.IMAGE) {
                return@forEach
            }

            val itemAdResource = entry.assetUrl

            if (itemAdResource.isBlank() || itemAdResource.isEmpty()) {
                return@forEach
            }

            if (entry.adActionUrl.isBlank()) {
                return@forEach
            }

            val itemCacheName = cachedFileNameOfAdRes(itemAdResource, entry.mediaType)

            val itemCacheFile = File(this.cacheBaseDir, itemCacheName)
            if (itemCacheFile.exists()) {
                return@forEach
            }

            out[itemAdResource] = itemCacheName
        }

        return out
    }


    private fun cachedFileNameOfAdRes(itemAdResource: String, itemMediaType:AdMediaType): String {
        if (itemAdResource.isBlank()) {
            return ""
        }
        val itemHash = itemAdResource.md5()
        return when (itemMediaType) {
            AdMediaType.IMAGE -> {
                "$itemHash.png"
            }
            AdMediaType.VIDEO -> {
                "$itemHash.mp4"
            }
            AdMediaType.HTML -> {
                "$itemHash.html"
            }
        }
    }

    fun cachedFileOfAdRes(adItem:AdEntry) : File? {
        val itemAdRes = adItem.assetUrl
        if (itemAdRes.isEmpty()) {
            return null
        }

        if (adItem.debugMode) {
            return File(itemAdRes)
        }

        val cacheName = cachedFileNameOfAdRes(itemAdRes, adItem.mediaType)

        if(cacheName.isEmpty() || cacheName.isBlank()) {
            return null
        }

        return File(this.cacheBaseDir, cacheName)
    }

    private fun isAdResourcePrefetched(adItem:AdEntry) : Boolean {
        if (adItem.mediaType == AdMediaType.HTML) {
            return true
        }
        val cacheFile = cachedFileOfAdRes(adItem) ?: return false
        return cacheFile.exists()
    }

    private fun beginPrefetchAdResource() {
        val unCached = getUnCachedAdResource()

        logger.info ( "un-cached items: ${unCached.size}" )

        unCached.forEach{ (itemAdResUrl, itemCacheFileName) ->
            val goodCacheFile = File(this.cacheBaseDir, itemCacheFileName)
            val tmpCacheFile = File(this.cacheBaseDir, "$itemCacheFileName.tmp")
            if (tmpCacheFile.exists()) {
                //incomplete download from last run, clear it.
                tmpCacheFile.delete()
            }

            try {
                download(itemAdResUrl, tmpCacheFile.absolutePath)
                tmpCacheFile.renameTo(goodCacheFile)
                logger.info ( "$tmpCacheFile => $goodCacheFile" )
            } catch (e:Throwable) {
                logger.warn ( "failed to download ad asset: $e" )
            }
        }
    }

    fun getNextAvailableAdItem() : AdEntry? {
        dbAndHistory.updateCounters()

        val copy = ArrayList(dbAndHistory.ads.adEntries)
        dbAndHistory.ads.adEntries.forEach {
            if (!isAdResourcePrefetched(it)) {
                copy.remove(it)
            }

            val counter = dbAndHistory.imprCounters[it.adItemId] ?: 0
            logger.info ( "max display count set to: ${it.maxDisplay}, current: $counter" )
            if (it.maxDisplay <= 0) {
                return@forEach
            }

            if (counter > it.maxDisplay) {
                copy.remove(it)
            }
        }

        if (copy.isEmpty()) {
            logger.info("no ad available.")
            return null
        }


        val readyAdsCounters = mutableMapOf<String, Int>()
        copy.forEach {
            val counter = dbAndHistory.imprCounters[it.adItemId] ?: 0
            readyAdsCounters[it.adItemId] = counter
        }

        val result = readyAdsCounters.toList().sortedBy { (_, value) -> value }
        val leastDisplayedAdItemId = result.first().first

        logger.info("will return item with id: $leastDisplayedAdItemId")

        return copy.find { it.adItemId == leastDisplayedAdItemId }
    }

    fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0').lowercase()
    }
}
