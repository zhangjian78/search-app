package jerry.gadgets;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

/**
 * https://stackoverflow.com/a/26428753/465620
 *
 * Detects Keyboard Status changes and fires events only once for each change
 */
public class KeyboardStatusDetector {
    private static final String TAG = "KbDetector";
    KeyboardVisibilityListener visibilityListener;

    boolean keyboardVisible = false;

    public void registerFragment(Fragment f) {
        registerView(f.getView());
    }

    public void registerActivity(Activity a) {
        registerView(a.getWindow().getDecorView().findViewById(android.R.id.content));
    }

    public KeyboardStatusDetector registerView(final View v) {
        v.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.d(TAG, "global layout detected.");
                Rect r = new Rect();
                v.getWindowVisibleDisplayFrame(r);

                int heightDiff = v.getRootView().getHeight() - (r.bottom - r.top);
                if (heightDiff > 100) { // if more than 100 pixels, its probably a keyboard...
                    /** Check this variable to debounce layout events */
                    if(!keyboardVisible) {
                        Log.d(TAG, "soft keyboard activated.");
                        keyboardVisible = true;
                        if(visibilityListener != null) visibilityListener.onVisibilityChanged(true);
                    }
                } else {
                    if(keyboardVisible) {
                        Log.d(TAG, "soft keyboard deactivated.");
                        keyboardVisible = false;
                        if(visibilityListener != null) visibilityListener.onVisibilityChanged(false);
                    }
                }
            }
        });

        return this;
    }

    public KeyboardStatusDetector setVisibilityListener(KeyboardVisibilityListener listener) {
        visibilityListener = listener;
        return this;
    }

    public interface KeyboardVisibilityListener {
        void onVisibilityChanged(boolean keyboardVisible);
    }
}