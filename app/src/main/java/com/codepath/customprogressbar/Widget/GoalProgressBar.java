package com.codepath.customprogressbar.Widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.codepath.customprogressbar.R;

public class GoalProgressBar extends View {

    public enum ProgressState {FORWARD, BACKWARDS, HALF_FORWARD, HALF_BACKWARDS}

    private Paint progressPaint;
    private int progress;
    private float wrappingHeight;
    private int firstColor, secondColor, thirdColor, fourthColor;
    private int unfilledSectionColor;
    private int barThickness;
    private ValueAnimator barAnimator;
    private boolean isRunning;

    private int[] mColors;
    private int mErrorColor;
    private int numOfFields = 5;
    private int progressDiff = -1;
    private int currentColorBlockPosition = 1;
    private int previousProgress;
    private int previousProgressEndX;
    private boolean isErrorState;
    private boolean isInBackwardsState;

    private double currentFieldPosition; // double to account for half steps

    private static final int ANIMATION_SPEED = 200; // completion time in ms

    public GoalProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        progressPaint = new Paint();
        progressPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        isErrorState = false;
        isInBackwardsState = false;
        isRunning = false;
        currentFieldPosition = 1;

        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.GoalProgressBar, 0, 0);
        try {
            setProgressBarHeight(typedArray.getDimensionPixelSize(R.styleable.GoalProgressBar_progressBarHeight, 3));
            setFirstColor(typedArray.getColor(R.styleable.GoalProgressBar_firstColor, Color.BLACK));
            setSecondColor(typedArray.getColor(R.styleable.GoalProgressBar_secondColor, Color.BLUE));
            setThirdColor(typedArray.getColor(R.styleable.GoalProgressBar_thirdColor, Color.BLACK));
            setFourthColor(typedArray.getColor(R.styleable.GoalProgressBar_fourthColor, Color.BLUE));
            setErrorColor(typedArray.getColor(R.styleable.GoalProgressBar_errorColor, Color.RED));
            setUnfilledSectionColor(typedArray.getColor(R.styleable.GoalProgressBar_unfilledSectionColor, Color.RED));
            setBarThickness(typedArray.getDimensionPixelOffset(R.styleable.GoalProgressBar_barThickness, 3));
            mColors = new int[] {firstColor, secondColor, thirdColor, fourthColor};
            previousProgress = 0;
        } finally {
            typedArray.recycle();
        }
    }

    public void initNumOfFields(@NonNull int numOfFields) {
        // arbitrary max number to keep the range within nice working numbers
        if (numOfFields > 0 && numOfFields < 30) {
            this.numOfFields = numOfFields;
            currentFieldPosition = 1;
            progressDiff = 100 / numOfFields;
            double newProgress = currentFieldPosition / numOfFields * 100;
            previousProgressEndX = (int) (getWidth() * newProgress / 100f);
            this.progress = (int) newProgress;
            setProgress(this.progress, true);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int halfHeight = getHeight() / 2;
        int progressEndX = (int) (getWidth() * progress / 100f);
        int choice;
        if (!isInBackwardsState) {
            choice = progressEndX > previousProgressEndX ? progressEndX : previousProgressEndX;
        } else {
            choice = progressEndX <= previousProgressEndX ? progressEndX : previousProgressEndX;
        }

        int sectionProgressEndX = choice * currentColorBlockPosition / mColors.length;
        int sectionProgressStartX = 0;

        for (int i = 0; i < mColors.length; i++) {
            progressPaint.setStrokeWidth(barThickness);
            int color;
            if (isErrorState) {
                color = mErrorColor;
            } else {
                color = mColors[i];
            }
            progressPaint.setColor(color);
            canvas.drawLine(sectionProgressStartX, halfHeight, sectionProgressEndX, halfHeight, progressPaint);
            sectionProgressStartX = sectionProgressEndX;
            currentColorBlockPosition++;
            sectionProgressEndX = choice * currentColorBlockPosition / mColors.length;
        }
        previousProgressEndX = choice;
        currentColorBlockPosition = 1;

        // draw the unfilled portion of the bar
        if (isErrorState) {
            progressPaint.setColor(mErrorColor);
        } else {
            progressPaint.setColor(unfilledSectionColor);
        }
        canvas.drawLine(choice, halfHeight, getWidth(), halfHeight, progressPaint);
        Rect outRect = new Rect();
        getDrawingRect(outRect);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // This is where the progress bar dimensions are measured out.
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int specHeight = MeasureSpec.getSize(heightMeasureSpec);
        int height;
        switch (MeasureSpec.getMode(heightMeasureSpec)) {

            // be exactly the given specHeight
            case MeasureSpec.EXACTLY:
                height = specHeight;
                break;

            // be at most the given specHeight
            case MeasureSpec.AT_MOST:
                height = (int) Math.min(wrappingHeight, specHeight);
                break;

            // be whatever size you want
            case MeasureSpec.UNSPECIFIED:
            default:
                height = specHeight;
                break;
        }

        // must call this, otherwise the app will crash
        setMeasuredDimension(width, height);
    }

    /**
     * This is the setProgress that outside classes will use.
     * @param state     state moves the progressBar forward or back
     */
    public void setProgress(ProgressState state) {
        if (!isRunning) {
            if (state == ProgressState.BACKWARDS) {
                if ((progress - progressDiff) > 0
                        && (currentFieldPosition - 1) >= 1) {
                    progress -= progressDiff;
                    currentFieldPosition -= 1;
                } else if ((progress - (progressDiff / 2) > 0
                        && (currentFieldPosition - 0.5) >= 1)) {
                    progress -= (progressDiff / 2);
                    currentFieldPosition -= 0.5;
                }
                setInBackwardsState(true);
            } else if(state == ProgressState.HALF_BACKWARDS) {
                if ((progress - (progressDiff / 2)) > 0
                        && (currentFieldPosition - 0.5) >= 1) {
                    progress -= (progressDiff / 2);
                    currentFieldPosition -= 0.5;
                    setInBackwardsState(true);
                }
            } else if (state == ProgressState.FORWARD) {
                if ((progress + progressDiff) < 100) {
                    progress += progressDiff;
                    currentFieldPosition += 1;
                } else if (progress + progressDiff >= 100) {
                    progress = 100;
                    currentFieldPosition = numOfFields;
                } else if ((progress + (progressDiff / 2)) <= 100) {
                    progress += (progressDiff / 2);
                    currentFieldPosition += 0.5;
                }
                setInBackwardsState(false);
            } else if (state == ProgressState.HALF_FORWARD) {
                if ((progress + (progressDiff / 2)) <= 100) {
                    progress += (progressDiff / 2);
                    currentFieldPosition += 0.5;
                    setInBackwardsState(false);
                }
            }
            setProgress(completeProgressIfNecessary(state, progress), true);
        }
    }

    private void setProgress(final int progress, boolean animate) {
        if (animate) {
            if (progress == 100) {
                barAnimator = ValueAnimator.ofFloat(previousProgress/100f, 1);
            } else {
                barAnimator = ValueAnimator.ofFloat(progress / 100f, 1);
            }

            barAnimator.setDuration(ANIMATION_SPEED);

            if (!isInBackwardsState) {
                barAnimator.setInterpolator(new LinearInterpolator());
            } else {
                barAnimator.setInterpolator(new ReverseInterpolater());
            }

            barAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float interpolation = (float) animation.getAnimatedValue();
                    if (!isInBackwardsState) {
                        interpolation *= progress;
                    }
                    else {
                        if ((interpolation * previousProgress) >= progress) {
                            interpolation *= previousProgress;
                        } else {
                            interpolation = progress;
                        }
                    }
                    setProgress((int) (interpolation), false);
                }
            });

            barAnimator.addListener(new ValueAnimator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {
                    isRunning = true;
                    previousProgressEndX = (int) (getWidth() * previousProgress / 100f);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    previousProgress = progress;
                    isRunning = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });

            if (!barAnimator.isStarted()) {
                barAnimator.start();
            }
        } else {
            this.progress = progress;
            postInvalidate();
        }
    }

    /**
     * Sometimes due to loss of precision the progress won't reach a full 100% so
     * this will just complete the entire progress so you won't see a portion incomplete
     * @param state
     * @param oldProgress
     * @return
     */
    private int completeProgressIfNecessary(ProgressState state, int oldProgress) {
        if (state == ProgressState.HALF_FORWARD || state == ProgressState.FORWARD) {
            if (oldProgress > (100 - progressDiff / 2)) {
                return 100;
            }
            if (oldProgress >= 100) {
                return 100;
            }
        }
        return oldProgress;
    }

    /**
     * This is for the overlay on the custom EditText - place in lieu of the
     * default underline
     *
     * @return Rect
     */
    public Rect getDimensionsForPlacement() {
        Rect out = new Rect();
        getDrawingRect(out);
        return out;
    }

    /**
     * turns the entire bar red. No animation for the color change.
     * @param ans
     */
    public void setErrorState(boolean ans) {
        this.isErrorState = ans;
        postInvalidate();
    }

    /**
     * This method will put decrease the user's progress
     * @param ans
     */
    public void setInBackwardsState(boolean ans) {
        this.isInBackwardsState = ans;
    }

    /**
     * Method that sets the progressbar height
     * @param goalIndicatorHeight
     */
    public void setProgressBarHeight(float goalIndicatorHeight) {
        this.wrappingHeight = goalIndicatorHeight;
        postInvalidate();
    }

    public void setBarThickness(int barThickness) {
        this.barThickness = barThickness;
        postInvalidate();
    }

    private class ReverseInterpolater implements Interpolator {
        //used only for when the progress decreases
        @Override
        public float getInterpolation(float input) {
            return Math.abs(input -1f);
        }
    }

    /** These set the experian color blocks **/
    private void setFirstColor(int color) {
        this.firstColor = color;
        postInvalidate();
    }

    private void setSecondColor(int color) {
        this.secondColor = color;
        postInvalidate();
    }

    private void setThirdColor(int color) {
        this.thirdColor = color;
        postInvalidate();
    }

    private void setFourthColor(int color) {
        this.fourthColor = color;
        postInvalidate();
    }

    private void setUnfilledSectionColor(int unfilledSectionColor) {
        this.unfilledSectionColor = unfilledSectionColor;
        postInvalidate();
    }

    private void setErrorColor(int color) {
        this.mErrorColor = color;
        postInvalidate();
    }
}