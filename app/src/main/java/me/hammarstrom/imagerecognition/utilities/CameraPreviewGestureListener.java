package me.hammarstrom.imagerecognition.utilities;

import android.support.annotation.IntDef;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Created by Fredrik Hammarström on 08/04/16.
 */
public class CameraPreviewGestureListener extends GestureDetector.SimpleOnGestureListener {

    private static final String TAG = "CameraPreviewGestureLis";

    public static final int PREVIEW_MODE_PREVIEW = 1;
    public static final int PREVIEW_MODE_PROCESSING = 2;
    public static final int PREVIEW_MODE_RESULT = 3;

    @IntDef({PREVIEW_MODE_PREVIEW, PREVIEW_MODE_PROCESSING, PREVIEW_MODE_RESULT})
    public @interface PreviewMode {}

    @PreviewMode
    private int mPreviewMode;

    public CameraPreviewGestureListener() {
        mPreviewMode = PREVIEW_MODE_PREVIEW;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {

        switch (mPreviewMode) {
            case PREVIEW_MODE_PREVIEW:
                Log.d(TAG, "onDoubleTap : PREVIEW");
                mPreviewMode = PREVIEW_MODE_PROCESSING;
                break;
            case PREVIEW_MODE_PROCESSING:
                Log.d(TAG, "onDoubleTap : PROCESSING");
                mPreviewMode = PREVIEW_MODE_RESULT;
                break;
            case PREVIEW_MODE_RESULT:
                Log.d(TAG, "onDoubleTap : RESULT");
                mPreviewMode = PREVIEW_MODE_PREVIEW;
                break;
        }

        return super.onDoubleTap(e);
    }

}
