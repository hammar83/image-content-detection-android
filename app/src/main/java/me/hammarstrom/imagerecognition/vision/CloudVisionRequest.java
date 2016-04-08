package me.hammarstrom.imagerecognition.vision;

import android.graphics.Bitmap;
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
import rx.Observable;
import rx.Subscriber;

/**
 * Created by Fredrik Hammarström on 08/04/16.
 */
public class CloudVisionRequest {

    private static final String TAG = "CloudVisionRequest";

    private static final String VISION_TYPE_LABEL = "LABEL_DETECTION";
    private static final String VISION_TYPE_FACE = "FACE_DETECTION";

    private static final int LABEL_MAX_RESULT = 5;
    private static final int FACES_MAX_RESULT = 10;

    public static Observable<BatchAnnotateImagesResponse> doRequest(final Bitmap b) {
        return Observable.create(new Observable.OnSubscribe<BatchAnnotateImagesResponse>() {
            @Override
            public void call(Subscriber<? super BatchAnnotateImagesResponse> subscriber) {
                subscriber.onNext(doVisionRequest(b));
            }
        });
    }

    private static BatchAnnotateImagesResponse doVisionRequest(Bitmap b) {
        final Bitmap bitmap = ImageHelper.scaleBitmapDown(b, 1100);

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
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                byte[] imageBytes = byteArrayOutputStream.toByteArray();

                // Base64 encode the JPEG
                base64EncodedImage.encodeContent(imageBytes);
                annotateImageRequest.setImage(base64EncodedImage);

                // add the features we want
                annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                    Feature labelDetection = new Feature();
                    labelDetection.setType(VISION_TYPE_LABEL);
                    labelDetection.setMaxResults(LABEL_MAX_RESULT);
                    add(labelDetection);

                    Feature faceDetection = new Feature();
                    faceDetection.setType(VISION_TYPE_FACE);
                    faceDetection.setMaxResults(FACES_MAX_RESULT);
                    add(faceDetection);
                }});



                // Add the list of one thing to the request
                add(annotateImageRequest);
            }});

            final Vision.Images.Annotate annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);

            // Due to a bug: requests to Vision API containing large images fail when GZipped.
            annotateRequest.setDisableGZipContent(true);


            return annotateRequest.execute();

        } catch (GoogleJsonResponseException e) {
            Log.d(TAG, "failed to make API request because " + e.getContent());
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " +
                    e.getMessage());
        }

        return null;
    }

}
