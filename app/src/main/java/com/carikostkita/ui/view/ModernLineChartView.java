package com.carikostkita.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.carikostkita.R;
import java.util.ArrayList;
import java.util.List;

/**
 * ModernLineChartView — Komponen Grafik Garis Interaktif Native
 * Didesain khusus untuk tema Maroon (#6D0808) & Warm Cream (#EEEAD7):
 * - Kurva Bezier halus dengan gradien lembut di bawah garis
 * - Titik interaktif dengan tooltip/scrubber sentuh
 * - Grid horizontal lembut dan label tanggal dinamis
 * - Responsif terhadap orientasi dan resolusi mobile
 */
public class ModernLineChartView extends View {

    public static class DataPoint {
        public final String label;
        public final float value;

        public DataPoint(String label, float value) {
            this.label = label;
            this.value = value;
        }
    }

    private final List<DataPoint> dataPoints = new ArrayList<>();
    private int selectedPointIndex = -1;

    // Paints
    private Paint linePaint;
    private Paint fillPaint;
    private Paint pointOuterPaint;
    private Paint pointInnerPaint;
    private Paint gridPaint;
    private Paint textPaint;
    private Paint tooltipBgPaint;
    private Paint tooltipTextPaint;

    // Paths
    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    // Dimensions & Paddings
    private float paddingLeft = 56f;
    private float paddingRight = 40f;
    private float paddingTop = 44f;
    private float paddingBottom = 60f;

    // Colors
    private int primaryColor;
    private int primaryDarkColor;
    private int gridColor;
    private int textColor;

    public ModernLineChartView(Context context) {
        super(context);
        init(context);
    }

    public ModernLineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ModernLineChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        primaryColor = ContextCompat.getColor(context, R.color.primary);
        primaryDarkColor = ContextCompat.getColor(context, R.color.primary_dark);
        gridColor = ContextCompat.getColor(context, R.color.divider);
        textColor = ContextCompat.getColor(context, R.color.text_secondary);

        float density = getResources().getDisplayMetrics().density;
        paddingLeft = 32f * density;
        paddingRight = 20f * density;
        paddingTop = 24f * density;
        paddingBottom = 28f * density;

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(primaryColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3f * density);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        pointOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointOuterPaint.setColor(primaryColor);
        pointOuterPaint.setStyle(Paint.Style.FILL);

        pointInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointInnerPaint.setColor(Color.WHITE);
        pointInnerPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(gridColor);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f * density);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{6f * density, 6f * density}, 0));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTextSize(11f * density);
        textPaint.setTextAlign(Paint.Align.CENTER);

        tooltipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipBgPaint.setColor(primaryDarkColor);
        tooltipBgPaint.setStyle(Paint.Style.FILL);

        tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipTextPaint.setColor(Color.WHITE);
        tooltipTextPaint.setTextSize(11f * density);
        tooltipTextPaint.setTextAlign(Paint.Align.CENTER);
        tooltipTextPaint.setFakeBoldText(true);
    }

    public void setData(List<DataPoint> points) {
        dataPoints.clear();
        if (points != null) {
            dataPoints.addAll(points);
        }
        selectedPointIndex = dataPoints.isEmpty() ? -1 : dataPoints.size() - 1;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();

        if (w <= 0 || h <= 0) return;

        if (dataPoints.isEmpty() || dataPoints.size() < 2) {
            drawEmptyState(canvas, w, h);
            return;
        }

        float chartLeft = paddingLeft;
        float chartRight = w - paddingRight;
        float chartTop = paddingTop;
        float chartBottom = h - paddingBottom;
        float chartWidth = chartRight - chartLeft;
        float chartHeight = chartBottom - chartTop;

        // Cari nilai maksimum
        float maxVal = 1f;
        for (DataPoint dp : dataPoints) {
            if (dp.value > maxVal) maxVal = dp.value;
        }
        // Round maxVal up to nice step
        float niceMax = (float) (Math.ceil(maxVal * 1.25));
        if (niceMax <= 0) niceMax = 5f;

        // 1. Gambar Garis Grid Horizontal (3 level: 0, 50%, 100%)
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i <= 3; i++) {
            float ratio = i / 3f;
            float y = chartBottom - (chartHeight * ratio);
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint);

            int labelVal = Math.round(niceMax * ratio);
            Paint yLabelPaint = new Paint(textPaint);
            yLabelPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.valueOf(labelVal), chartLeft - (6f * density), y + (4f * density), yLabelPaint);
        }

        // 2. Hitung Titik-titik Koordinat
        int n = dataPoints.size();
        float[] xCoords = new float[n];
        float[] yCoords = new float[n];

        for (int i = 0; i < n; i++) {
            xCoords[i] = chartLeft + (chartWidth * (float) i / (n - 1));
            float normalized = dataPoints.get(i).value / niceMax;
            yCoords[i] = chartBottom - (chartHeight * normalized);
        }

        // 3. Bangun Smooth Path (Cubic Bezier)
        linePath.reset();
        fillPath.reset();

        linePath.moveTo(xCoords[0], yCoords[0]);
        fillPath.moveTo(xCoords[0], chartBottom);
        fillPath.lineTo(xCoords[0], yCoords[0]);

        for (int i = 0; i < n - 1; i++) {
            float x1 = xCoords[i];
            float y1 = yCoords[i];
            float x2 = xCoords[i + 1];
            float y2 = yCoords[i + 1];

            float cx1 = x1 + (x2 - x1) / 2f;
            float cy1 = y1;
            float cx2 = x1 + (x2 - x1) / 2f;
            float cy2 = y2;

            linePath.cubicTo(cx1, cy1, cx2, cy2, x2, y2);
            fillPath.cubicTo(cx1, cy1, cx2, cy2, x2, y2);
        }

        fillPath.lineTo(xCoords[n - 1], chartBottom);
        fillPath.close();

        // 4. Gambar Gradient Area Bawah
        int startFill = Color.argb(80, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor));
        int endFill = Color.argb(0, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor));
        fillPaint.setShader(new LinearGradient(0, chartTop, 0, chartBottom, startFill, endFill, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);

        // 5. Gambar Garis Kurva
        canvas.drawPath(linePath, linePaint);

        // 6. Gambar Titik-titik & Label Sumbu X
        for (int i = 0; i < n; i++) {
            boolean isSelected = (i == selectedPointIndex);
            float radius = isSelected ? (6f * density) : (4f * density);
            float innerRadius = isSelected ? (3.5f * density) : (2f * density);

            canvas.drawCircle(xCoords[i], yCoords[i], radius, pointOuterPaint);
            canvas.drawCircle(xCoords[i], yCoords[i], innerRadius, pointInnerPaint);

            // Tampilkan label tanggal (selang-seling jika terlalu rapat)
            if (n <= 7 || i == 0 || i == n - 1 || i % Math.max(1, n / 5) == 0) {
                canvas.drawText(dataPoints.get(i).label, xCoords[i], chartBottom + (18f * density), textPaint);
            }
        }

        // 7. Gambar Tooltip Melayang pada Titik Terpilih
        if (selectedPointIndex >= 0 && selectedPointIndex < n) {
            float selX = xCoords[selectedPointIndex];
            float selY = yCoords[selectedPointIndex];
            DataPoint selPoint = dataPoints.get(selectedPointIndex);

            String tipText = Math.round(selPoint.value) + " • " + selPoint.label;
            float tipTextWidth = tooltipTextPaint.measureText(tipText);
            float tipPaddingH = 10f * density;
            float tipPaddingV = 6f * density;
            float tipW = tipTextWidth + (tipPaddingH * 2);
            float tipH = (14f * density) + (tipPaddingV * 2);

            float tipLeft = selX - (tipW / 2f);
            if (tipLeft < chartLeft) tipLeft = chartLeft;
            if (tipLeft + tipW > chartRight) tipLeft = chartRight - tipW;
            float tipTop = selY - tipH - (8f * density);
            if (tipTop < 0) tipTop = selY + (12f * density);

            RectF tipRect = new RectF(tipLeft, tipTop, tipLeft + tipW, tipTop + tipH);
            canvas.drawRoundRect(tipRect, 8f * density, 8f * density, tooltipBgPaint);
            canvas.drawText(tipText, tipRect.centerX(), tipRect.centerY() + (4f * density), tooltipTextPaint);
        }
    }

    private void drawEmptyState(Canvas canvas, float w, float h) {
        float density = getResources().getDisplayMetrics().density;
        Paint emptyPaint = new Paint(textPaint);
        emptyPaint.setTextSize(12f * density);
        emptyPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_muted));
        canvas.drawText("Belum ada statistik pada periode ini", w / 2f, h / 2f, emptyPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (dataPoints.size() < 2) return super.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            float touchX = event.getX();
            float w = getWidth();
            float chartLeft = paddingLeft;
            float chartRight = w - paddingRight;
            float chartWidth = chartRight - chartLeft;

            int n = dataPoints.size();
            int closest = 0;
            float minDiff = Float.MAX_VALUE;

            for (int i = 0; i < n; i++) {
                float ptX = chartLeft + (chartWidth * (float) i / (n - 1));
                float diff = Math.abs(touchX - ptX);
                if (diff < minDiff) {
                    minDiff = diff;
                    closest = i;
                }
            }

            if (selectedPointIndex != closest) {
                selectedPointIndex = closest;
                invalidate();
            }
            return true;
        }

        return super.onTouchEvent(event);
    }
}
