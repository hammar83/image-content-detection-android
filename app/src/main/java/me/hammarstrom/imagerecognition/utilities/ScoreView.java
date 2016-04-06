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

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.hammarstrom.imagerecognition.R;

/**
 * Created by Fredrik Hammarström on 04/04/16.
 */

public class ScoreView extends View {

    private Rect mLabelTextBounds;
    private Rect mScoreTextBounds;

    private Paint mOverflowPaint;
    private Paint mScorePaint;
    private Paint mLabelTextPaint;
    private Paint mScoreTextPaint;

    private int mOverflowColor;
    private int mScoreColor;
    private int mRadius;
    private int mCenterX;
    private int mCenterY;
    private int mLabelTextX;
    private int mLabelTextY;
    private int mScoreTextX;
    private int mScoreTextY;

    private float mSweepAngle;
    private float mScore;

    private String mLabelText;
    private String mScoreText;

    private int mLabelPosition;

    public static final int LABEL_POSITION_LEFT = 0;
    public static final int LABEL_POSITION_TOP = 1;
    public static final int LABEL_POSITION_RIGHT = 2;
    public static final int LABEL_POSITION_BOTTOM = 3;


    public ScoreView(Context context) {
        this(context, null);
    }

    public ScoreView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScoreView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ScoreView, 0, 0);

        try {
            mOverflowColor = typedArray.getColor(R.styleable.ScoreView_overflowColor, Color.parseColor("#70FFFFFF"));
            mScoreColor = typedArray.getColor(R.styleable.ScoreView_scoreColor, Color.parseColor("#2196F3"));
            setScore(typedArray.getFloat(R.styleable.ScoreView_score, 0.0f));
            switch (typedArray.getInt(R.styleable.ScoreView_labelPosition, LABEL_POSITION_RIGHT)) {
                case LABEL_POSITION_LEFT:
                    mLabelPosition = LABEL_POSITION_LEFT;
                    break;
                case LABEL_POSITION_TOP:
                    mLabelPosition = LABEL_POSITION_TOP;
                    break;
                case LABEL_POSITION_RIGHT:
                    mLabelPosition = LABEL_POSITION_RIGHT;
                    break;
                case LABEL_POSITION_BOTTOM:
                    mLabelPosition = LABEL_POSITION_BOTTOM;
                    break;
            }
            mLabelText = typedArray.getString(R.styleable.ScoreView_labelText);
        } finally {
            typedArray.recycle();
        }

        initiate();
    }

    private void initiate() {
        mRadius = (int) DeviceDimensionsHelper.convertDpToPixel(80.0f, getContext());
        mCenterX = getLeft() + mRadius;
        mCenterY = getTop() + mRadius;
        mSweepAngle = 0.0f;

        if (mLabelText == null || mLabelText.length() == 0) {
            mLabelText = "";
        }

        mScoreText = String.format(Locale.getDefault(), "%.2f", mScore);

        mOverflowPaint = new Paint();
        mOverflowPaint.setColor(mOverflowColor);
        mOverflowPaint.setAntiAlias(true);
        mOverflowPaint.setStrokeWidth(10);
        mOverflowPaint.setStyle(Paint.Style.STROKE);
        mOverflowPaint.setStrokeJoin(Paint.Join.ROUND);
        mOverflowPaint.setStrokeCap(Paint.Cap.ROUND);

        mScorePaint = new Paint();
        mScorePaint.setColor(mScoreColor);
        mScorePaint.setAntiAlias(true);
        mScorePaint.setStrokeWidth(10);
        mScorePaint.setStyle(Paint.Style.STROKE);
        mOverflowPaint.setStrokeJoin(Paint.Join.ROUND);
        mOverflowPaint.setStrokeCap(Paint.Cap.ROUND);

        mLabelTextPaint = new Paint();
        mLabelTextPaint.setColor(Color.WHITE);
        mLabelTextPaint.setAntiAlias(true);
        mLabelTextPaint.setTypeface(Typeface.SANS_SERIF);
        mLabelTextPaint.setTextSize(DeviceDimensionsHelper.convertDpToPixel(14, getContext()));

        mLabelTextBounds = new Rect();
        mLabelTextPaint.getTextBounds(mLabelText, 0, mLabelText.length(), mLabelTextBounds);

        mScoreTextPaint = new Paint();
        mScoreTextPaint.setColor(Color.WHITE);
        mScoreTextPaint.setAntiAlias(true);
        mScoreTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        mScoreTextPaint.setTextSize(DeviceDimensionsHelper.convertDpToPixel(16, getContext()));

        mScoreTextBounds = new Rect();
        mScoreTextPaint.getTextBounds(mScoreText, 0, mScoreText.length(), mScoreTextBounds);
    }

    /**
     * Set label position
     *
     * @param {@link me.hammarstrom.imagerecognition.utilities.ScoreView.LabelPosition} labelPosition
     */
    public void setLabelPosition(int labelPosition) {
        mLabelPosition = labelPosition;
    }

    /**
     * Get label position
     *
     * @return mLabelPosition
     */
    public int getLabelPosition() {
        return mLabelPosition;
    }

    /**
     * Get current score value
     *
     * @return float mScore
     */
    public float getScore() {
        return mScore;
    }

    /**
     * Set new score value
     *
     * @param score between 0 and 1
     * @throws IllegalArgumentException
     */
    public void setScore(float score) throws IllegalArgumentException {
        if (score < 0.0f || score > 1.0f) {
            throw new IllegalArgumentException("Score has to be a value between 0.0 and 1.0");
        }
        mScore = score;

    }

    public void setLabelText(String text) {
        mLabelText = text;
        mLabelTextBounds = new Rect();
        mLabelTextPaint.getTextBounds(mLabelText, 0, mLabelText.length(), mLabelTextBounds);
        requestLayout();
        invalidate();
    }

    /**
     * Hide the view (animated)
     */
    public void hide() {
        animate().scaleX(0.0f).scaleY(0.0f).setDuration(300).start();
    }

    /**
     * Show the view (animated)
     */
    public void show() {
        animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new OvershootInterpolator()).start();
    }

    /**
     * Get show score animations
     *
     * @return list
     */
    public List<Animator> getShowScoreAnimationsList() {
        float max = 360.0f;
        float toValue = max * mScore;

        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, toValue);
        animator.setDuration(300);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mSweepAngle = (float) animation.getAnimatedValue();
                invalidate();
            }
        });

        ValueAnimator countUpAnimation = new ValueAnimator();
        countUpAnimation.setObjectValues(0.00f, mScore);
        countUpAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mScoreText = String.format(Locale.getDefault(), "%.2f", animation.getAnimatedValue());
            }
        });
        countUpAnimation.setEvaluator(new TypeEvaluator<Float>() {
            @Override
            public Float evaluate(float fraction, Float startValue, Float endValue) {
                return startValue + (endValue - startValue) * fraction;
            }
        });

        countUpAnimation.setDuration(300);

        List<Animator> list = new ArrayList<>();
        list.add(animator);
        list.add(countUpAnimation);

        return list;
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        switch (mLabelPosition) {
            case LABEL_POSITION_BOTTOM:
            case LABEL_POSITION_TOP:
            default:
                return (int) DeviceDimensionsHelper.convertDpToPixel(80, getContext());
            case LABEL_POSITION_LEFT:
            case LABEL_POSITION_RIGHT:
                return Math.round( Math.max(DeviceDimensionsHelper.convertDpToPixel(160, getContext()), (mRadius + getPaddingLeft() + mLabelTextBounds.width())) );
//                return (int) DeviceDimensionsHelper.convertDpToPixel(160, getContext());
        }
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        switch (mLabelPosition) {
            case LABEL_POSITION_BOTTOM:
            case LABEL_POSITION_TOP:
                return (int) DeviceDimensionsHelper.convertDpToPixel(100, getContext());
            case LABEL_POSITION_LEFT:
            case LABEL_POSITION_RIGHT:
            default:
                return (int) DeviceDimensionsHelper.convertDpToPixel(50, getContext());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
//        int w = Math.max(minw, MeasureSpec.getSize(widthMeasureSpec));

        int h = getPaddingTop() + getPaddingBottom() + getSuggestedMinimumHeight();
//        int h = Math.max(minh, MeasureSpec.getSize(heightMeasureSpec));

        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mRadius = Math.min( ((w - getPaddingRight() - getPaddingLeft()) / 2), ((h - getPaddingBottom() - getPaddingTop()) /2) );

        switch (mLabelPosition) {
            case LABEL_POSITION_LEFT:
                mLabelTextPaint.setTextAlign(Paint.Align.RIGHT);

                mCenterY = h / 2;
                mCenterX = w - mCenterY;

                mLabelTextX = getPaddingLeft() + mLabelTextBounds.width();
                mLabelTextY = mCenterY + (mLabelTextBounds.height() / 2);

                break;
            case LABEL_POSITION_TOP:
                mLabelTextPaint.setTextAlign(Paint.Align.CENTER);

                mCenterX = w / 2;
                mCenterY = h - getPaddingBottom() - mRadius;

                mLabelTextX = mCenterX;
                mLabelTextY = (mCenterY - mRadius) / 2;

                break;
            case LABEL_POSITION_RIGHT:
                mLabelTextPaint.setTextAlign(Paint.Align.LEFT);

                mCenterY = h / 2;
                mCenterX = mCenterY;

                mLabelTextX = mCenterX + mRadius + getPaddingLeft();
                mLabelTextY = mCenterY + (mLabelTextBounds.height() / 3);

                break;
            case LABEL_POSITION_BOTTOM:
                mLabelTextPaint.setTextAlign(Paint.Align.CENTER);

                mCenterX = w / 2;
                mCenterY = mCenterX;

                mLabelTextX = mCenterX;
                mLabelTextY = mCenterY + mRadius + ((h - (mCenterY + mRadius)) / 2) + (mLabelTextBounds.height() / 2);

                break;
        }

        mScoreTextX = mCenterX - (mScoreTextBounds.width() / 2);
        mScoreTextY = mCenterY + (mScoreTextBounds.height() / 3);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(mCenterX, mCenterY, mRadius, mOverflowPaint);
        canvas.drawText(mLabelText, mLabelTextX, mLabelTextY, mLabelTextPaint);
        canvas.drawText(mScoreText, mScoreTextX, mScoreTextY, mScoreTextPaint);

        RectF arc = new RectF();
        arc.set(mCenterX - mRadius, mCenterY - mRadius, mCenterX + mRadius, mCenterY + mRadius);
        canvas.drawArc(arc, -90, mSweepAngle, false, mScorePaint);

    }
}
