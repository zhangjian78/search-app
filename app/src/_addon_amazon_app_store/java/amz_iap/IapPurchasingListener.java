package amz_iap;

import android.util.Log;

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserDataResponse;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of {@link PurchasingListener} that listens to Amazon
 * InAppPurchase SDK's events, and call {@link IapManager} to handle the
 * purchase business logic.
 */
public class IapPurchasingListener implements PurchasingListener {

    private static final String TAG = "IAP_Listener";

    private final IapManager iapManager;

    public IapPurchasingListener(final IapManager iapManager) {
        this.iapManager = iapManager;
    }

    /**
     * This is the callback for {@link PurchasingService#getUserData}. For
     * successful case, get the current user from {@link UserDataResponse} and
     * call {@link IapManager#setAmazonUserId} method to load the Amazon
     * user and related purchase information
     * 
     * @param response
     */
    @Override
    public void onUserDataResponse(final UserDataResponse response) {
        Log.w(TAG, "onGetUserDataResponse: requestId (" + response.getRequestId()
                   + ") userIdRequestStatus: "
                   + response.getRequestStatus()
                   + ")");

        final UserDataResponse.RequestStatus status = response.getRequestStatus();
        switch (status) {
        case SUCCESSFUL:
            Log.w(TAG, "onUserDataResponse: get user id (" + response.getUserData().getUserId()
                       + ", marketplace ("
                       + response.getUserData().getMarketplace()
                       + ") ");
            iapManager.setAmazonUserId(response.getUserData().getUserId(), response.getUserData().getMarketplace());
            break;

        case FAILED:
        case NOT_SUPPORTED:
            Log.w(TAG, "onUserDataResponse failed, status code is " + status);
            iapManager.setAmazonUserId(null, null);
            break;
        }
    }

    /**
     * This is the callback for {@link PurchasingService#getProductData}. After
     * SDK sends the product details and availability to this method, it will
     * call {@link IapManager#enablePurchaseForSkus}
     * {@link IapManager#disablePurchaseForSkus} or
     * {@link IapManager#disableAllPurchases} method to set the purchase
     * status accordingly.
     */
    @Override
    public void onProductDataResponse(final ProductDataResponse response) {
        final ProductDataResponse.RequestStatus status = response.getRequestStatus();
        Log.w(TAG, "onProductDataResponse: RequestStatus (" + status + ")");

        switch (status) {
        case SUCCESSFUL:
            Log.w(TAG, "onProductDataResponse: successful.  The item data map in this response includes the valid SKUs");
            final Set<String> unavailableSkus = response.getUnavailableSkus();
            Log.w(TAG, "onProductDataResponse: " + unavailableSkus.size() + " unavailable skus");
            iapManager.enablePurchaseForSkus(response.getProductData());
            iapManager.disablePurchaseForSkus(response.getUnavailableSkus());

            break;
        case FAILED:
        case NOT_SUPPORTED:
            Log.w(TAG, "onProductDataResponse: failed, should retry request");
            iapManager.disableAllPurchases();
            break;
        }
    }

    /**
     * This is the callback for {@link PurchasingService#getPurchaseUpdates}.
     * 
     * You will receive receipts for all possible Subscription history from this
     * callback
     * 
     */
    @Override
    public void onPurchaseUpdatesResponse(final PurchaseUpdatesResponse response) {
        Log.w(TAG, "onPurchaseUpdatesResponse: requestId (" + response.getRequestId()
                   + ") purchaseUpdatesResponseStatus ("
                   + response.getRequestStatus()
                   + ") userId ("
                   + response.getUserData().getUserId()
                   + ")");
        final PurchaseUpdatesResponse.RequestStatus status = response.getRequestStatus();
        switch (status) {
        case SUCCESSFUL:
            Log.w(TAG, "Update/refresh IAP status, receipts in response: " + response.getReceipts());
            iapManager.setAmazonUserId(response.getUserData().getUserId(), response.getUserData().getMarketplace());
            for (final Receipt receipt : response.getReceipts()) {
                iapManager.handleReceipt(response.getRequestId().toString(), receipt, response.getUserData());
            }
            if (response.hasMore()) {
                PurchasingService.getPurchaseUpdates(false);
            }
            iapManager.reloadSubscriptionStatus();
            break;
        case FAILED:
        case NOT_SUPPORTED:
            Log.w(TAG, "onProductDataResponse: failed, should retry request");
            iapManager.disableAllPurchases();
            break;
        }

    }

    /**
     * This is the callback for {@link PurchasingService#purchase}. For each
     * time the application sends a purchase request
     * {@link PurchasingService#purchase}, Amazon Appstore will call this
     * callback when the purchase request is completed. If the RequestStatus is
     * Successful or AlreadyPurchased then application needs to call
     * {@link IapManager#handleReceipt} to handle the purchase
     * fulfillment. If the RequestStatus is INVALID_SKU, NOT_SUPPORTED, or
     * FAILED, notify corresponding method of {@link IapManager} .
     */
    @Override
    public void onPurchaseResponse(final PurchaseResponse response) {
        final String requestId = response.getRequestId().toString();
        final String userId = response.getUserData().getUserId();
        final PurchaseResponse.RequestStatus status = response.getRequestStatus();
        final Receipt receipt = response.getReceipt();
        Log.w(TAG, "onPurchaseResponse: requestId (" + requestId
                   + ") userId ("
                   + userId
                   + ") purchaseRequestStatus ("
                   + status
                   + ")");

        switch (status) {
        case SUCCESSFUL:
            Log.w(TAG, "onPurchaseResponse: receipt json:" + receipt.toJSON());
            iapManager.handleReceipt(requestId, receipt, response.getUserData());
            iapManager.reloadSubscriptionStatus();
            break;
        case ALREADY_PURCHASED:
            Log.w(TAG,
                  "onPurchaseResponse: already purchased, you should verify the subscription purchase on your side and make sure the purchase was granted to customer");

//            String sku = iapManager.skuForPurchaseReqId(requestId);
//            iapManager.amzIapStateAware.purchaseSucceeded(sku, true);
            break;
        case INVALID_SKU:
            Log.w(TAG,
                  "onPurchaseResponse: invalid SKU!  onProductDataResponse should have disabled buy button already.");
            final Set<String> unavailableSkus = new HashSet<String>();
            unavailableSkus.add(receipt.getSku());
            iapManager.disablePurchaseForSkus(unavailableSkus);
            break;
        case FAILED:
        case NOT_SUPPORTED:
            Log.w(TAG, "onPurchaseResponse: failed so remove purchase request from local storage");
            iapManager.purchaseFailed(receipt, receipt.getSku());
            break;
        }

    }

}
