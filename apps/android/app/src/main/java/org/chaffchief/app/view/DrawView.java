package org.chaffchief.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

import java.util.ArrayList;

class Circle {
    float x, y, radius;
    Paint paint;
    public Circle(float x, float y, float radius, Paint paint) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.paint = paint;
    }
}

class Line {
    float startX, startY, stopX, stopY;
    Paint paint;
    public Line(float startX, float startY, float stopX, float stopY, Paint paint) {
        this.startX = startX;
        this.startY = startY;
        this.stopX = stopX;
        this.stopY = stopY;
        this.paint = paint;
    }
    public Line(float startX, float startY, Paint paint) {
        this(startX, startY, startX, startY, paint);
    }
}

class Pathy {
    Path path;
    Paint paint;
    public Pathy(Path p, Paint paint) {
        this.path = p;
        this.paint = paint;
    }
}

class Label {
    private float x;
    private float y;
    private Paint paint;
    private String text;
    private int color;

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public Paint getPaint() {
        return paint;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}

public class DrawView extends android.support.v7.widget.AppCompatImageView {
    Paint paint = new Paint();

    Paint paintText = new Paint();

    Paint paintCircle = new Paint();

    public ArrayList<Line> lines = new ArrayList<Line>();
    public ArrayList<Pathy> pathys = new ArrayList<Pathy>();
    public ArrayList<Label> labels = new ArrayList<Label>();
    public ArrayList<Circle> circles = new ArrayList<Circle>();

    public DrawView(Context context) {
        super(context);

        paint.setAntiAlias(true);
        paint.setStrokeWidth(6f);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);

        paintText.setAntiAlias(true);
        paintText.setStrokeWidth(0.0f);
        paintText.setColor(Color.BLACK);
        paintText.setStyle(Paint.Style.FILL);
        paintText.setStrokeJoin(Paint.Join.ROUND);
        paintText.setTextSize(40.0f);
        paintText.setTextAlign(Paint.Align.RIGHT);

        paintCircle.setAntiAlias(true);
        paintCircle.setStrokeWidth(0.0f);
        paintCircle.setColor(Color.BLACK);
        paintCircle.setStyle(Paint.Style.FILL);
        paintCircle.setStrokeJoin(Paint.Join.ROUND);
    }

    public boolean performClick() {
        return true;
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint.setAntiAlias(true);
        paint.setStrokeWidth(6f);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);

        paintText.setAntiAlias(true);
        paintText.setStrokeWidth(0.0f);
        paintText.setColor(Color.BLACK);
        paintText.setStyle(Paint.Style.FILL);
        paintText.setStrokeJoin(Paint.Join.ROUND);
        paintText.setTextSize(40.0f);
        paintText.setTextAlign(Paint.Align.RIGHT);

        paintCircle.setAntiAlias(true);
        paintCircle.setStrokeWidth(0.0f);
        paintCircle.setColor(Color.BLACK);
        paintCircle.setStyle(Paint.Style.FILL);
        paintCircle.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setTextSize(float size) {
        this.paintText.setTextSize(size);
    }

    public void addCircle(float x, float y, float radius, Paint paint) {
        circles.add(new Circle(x, y, radius, paint));
    }

    public void addCircle(float x, float y, float radius) {
        circles.add(new Circle(x, y, radius, this.paintCircle));
    }

    public void addLine(float x1, float y1, float x2, float y2, Paint paint) {
        lines.add(new Line(x1, y1, x2, y2, paint));
    }

    public void addPathy(Pathy p) {
        pathys.add(p);
    }

    public void addPathy(Path p, Paint paint) {
        pathys.add(new Pathy(p, paint));
    }

    public void addLabel(float x, float y, String text, Paint paint) {
        Label l = new Label();

        l.setX(x);
        l.setY(y);
        l.setText(text);
        l.setPaint(paint);
        l.setColor(paint.getColor());

        labels.add(l);
    }

    public void addLabel(float x, float y, String text, int color) {
        Label l = new Label();

        l.setX(x);
        l.setY(y);
        l.setText(text);

        l.setColor(color);

        l.setPaint(paintText);

        labels.add(l);
    }

    public void addLabel(float x, float y, String text) {
        Label l = new Label();

        l.setX(x);
        l.setY(y);
        l.setText(text);
        l.setPaint(paintText);
        l.setColor(paintText.getColor());

        labels.add(l);
    }

    public void clear() {
        lines.clear();
        labels.clear();
        circles.clear();
        pathys.clear();
    }

    public void invalidateCanvas() {
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        Paint paint2 = new Paint();
        paint2.setColor(Color.BLACK);

        for (Line l : lines) {
            if (l.paint == null) {
                canvas.drawLine(l.startX, l.startY, l.stopX, l.stopY, paint);
            }
            else {
                canvas.drawLine(l.startX, l.startY, l.stopX, l.stopY, l.paint);
            }
        }

        for (Pathy p : pathys) {
            if (p.paint == null) {
                canvas.drawPath(p.path, paint);
            }
            else {
                canvas.drawPath(p.path, p.paint);
            }
        }

        for (Circle c : circles) {
            if (c.paint == null) {
                canvas.drawCircle(c.x, c.y, c.radius, paint);
            }
            else {
                canvas.drawCircle(c.x, c.y, c.radius, c.paint);
            }
        }

        for (Label l : labels) {
            Paint p = l.getPaint();
            int oldColor = p.getColor();
            p.setColor(l.getColor());
            canvas.drawText(l.getText(), l.getX(), l.getY(), p);
            p.setColor(oldColor);
        }

    }
}