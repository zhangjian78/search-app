package amz_adm;

import android.util.Log;

import com.amazon.device.messaging.ADMMessageReceiver;



public class MyReceiver extends ADMMessageReceiver {

    public static int SRV_JOB_ID = 97531;

    private static final String TAG = "AMZ_ADM_RECEIVER";

    public MyReceiver() {
        // This is needed for backward compatibility
        super(MyADMLegacyMessageHandler.class);
        // Where available, prefer using the new job based
        if (DeviceMessageManager.isSupportNewApi()) {
            registerJobServiceClass(MyADMMessageHandler.class, SRV_JOB_ID);
        }


        Log.d(TAG, "receiver created: " + this);
    }
    // Nothing else is required here; your broadcast receiver automatically
    // forwards intents to your service for processing.
}
