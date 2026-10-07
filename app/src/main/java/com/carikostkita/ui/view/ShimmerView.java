package com.carikostkita.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Pembungkus skeleton dengan kilau bergerak dari kiri ke kanan.
 * Kilau hanya digambar di atas bentuk skeleton (SRC_ATOP), sehingga area kosong tetap bersih.
 * Animasi berjalan otomatis selama view terlihat dan berhenti saat disembunyikan.
 */
public class ShimmerView extends FrameLayout {

    private static final long DURATION_MS = 1300;

    private final Paint shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix shaderMatrix = new Matrix();
    private LinearGradient gradient;
    private ValueAnimator animator;
    private float progress = 0f;

    public ShimmerView(@NonNull Context context) {
        this(context, null);
    }

    public ShimmerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        shimmerPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float band = Math.max(w * 0.45f, 1f);
        gradient = new LinearGradient(0, 0, band, 0,
                new int[]{0x00FFFFFF, 0x99FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
        shimmerPaint.setShader(gradient);
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (gradient == null || getWidth() == 0) {
            super.dispatchDraw(canvas);
            return;
        }
        int save = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
        super.dispatchDraw(canvas);
        float width = getWidth();
        float offset = -width * 0.45f + progress * (width * 1.45f);
        shaderMatrix.setTranslate(offset, 0);
        shaderMatrix.postSkew(-0.25f, 0);
        gradient.setLocalMatrix(shaderMatrix);
        canvas.drawRect(0, 0, getWidth(), getHeight(), shimmerPaint);
        canvas.restoreToCount(save);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimation();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateAnimation();
    }

    private void updateAnimation() {
        if (isAttachedToWindow() && isShown()) startAnimation();
        else stopAnimation();
    }

    private void startAnimation() {
        if (animator != null && animator.isRunning()) return;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DURATION_MS);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private void stopAnimation() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }
}
