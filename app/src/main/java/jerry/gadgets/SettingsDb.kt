package jerry.gadgets

import amz_iap.IapDataSource
import amz_iap.SubscriptionRecord
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.io.File

private val logger = Logger("SETTING_DB")

class SettingsDb private constructor() : IapDataSource.IapDataStorage {

    companion object {

        private const val dbJsonFile = "settings.json"
        private var instance = SettingsDb()

        fun db(): SettingsDb {
            return instance
        }

        fun init(callback:(Boolean)->Unit) {
            NamedExecutor.singleThreadExecutor.execute {
                var haveOldData = false
                try {
                    logger.debug{"load setting db json"}
                    val baseDir = DataHelper.cacheBaseDirectory ?: return@execute

                    if (!baseDir.isDirectory || !baseDir.canWrite()) {
                        return@execute
                    }

                    val dbFile = File(baseDir, dbJsonFile)
                    if (!dbFile.exists()) {
                        return@execute
                    }

                    val buf = ByteArrayOutputStream()
                    val ok = CiperUtil.decryptFileToByteArray(dbFile, buf)
                    if (!ok || buf.size() == 0) {
                        return@execute
                    }

                    val json = String(buf.toByteArray())
                    val savedDb = Gson().fromJson(json,
                        SettingsDb::class.java
                    )
                    instance = savedDb
                    logger.debug{"restored setting db from json\n: $json"}
                    haveOldData = true
                } catch (t:Throwable) {
                    logger.warn("restored setting db from json failure: $t")
                } finally {
                    callback(haveOldData)
                }
            }
        }

        fun saveJsonDb() {
            NamedExecutor.singleThreadExecutor.execute {
                logger.debug{"save setting db json"}
                val baseDir = DataHelper.cacheBaseDirectory ?: return@execute

                if (!baseDir.isDirectory || !baseDir.canWrite()) {
                    return@execute
                }

                val backUpDbFile = File(baseDir, "$dbJsonFile.bak")
                var dbFile = File(baseDir, dbJsonFile)

                if (dbFile.exists()) {
                    backUpDbFile.delete()
                    dbFile.renameTo(backUpDbFile)

                    dbFile = File(baseDir, dbJsonFile)
                }

                val jsonDb = Gson().toJson(instance)
                val ok = CiperUtil.encryptByteArrayToFile(jsonDb.toByteArray(), dbFile)
                logger.debug{"setting saved? $ok"}
                logger.debug{"setting db:\n $jsonDb"}
            }
        }
    }

    val subscriptionRecords: MutableList<SubscriptionRecord> = mutableListOf()
    val purchaseRequestIds: MutableMap<String, String> = mutableMapOf()

    var subscriptionActiveStatusSinceLastCheck = false
    var entitlementActiveStatusSinceLastCheck = false


    override fun saveOrUpdateAmzIapRecord(r: SubscriptionRecord) {
        logger.info{"save/update subscription IAP record: ${r.amazonReceiptId} from AMZ user ${r.amazonUserId}"}
        val old = subscriptionRecords.find { e -> e.amazonReceiptId == r.amazonReceiptId }
        if (old != null) {
            subscriptionRecords.remove(old)
        }
        subscriptionRecords.add(r)

        saveJsonDb()
    }

    override fun getAmzIapRecordOfUser(userId: String): MutableList<SubscriptionRecord> {
        val out = mutableListOf<SubscriptionRecord>()
        subscriptionRecords.forEach { e -> if (e.amazonUserId == userId) out.add(e) }
        logger.info{"query subscription IAP record for AMZ user ${userId}, found ${out.size}"}
        return out
    }

    override fun getAmzIapRecordOfReceipt(receiptId: String): SubscriptionRecord? {
        logger.info{"query subscription IAP record with receipt Id: $receiptId"}
        return subscriptionRecords.find { e -> e.amazonReceiptId == receiptId }
    }

    override fun getAllAmzIapRecords(): List<SubscriptionRecord> {
        val copy = mutableListOf<SubscriptionRecord>()
        copy.addAll(subscriptionRecords)
        logger.info{"query all subscription IAP records, found: ${copy.size}"}
        return copy
    }

    override fun countOfAmzIapRecords(): Int {
        return subscriptionRecords.size
    }

    override fun deleteAllAmzIapRecords(): Int {
        val count = subscriptionRecords.size
        subscriptionRecords.clear()
        logger.info{"delete all subscription IAP records, deleted: $count"}
        return count
    }

    override fun saveAllAmzIapRecords(all: MutableList<SubscriptionRecord>) {
        subscriptionRecords.clear()
        subscriptionRecords.addAll(all)
        logger.info{"save/replace all subscription IAP records, current: ${subscriptionRecords.size}"}

        saveJsonDb()
    }

    override fun recordPurchaseRequestIdForSku(purchaseRequestId: String, sku: String) {
        purchaseRequestIds[purchaseRequestId] = sku
    }

    override fun querySkuForPurchaseId(reqId: String): String? {
        return purchaseRequestIds[reqId]
    }
}
