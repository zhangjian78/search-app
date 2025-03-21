package jerry.gadgets;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.VideoView;

public class SplashVideoView extends VideoView {

    private static final String TAG = "SPLASH_V";
    private int mVideoWidth;
    private int mVideoHeight;

    public SplashVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SplashVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SplashVideoView(Context context) {
        super(context);
    }


    @Override
    public void setVideoURI(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(this.getContext(), uri);
        mVideoWidth = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        mVideoHeight = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        super.setVideoURI(uri);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.i(TAG, "on measure, video=" + mVideoWidth + "x" + mVideoHeight);
        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        Log.i(TAG, "on measure, window=" + width + "x" + height);
        if (mVideoWidth <= 0 || mVideoHeight <= 0 || width <= 0 || height <= 0) {
            setMeasuredDimension(width, height);
            return;
        }

        double vAspect = mVideoWidth*1.0f/mVideoHeight;
        double wAspect = width*1.0f/height;
        Log.i(TAG, vAspect + " : " + wAspect);

        if (vAspect >= wAspect) {
            width = new Double(height * vAspect).intValue();
        } else {
            height = new Double(width/vAspect).intValue();
        }
        Log.i(TAG, "setting window size to: " + width + 'x' + height);
        setMeasuredDimension(width, height);
    }
}