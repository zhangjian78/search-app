package amz_iap;

import android.util.Log;

import com.amazon.device.drm.LicensingListener;
import com.amazon.device.drm.LicensingService;
import com.amazon.device.drm.model.LicenseResponse;
import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.internal.model.ReceiptBuilder;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.RequestId;
import com.amazon.device.iap.model.UserData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * From AMZ example, showing how an application may handle InAppPurchasing. The major
 * functions includes
 * <ul>
 * <li>Simple user and subscription history management</li>
 * <li>Grant subscription purchases</li>
 * <li>Enable/disable subscribe from GUI</li>
 * <li>Save persistent subscriptions data into SQLite database</li>
 * </ul>
 *
 *
 */
public class IapManager implements LicensingListener {

    private boolean drmCheckOk = false;

    @Override
    public void onLicenseCommandResponse(LicenseResponse licenseResponse) {
        final LicenseResponse.RequestStatus status = licenseResponse.getRequestStatus();

        drmCheckOk = (status == LicenseResponse.RequestStatus.LICENSED);

        Log.i(TAG, "AMZ DRM mode: " + LicensingService.getAppstoreSDKMode());
        Log.i(TAG, "AMZ DRM check status:  " + status);
    }

    public interface IapStateAware {
        void storePurchaseUserIdGot(String userId, String market);
        void availableSkuUpdated(Set<String> skus);
        void purchaseFailed(String sku);
        void purchaseCancelled(String sku);
        void purchaseSucceeded(String sku, boolean purchasedAlreadyFlag);
        void receiptVerificationFailure(String sku, Throwable onError);
        IapDataSource.IapDataStorage dataStorage();
        IapDataSource.IapDataAnalytics analytics();
    }

    private static final String TAG = "IAP_Mgr";
    final IapDataSource dataSource;
    final IapStateAware storeIapStateAware;

    private UserIapData userIapData;

    private final HashMap<String, Product> availableProducts = new HashMap<>();
    private final HashMap<String, String> subscriptionTermSkuMapping = new HashMap<>();

    public void setSubscriptionTermSkuMapping(String termSku, String parentSku) {
        subscriptionTermSkuMapping.put(termSku, parentSku);
    }

    private boolean isNullSkuString(String sku) {
        if (sku == null || sku.isEmpty()
                || sku.equalsIgnoreCase("null")
                || sku.equalsIgnoreCase("\"null\"")) {
            return true;
        }
        return false;
    }

    private String fixNullSkuFrom(String termSku, String reqId, String receiptId) {

        Log.i(TAG, "restore SKU, term SKU set? " + termSku + ", has request Id?" + reqId + ")");
        String sku = null;
        // only with newer Amazon Store SDK.
        if (termSku != null && !termSku.isEmpty()) {
            sku = termSku;
            Log.i(TAG, "term SKU used as SKU: " + termSku );
            Log.i(TAG, "has parent SKU? " + subscriptionTermSkuMapping.get(termSku));
        }

        // for older IAP SDK.
        if (sku == null && reqId != null) {
            sku = storeIapStateAware.dataStorage().querySkuForPurchaseId(reqId);
            Log.i(TAG, "request Id: " + reqId + " => " + " SKU: " + sku);
        }

        if (sku == null && receiptId != null) {
            SubscriptionRecord r = storeIapStateAware.dataStorage().getAmzIapRecordOfReceipt(receiptId);
            if (r != null) {
                sku = r.getSku();
            }
            Log.i(TAG, "receipt Id: " + receiptId + " => " + " SKU: " + sku);
        }

        return sku;
    }

    private Receipt fixNullSkuForSubscriptionReceipt(Receipt r, String sku) {
        ReceiptBuilder rb = new ReceiptBuilder();

        rb.setSku(sku);
        rb.setReceiptId(r.getReceiptId());
        rb.setProductType(r.getProductType());
        rb.setPurchaseDate(r.getPurchaseDate());
        rb.setCancelDate(r.getCancelDate());
        // Only valid with newer Amazon Store SDK.
//        rb.setTermSku(r.getTermSku());
//        rb.setDeferredSku(r.getDeferredSku());
//        rb.setDeferredDate(r.getDeferredDate());

        return rb.build();
    }

    public Set<String> availableSkus() {
        return availableProducts.keySet();
    }


    public IapManager(final IapStateAware aware) {
        this.storeIapStateAware = aware;
        this.dataSource = new IapDataSource(storeIapStateAware.dataStorage(), storeIapStateAware.analytics());
    }

    /**
     * Method to set the app's amazon user id and marketplace from IAP SDK
     * responses.
     *
     * @param newAmazonUserId
     * @param newAmazonMarketplace
     */
    public void setAmazonUserId(final String newAmazonUserId, final String newAmazonMarketplace) {
        Log.i(TAG, "set AMZ user Id to " + newAmazonUserId + "(" + newAmazonMarketplace + ")");
        // Reload everything if the Amazon user has changed.
        if (newAmazonUserId == null) {
            // A null user id typically means there is no registered Amazon
            // account.
            if (userIapData != null) {
                userIapData = null;
                storeIapStateAware.availableSkuUpdated(availableSkus());
            }

            storeIapStateAware.storePurchaseUserIdGot(null, newAmazonMarketplace);
        } else if (userIapData == null || !newAmazonUserId.equals(userIapData.getAmazonUserId())) {
            // If there was no existing Amazon user then either no customer was
            // previously registered or the application has just started.

            // If the user id does not match then another Amazon user has
            // registered.
            userIapData = new UserIapData(newAmazonUserId, newAmazonMarketplace);

            userIapData.setSubscriptionRecords(dataSource.storage.getAmzIapRecordOfUser(newAmazonUserId));
            userIapData.reloadSubscriptionStatus();

            storeIapStateAware.availableSkuUpdated(availableSkus());
            Log.i(TAG, "user data set, existing records: " + storeIapStateAware.dataStorage().getAllAmzIapRecords().size());

            storeIapStateAware.storePurchaseUserIdGot(newAmazonUserId, newAmazonMarketplace);
        }
    }

    /**
     * Enable the magazine subscription.
     *
     * @param productData
     */
    public void enablePurchaseForSkus(final Map<String, Product> productData) {
        this.availableProducts.putAll(productData);
        this.storeIapStateAware.availableSkuUpdated(this.availableSkus());
    }

    /**
     * Disable the magazine subscription.
     *
     * @param unavailableSkus
     */
    public void disablePurchaseForSkus(final Set<String> unavailableSkus) {
        for (String sku : unavailableSkus) {
            this.availableProducts.remove(sku);
        }
        this.storeIapStateAware.availableSkuUpdated(this.availableSkus());
    }

    public boolean canDoPurchaseOnSku(final String sku) {
        if (userIapData != null && availableProducts.containsKey(sku)) {
            return true;
        } else {
            Log.e(TAG, "can not purchase " + sku + ", user data? " + userIapData + ", available skus: " + this.availableSkus());
            return false;
        }
    }

    public String doPurchaseOnSku(final String sku) {
        RequestId requestId = PurchasingService.purchase(sku);
        if (requestId != null) {
            String s = requestId.toString();
            this.storeIapStateAware.dataStorage().recordPurchaseRequestIdForSku(s, sku);
            return s;
        } else {
            return null;
        }
    }

    public void disableAllPurchases() {
        int oldSize = this.availableProducts.size();
        this.availableProducts.clear();

        if (oldSize > 0) {
            this.storeIapStateAware.availableSkuUpdated(this.availableSkus());
        }
    }

    /**
     * This method contains the business logic to fulfill the customer's
     * purchase based on the receipt received from InAppPurchase SDK's
     * {@link PurchasingListener#onPurchaseResponse} or
     * {@link PurchasingListener#onPurchaseUpdatesResponse} method.
     *
     *
     */
    private void handleSubscriptionPurchase(String requestId, Receipt receipt, UserData userData) {
        try {
            if (isNullSkuString(receipt.getSku())) {
                String sku = fixNullSkuFrom(null, requestId, receipt.getReceiptId());

                receipt = fixNullSkuForSubscriptionReceipt(receipt, sku);

                Log.i(TAG, "subscription receipt fixed from term SKU: " + receipt.toString());
            }
            if (receipt.isCanceled()) {
                // Check whether this receipt is for an expired or canceled
                // subscription
                Log.i(TAG, "It is a cancelled subscription receipt.");
                revokeSubscription(receipt, userData.getUserId());
                this.storeIapStateAware.purchaseCancelled(receipt.getSku());
            } else {
                // We strongly recommend that you verify the receipt on
                // server-side.
                Log.i(TAG, "Subscription receipt verifying ...");
                if (!verifyReceiptFromYourService(receipt, userData)) {
                    // if the purchase cannot be verified,
                    // show relevant error message to the customer.
                    Log.i(TAG, "Subscription receipt rejected by verification.");
                    this.storeIapStateAware.receiptVerificationFailure(receipt.getSku(), null);
                    return;
                }
                Log.i(TAG, "Subscription receipt ready, granted.");
                grantSubscriptionPurchase(receipt, userData);
            }
            return;
        } catch (final Throwable e) {
            Log.e(TAG, "Subscription receipt handle failure: ", e);
            this.storeIapStateAware.receiptVerificationFailure(receipt.getSku(), e);
        }

    }

    private void grantSubscriptionPurchase(final Receipt receipt, final UserData userData) {

        if (!availableProducts.containsKey(receipt.getSku())
                && !subscriptionTermSkuMapping.containsKey(receipt.getSku())
                && !subscriptionTermSkuMapping.containsValue(receipt.getSku())) {
            Log.i(TAG, "The SKU [" + receipt.getSku() + "] in the receipt is not valid anymore ");
            // if the sku is not applicable anymore, call
            // PurchasingService.notifyFulfillment with status "UNAVAILABLE"
            PurchasingService.notifyFulfillment(receipt.getReceiptId(), FulfillmentResult.UNAVAILABLE);
            return;
        }
        try {
            // Set the purchase status to fulfilled for your application
            saveSubscriptionRecord(receipt, userData.getUserId());
            PurchasingService.notifyFulfillment(receipt.getReceiptId(), FulfillmentResult.FULFILLED);
            Log.i(TAG, "Check latest active state in DB: " + this.userIapData.isSubsActiveCurrently());
            this.storeIapStateAware.purchaseSucceeded(receipt.getSku(), false);
        } catch (final Throwable e) {
            // If for any reason the app is not able to fulfill the purchase,
            // add your own error handling code here.
            Log.i(TAG, "Failed to grant entitlement purchase, with error " + e.getMessage());
        }

    }

    /**
     * Method to handle receipt
     *
     * @param requestId
     * @param receipt
     * @param userData
     */
    public void handleReceipt(final String requestId, final Receipt receipt, final UserData userData) {
        Log.i(TAG, "receipt of SKU [" + receipt.getSku() + "] from request" + requestId + " arrived with user data: " + userData);
        switch (receipt.getProductType()) {
            case CONSUMABLE:
                // check consumable sample for how to handle consumable purchases
                break;
            case ENTITLED:
                // check entitlement sample for how to handle consumable purchases
                break;
            case SUBSCRIPTION:
                handleSubscriptionPurchase(requestId, receipt, userData);
                break;
        }
    }

    /**
     * Show purchase failed message
     * @param sku
     */
    public void purchaseFailed(final Receipt receipt, final String sku) {
        this.storeIapStateAware.purchaseFailed(sku);
    }

    public UserIapData getUserIapData() {
        return this.userIapData;
    }


    /**
     * Reload the subscription history from database
     */
    public void reloadSubscriptionStatus() {
        final List<SubscriptionRecord> subsRecords = dataSource.getSubscriptionRecords(userIapData.getAmazonUserId());
        userIapData.setSubscriptionRecords(subsRecords);
        userIapData.reloadSubscriptionStatus();
    }

    /**
     *
     * Save & caching subscription purchase detail locally.
     *
     * We strongly recommend that you save the purchase information on a server.
     *
     *
     *
     * @param receipt
     * @param userId
     */
    private void saveSubscriptionRecord(final Receipt receipt, final String userId) {
        dataSource.saveSubscriptionRecord(receipt, userId);
        userIapData.setSubscriptionRecords(dataSource.storage.getAmzIapRecordOfUser(userId));
        userIapData.reloadSubscriptionStatus();
    }

    /**
     * We strongly recommend verifying the receipt on your own server side
     * first. The server side verification ideally should include checking with
     * Amazon RVS (Receipt Verification Service) to verify the receipt details.
     *
     * @see <a href=
     *      "https://developer.amazon.com/appsandservices/apis/earn/in-app-purchasing/docs/rvs"
     *      >Appstore's Receipt Verification Service</a>
     *
     * @param receipt
     * @return
     */
    private boolean verifyReceiptFromYourService(final Receipt receipt, final UserData userData) {
        // TODO Add your own server side accessing and verification code
        return true;
    }

    /**
     * Private method to revoke a subscription purchase from the customer
     *
     * Please implement your application-specific logic to handle the revocation
     * of a subscription purchase.
     *
     *
     * @param receipt
     * @param userId
     */
    private void revokeSubscription(final Receipt receipt, final String userId) {
        final String receiptId = receipt.getReceiptId();
        dataSource.cancelSubscription(receiptId, receipt.getCancelDate().getTime());
        userIapData.setSubscriptionRecords(dataSource.storage.getAmzIapRecordOfUser(userId));
        userIapData.reloadSubscriptionStatus();
    }

}
