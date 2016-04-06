package me.hammarstrom.imagerecognition.utilities;

import android.content.Context;

import com.google.api.services.vision.v1.model.FaceAnnotation;

import java.util.List;

import me.hammarstrom.imagerecognition.R;

/**
 * Helper class to analyze detected faces {@link FaceAnnotation}
 * and create text to be used by TextToSpeech engine.
 *
 * Created by Fredrik Hammarström on 06/04/16.
 */
public class FaceFoundHelper {

    public static String getFacesFoundString(Context context, List<FaceAnnotation> faces) {
        String facesFound = "";

        facesFound = "I think the image also contains ";
        facesFound += faces.size() > 1 ? faces.size() + " faces" : " 1 face.";

        for(int i = 0; i < faces.size(); i++) {
            FaceAnnotation f = faces.get(i);
            facesFound += "... Face " + (i + 1);
            facesFound += getFaceData(context, f);
        }

        return facesFound;
    }

    private static String getFaceData(Context context, FaceAnnotation f) {
        boolean foundExpression = false;
        String faceData = "";

        if(isExpressionLikely(f.getJoyLikelihood())) {
            faceData += context.getString(R.string.face_data_expression, context.getString(R.string.happy));
            foundExpression = true;
        }

        if(isExpressionLikely(f.getSorrowLikelihood())) {
            faceData += context.getString(R.string.face_data_expression, context.getString(R.string.sad));
            foundExpression = true;
        }

        if(isExpressionLikely(f.getAngerLikelihood())) {
            faceData += context.getString(R.string.face_data_expression, context.getString(R.string.angry));
            foundExpression = true;
        }

        if(isExpressionLikely(f.getSurpriseLikelihood())) {
            faceData += context.getString(R.string.face_data_expression, context.getString(R.string.surprised));
            foundExpression = true;
        }

        if(!foundExpression) {
            faceData += context.getString(R.string.face_data_expression_not_detected);
        }

        return faceData;
    }

    private static boolean isExpressionLikely(String expression) {
        return !expression.contains("UNLIKELY");
    }

}
