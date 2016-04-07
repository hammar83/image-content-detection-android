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
package me.hammarstrom.imagerecognition.vision;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import me.hammarstrom.imagerecognition.utilities.Constants;
import me.hammarstrom.imagerecognition.utilities.ImageHelper;

/**
 * Created by Fredrik Hammarström on 03/04/16.
 */
public class CloudVisionTask extends AsyncTask<Void, Void, BatchAnnotateImagesResponse> {
    private final String TAG = CloudVisionTask.this.getClass().getName();

    private Bitmap mBitmap;
    private CloudVisionTaskDoneListener mListener;

    public CloudVisionTask(Bitmap bitmap, CloudVisionTaskDoneListener listener) {
        mBitmap = ImageHelper.scaleBitmapDown(bitmap, 1100);
        mListener = listener;
    }

    @Override
    protected BatchAnnotateImagesResponse doInBackground(Void... params) {
        try {
            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            Vision vision = new Vision.Builder(httpTransport, jsonFactory, null)
                    .setApplicationName(Constants.APPLICATION_NAME)
                    .setVisionRequestInitializer(new VisionRequestInitializer(Constants.CLOUD_VISION_API_KEY))
                    .build();

            BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();


            batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                Image base64EncodedImage = new Image();

                // Convert the bitmap to a JPEG
                // Just in case it's a format that Android understands but Cloud Vision
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                byte[] imageBytes = byteArrayOutputStream.toByteArray();

                // Base64 encode the JPEG
                base64EncodedImage.encodeContent(imageBytes);
                annotateImageRequest.setImage(base64EncodedImage);

                // add the features we want
                annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                    Feature labelDetection = new Feature();
                    labelDetection.setType("LABEL_DETECTION");
                    labelDetection.setMaxResults(4);
                    add(labelDetection);

                    Feature faceDetection = new Feature();
                    faceDetection.setType("FACE_DETECTION");
                    faceDetection.setMaxResults(5);
                    add(faceDetection);
                }});



                // Add the list of one thing to the request
                add(annotateImageRequest);
            }});

            Vision.Images.Annotate annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);

            // Due to a bug: requests to Vision API containing large images fail when GZipped.
            annotateRequest.setDisableGZipContent(true);

            BatchAnnotateImagesResponse response = annotateRequest.execute();
            return response;

        } catch (GoogleJsonResponseException e) {
            Log.d(TAG, "failed to make API request because " + e.getContent());
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " +
                    e.getMessage());
        }

        return null;
    }

    @Override
    protected void onPostExecute(BatchAnnotateImagesResponse response) {
        super.onPostExecute(response);
        mListener.onTaskDone(response);
    }

}
