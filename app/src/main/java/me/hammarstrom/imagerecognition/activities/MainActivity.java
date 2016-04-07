/*
 Copyright 2016 Fredrik Hammarström

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package me.hammarstrom.imagerecognition.activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.FaceAnnotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import me.hammarstrom.imagerecognition.R;
import me.hammarstrom.imagerecognition.utilities.CameraPreview;
import me.hammarstrom.imagerecognition.utilities.DeviceDimensionsHelper;
import me.hammarstrom.imagerecognition.utilities.FaceFoundHelper;
import me.hammarstrom.imagerecognition.utilities.FaceGraphicOverlay;
import me.hammarstrom.imagerecognition.utilities.PermissionUtils;
import me.hammarstrom.imagerecognition.utilities.ScoreView;
import me.hammarstrom.imagerecognition.vision.CloudVisionTask;
import me.hammarstrom.imagerecognition.vision.CloudVisionTaskDoneListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = MainActivity.this.getClass().getName();

    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private FrameLayout mCameraPreviewLayout;
    private RelativeLayout mProcessingLayout;
    private TextToSpeech mTts;

    private Toolbar mToolbar;
    private LinearLayout mScoreResultLayout;
    private LinearLayout mLoadingLayout;
    private Button mButtonReset;


    /**
     * Called when a response from Google Cloud Vision API is ready
     *
     * @param response The response from {@link CloudVisionTask}
     */
    private CloudVisionTaskDoneListener mVisionTaskListener = new CloudVisionTaskDoneListener() {
        @Override
        public void onTaskDone(BatchAnnotateImagesResponse response) {
            showLoading(false);
            mProcessingLayout.setVisibility(View.VISIBLE);
            convertResponseToString(response);
        }
    };

    /**
     *
     */
    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            final Bitmap tmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            new CloudVisionTask(tmp, mVisionTaskListener).execute();
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
        mButtonReset.setOnClickListener(this);

        mTts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
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
        mButtonReset = (Button) findViewById(R.id.button_reset);
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

            // Set auto focus mode
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(parameters);

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
    private void showLoading(final boolean show) {
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(mLoadingLayout, "alpha", show ? 0f : 1f, show ? 1f : 0f);
        alphaAnimator.setDuration(200);
        alphaAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if(!show) {
                    mLoadingLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if(show) {
                    mLoadingLayout.setAlpha(0f);
                    mLoadingLayout.setVisibility(View.VISIBLE);
                }
            }
        });
        alphaAnimator.start();
    }

    private void createCameraSource() {
        // Make sure we have permission to use camera
        if(PermissionUtils.requestPermission(this, RC_HANDLE_CAMERA_PERM, Manifest.permission.CAMERA)) {
            mCamera = getCameraInstance();
            mCameraPreview = new CameraPreview(this, mCamera);
            mCameraPreviewLayout.addView(mCameraPreview);
        }
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        createCameraSource();
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
                mCameraPreviewLayout.setOnClickListener(null);
                mCamera.takePicture(null, null, mPictureCallback);
                //mCamera.setPreviewCallback(null);
                //mCamera.stopPreview();
                break;
            case R.id.button_reset:
                resetPreview();
                break;
        }
    }

    private void convertResponseToString(BatchAnnotateImagesResponse response) {
        Log.d(TAG, ":: " + response.getResponses().toString());
        List<FaceAnnotation> faces = response.getResponses().get(0).getFaceAnnotations();
        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();

        // Label string to be populated with data for TextToSpeech
        String label = "";
        if (labels != null && labels.size() > 0) {
            label = "The image may contain ";
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

            // Set reset button visibility to visible
            mButtonReset.setVisibility(View.VISIBLE);

            // Setup and play the animations
            AnimatorSet translationSet = new AnimatorSet();
            translationSet.playSequentially(scoreViewAnimations);
            translationSet.setDuration(300);

            AnimatorSet alphaSet = new AnimatorSet();
            alphaSet.playSequentially(scoreAlphaAnimations);
            alphaSet.setDuration(300);

            AnimatorSet showScoreSet = new AnimatorSet();
            showScoreSet.playTogether(showScoreAnimations);

            showLoading(false);

            AnimatorSet set = new AnimatorSet();
            set.play(translationSet).with(alphaSet).before(showScoreSet);
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mButtonReset.animate().alpha(1f).start();
                }
            });
            set.start();
        } else {
            // Set reset button visibility to visible
            mButtonReset.setVisibility(View.VISIBLE);
            mButtonReset.setAlpha(1f);
        }

        // Handle detected faces
        String facesFound = "";
        if(faces != null && faces.size() > 0) {
            FaceGraphicOverlay faceGraphicOverlay = new FaceGraphicOverlay(MainActivity.this);
            faceGraphicOverlay.addFaces(faces);
            faceGraphicOverlay.setTag("faceOverlay");
            mCameraPreviewLayout.addView(faceGraphicOverlay);

            facesFound = FaceFoundHelper.getFacesFoundString(this, faces);
        }

        // Add the detected image data to TTS engine
        mTts.speak(label, TextToSpeech.QUEUE_FLUSH, null);
        mTts.speak(facesFound, TextToSpeech.QUEUE_ADD, null);
    }

    /**
     * Reset the camera preview
     */
    private void resetPreview() {
        /**
         * TODO
         *
         * Implement animation to fade out/translate ScoreViews
         *
         */
        mProcessingLayout.animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mProcessingLayout.setVisibility(View.GONE);
                        mProcessingLayout.setAlpha(1f);

                        // Remove all child views
                        mScoreResultLayout.removeAllViews();

                        // Hide the reset button
                        mButtonReset.setAlpha(0f);
                        mButtonReset.setVisibility(View.GONE);

                        // Start camera preview and set click listener
                        mCameraPreviewLayout.setOnClickListener(MainActivity.this);
                        mCamera.startPreview();

                        mCameraPreviewLayout.removeView(mCameraPreviewLayout.findViewWithTag("faceOverlay"));
                    }
                }).start();
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length + " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Image recognition")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }
}
