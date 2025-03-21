package amz_iap;

import android.util.Log;

import com.amazon.device.iap.model.Receipt;

import java.util.ArrayList;
import java.util.List;

/**
 * Purchase data that will be persisted/cached.
 *
 *
 */
public class IapDataSource {

    public IapDataSource(IapDataStorage storage, IapDataAnalytics analytics) {
        this.storage = storage;
        this.analytics = analytics;
    }

    public interface IapDataStorage {
        void saveOrUpdateAmzIapRecord(final SubscriptionRecord r);
        List<SubscriptionRecord> getAmzIapRecordOfUser(String userId);
        SubscriptionRecord getAmzIapRecordOfReceipt(String receiptId);
        List<SubscriptionRecord> getAllAmzIapRecords();
        int countOfAmzIapRecords();
        int deleteAllAmzIapRecords();
        void saveAllAmzIapRecords(List<SubscriptionRecord> all);

        // purchase request handle, needed with buggy SDK
        void recordPurchaseRequestIdForSku(final String purchaseRequestId, final String sku);
        String querySkuForPurchaseId(String requestId);
    }


    public static class NoopIapDataStorage implements IapDataStorage {

        @Override
        public void saveOrUpdateAmzIapRecord(SubscriptionRecord r) {

        }

        @Override
        public List<SubscriptionRecord> getAmzIapRecordOfUser(String userId) {
            return new ArrayList<>();
        }


        @Override
        public SubscriptionRecord getAmzIapRecordOfReceipt(String receiptId) {
            return null;
        }

        @Override
        public List<SubscriptionRecord> getAllAmzIapRecords() {
            return new ArrayList<>();
        }

        @Override
        public int countOfAmzIapRecords() {
            return 0;
        }

        @Override
        public int deleteAllAmzIapRecords() {
            return 0;
        }

        @Override
        public void saveAllAmzIapRecords(List<SubscriptionRecord> all) {

        }

        @Override
        public void recordPurchaseRequestIdForSku(String purchaseRequestId, String sku) {

        }

        @Override
        public String querySkuForPurchaseId(String requestId) {
            return null;
        }
    }

    public interface IapDataAnalytics {
        // analytics
        void analyticsOnSubscriptionNewActivation(SubscriptionRecord r);
        void analyticsOnSubscriptionCanceled(SubscriptionRecord r);

    }

    private static final String TAG = "IAP_DataMgr";

    final IapDataStorage storage;
    final IapDataAnalytics analytics;

    /**
     * Return all subscription records for the user
     *
     * @param userId
     *            user id used to verify the purchase record
     * @return
     */
    public final List<SubscriptionRecord> getSubscriptionRecords(final String userId) {
        Log.d(TAG, "getSubscriptionRecord: userId (" + userId + ")");

        final List<SubscriptionRecord> results = storage.getAmzIapRecordOfUser(userId);

        Log.d(TAG, "getSubscriptionRecord: found " + results.size() + " records");

        return storage.getAmzIapRecordOfUser(userId);

    }

    public static SubscriptionRecord fromReceipt(Receipt rpt, String userId) {
        SubscriptionRecord r = new SubscriptionRecord();
        r.setFrom(rpt.getPurchaseDate().getTime());
        r.setSku(rpt.getSku());
        r.setAmazonUserId(userId);
        r.setAmazonReceiptId(rpt.getReceiptId());
        r.setTo(rpt.getCancelDate()==null?SubscriptionRecord.TO_DATE_NOT_SET:rpt.getCancelDate().getTime());
        return r;
    }

    /**
     * Insert or update the subscription record.
     *
     */
    public void saveSubscriptionRecord(final Receipt receipt, final String userId) {
        Log.d(TAG, "insertOrUpdateSubscriptionRecord: receiptId (" + receipt.getReceiptId() + "),userId (" + userId + ")");

        SubscriptionRecord oldOne = storage.getAmzIapRecordOfReceipt(receipt.getReceiptId());

        SubscriptionRecord newOne = fromReceipt(receipt, userId);
        if (oldOne != null) {
            if (oldOne.getTo() > 0) {

                // There are record with given receipt id and cancel_date>0 in the
                // table, this record should be final and cannot be overwritten
                // anymore.
            } else {
                storage.saveOrUpdateAmzIapRecord(newOne);
            }
        } else {
            storage.saveOrUpdateAmzIapRecord(newOne);
            analytics.analyticsOnSubscriptionNewActivation(newOne);
        }
    }

    /**
     * Cancel a subscription by set the cancel date for the subscription record
     *
     * @param receiptId
     *            The receipt id
     * @param cancelDate
     *            Timestamp for the cancel date
     * @return
     */
    public boolean cancelSubscription(final String receiptId, final long cancelDate) {
        Log.d(TAG, "cancelSubscription: receiptId (" + receiptId + "), cancelDate:(" + cancelDate + ")");

        SubscriptionRecord r = storage.getAmzIapRecordOfReceipt(receiptId);
        if (r == null) {
            return false;
        }

        long oldCancelDate = r.getTo();
        r.setTo(cancelDate);

        storage.saveOrUpdateAmzIapRecord(r);
        if (cancelDate > oldCancelDate) {
            analytics.analyticsOnSubscriptionCanceled(r);
        }

        return true;

    }
}
