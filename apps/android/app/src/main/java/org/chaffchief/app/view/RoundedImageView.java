package org.chaffchief.app.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

public class RoundedImageView extends ImageView {

    public RoundedImageView(Context ctx) {
        super(ctx);

        paint.setColor(Color.BLACK);
    }

    public RoundedImageView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);

        paint.setColor(Color.BLACK);
    }

    private final Paint paint = new Paint();

    private boolean isFilled = true;

    public void setFilled(boolean filled) {
        this.isFilled = filled;
    }

    public boolean getFilled() {
        return this.isFilled;
    }

    public void setColor(int color) {
        this.paint.setColor(color);
        this.invalidate();
    }

    public int getColor() {
        return this.paint.getColor();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        int w = getWidth(), h = getHeight();

        int oldColor = paint.getColor();
        paint.setStrokeWidth(5);
        if (!isFilled) {
            paint.setColor(Color.TRANSPARENT);
            canvas.drawRect(0, 0, w, h, paint);
            paint.setColor(oldColor);

            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(w / 2, h / 2, w / 13, paint);

            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(w / 2, h / 2, w / 7 - 5, paint);
        }
        else {
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawCircle(w / 2, h / 2, w / 7 - 5, paint);
        }

        invalidate();
    }

    public static Bitmap getRoundedCroppedBitmap(Bitmap bitmap, int radius) {
        Bitmap finalBitmap;
        if (bitmap.getWidth() != radius || bitmap.getHeight() != radius)
            finalBitmap = Bitmap.createScaledBitmap(bitmap, radius, radius,
                    false);
        else
            finalBitmap = bitmap;
        Bitmap output = Bitmap.createBitmap(finalBitmap.getWidth(),
                finalBitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, finalBitmap.getWidth(),
                finalBitmap.getHeight());

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.parseColor("#BAB399"));
        canvas.drawCircle(finalBitmap.getWidth() / 2 + 0.7f,
                finalBitmap.getHeight() / 2 + 0.7f,
                finalBitmap.getWidth() / 2 + 0.1f, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(finalBitmap, rect, rect, paint);

        return output;
    }
}