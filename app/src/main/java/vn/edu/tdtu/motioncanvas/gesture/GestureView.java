package vn.edu.tdtu.motioncanvas.gesture;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import vn.edu.tdtu.motioncanvas.R;

/** Nhận cử chỉ và vẽ thẻ trong hệ tọa độ riêng của Canvas */
public class GestureView extends View {
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 3f;
    private static final int NO_POINTER = -1;

    private static final FloatPropertyCompat<GestureView> CENTER_X =
            new FloatPropertyCompat<>("centerX") {
                @Override
                public float getValue(GestureView view) {
                    return view.centerX;
                }

                @Override
                public void setValue(GestureView view, float value) {
                    view.centerX = value;
                    view.invalidate();
                }
            };

    private static final FloatPropertyCompat<GestureView> CENTER_Y =
            new FloatPropertyCompat<>("centerY") {
                @Override
                public float getValue(GestureView view) {
                    return view.centerY;
                }

                @Override
                public void setValue(GestureView view, float value) {
                    view.centerY = value;
                    view.invalidate();
                }
            };

    private static final FloatPropertyCompat<GestureView> SCALE =
            new FloatPropertyCompat<>("cardScale") {
                @Override
                public float getValue(GestureView view) {
                    return view.cardScale;
                }

                @Override
                public void setValue(GestureView view, float value) {
                    view.cardScale = value;
                    view.invalidate();
                }
            };

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subtitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF cardRect = new RectF();
    private final float halfWidth;
    private final float halfHeight;
    private final float cornerRadius;
    private final String title;
    private final String subtitle;
    private final FlingAnimation flingX;
    private final FlingAnimation flingY;
    private final SpringAnimation springX;
    private final SpringAnimation springY;
    private final SpringAnimation springScale;
    private final GestureDetector gestures;
    private final ScaleGestureDetector scales;
    private float centerX;
    private float centerY;
    private float cardScale = 1f;
    private float lastX;
    private float lastY;
    private float scaleFocusX;
    private float scaleFocusY;
    private int activePointer = NO_POINTER;
    private boolean touchingCard;
    private boolean usedMultiplePointers;
    private boolean resetting;
    private boolean flung;
    private boolean tapped;

    public GestureView(Context context) {
        this(context, null);
    }

    public GestureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = getResources().getDisplayMetrics().density;
        halfWidth = 100f * density;
        halfHeight = 140f * density;
        cornerRadius = 24f * density;
        cardRect.set(-halfWidth, -halfHeight, halfWidth, halfHeight);
        cardPaint.setShader(new LinearGradient(-halfWidth, -halfHeight, halfWidth, halfHeight,
                0xFF673AB7, 0xFF1976D2, Shader.TileMode.CLAMP));
        title = context.getString(R.string.gesture_card_title);
        subtitle = context.getString(R.string.gesture_card_subtitle);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                22f, getResources().getDisplayMetrics()));
        titlePaint.setFakeBoldText(true);
        subtitlePaint.setColor(Color.WHITE);
        subtitlePaint.setTextAlign(Paint.Align.CENTER);
        subtitlePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                12f, getResources().getDisplayMetrics()));
        // Chữ trên thẻ phải vừa thẻ; hướng dẫn bên dưới vẫn theo cỡ chữ hệ thống
        fitText(titlePaint, title, 2f * halfWidth - 24f * density);
        fitText(subtitlePaint, subtitle, 2f * halfWidth - 24f * density);

        // Preview chỉ cần vẽ thẻ, không khởi tạo bộ nhận chạm và animation
        if (isInEditMode()) {
            flingX = null;
            flingY = null;
            springX = null;
            springY = null;
            springScale = null;
            gestures = null;
            scales = null;
            return;
        }

        flingX = new FlingAnimation(this, CENTER_X).setFriction(1.1f);
        flingY = new FlingAnimation(this, CENTER_Y).setFriction(1.1f);
        springX = new SpringAnimation(this, CENTER_X);
        springY = new SpringAnimation(this, CENTER_Y);
        springScale = new SpringAnimation(this, SCALE);
        springScale.setMinValue(MIN_SCALE).setMaxValue(MAX_SCALE);
        springScale.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE);

        gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent event) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent event) {
                tapped = true;
                return true;
            }

            @Override
            public boolean onFling(@Nullable MotionEvent first, @NonNull MotionEvent last, float vx, float vy) {
                if (!touchingCard || usedMultiplePointers || resetting) return false;
                flung = true;
                releaseCard(vx, vy);
                return true;
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent event) {
                resetting = true;
                resetToCenter();
                return true;
            }
        });
        gestures.setIsLongpressEnabled(false);

        scales = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
                        scaleFocusX = detector.getFocusX();
                        scaleFocusY = detector.getFocusY();
                        return touchingCard;
                    }

                    @Override
                    public boolean onScale(@NonNull ScaleGestureDetector detector) {
                        float factor = detector.getScaleFactor();
                        if (!Float.isFinite(factor) || factor <= 0f) return false;
                        float nextScale = GestureMath.clamp(cardScale * factor, MIN_SCALE, MAX_SCALE);
                        float ratio = nextScale / cardScale;
                        // Giữ điểm dưới hai ngón tay khi đổi tỉ lệ
                        centerX = detector.getFocusX() + (centerX - scaleFocusX) * ratio;
                        centerY = detector.getFocusY() + (centerY - scaleFocusY) * ratio;
                        cardScale = nextScale;
                        scaleFocusX = detector.getFocusX();
                        scaleFocusY = detector.getFocusY();
                        keepCenterVisible();
                        invalidate();
                        return true;
                    }
                });
        // Chạm đúp dành riêng cho spring, không dùng quick scale một ngón
        scales.setQuickScaleEnabled(false);
        scales.setStylusScaleEnabled(false);
        setClickable(true);
    }

    private static void fitText(Paint paint, String text, float width) {
        float measured = paint.measureText(text);
        if (measured > width) paint.setTextSize(paint.getTextSize() * width / measured);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        pause();
        centerX = w / 2f;
        centerY = h / 2f;
        cardScale = 1f;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0xFFECEFF1);
        int saved = canvas.save();
        canvas.translate(centerX, centerY);
        canvas.scale(cardScale, cardScale);
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardPaint);
        canvas.drawText(title, 0f, -titlePaint.getTextSize() * 0.3f, titlePaint);
        canvas.drawText(subtitle, 0f, subtitlePaint.getTextSize() * 1.8f, subtitlePaint);
        canvas.restoreToCount(saved);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isInEditMode()) return false;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            // Hủy trước khi đưa vào detector để không hủy spring của lần chạm đúp
            cancelAnimations();
            activePointer = event.getPointerId(0);
            lastX = event.getX();
            lastY = event.getY();
            touchingCard = Math.abs(lastX - centerX) <= halfWidth * cardScale
                    && Math.abs(lastY - centerY) <= halfHeight * cardScale;
            usedMultiplePointers = false;
            resetting = false;
            flung = false;
            tapped = false;
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
        } else if (activePointer == NO_POINTER) {
            return false;
        }

        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            usedMultiplePointers = true;
            resetting = false;
            cancelAnimations();
        }
        scales.onTouchEvent(event);
        gestures.onTouchEvent(event);

        if (action == MotionEvent.ACTION_MOVE) {
            int index = event.findPointerIndex(activePointer);
            if (index < 0) {
                pause();
                return true;
            }
            float x = event.getX(index);
            float y = event.getY(index);
            if (touchingCard && !resetting && event.getPointerCount() == 1) {
                centerX += x - lastX;
                centerY += y - lastY;
                keepCenterVisible();
                invalidate();
            }
            lastX = x;
            lastY = y;
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            // Đổi ID và mốc tọa độ cùng lúc để thẻ không nhảy theo ngón mới
            int lifted = event.getActionIndex();
            if (event.getPointerId(lifted) == activePointer) {
                int remaining = lifted == 0 ? 1 : 0;
                activePointer = event.getPointerId(remaining);
            }
            int index = event.findPointerIndex(activePointer);
            lastX = event.getX(index);
            lastY = event.getY(index);
        } else if (action == MotionEvent.ACTION_UP) {
            if (tapped) performClick();
            if (!resetting && !flung) releaseCard(0f, 0f);
            finishTouch();
        } else if (action == MotionEvent.ACTION_CANCEL) {
            cancelAnimations();
            finishTouch();
        }
        return true;
    }

    private void keepCenterVisible() {
        // Cho kéo ra nửa thẻ để minh họa xử lý biên khi thả
        centerX = GestureMath.clamp(centerX, 0f, getWidth());
        centerY = GestureMath.clamp(centerY, 0f, getHeight());
    }

    private void releaseCard(float vx, float vy) {
        if (getWidth() == 0 || getHeight() == 0) return;
        float[] horizontal = GestureMath.centerRange(getWidth(), halfWidth * cardScale);
        float[] vertical = GestureMath.centerRange(getHeight(), halfHeight * cardScale);
        releaseAxis(flingX, springX, centerX, vx, horizontal);
        releaseAxis(flingY, springY, centerY, vy, vertical);
    }

    private void releaseAxis(FlingAnimation fling, SpringAnimation spring,
                             float position, float velocity, float[] range) {
        float target = GestureMath.clamp(position, range[0], range[1]);
        if (target != position) {
            // Không chạy fling từ ngoài biên; spring kéo về mép gần nhất
            startSpring(spring, target, 1f);
        } else if (range[0] < range[1] && velocity != 0f) {
            fling.setMinValue(range[0]).setMaxValue(range[1]);
            fling.setStartValue(position).setStartVelocity(velocity).start();
        }
    }

    private void resetToCenter() {
        cancelAnimations();
        startSpring(springX, getWidth() / 2f, 0.65f);
        startSpring(springY, getHeight() / 2f, 0.65f);
        startSpring(springScale, 1f, 0.65f);
    }

    private void startSpring(SpringAnimation animation, float target, float damping) {
        animation.setSpring(new SpringForce(target)
                .setStiffness(SpringForce.STIFFNESS_LOW)
                .setDampingRatio(damping));
        animation.setStartVelocity(0f);
        animation.start();
    }

    private void cancelAnimations() {
        flingX.cancel();
        flingY.cancel();
        springX.cancel();
        springY.cancel();
        springScale.cancel();
    }

    private void finishTouch() {
        activePointer = NO_POINTER;
        touchingCard = false;
        usedMultiplePointers = false;
        resetting = false;
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
    }

    /** Dừng animation và bỏ cử chỉ dở khi rời màn */
    public void pause() {
        if (isInEditMode()) return;
        cancelAnimations();
        long now = SystemClock.uptimeMillis();
        MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0);
        scales.onTouchEvent(cancel);
        gestures.onTouchEvent(cancel);
        cancel.recycle();
        finishTouch();
    }

    @Override
    protected void onDetachedFromWindow() {
        pause();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
