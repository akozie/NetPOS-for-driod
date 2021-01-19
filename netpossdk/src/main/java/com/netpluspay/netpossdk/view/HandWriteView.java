package com.netpluspay.netpossdk.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class HandWriteView extends View {
    private static final float PAINT_SIZE = 3.5f;
    private Context context;
    private Paint paint;
    private Canvas cacheCanvas;
    private Bitmap cacheBitmap;
    private Path path;
    private int width;
    private int height;
    private float startX, startY;
    private float clickX, clickY;

    private RectF dirtyRect;

    private long count;

    public HandWriteView(Context context) {
        super(context);
        this.context = context;
    }

    public HandWriteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = measureWidth(widthMeasureSpec);
        height = measureHeight(heightMeasureSpec);
        setMeasuredDimension(width, height);
        init(width, height);
    }

    private int measureHeight(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        int result = 500;
        if (specMode == MeasureSpec.AT_MOST) {
            result = specSize;
        } else if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        }
        return result;
    }

    private int measureWidth(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        int result = 500;
        if (specMode == MeasureSpec.AT_MOST) {
            result = specSize;
        } else if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        }
        return result;
    }

    private void init(int width, int height) {
        requestFocus();

        startX = 0;
        startY = 0;
        clickX = 0;
        clickY = 0;
        count = 0;

        float scale = context.getResources().getDisplayMetrics().density;
        float pageSize = PAINT_SIZE * scale;
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(pageSize);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.rgb(245, 245, 245));
        paint.setARGB(255, 75, 75, 73);
        paint.setDither(true);
        paint.setFilterBitmap(true);
        paint.setSubpixelText(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        path = new Path();
        cacheBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        cacheCanvas = new Canvas(cacheBitmap);

        dirtyRect = new RectF();
    }

    public void clear() {
        count = 0;
        path.reset();
        path.moveTo(startX, startY);
        if (cacheCanvas != null) {
            cacheBitmap.eraseColor(Color.TRANSPARENT);
            cacheCanvas.drawColor(Color.TRANSPARENT);
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(cacheBitmap, 0, 0, null);
        canvas.drawPath(path, paint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int curW = cacheBitmap != null ? cacheBitmap.getWidth() : 0;
        int curH = cacheBitmap != null ? cacheBitmap.getHeight() : 0;

        curW = Math.max(curW, w);
        curH = Math.max(curH, h);
        Bitmap newBitmap = Bitmap.createBitmap(curW, curH, Config.ARGB_8888);
        Canvas newCanvas = new Canvas();
        newCanvas.setBitmap(newBitmap);
        if (cacheBitmap != null) {
            newCanvas.drawBitmap(cacheBitmap, 0, 0, null);
        }
        cacheBitmap = newBitmap;
        cacheCanvas = newCanvas;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        startX = event.getX();
        startY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDown(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                touchMove(event);
                return true;
            case MotionEvent.ACTION_UP:
                touchUp(event);
                return true;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private void touchDown(MotionEvent event) {
        clickX = startX;
        clickY = startY;
        path.moveTo(startX, startY);
    }

    private void touchMove(MotionEvent event) {
        path.quadTo(clickX, clickY, (clickX + startX) / 2, (clickY + startY) / 2);
        count++;
        resetDirtyRect();
        clickX = startX;
        clickY = startY;
        this.invalidate();
    }

    private void touchUp(MotionEvent event) {
        cacheCanvas.drawPath(path, paint);
        path.reset();
    }

    private void resetDirtyRect() {
        dirtyRect.left = Math.min(startX, clickX);
        dirtyRect.right = Math.max(startX, clickX);
        dirtyRect.top = Math.min(startY, clickY);
        dirtyRect.bottom = Math.max(startY, clickY);
    }

    public boolean isValid() {
        if (count >= 3) {
            return true;
        }
        return false;
    }

    private static final int WIDTH = 5 * 8 * 4;
    private static final int HEIGHT = 9 * 8 * 4;

    public Bitmap getCacheBitmap() {
        Bitmap validBitmap;
        if (!path.isEmpty()) {
            cacheCanvas.drawPath(path, paint);
            path.reset();
        }
        boolean fixW = width < height ? true : false;
        if (fixW) {
            validBitmap = Bitmap.createScaledBitmap(cacheBitmap, WIDTH, height * WIDTH / width, true);
        } else {
            validBitmap = Bitmap.createScaledBitmap(cacheBitmap, width * WIDTH / height, WIDTH, true);
        }
        return validBitmap;
    }
}