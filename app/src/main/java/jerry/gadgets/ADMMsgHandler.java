package jerry.gadgets;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import amz_adm.DeviceMessageManager;

public class ADMMsgHandler implements DeviceMessageManager.DeviceMessagingAware {

    public static final String TAG = "AMZ_ADM_RT";

    public static String amzMsgDeviceRegId = null;

    @Override
    public void regOk(String regId) {
        Log.d(TAG, "registration ok: " + regId);
        amzMsgDeviceRegId = regId;
    }

    @Override
    public void regFailure(String errorMsg) {


        Log.d(TAG, "registration error: " + errorMsg);
    }

    @Override
    public void regRemoved(String regId) {


        Log.d(TAG, "registration removed: " + regId);
    }

    private ADM2NotificationBuilder builder;

    @Override
    public void onIntentFromMessaging(Context ctx, Intent intent) {
        Log.d(TAG, "messaging intent received: " + intent);
        Log.d(TAG, "using context: " + ctx);

        if (builder != null) {
            Log.d(TAG, "reusing builder.");
        } else {
            try {
                Class<?> clazz = Class.forName("jerry.gadgets.NotificationBuilder");
                builder = (ADM2NotificationBuilder) clazz.newInstance();
            } catch (Exception e) {
                Log.d(TAG, "no builder found", e);
                return;
            }
        }

        try {
            builder.buildAndShow(ctx, intent);
        } catch (Exception e) {
            Log.d(TAG, "failed to post notification", e);
        }
    }
}
