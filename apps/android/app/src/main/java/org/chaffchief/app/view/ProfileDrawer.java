package org.chaffchief.app.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.chaffchief.app.R;
import org.chaffchief.app.database.DatabaseHelper;
import org.chaffchief.app.database.model.Log;
import org.chaffchief.app.database.model.LogEvent;
import org.chaffchief.app.database.model.LogPoint;
import org.chaffchief.app.database.model.Profile;
import org.chaffchief.app.database.model.ProfilePoint;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class XYConverterProfileDrawer {
    final public float convertX(float x, float width, int maxProfileTime) {
        return 150 + (float)(1 * (((double)(width - 300) / (maxProfileTime / 1000)) * (x / 1000)));
    }

    final public float convertY(float y, float height) {
        return height - 40 - (float)((((double)height - 40.0) / 280.0) * (double)y);
    }
}

public class ProfileDrawer {

    public void drawProfileOnView(Context context, Resources resources, String profileUuid, DrawView drawView, int drawViewWidth, int drawViewHeight, int maxProfileTime, boolean drawFan) {

        int pointSizePx = 8;
        int halfPointSizePx = pointSizePx / 2;

        DatabaseHelper db = new DatabaseHelper(context);
        Profile thisLogProfile = null;

        ArrayList<ProfilePoint> profilePoints = new ArrayList<ProfilePoint>();

        RelativeLayout.LayoutParams drawViewParams = new RelativeLayout.LayoutParams(20, 20);
        drawViewParams.width = drawViewWidth;
        drawViewParams.height = drawViewHeight;
        drawView.setLayoutParams(drawViewParams);

        drawView.clear();

        drawView.setTextSize(drawViewHeight / 41.0f);

        int i;

        Paint paintGray = new Paint();
        paintGray.setAntiAlias(true);
        paintGray.setStrokeWidth(3f);
        paintGray.setColor(Color.parseColor("#dbdbdb"));
        paintGray.setStyle(Paint.Style.STROKE);
        paintGray.setStrokeJoin(Paint.Join.ROUND);

        XYConverterProfileDrawer xy = new XYConverterProfileDrawer();

        for (i = 0; i <= maxProfileTime; i += 60000) {
            drawView.addLine(
                    xy.convertX(i, drawViewWidth, maxProfileTime),
                    0,
                    xy.convertX(i, drawViewWidth, maxProfileTime),
                    drawViewHeight,
                    paintGray
            );

            if (i > 0) {
                drawView.addLabel(
                        xy.convertX(i - 2000, drawViewWidth, maxProfileTime),
                        xy.convertY(1, drawViewHeight),
                        i / 60000 + "min",
                        Color.parseColor("#c4c4c4")
                );
            }
        }

        for (i = 0; i <= 250; i += 50) {
            drawView.addLine(
                    0,
                    xy.convertY(i, drawViewHeight),
                    drawViewWidth,
                    xy.convertY(i, drawViewHeight),
                    paintGray
            );

            drawView.addLabel(
                    xy.convertX(-2000, drawViewWidth, maxProfileTime),
                    xy.convertY(i + 2, drawViewHeight),
                    i + "\u00b0",
                    Color.parseColor("#c4c4c4")
            );
        }

        Paint paintBlack = new Paint();
        paintBlack.setAntiAlias(true);
        paintBlack.setStrokeWidth(4.5f);
        paintBlack.setColor(Color.parseColor("#AAAAAA"));
        paintBlack.setStyle(Paint.Style.STROKE);
        paintBlack.setStrokeJoin(Paint.Join.ROUND);
        paintBlack.setDither(true);
        paintBlack.setStrokeCap(Paint.Cap.ROUND);

        Paint paintFan = new Paint();
        paintFan.setAntiAlias(true);
        paintFan.setStrokeWidth(4.5f);
        paintFan.setColor(Color.parseColor("#AAAAAA"));
        paintFan.setStrokeJoin(Paint.Join.ROUND);
        paintFan.setDither(true);
        paintFan.setStrokeCap(Paint.Cap.ROUND);
        paintFan.setTextSize(drawViewHeight / 60);

        Paint fanPaint = new Paint();
        fanPaint.setAntiAlias(true);
        fanPaint.setStrokeWidth(4.5f);
        fanPaint.setColor(Color.parseColor("#AAAAAA"));
        fanPaint.setStyle(Paint.Style.STROKE);
        fanPaint.setStrokeJoin(Paint.Join.ROUND);

        Paint fanLabelPaint = new Paint();
        fanLabelPaint.setAntiAlias(true);
        fanLabelPaint.setStrokeWidth(6f);
        fanLabelPaint.setColor(Color.parseColor("#AAAAAA"));
        fanLabelPaint.setStrokeJoin(Paint.Join.ROUND);
        fanLabelPaint.setTextSize(drawViewHeight / 60);
        fanLabelPaint.setTextAlign(Paint.Align.RIGHT);

        Path profileDrawPath = new Path();
        profileDrawPath.reset();

        thisLogProfile = db.getProfileByUuid(profileUuid);


        String profileText = thisLogProfile.getProfile();

        BufferedReader reader = new BufferedReader(new StringReader(profileText));

        String lineToParseLocal;

        ProfilePoint profilePoint = null;

        profilePoints.clear();

        i = 0;
        try {
            lineToParseLocal = reader.readLine();
            while (lineToParseLocal != null) {

                String[] tokens = lineToParseLocal.split(",");

                profilePoint = new ProfilePoint();

                if (tokens.length >= 3) {
                    profilePoint.setId(i);
                    profilePoint.setTime(Integer.parseInt(tokens[0]));
                    profilePoint.setTemperature(Integer.parseInt(tokens[1]));
                    profilePoint.setFan(Integer.parseInt(tokens[2]));
                }

                profilePoints.add(profilePoint);

                i++;
                lineToParseLocal = reader.readLine();
            }
        } catch (Exception e) {
        }

        for (i = 0; i < profilePoints.size(); i++) {

            drawView.addCircle(xy.convertX(profilePoints.get(i).getTime(), drawViewWidth, maxProfileTime), xy.convertY(profilePoints.get(i).getTemperature(), drawViewHeight), halfPointSizePx);

            // profile point - temperature labels
            StringBuilder timeString = new StringBuilder();
            int seconds = (profilePoints.get(i).getTime() / 1000) % 60;
            int minutes = (profilePoints.get(i).getTime() / 1000) / 60;
            timeString.append(String.format("%02d", minutes));
            timeString.append(":");
            timeString.append(String.format("%02d", seconds));
            drawView.addLabel(
                    xy.convertX(profilePoints.get(i).getTime() - 4000, drawViewParams.width, maxProfileTime),
                    xy.convertY(profilePoints.get(i).getTemperature() + 5, drawViewParams.height),
                    profilePoints.get(i).getTemperature() + "\u00b0",
                    paintFan
            );
            drawView.addLabel(
                    xy.convertX(profilePoints.get(i).getTime() - 4000, drawViewParams.width, maxProfileTime),
                    xy.convertY(profilePoints.get(i).getTemperature() + 10, drawViewParams.height),
                    timeString.toString(),
                    paintFan
            );
            // profile point - temperature labels

            if (i < profilePoints.size() - 1) {
                if (i == 0) {
                    profileDrawPath.moveTo(
                            xy.convertX(profilePoints.get(i).getTime(), drawViewWidth, maxProfileTime),
                            xy.convertY(profilePoints.get(i).getTemperature(), drawViewHeight)
                    );
                }
                profileDrawPath.lineTo(
                        xy.convertX(profilePoints.get(i + 1).getTime(), drawViewWidth, maxProfileTime),
                        xy.convertY(profilePoints.get(i + 1).getTemperature(), drawViewHeight)
                );

            }

            // fan
            drawView.addCircle(xy.convertX(profilePoints.get(i).getTime(), drawViewWidth, maxProfileTime), xy.convertY(profilePoints.get(i).getFan(), drawViewHeight), halfPointSizePx);
            if (i < profilePoints.size() - 1) {
                drawView.addLine(
                        xy.convertX(profilePoints.get(i).getTime(), drawViewParams.width, maxProfileTime),
                        xy.convertY(profilePoints.get(i).getFan(), drawViewParams.height),
                        xy.convertX(profilePoints.get(i + 1).getTime(), drawViewParams.width, maxProfileTime),
                        xy.convertY(profilePoints.get(i + 1).getFan(), drawViewParams.height),
                        paintFan
                );
            }
            drawView.addLabel(
                    xy.convertX(profilePoints.get(i).getTime() - 4000, drawViewParams.width, maxProfileTime),
                    xy.convertY(profilePoints.get(i).getFan() + 5, drawViewParams.height),
                    profilePoints.get(i).getFan() + "%",
                    paintFan
            );
            // fan
        }

        drawView.addPathy(profileDrawPath, paintBlack);

        drawView.invalidateCanvas();
    }

    public void drawLogOnView(Context context, Resources resources, int logId, DrawView drawView, int drawViewWidth, int drawViewHeight, int maxProfileTime) {

        int pointSizePx = 8;
        int halfPointSizePx = pointSizePx / 2;

        DatabaseHelper db = new DatabaseHelper(context);
        Log log = db.getLog(logId);
        ArrayList<String> logLines = log.getLines();
        ArrayList<LogPoint> logPoints = new ArrayList<LogPoint>();
        Profile thisLogProfile = null;

        ArrayList<ProfilePoint> profilePoints = new ArrayList<ProfilePoint>();

        String lineToParse;
        for (int i = 0; i < logLines.size(); i++) {
            lineToParse = logLines.get(i);

            ArrayList<String> reads = new ArrayList<String>();
            Pattern pattern = Pattern.compile("([\\d|.]+)+");
            Matcher matcher = pattern.matcher(lineToParse);
            while (matcher.find()) {
                reads.add(matcher.group());
            }
            if (reads.size() >= 6) {
                logPoints.add(
                        new LogPoint(Integer.parseInt(reads.get(0)), Double.parseDouble(reads.get(1)), Integer.parseInt(reads.get(2)), Integer.parseInt(reads.get(3)), Double.parseDouble(reads.get(4)), Double.parseDouble(reads.get(5)))
                );
            }
        }

        if (log.getProfileVersionUuid() != null && !log.getProfileVersionUuid().isEmpty()) {
            thisLogProfile = db.getProfileByUuid(log.getProfileVersionUuid());
        }

        RelativeLayout.LayoutParams drawViewParams = new RelativeLayout.LayoutParams(20, 20);
        drawViewParams.width = drawViewWidth;
        drawViewParams.height = drawViewHeight;
        drawView.setLayoutParams(drawViewParams);

        drawView.clear();

        drawView.setTextSize(drawViewHeight / 41.0f);

        int i;

        Paint paintGray = new Paint();
        paintGray.setAntiAlias(true);
        paintGray.setStrokeWidth(3f);
        paintGray.setColor(Color.parseColor("#dbdbdb"));
        paintGray.setStyle(Paint.Style.STROKE);
        paintGray.setStrokeJoin(Paint.Join.ROUND);

        XYConverterProfileDrawer xy = new XYConverterProfileDrawer();

        for (i = 0; i <= maxProfileTime; i += 60000) {
            drawView.addLine(
                    xy.convertX(i, drawViewWidth, maxProfileTime),
                    0,
                    xy.convertX(i, drawViewWidth, maxProfileTime),
                    drawViewHeight,
                    paintGray
            );

            if (i > 0) {
                drawView.addLabel(
                        xy.convertX(i - 2000, drawViewWidth, maxProfileTime),
                        xy.convertY(1, drawViewHeight),
                        i / 60000 + "min",
                        Color.parseColor("#c4c4c4")
                );
            }
        }

        for (i = 0; i <= 250; i += 50) {
            drawView.addLine(
                    0,
                    xy.convertY(i, drawViewHeight),
                    drawViewWidth,
                    xy.convertY(i, drawViewHeight),
                    paintGray
            );

            drawView.addLabel(
                    xy.convertX(-2000, drawViewWidth, maxProfileTime),
                    xy.convertY(i + 2, drawViewHeight),
                    i + "\u00b0",
                    Color.parseColor("#c4c4c4")
            );
        }

        // if profile is known, draw it
        if (thisLogProfile != null) {

            Paint paintBlack = new Paint();
            paintBlack.setAntiAlias(true);
            paintBlack.setStrokeWidth(4.5f);
            paintBlack.setColor(Color.parseColor("#AAAAAA"));
            paintBlack.setStyle(Paint.Style.STROKE);
            paintBlack.setStrokeJoin(Paint.Join.ROUND);
            paintBlack.setDither(true);
            paintBlack.setStrokeCap(Paint.Cap.ROUND);

            Paint fanPaint = new Paint();
            fanPaint.setAntiAlias(true);
            fanPaint.setStrokeWidth(4.5f);
            fanPaint.setColor(Color.parseColor("#AAAAAA"));
            fanPaint.setStyle(Paint.Style.STROKE);
            fanPaint.setStrokeJoin(Paint.Join.ROUND);

            Paint fanLabelPaint = new Paint();
            fanLabelPaint.setAntiAlias(true);
            fanLabelPaint.setStrokeWidth(6f);
            fanLabelPaint.setColor(Color.parseColor("#AAAAAA"));
            fanLabelPaint.setStrokeJoin(Paint.Join.ROUND);
            fanLabelPaint.setTextSize(drawViewHeight / 60);
            fanLabelPaint.setTextAlign(Paint.Align.RIGHT);

            Path profileDrawPath = new Path();
            profileDrawPath.reset();

            String profileText = thisLogProfile.getProfile();

            BufferedReader reader = new BufferedReader(new StringReader(profileText));

            String lineToParseLocal;

            ProfilePoint profilePoint = null;

            profilePoints.clear();

            i = 0;
            try {
                lineToParseLocal = reader.readLine();
                while (lineToParseLocal != null) {

                    String[] tokens = lineToParseLocal.split(",");

                    profilePoint = new ProfilePoint();

                    if (tokens.length >= 3) {
                        profilePoint.setId(i);
                        profilePoint.setTime(Integer.parseInt(tokens[0]));
                        profilePoint.setTemperature(Integer.parseInt(tokens[1]));
                        profilePoint.setFan(Integer.parseInt(tokens[2]));
                    }

                    profilePoints.add(profilePoint);

                    i++;
                    lineToParseLocal = reader.readLine();
                }
            } catch (Exception e) {
            }

            for (i = 0; i < profilePoints.size(); i++) {
                drawView.addCircle(xy.convertX(profilePoints.get(i).getTime(), drawViewWidth, maxProfileTime), xy.convertY(profilePoints.get(i).getTemperature(), drawViewHeight), halfPointSizePx);

                if (i < profilePoints.size() - 1) {
                    if (i == 0) {
                        profileDrawPath.moveTo(
                                xy.convertX(profilePoints.get(i).getTime(), drawViewWidth, maxProfileTime),
                                xy.convertY(profilePoints.get(i).getTemperature(), drawViewHeight)
                        );
                    }
                    profileDrawPath.lineTo(
                            xy.convertX(profilePoints.get(i + 1).getTime(), drawViewWidth, maxProfileTime),
                            xy.convertY(profilePoints.get(i + 1).getTemperature(), drawViewHeight)
                    );

                }

            }

            drawView.addPathy(profileDrawPath, paintBlack);
        }

        Map<Integer, String> eventsMap = new HashMap<Integer, String>();
        for (LogEvent event: log.getEvents()) {
            if (eventsMap.containsKey(event.getTime())) {
                eventsMap.put(event.getTime(), eventsMap.get(event.getTime()) + event.getDescription());
            }
            else {
                eventsMap.put(event.getTime(), event.getDescription());
            }
        }

        Paint paintProfile = new Paint();
        paintProfile.setAntiAlias(true);
        paintProfile.setStrokeWidth(4.5f);
        paintProfile.setColor(Color.parseColor("#000000"));
        paintProfile.setStyle(Paint.Style.STROKE);
        paintProfile.setStrokeJoin(Paint.Join.ROUND);
        paintProfile.setDither(true);
        paintProfile.setPathEffect(new CornerPathEffect((float)10.0));
        paintProfile.setStrokeCap(Paint.Cap.ROUND);

        Paint paintReading = new Paint();
        paintReading.setAntiAlias(true);
        paintReading.setStrokeWidth(2.5f);
        paintReading.setColor(Color.parseColor("#fc640c"));
        paintReading.setStyle(Paint.Style.STROKE);
        paintReading.setStrokeJoin(Paint.Join.ROUND);
        paintReading.setDither(true);
        paintReading.setPathEffect(new CornerPathEffect((float)10.0));
        paintReading.setStrokeCap(Paint.Cap.ROUND);

        Path unknownProfileDrawPath = new Path();
        unknownProfileDrawPath.reset();

        Path logDrawPath = new Path();
        logDrawPath.reset();

        for (i = 0; i < logPoints.size(); i++) {
            if (i < logPoints.size() - 1) {

                // unknown profile?
                if (thisLogProfile == null) {
                    if (i == 0) {
                        unknownProfileDrawPath.moveTo(
                                xy.convertX(logPoints.get(i).getTime(), drawViewWidth, maxProfileTime),
                                xy.convertY((float)logPoints.get(i).getTargetTemperature(), drawViewHeight)
                        );
                    }
                    unknownProfileDrawPath.lineTo(
                            xy.convertX(logPoints.get(i + 1).getTime(), drawViewWidth, maxProfileTime),
                            xy.convertY((float)logPoints.get(i + 1).getTargetTemperature(), drawViewHeight)
                    );
                }

                if (i == 0) {
                    logDrawPath.moveTo(
                            xy.convertX(logPoints.get(i).getTime(), drawViewWidth, maxProfileTime),
                            xy.convertY((float)logPoints.get(i).getTemperature(), drawViewHeight)
                    );
                }
                logDrawPath.lineTo(
                        xy.convertX(logPoints.get(i + 1).getTime(), drawViewWidth, maxProfileTime),
                        xy.convertY((float)logPoints.get(i + 1).getTemperature(), drawViewHeight)
                );

                // mark cooling time with a point and labels, if greater than 0
                if (log.getCoolingTime() > 0 && log.getCoolingTime() == logPoints.get(i).getTime()) {
                    drawView.addCircle(xy.convertX(logPoints.get(i).getTime(), drawViewWidth, maxProfileTime), xy.convertY((float)logPoints.get(i).getTemperature(), drawViewHeight), halfPointSizePx);

                    Paint paintText = new Paint();
                    paintText.setAntiAlias(true);
                    paintText.setStrokeWidth(0.0f);
                    paintText.setColor(Color.BLACK);
                    paintText.setStyle(Paint.Style.FILL);
                    paintText.setStrokeJoin(Paint.Join.ROUND);
                    paintText.setTextSize(drawViewHeight / 60);

                    Paint paintTextTitle = new Paint(paintText);
                    paintTextTitle.setFakeBoldText(true);

                    StringBuilder timeString = new StringBuilder();
                    int seconds = (logPoints.get(i).getTime() / 1000) % 60;
                    int minutes = (logPoints.get(i).getTime() / 1000) / 60;
                    timeString.append(String.format("%02d", minutes));
                    timeString.append(":");
                    timeString.append(String.format("%02d", seconds));

                    drawView.addLabel(
                            xy.convertX(logPoints.get(i).getTime(), drawViewWidth, maxProfileTime),
                            xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 15, drawViewHeight),
                            new Double(logPoints.get(i).getTemperature()).intValue() + "\u00b0",
                            paintText
                    );
                    drawView.addLabel(
                            xy.convertX(logPoints.get(i).getTime(), drawViewWidth, maxProfileTime),
                            xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 10, drawViewHeight),
                            timeString.toString(),
                            paintText
                    );
                    drawView.addLabel(
                            xy.convertX(logPoints.get(i).getTime(), drawViewWidth, maxProfileTime),
                            xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 5, drawViewHeight),
                            resources.getString(R.string.lbl_cooling_point),
                            paintTextTitle
                    );
                }

                // mark events with point and labels
                if (eventsMap.containsKey(logPoints.get(i).getTime())) {
                    drawView.addCircle(xy.convertX(logPoints.get(i).getTime(), drawViewWidth, maxProfileTime), xy.convertY((float)logPoints.get(i).getTemperature(), drawViewHeight), halfPointSizePx);

                    Paint paintText = new Paint();
                    paintText.setAntiAlias(true);
                    paintText.setStrokeWidth(0.0f);
                    paintText.setColor(Color.BLACK);
                    paintText.setStyle(Paint.Style.FILL);
                    paintText.setStrokeJoin(Paint.Join.ROUND);
                    paintText.setTextSize(drawViewHeight / 80);

                    Paint paintTextTitle = new Paint(paintText);
                    paintTextTitle.setFakeBoldText(true);

                    StringBuilder timeString = new StringBuilder();
                    int seconds = (logPoints.get(i).getTime() / 1000) % 60;
                    int minutes = (logPoints.get(i).getTime() / 1000) / 60;
                    timeString.append(String.format("%02d", minutes));
                    timeString.append(":");
                    timeString.append(String.format("%02d", seconds));

                    drawView.addLabel(
                            xy.convertX(logPoints.get(i).getTime(), drawViewWidth, maxProfileTime),
                            xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 13, drawViewHeight),
                            new Double(logPoints.get(i).getTemperature()).intValue() + "\u00b0",
                            paintText
                    );
                    drawView.addLabel(
                            xy.convertX(logPoints.get(i).getTime(), drawViewWidth, maxProfileTime),
                            xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 9, drawViewHeight),
                            timeString.toString(),
                            paintText
                    );
                    drawView.addLabel(
                            xy.convertX(logPoints.get(i).getTime(), drawViewWidth, maxProfileTime),
                            xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 5, drawViewHeight),
                            eventsMap.get(logPoints.get(i).getTime()),
                            paintTextTitle
                    );
                }

            }
        }
        if (thisLogProfile == null) drawView.addPathy(unknownProfileDrawPath, paintProfile);
        drawView.addPathy(logDrawPath, paintReading);

        if (logPoints.size() > 0) {
            i = logPoints.size() - 1;
            Paint paintText = new Paint();
            paintText.setAntiAlias(true);
            paintText.setStrokeWidth(0.0f);
            paintText.setColor(Color.BLACK);
            paintText.setStyle(Paint.Style.FILL);
            paintText.setStrokeJoin(Paint.Join.ROUND);

            paintText.setTextSize(drawViewHeight / 60);

            Paint paintTextTitle = new Paint(paintText);
            paintTextTitle.setFakeBoldText(true);

            StringBuilder timeString = new StringBuilder();
            int seconds = (logPoints.get(i).getTime() / 1000) % 60;
            int minutes = (logPoints.get(i).getTime() / 1000) / 60;
            timeString.append(String.format("%02d", minutes));
            timeString.append(":");
            timeString.append(String.format("%02d", seconds));

            drawView.addLabel(
                    xy.convertX(logPoints.get(i).getTime(), drawViewWidth, maxProfileTime),
                    xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() + 16, drawViewHeight),
                    "Finish",
                    paintTextTitle
            );
            drawView.addLabel(
                    xy.convertX(logPoints.get(i).getTime(), drawViewWidth, maxProfileTime),
                    xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() + 11, drawViewHeight),
                    timeString.toString(),
                    paintText
            );
            drawView.addLabel(
                    xy.convertX(logPoints.get(i).getTime(), drawViewWidth, maxProfileTime),
                    xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() + 6, drawViewHeight),
                    new Double(logPoints.get(i).getTemperature()).intValue() + "\u00b0",
                    paintText
            );

        }

        drawView.invalidateCanvas();

    }
}
