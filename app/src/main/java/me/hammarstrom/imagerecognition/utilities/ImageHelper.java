package me.hammarstrom.imagerecognition.utilities;

import android.graphics.Bitmap;

/**
 * Helper class to handle images to be analyzed
 * by the Vision API.
 *
 * Created by Fredrik Hammarström on 07/04/16.
 */
public class ImageHelper {

    // Static members to hold latest scaled bitmap size
    public static float imageWidth = 800f;
    public static float imageHeight = 600f;

    /**
     * Scale bitmap down
     *
     * @param bitmap The bitmap to be scaled
     * @param maxDimension The maximum dimension (height or width)
     * @return Scaled bitmap
     */
    public static Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }

        ImageHelper.imageWidth = (float) resizedWidth;
        ImageHelper.imageHeight = (float) resizedHeight;

        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

}
