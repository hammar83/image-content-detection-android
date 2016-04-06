package me.hammarstrom.imagerecognition.vision;

import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;

/**
 * Created by Fredrik Hammarström on 03/04/16.
 */
public interface CloudVisionTaskDoneListener {
    void onTaskDone(BatchAnnotateImagesResponse response);
}
