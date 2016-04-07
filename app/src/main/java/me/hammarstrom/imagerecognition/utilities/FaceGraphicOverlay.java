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
package me.hammarstrom.imagerecognition.utilities;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.google.api.services.vision.v1.model.BoundingPoly;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Vertex;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple view class to be used as an overlay
 * to mark out the detected face(s) in the camera
 * preview image.
 *
 * Created by Fredrik Hammarström on 07/04/16.
 */
public class FaceGraphicOverlay extends View {
    private final String TAG = FaceGraphicOverlay.this.getClass().getName();

    private List<BoundingPoly> mBoundingPolys;
    private Paint mPaint;

    public FaceGraphicOverlay(Context context) {
        this(context, null);
    }

    public FaceGraphicOverlay(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FaceGraphicOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mBoundingPolys = new ArrayList<>();

        mPaint = new Paint();
        mPaint.setColor(Color.parseColor("#2196F3"));
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(10);
    }

    /**
     * Add list bounding polys by supplying list of {@link FaceAnnotation}
     *
     * @param faceAnnotations
     */
    public void addFaces(List<FaceAnnotation> faceAnnotations) {
        for(FaceAnnotation f : faceAnnotations) {
            mBoundingPolys.add(f.getFdBoundingPoly());
        }

        requestLayout();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for(BoundingPoly bp : mBoundingPolys) {
            Path path = new Path();
            for(int i = 0; i < bp.getVertices().size(); i++) {
                Vertex v = bp.getVertices().get(i);

                // Have to re-calculate the vertex according to
                // image size sent to Vision API vs. canvas size
                float x = v.getX() / (ImageHelper.imageWidth  / (float) canvas.getWidth());
                float y = v.getY() / (ImageHelper.imageHeight / (float) canvas.getHeight());
                y -= (DeviceDimensionsHelper.getDisplayHeight(getContext()) - canvas.getHeight());

                if(i == 0) {
                    // Move to beginning of path
                    path.moveTo(x, y);
                } else {
                    // Draw line
                    path.lineTo(x, y);
                }

                // If we reached the last vertex, close the path!
                if(i == bp.getVertices().size() - 1) {
                    path.close();
                }
            }

            canvas.drawPath(path, mPaint);
        }

    }
}
