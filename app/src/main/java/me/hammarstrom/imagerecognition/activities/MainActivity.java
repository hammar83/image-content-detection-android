package me.hammarstrom.imagerecognition.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.FaceAnnotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.hammarstrom.imagerecognition.R;
import me.hammarstrom.imagerecognition.utilities.CameraPreview;
import me.hammarstrom.imagerecognition.utilities.DeviceDimensionsHelper;
import me.hammarstrom.imagerecognition.utilities.FaceFoundHelper;
import me.hammarstrom.imagerecognition.utilities.ScoreView;
import me.hammarstrom.imagerecognition.vision.CloudVisionTask;
import me.hammarstrom.imagerecognition.vision.CloudVisionTaskDoneListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, CloudVisionTaskDoneListener {

    private final String TAG = MainActivity.this.getClass().getName();

    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private FrameLayout mCameraPreviewLayout;
    private RelativeLayout mProcessingLayout;
    private TextToSpeech mTts;

    private Toolbar mToolbar;
    private LinearLayout mScoreResultLayout;
    private LinearLayout mLoadingLayout;

    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "onPicTaken");
            Bitmap tmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            new CloudVisionTask(tmp, MainActivity.this).execute();
            //mCamera.startPreview();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);
        mCameraPreviewLayout.setOnClickListener(this);

        mTts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    Log.d(TAG, "TTS INITIALIZED");
                    mTts.setLanguage(Locale.UK);
                }
            }
        });
    }

    /**
     * Bind views associated with current layout
     */
    private void bindViews() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mCameraPreviewLayout = (FrameLayout) findViewById(R.id.camera_preview);
        mProcessingLayout = (RelativeLayout) findViewById(R.id.processing_layout);
        mScoreResultLayout = (LinearLayout) findViewById(R.id.score_result_layout);
        mLoadingLayout = (LinearLayout) findViewById(R.id.loading_layout);
    }

    /**
     * Helper method to get {@link Camera} instance
     *
     * @return camera
     */
    private Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            // cannot get camera or does not exist
        }
        return camera;
    }

    /**
     * Helper method to show / hide progress bar
     *
     * @param show if progress should be shown or not
     */
    private void showLoading(boolean show) {
        if(show) {
            mLoadingLayout.setAlpha(0f);
            mLoadingLayout.setVisibility(View.VISIBLE);
            mLoadingLayout.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        } else {
            mLoadingLayout.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mLoadingLayout.setVisibility(View.GONE);
                        }
                    }).start();
        }
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        mCamera = getCameraInstance();
        mCameraPreview = new CameraPreview(this, mCamera);
        mCameraPreviewLayout.addView(mCameraPreview);
    }

    /**
     * Releases the camera
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null){
            mCamera.setPreviewCallback(null);
            mCameraPreview.getHolder().removeCallback(mCameraPreview);
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * Shutdown TextToSpeech
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTts.shutdown();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.camera_preview:
                showLoading(true);
                mProcessingLayout.setVisibility(View.VISIBLE);
                mCameraPreviewLayout.setOnClickListener(null);
                mCamera.takePicture(null, null, mPictureCallback);
//                mCamera.setPreviewCallback(null);
//                mCamera.stopPreview();
                break;
        }
    }

    private void convertResponseToString(BatchAnnotateImagesResponse response) {
        Log.d(TAG, ":: " + response.getResponses().toString());
        List<FaceAnnotation> faces = response.getResponses().get(0).getFaceAnnotations();
        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();

        // Label string to be populated with data for TextToSpeech
        String label = "The image may contain ";
        if (labels != null) {
            List<Animator> scoreViewAnimations = new ArrayList<>();
            List<Animator> scoreAlphaAnimations = new ArrayList<>();
            List<Animator> showScoreAnimations = new ArrayList<>();

            for (EntityAnnotation l : labels) {
                if(l.getScore() < 0.6f) {
                    continue;
                }

                // Add label description (ex. laptop, desk, person, etc.)
                label += l.getDescription() + ", ";

                /**
                 * Create a new {@link ScoreView} and populate it with label description and score
                 */
                ScoreView scoreView = new ScoreView(MainActivity.this);
                int padding = (int) DeviceDimensionsHelper.convertDpToPixel(8, this);
                scoreView.setPadding(padding, padding, padding, padding);
                scoreView.setScore(l.getScore());
                scoreView.setLabelPosition(ScoreView.LABEL_POSITION_RIGHT);
                scoreView.setLabelText(l.getDescription());
                scoreView.setAlpha(0f);
                scoreView.setTranslationX( (DeviceDimensionsHelper.getDisplayWidth(this) / 2) * -1 );

                // Add ScoreView to result layout
                mScoreResultLayout.addView(scoreView);

                // Create animations to used to show the ScoreView in a nice way
                ObjectAnimator animator = ObjectAnimator.ofFloat(scoreView, "translationX", (DeviceDimensionsHelper.getDisplayWidth(this) / 2) * -1, 0f);
                animator.setInterpolator(new OvershootInterpolator());
                scoreViewAnimations.add(animator);

                ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(scoreView, "alpha", 0f, 1f);
                scoreAlphaAnimations.add(alphaAnimator);

                // Get the animation to show the actual score from ScoreView object
                showScoreAnimations.addAll(scoreView.getShowScoreAnimationsList());
            }

            // Setup and play the animations
            AnimatorSet translationSet = new AnimatorSet();
            translationSet.playSequentially(scoreViewAnimations);
            translationSet.setDuration(300);

            AnimatorSet alphaSet = new AnimatorSet();
            alphaSet.playSequentially(scoreAlphaAnimations);
            alphaSet.setDuration(300);

            AnimatorSet showScoreSet = new AnimatorSet();
            showScoreSet.playTogether(showScoreAnimations);

            AnimatorSet set = new AnimatorSet();
            set.play(translationSet).with(alphaSet).before(showScoreSet);
            set.start();
        }

        // Handle detected faces
        String facesFound = "";
        if(faces != null && faces.size() > 0) {
            facesFound = FaceFoundHelper.getFacesFoundString(this, faces);
        }

        // Add the detected image data to TTS engine
        mTts.speak(label, TextToSpeech.QUEUE_FLUSH, null);
        mTts.speak(facesFound, TextToSpeech.QUEUE_ADD, null);
    }


    @Override
    public void onTaskDone(BatchAnnotateImagesResponse response) {
        showLoading(false);
        //mProcessingLayout.setVisibility(View.GONE);
        mCameraPreviewLayout.setOnClickListener(this);
        mCamera.startPreview();

        convertResponseToString(response);
    }
}
