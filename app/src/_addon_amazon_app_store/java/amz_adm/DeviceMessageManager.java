package amz_adm;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.amazon.device.messaging.ADM;
import com.amazon.device.messaging.development.ADMManifest;

public class DeviceMessageManager {

    public static final String TAG = "AMZ_ADM";
    private final Context ctx;

    public interface DeviceMessagingAware {
        void regOk(String regId);
        void regFailure(String errorMsg);
        void regRemoved(String regId);
        void onIntentFromMessaging(Context ctx, Intent intent);
    }

    public DeviceMessageManager(Context ctx) {
        Log.d(TAG, "ADM manager created with context: " + ctx);
        this.ctx = ctx;
    }

    static DeviceMessagingAware dmAware = null;

    static {
        try {
            Class c = Class.forName("jerry.gadgets.ADMMsgHandler");
            Log.d(TAG, "will try to load " + c + " as message callback.");
            Object o = c.newInstance();
            dmAware = (DeviceMessagingAware) o;
            Log.d(TAG, "runtime callback loading succeeded: " + dmAware);
        } catch (Exception e) {
            Log.d(TAG, "exception in loading runtime callback", e);
            Log.d(TAG, "using default debug only ADM callback.");
            /**
             * default implementation is for debug only.
             */
            dmAware = new DeviceMessagingAware() {
                @Override
                public void regOk(String regId) {


                    Log.d(TAG, "registration ok: " + regId);
                }

                @Override
                public void regFailure(String errorMsg) {


                    Log.d(TAG, "registration error: " + errorMsg);
                }

                @Override
                public void regRemoved(String regId) {


                    Log.d(TAG, "registration removed: " + regId);
                }

                @Override
                public void onIntentFromMessaging(Context ctx, Intent intent) {
                    Log.d(TAG, "messaging intent received: " + intent);
                    Log.d(TAG, "using context: " + ctx);
                }
            };
        }
    }

    public void setDeviceRegistrationAware(DeviceMessagingAware o) {
        dmAware = o;
    }

    private static boolean supportNewApi = false;

    public static boolean isSupportNewApi() {
        return supportNewApi;
    }

    public boolean doCheckSetup() {

        try{
            Class.forName( "com.amazon.device.messaging.ADMMessageHandlerJobBase" );
            supportNewApi = true;
        }
        catch (ClassNotFoundException e)
        {
            // Handle the exception.
        }

        Log.d(TAG, "checking ADM setup ... ");
        try {
            ADMManifest.checkManifestAuthoredProperly(ctx);
            Log.d(TAG, "ADM setup initial checking passed, using new API?  " + supportNewApi);
            return true;
        } catch (Exception e) {
            Log.d(TAG, "ADM setup checking failed, exception got.", e);
            return false;
        }
    }

    public void doDeviceRegister() {
        final ADM adm = new ADM(this.ctx);
        final String deviceRegId = adm.getRegistrationId();
        if (deviceRegId == null) {
            // startRegister() is asynchronous; your app is notified via the
            // onRegistered() callback when the registration ID is available.
            adm.startRegister();
        } else {
            Log.d(TAG, "got existing registration id, callback invoke: " + dmAware);
            dmAware.regOk(deviceRegId);
        }
    }
}
