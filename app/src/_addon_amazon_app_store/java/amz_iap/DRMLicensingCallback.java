package amz_iap;

import android.util.Log;

import com.amazon.device.drm.model.LicenseResponse;

public class DRMLicensingCallback implements com.amazon.device.drm.LicensingListener {

    private static final String TAG = "AMZ_DRM";
    @Override
    public void onLicenseCommandResponse(LicenseResponse licenseResponse) {
        final LicenseResponse.RequestStatus status = licenseResponse.getRequestStatus();
        Log.d(TAG, "onLicenseCommandResponse: RequestStatus (" + status + ")");
        switch (status) {
            case LICENSED:
                Log.d(TAG, "onLicenseCommandResponse: LICENSED");
                break;
            case NOT_LICENSED:
                Log.d(TAG, "onLicenseCommandResponse: NOT_LICENSED");
                break;
            case ERROR_VERIFICATION:
                Log.d(TAG, "onLicenseCommandResponse: ERROR_VERIFICATION");
                break;
            case ERROR_INVALID_LICENSING_KEYS:
                Log.d(TAG, "onLicenseCommandResponse: ERROR_INVALID_LICENSING_KEYS");
                break;
            case EXPIRED:
                Log.d(TAG, "onLicenseCommandResponse: EXPIRED");
                break;
            case UNKNOWN_ERROR:
                Log.d(TAG, "onLicenseCommandResponse: ERROR");
        }
    }
}