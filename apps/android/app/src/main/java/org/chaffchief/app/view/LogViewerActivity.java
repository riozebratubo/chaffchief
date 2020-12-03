package org.chaffchief.app.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.TooltipCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.chaffchief.app.R;
import org.chaffchief.app.database.DatabaseHelper;
import org.chaffchief.app.database.model.CurrentLog;
import org.chaffchief.app.database.model.LogEvent;
import org.chaffchief.app.database.model.LogPoint;
import org.chaffchief.app.database.model.Profile;
import org.chaffchief.app.database.model.ProfilePoint;

import java.io.BufferedReader;
import java.io.StringReader;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.akexorcist.bluetotohspp.library.BluetoothState;

class XYConverterLocal {
    public static final int MAX_PROFILE_TIME = 1200000;

    final public float convertX(float x, float width) {
        return 150 + (float)(1 * (((double)(width - 300) / (MAX_PROFILE_TIME / 1000)) * (x / 1000)));
    }

    final public float convertY(float y, float height) {
        return height - 40 - (float)((((double)height - 40.0) / 260.0) * (double)y);
    }
}

public class LogViewerActivity extends AppCompatActivity {

    public static final String INTENT_MESSAGE_PREFIX = "org.chaffchief.app.";

    public static final int PROFILE_EDITOR_POINT_SIZE_PX = 50;

    public static final int MAX_PROFILE_TIME = 1200000;
    public static final int VERTICAL_SCREEN_SIZE_MULTIPLIER = 4;

    private ScaleGestureDetector scaleGestureDetector;
    private float mScaleFactor = 1.f;

    private DatabaseHelper db;

    DrawView drawView = null;

    TooltipWindow tooltipWindow;

    HashMap profilePointsPointIndexesToDrawings = new HashMap();

    View yellowingPoint = null;
    View crackPoint = null;
    View coolingPoint = null;

    int coolingPointIndex = 0;

    HashMap interestPointIndexesToDrawings = new HashMap();

    final private ArrayList<ProfilePoint> profilePoints = new ArrayList<ProfilePoint>();

    private int scrollLevel = 0;

    RelativeLayout.LayoutParams drawViewParams;

    Activity thisActivity = null;

    int thisActivityOriginalWidth = 0;
    int thisActivityOriginalHeight = 0;
    int getThisActivityOriginalOrientation = 0;

    private void addScrollLevel(int scroll) {
        this.scrollLevel += scroll;
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // API 5+ solution
                onBackPressed();
                return true;
            default:
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(1.0f, Math.min(mScaleFactor, 3.0f));

            drawView.setScaleX(mScaleFactor);
            drawView.setScaleY(mScaleFactor);

            drawView.invalidateCanvas();

            return true;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_log_view);

        thisActivity = this;

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().hide();

        db = new DatabaseHelper(this);

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        tooltipWindow = new TooltipWindow(this);

        Button buttonScrollRight = findViewById(R.id.button_log_view_scroll_right);
        buttonScrollRight.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                RelativeLayout imgHolder = findViewById(R.id.log_view);

                imgHolder.scrollBy(100, 0);
                addScrollLevel(100);
                drawLogOnCurrentState();

            }
        });

        Button buttonScrollLeft = findViewById(R.id.button_log_view_scroll_left);
        buttonScrollLeft.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                RelativeLayout imgHolder = findViewById(R.id.log_view);

                imgHolder.scrollBy(-100, 0);
                addScrollLevel(-100);
                drawLogOnCurrentState();

            }
        });

        ImageButton buttonBack = findViewById(R.id.button_log_view_back);
        buttonBack.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                setResult(Activity.RESULT_OK, resultIntent);

                finish();
            }
        });

        ImageButton buttonComments = findViewById(R.id.button_log_view_comments);
        buttonComments.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

            String currentComments = db.getCurrentLog().getComments();

            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle(R.string.lbl_log_comments);
            View viewInflated = LayoutInflater.from(v.getContext()).inflate(R.layout.input_dialog, null);
            final EditText input = viewInflated.findViewById(R.id.input_dialog_input_text);
            input.setText(currentComments);
            input.setSelection(input.getText().toString().length());
            builder.setView(viewInflated);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                    db.changeCurrentLogComments(input.getText().toString());
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    dialog.cancel();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.getWindow().setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            dialog.show();
            }
        });

        ImageButton buttonAddEvent = findViewById(R.id.button_log_view_add_event);
        buttonAddEvent.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                final View baseV = v;

                PopupMenu popup = new PopupMenu(LogViewerActivity.this, v);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        final int currentTime = logDataStore.getLogPoints().size() > 0 ? logDataStore.getLogPoints().get(logDataStore.getLogPoints().size() - 1).getTime() : 0;

                        switch (menuItem.getItemId()) {
                            case R.id.menu_add_event_yellowing:
                                logDataStore.setYellowingTime(currentTime);
                                db.changeCurrentLogYellowingTime(currentTime);
                                return true;

                            case R.id.menu_add_event_first_crack:
                                logDataStore.setCrackTime(currentTime);
                                db.changeCurrentLogCrackTime(currentTime);
                                return true;

                            case R.id.menu_add_event_custom:

                                String currentDescription = logDataStore.getEventsAt(currentTime);

                                AlertDialog.Builder builder = new AlertDialog.Builder(baseV.getContext());
                                builder.setTitle(R.string.lbl_log_add_event);
                                View viewInflated = LayoutInflater.from(baseV.getContext()).inflate(R.layout.input_dialog, null);
                                final EditText input = viewInflated.findViewById(R.id.input_dialog_input_text);
                                input.setText(currentDescription);
                                input.setSelection(input.getText().toString().length());
                                builder.setView(viewInflated);

                                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        db.setCurrentLogEventAt(currentTime, input.getText().toString());
                                        logDataStore.setEventAt(currentTime, input.getText().toString());
                                    }
                                });
                                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        dialog.cancel();
                                    }
                                });

                                AlertDialog dialog = builder.create();
                                dialog.getWindow().setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                                dialog.show();

                                return true;
                        }
                        return false;
                    }
                });
                popup.inflate(R.menu.menu_add_event);
                popup.show();
            }
        });


        ImageButton btnStartStop = findViewById(R.id.button_log_start_stop);
        btnStartStop.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){

                if (BluetoothServiceContainer.getInstance().bt != null) {
                    if(BluetoothServiceContainer.getInstance().bt.getServiceState() == BluetoothState.STATE_CONNECTED) {
                        BluetoothServiceContainer.getInstance().bt.send("command_start_stop", true);
                    }
                }

            }
        });

        TooltipCompat.setTooltipText(findViewById(R.id.button_log_view_scroll_left), "Scroll left");
        TooltipCompat.setTooltipText(findViewById(R.id.button_log_view_scroll_right), "Scroll right");
        TooltipCompat.setTooltipText(findViewById(R.id.button_log_view_back), "Go back");

        Button buttonClearAndSave = findViewById(R.id.button_log_view_clear_and_save);
        buttonClearAndSave.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){

                // save if there is a log
                if (ReceivedDataStore.getInstance().size() > 0) {

                    db.changeCurrentLogYellowingTime(ReceivedDataStore.getInstance().getYellowingTime());
                    db.changeCurrentLogCrackTime(ReceivedDataStore.getInstance().getCrackTime());
                    db.changeCurrentLogCoolingTime(ReceivedDataStore.getInstance().getCoolingTime());

                    CurrentLog currentLog = db.getCurrentLog();
                    List<LogEvent> currentLogEvents = db.getCurrentLogEvents();

                    JSONObject document = new JSONObject();
                    try {
                        JSONArray eventList = new JSONArray();
                        for (LogEvent logEvent : currentLogEvents) {
                            JSONObject event = new JSONObject();
                            event.put("time", logEvent.getTime());
                            event.put("description", logEvent.getDescription());
                            eventList.put(event);
                        }
                        document.put("events", eventList);
                    }
                    catch (Exception e) {

                    }

                    db.insertLog(currentLog.getUuid(), currentLog.getName(), currentLog.getTimestamp(), TextUtils.join("\n", ReceivedDataStore.getInstance().getReceivedLogLines()), currentLog.getComments(), currentLog.getYellowingTime(), currentLog.getCrackTime(), currentLog.getCoolingTime(), document.toString(), currentLog.getRoasterName(), currentLog.getRoasterAddress());
                }

                // clear and restart
                ReceivedDataStore.getInstance().clearAll();
                db.clearCurrentLogAll();

                // set name and date just to renew - values won't be used
                db.changeCurrentLogName("log " + randomString(8));
                db.changeCurrentLogTimestamp();
                db.changeCurrentLogProfileUuid("");

                finish();
            }
        });

        if (ReceivedDataStore.getInstance().size() > 0 && !ReceivedDataStore.getInstance().isLogging()) {
            buttonClearAndSave.setVisibility(View.VISIBLE);
            btnStartStop.setVisibility(View.GONE);
        }

        if (ReceivedDataStore.getInstance().size() > 0 || ReceivedDataStore.getInstance().isLogging()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        drawLogOnCurrentState();

    }

    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom rnd = new SecureRandom();

    String randomString(int len) {
        StringBuilder sb = new StringBuilder( len );
        for( int i = 0; i < len; i++ ) {
            sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        }
        return sb.toString();
    }

    private DataUpdateReceiver dataUpdateReceiver;

    private class DataUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Button buttonClearAndSave = findViewById(R.id.button_log_view_clear_and_save);
            ImageButton btnStartStop = findViewById(R.id.button_log_start_stop);

            if (intent.getAction().equals(MainActivity.INTENT_MESSAGE_PREFIX + "LOG_DATA_CHANGED")) {

                if (ReceivedDataStore.getInstance().isLogging()) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            drawLogOnCurrentState();
                        }
                    });
                }

            }
            else if (intent.getAction().equals(MainActivity.INTENT_MESSAGE_PREFIX + "LOG_DATA_STARTED")) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                buttonClearAndSave.setVisibility(View.INVISIBLE);
                btnStartStop.setVisibility(View.VISIBLE);
            }
            else if (intent.getAction().equals(MainActivity.INTENT_MESSAGE_PREFIX + "LOG_DATA_COOLING")) {
                //
            }
            else if (intent.getAction().equals(MainActivity.INTENT_MESSAGE_PREFIX + "LOG_DATA_ENDED")) {

                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                buttonClearAndSave.setVisibility(View.VISIBLE);
                btnStartStop.setVisibility(View.GONE);
            }

        }
    }

    ReceivedDataStore logDataStore;

    float scroll_dX;
    float scroll_dY;
    int scroll_lastAction;

    public void drawLogOnCurrentState() {

        final RelativeLayout imgHolder = findViewById(R.id.log_view);

        logDataStore = ReceivedDataStore.getInstance();

        int i;

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        final RelativeLayout.LayoutParams drawViewParams;

        if (this.drawView == null) {
            final DrawView drawView = new DrawView(imgHolder.getContext());
            drawViewParams = new RelativeLayout.LayoutParams(20, 20);
            drawViewParams.topMargin = 0;
            drawViewParams.leftMargin = 0;
            drawView.setLayoutParams(drawViewParams);
            imgHolder.addView(drawView);
            this.drawView = drawView;
        }
        else {
            drawViewParams = (RelativeLayout.LayoutParams) drawView.getLayoutParams();
        }

        this.drawViewParams = drawViewParams;

        drawView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        scroll_dX = event.getX();
                        scroll_dY = event.getY();
                        scroll_lastAction = MotionEvent.ACTION_DOWN;

                        break;
                    case MotionEvent.ACTION_MOVE:

                        View a = thisActivity.getWindow().getDecorView();
                        Rect outRect = new Rect();
                        a.getWindowVisibleDisplayFrame(outRect);
                        int screenWidth = outRect.width();
                        int screenHeight = outRect.height();

                        int resultingScrollX = (int) (scroll_dX - event.getX());
                        if ((imgHolder.getScrollX() + resultingScrollX) < -300 || (imgHolder.getScrollX() + resultingScrollX) > (imgHolder.getWidth() - screenWidth) + 300) resultingScrollX = 0;

                        int resultingScrollY = 0;

                        imgHolder.scrollBy(resultingScrollX, resultingScrollY);

                        break;

                    default:
                        break;
                }


                return true;
            }
        });

        runJustBeforeBeingDrawn(imgHolder, new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {

                View a = thisActivity.getWindow().getDecorView();

                Rect outRect = new Rect();
                a.getWindowVisibleDisplayFrame(outRect);

                int screenWidth = outRect.width();
                int screenHeight = outRect.height();

                thisActivityOriginalWidth = screenWidth;
                thisActivityOriginalHeight = screenHeight;
                getThisActivityOriginalOrientation = getResources().getConfiguration().orientation;

                ViewGroup.LayoutParams imgHolderLayoutParams = imgHolder.getLayoutParams();

                if (screenHeight > screenWidth) {
                    drawViewParams.width = screenWidth * VERTICAL_SCREEN_SIZE_MULTIPLIER;
                    imgHolderLayoutParams.width = screenWidth * VERTICAL_SCREEN_SIZE_MULTIPLIER;

                }
                else {
                    drawViewParams.width = screenWidth;
                    imgHolderLayoutParams.width = screenWidth;
                }
                drawViewParams.height = screenHeight;
                drawView.setLayoutParams(drawViewParams);
                imgHolder.setLayoutParams(imgHolderLayoutParams);

                drawView.clear();

                drawView.setTextSize(screenHeight / 40);

                int i;

                Paint paintGray = new Paint();
                paintGray.setAntiAlias(true);
                paintGray.setStrokeWidth(4f);
                paintGray.setColor(Color.parseColor("#dbdbdb"));
                paintGray.setStyle(Paint.Style.STROKE);
                paintGray.setStrokeJoin(Paint.Join.ROUND);

                XYConverterLocal xy = new XYConverterLocal();

                for (i = 0; i <= MAX_PROFILE_TIME; i += 60000) {
                    drawView.addLine(
                            xy.convertX(i, drawViewParams.width),
                            0,
                            xy.convertX(i, drawViewParams.width),
                            screenHeight,
                            paintGray
                    );

                    if (i > 0) {
                        drawView.addLabel(
                                xy.convertX(i - 2000, drawViewParams.width),
                                xy.convertY(1, drawViewParams.height),
                                i / 60000 + "min",
                                Color.parseColor("#c4c4c4")
                        );
                    }
                }

                for (i = 0; i <= 250; i += 50) {
                    drawView.addLine(
                            0,
                            xy.convertY(i, screenHeight),
                            drawViewParams.width,
                            xy.convertY(i, screenHeight),
                            paintGray
                    );

                    drawView.addLabel(
                            scrollLevel > 0 ? scrollLevel - 80 + xy.convertX(-2000, drawViewParams.width) : xy.convertX(-2000, drawViewParams.width),
                            xy.convertY(i + 2, screenHeight),
                            i + "\u00b0",
                            Color.parseColor("#c4c4c4")
                    );
                }

                int pointSizePx = PROFILE_EDITOR_POINT_SIZE_PX;
                int halfPointSizePx = pointSizePx / 2;

                // if profile is known, draw it
                if (logDataStore.hasKnownRunningProfile()) {
                    Paint paintBlack = new Paint();
                    paintBlack.setAntiAlias(true);
                    paintBlack.setDither(true);
                    paintBlack.setStrokeWidth(8f);
                    paintBlack.setColor(Color.parseColor("#AAAAAA"));
                    paintBlack.setStyle(Paint.Style.STROKE);
                    paintBlack.setStrokeJoin(Paint.Join.ROUND);
                    paintBlack.setStrokeCap(Paint.Cap.ROUND);

                    Paint fanPaint = new Paint();
                    fanPaint.setAntiAlias(true);
                    fanPaint.setStrokeWidth(6f);
                    fanPaint.setColor(Color.parseColor("#AAAAAA"));
                    fanPaint.setStyle(Paint.Style.STROKE);
                    fanPaint.setStrokeJoin(Paint.Join.ROUND);

                    Paint fanLabelPaint = new Paint();
                    fanLabelPaint.setAntiAlias(true);
                    fanLabelPaint.setStrokeWidth(6f);
                    fanLabelPaint.setColor(Color.parseColor("#AAAAAA"));
                    fanLabelPaint.setStrokeJoin(Paint.Join.ROUND);
                    fanLabelPaint.setTextSize(screenHeight / 50);
                    fanLabelPaint.setTextAlign(Paint.Align.RIGHT);

                    Path profileDrawPath = new Path();
                    profileDrawPath.reset();

                    String profileText = logDataStore.getRunningProfile().getProfile();

                    BufferedReader reader = new BufferedReader(new StringReader(profileText));

                    String lineToParse;

                    ProfilePoint profilePoint;

                    // remove all points previously created
                    View imageViewToDelete;
                    ViewGroup parent;
                    for (i = 0; i < profilePoints.size(); i++) {
                        imageViewToDelete = (View) profilePointsPointIndexesToDrawings.get(i);
                        parent = (ViewGroup) imageViewToDelete.getParent();
                        if (parent != null) {
                            parent.removeView(imageViewToDelete);
                        }
                    }
                    if (yellowingPoint != null) {
                        parent = (ViewGroup) yellowingPoint.getParent();
                        if (parent != null) {
                            parent.removeView(yellowingPoint);
                        }
                    }
                    if (crackPoint != null) {
                        parent = (ViewGroup) crackPoint.getParent();
                        if (parent != null) {
                            parent.removeView(crackPoint);
                        }
                    }
                    if (coolingPoint != null) {
                        parent = (ViewGroup) coolingPoint.getParent();
                        if (parent != null) {
                            parent.removeView(coolingPoint);
                        }
                    }

                    profilePoints.clear();

                    i = 0;
                    try {
                        lineToParse = reader.readLine();
                        while (lineToParse != null) {

                            String[] tokens = lineToParse.split(",");

                            profilePoint = new ProfilePoint();

                            if (tokens.length >= 3) {
                                profilePoint.setId(i);
                                profilePoint.setTime(Integer.parseInt(tokens[0]));
                                profilePoint.setTemperature(Integer.parseInt(tokens[1]));
                                profilePoint.setFan(Integer.parseInt(tokens[2]));
                            }

                            profilePoints.add(profilePoint);

                            i++;
                            lineToParse = reader.readLine();
                        }
                    }
                    catch(Exception e) { }

                    for (i = 0; i < profilePoints.size(); i++) {

                        RoundedImageView imageViewRound = new RoundedImageView(imgHolder.getContext());
                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(pointSizePx, pointSizePx);
                        params.topMargin = (int)xy.convertY(profilePoints.get(i).getTemperature(), screenHeight) - halfPointSizePx;
                        params.leftMargin = (int)xy.convertX(profilePoints.get(i).getTime(), drawViewParams.width) - halfPointSizePx;
                        imageViewRound.setLayoutParams(params);
                        imgHolder.addView(imageViewRound);
                        profilePointsPointIndexesToDrawings.put(i, imageViewRound);

                        if (i < profilePoints.size() - 1) {

                            if (i == 0) {
                                profileDrawPath.moveTo(
                                        xy.convertX(profilePoints.get(i).getTime(), drawViewParams.width),
                                        xy.convertY(profilePoints.get(i).getTemperature(), screenHeight)
                                );
                            }
                            profileDrawPath.lineTo(
                                    xy.convertX(profilePoints.get(i + 1).getTime(), drawViewParams.width),
                                    xy.convertY(profilePoints.get(i + 1).getTemperature(), screenHeight)
                            );

                        }

                    }

                    drawView.addPathy(profileDrawPath, paintBlack);
                }

                ArrayList<LogPoint> logPoints = logDataStore.getLogPoints();

                Map<Integer, String> eventsMap = new HashMap<Integer, String>();
                for (LogEvent event: logDataStore.getEvents()) {
                    if (eventsMap.containsKey(event.getTime())) {
                        eventsMap.put(event.getTime(), eventsMap.get(event.getTime()) + event.getDescription());
                    }
                    else {
                        eventsMap.put(event.getTime(), event.getDescription());
                    }
                }

                Paint paintProfile = new Paint();
                paintProfile.setAntiAlias(true);
                paintProfile.setStrokeWidth(10f);
                paintProfile.setColor(Color.parseColor("#000000"));
                paintProfile.setStyle(Paint.Style.STROKE);
                paintProfile.setStrokeJoin(Paint.Join.ROUND);
                paintProfile.setDither(true);
                paintProfile.setPathEffect(new CornerPathEffect((float)10.0));
                paintProfile.setStrokeCap(Paint.Cap.ROUND);

                Paint paintReading = new Paint();
                paintReading.setAntiAlias(true);
                paintReading.setStrokeWidth(4f);
                paintReading.setColor(Color.parseColor("#fc640c"));
                paintReading.setStyle(Paint.Style.STROKE);
                paintReading.setStrokeJoin(Paint.Join.ROUND);
                paintReading.setDither(true);
                paintReading.setPathEffect(new CornerPathEffect((float)10.0));
                paintReading.setStrokeCap(Paint.Cap.ROUND);

                Paint paintPower = new Paint();
                paintPower.setAntiAlias(true);
                paintPower.setStrokeWidth(4f);
                paintPower.setColor(Color.parseColor("#fc7171"));
                paintPower.setStyle(Paint.Style.STROKE);
                paintPower.setStrokeJoin(Paint.Join.ROUND);
                paintPower.setDither(true);
                paintPower.setPathEffect(new CornerPathEffect((float)10.0));
                paintPower.setStrokeCap(Paint.Cap.ROUND);

                Paint paintSectionDividerYellowing = new Paint();
                paintSectionDividerYellowing.setAntiAlias(true);
                paintSectionDividerYellowing.setStrokeWidth(4f);
                paintSectionDividerYellowing.setColor(Color.parseColor("#32c93e"));
                paintSectionDividerYellowing.setAlpha(20);
                paintSectionDividerYellowing.setStyle(Paint.Style.FILL_AND_STROKE);
                paintSectionDividerYellowing.setStrokeJoin(Paint.Join.ROUND);
                paintSectionDividerYellowing.setDither(true);
                paintSectionDividerYellowing.setStrokeCap(Paint.Cap.ROUND);

                Paint paintSectionDividerCrack = new Paint(paintSectionDividerYellowing);
                paintSectionDividerCrack.setColor(Color.parseColor("#fce176"));
                paintSectionDividerCrack.setAlpha(20);

                Paint paintSectionDividerElse = new Paint(paintSectionDividerYellowing);
                paintSectionDividerElse.setColor(Color.parseColor("#c96100"));
                paintSectionDividerElse.setAlpha(20);

                Path unknownProfileDrawPath = new Path();
                Path logDrawPath = new Path();
                Path powerDrawPath = new Path();

                Switch showPowerSwitch = findViewById(R.id.switch_show_power);

                coolingPointIndex = 0;
                if (logDataStore.getCoolingTime() > 0) {
                    for (i = 0; i < logPoints.size(); i++) {
                        if (logDataStore.getCoolingTime() == logPoints.get(i).getTime()) {
                            coolingPointIndex = i;
                            break;
                        }
                    }
                }

                for (i = 0; i < logPoints.size(); i++) {
                    if (i < logPoints.size() - 1) {

                        if (!logDataStore.hasKnownRunningProfile()) {
                            if (i == 0) {
                                unknownProfileDrawPath.moveTo(
                                        xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                        xy.convertY((float)logPoints.get(i).getTargetTemperature(), screenHeight)
                                );
                            }
                            unknownProfileDrawPath.lineTo(
                                    xy.convertX(logPoints.get(i + 1).getTime(), drawViewParams.width),
                                    xy.convertY((float)logPoints.get(i + 1).getTargetTemperature(), screenHeight)
                            );


                        }

                        if (i == 0) {
                            logDrawPath.moveTo(
                                    xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                    xy.convertY((float)logPoints.get(i).getTemperature(), screenHeight)
                            );
                        }
                        logDrawPath.lineTo(
                                xy.convertX(logPoints.get(i + 1).getTime(), drawViewParams.width),
                                xy.convertY((float)logPoints.get(i + 1).getTemperature(), screenHeight)
                        );

                        if (showPowerSwitch.isChecked()) {
                            if (i == 0) {
                                powerDrawPath.moveTo(
                                        xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                        xy.convertY((float)logPoints.get(i).getPower(), screenHeight)
                                );
                            }
                            powerDrawPath.lineTo(
                                    xy.convertX(logPoints.get(i + 1).getTime(), drawViewParams.width),
                                    xy.convertY((float)logPoints.get(i + 1).getPower(), screenHeight)
                            );
                        }

                        // mark yellowing time with a point and labels, if greater than 0
                        if (logDataStore.getYellowingTime() > 0 && logDataStore.getYellowingTime() == logPoints.get(i).getTime()) {
                            RoundedImageView imageViewRound = new RoundedImageView(imgHolder.getContext());
                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(pointSizePx, pointSizePx);
                            params.topMargin = (int)xy.convertY((int) Math.round(logPoints.get(i).getTemperature()), screenHeight) - halfPointSizePx;
                            params.leftMargin = (int)xy.convertX(logPoints.get(i).getTime(), drawViewParams.width) - halfPointSizePx;
                            imageViewRound.setLayoutParams(params);
                            imgHolder.addView(imageViewRound);
                            yellowingPoint = imageViewRound;

                            Paint paintText = new Paint();
                            paintText.setAntiAlias(true);
                            paintText.setStrokeWidth(0.0f);
                            paintText.setColor(Color.BLACK);
                            paintText.setStyle(Paint.Style.FILL);
                            paintText.setStrokeJoin(Paint.Join.ROUND);
                            paintText.setTextSize(screenHeight / 80);

                            Paint paintTextTitle = new Paint(paintText);
                            paintTextTitle.setFakeBoldText(true);

                            StringBuilder timeString = new StringBuilder();
                            int seconds = (logPoints.get(i).getTime() / 1000) % 60;
                            int minutes = (logPoints.get(i).getTime() / 1000) / 60;
                            timeString.append(String.format("%02d", minutes));
                            timeString.append(":");
                            timeString.append(String.format("%02d", seconds));

                            drawView.addLabel(
                                    xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                    xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 13, screenHeight),
                                    new Double(logPoints.get(i).getTemperature()).intValue() + "\u00b0",
                                    paintText
                            );
                            drawView.addLabel(
                                    xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                    xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 9, screenHeight),
                                    timeString.toString(),
                                    paintText
                            );
                            drawView.addLabel(
                                    xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                    xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 5, screenHeight),
                                    getResources().getString(R.string.lbl_yellowing_point),
                                    paintTextTitle
                            );

                            Path yellowingDrawPath = new Path();
                            yellowingDrawPath.moveTo(
                                    xy.convertX(0, drawViewParams.width),
                                    xy.convertY(250, screenHeight)
                            );
                            yellowingDrawPath.lineTo(
                                    xy.convertX(0, drawViewParams.width),
                                    xy.convertY(0, screenHeight)
                            );
                            yellowingDrawPath.lineTo(
                                    xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                    xy.convertY(0, screenHeight)
                            );
                            yellowingDrawPath.lineTo(
                                    xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                    xy.convertY(250, screenHeight)
                            );
                            yellowingDrawPath.close();
                            drawView.addPathy(yellowingDrawPath, paintSectionDividerYellowing);

                        }

                        // mark crack time with a point and labels, if greater than 0
                        if (logDataStore.getCrackTime() > 0 && logDataStore.getCrackTime() == logPoints.get(i).getTime()) {
                            RoundedImageView imageViewRound = new RoundedImageView(imgHolder.getContext());
                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(pointSizePx, pointSizePx);
                            params.topMargin = (int)xy.convertY((int) Math.round(logPoints.get(i).getTemperature()), screenHeight) - halfPointSizePx;
                            params.leftMargin = (int)xy.convertX(logPoints.get(i).getTime(), drawViewParams.width) - halfPointSizePx;
                            imageViewRound.setLayoutParams(params);
                            imgHolder.addView(imageViewRound);
                            crackPoint = imageViewRound;

                            Paint paintText = new Paint();
                            paintText.setAntiAlias(true);
                            paintText.setStrokeWidth(0.0f);
                            paintText.setColor(Color.BLACK);
                            paintText.setStyle(Paint.Style.FILL);
                            paintText.setStrokeJoin(Paint.Join.ROUND);
                            paintText.setTextSize(screenHeight / 80);

                            Paint paintTextTitle = new Paint(paintText);
                            paintTextTitle.setFakeBoldText(true);

                            StringBuilder timeString = new StringBuilder();
                            int seconds = (logPoints.get(i).getTime() / 1000) % 60;
                            int minutes = (logPoints.get(i).getTime() / 1000) / 60;
                            timeString.append(String.format("%02d", minutes));
                            timeString.append(":");
                            timeString.append(String.format("%02d", seconds));

                            drawView.addLabel(
                                    xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                    xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 13, screenHeight),
                                    new Double(logPoints.get(i).getTemperature()).intValue() + "\u00b0",
                                    paintText
                            );
                            drawView.addLabel(
                                    xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                    xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 9, screenHeight),
                                    timeString.toString(),
                                    paintText
                            );
                            drawView.addLabel(
                                    xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                    xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 5, screenHeight),
                                    getResources().getString(R.string.lbl_crack_point),
                                    paintTextTitle
                            );

                            Path crackDrawPath = new Path();
                            crackDrawPath.moveTo(
                                    xy.convertX(logDataStore.getYellowingTime(), drawViewParams.width),
                                    xy.convertY(250, screenHeight)
                            );
                            crackDrawPath.lineTo(
                                    xy.convertX(logDataStore.getYellowingTime(), drawViewParams.width),
                                    xy.convertY(0, screenHeight)
                            );
                            crackDrawPath.lineTo(
                                    xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                    xy.convertY(0, screenHeight)
                            );
                            crackDrawPath.lineTo(
                                    xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                    xy.convertY(250, screenHeight)
                            );
                            crackDrawPath.close();
                            drawView.addPathy(crackDrawPath, paintSectionDividerCrack);

                            Path elseDrawPath = new Path();
                            elseDrawPath.moveTo(
                                    xy.convertX(logDataStore.getCrackTime(), drawViewParams.width),
                                    xy.convertY(250, screenHeight)
                            );
                            elseDrawPath.lineTo(
                                    xy.convertX(logDataStore.getCrackTime(), drawViewParams.width),
                                    xy.convertY(0, screenHeight)
                            );
                            elseDrawPath.lineTo(
                                    xy.convertX(logPoints.get(coolingPointIndex > 0 ? coolingPointIndex : logPoints.size() - 1).getTime(), drawViewParams.width),
                                    xy.convertY(0, screenHeight)
                            );
                            elseDrawPath.lineTo(
                                    xy.convertX(logPoints.get(coolingPointIndex > 0 ? coolingPointIndex : logPoints.size() - 1).getTime(), drawViewParams.width),
                                    xy.convertY(250, screenHeight)
                            );
                            elseDrawPath.close();
                            drawView.addPathy(elseDrawPath, paintSectionDividerElse);

                            // calc development
                            double development = 0.0;
                            int endTime = logPoints.get(coolingPointIndex > 0 ? coolingPointIndex : logPoints.size() - 1).getTime();
                            TextView developmentTextView = findViewById(R.id.text_development);
                            if (endTime > 0) {
                                development = (endTime - logDataStore.getCrackTime()) * 100 / (double) endTime;
                                StringBuilder developmentString = new StringBuilder();
                                developmentString.append(String.format("%.1f", development));
                                developmentString.append("%");
                                developmentTextView.setText(developmentString.toString());
                            }
                            else {
                                developmentTextView.setText("");
                            }
                        }

                        // mark cooling time with a point and labels, if greater than 0
                        if (logDataStore.getCoolingTime() > 0 && logDataStore.getCoolingTime() == logPoints.get(i).getTime()) {
                            RoundedImageView imageViewRound = new RoundedImageView(imgHolder.getContext());
                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(pointSizePx, pointSizePx);
                            params.topMargin = (int)xy.convertY((int) Math.round(logPoints.get(i).getTemperature()), screenHeight) - halfPointSizePx;
                            params.leftMargin = (int)xy.convertX(logPoints.get(i).getTime(), drawViewParams.width) - halfPointSizePx;
                            imageViewRound.setLayoutParams(params);
                            imgHolder.addView(imageViewRound);
                            coolingPoint = imageViewRound;

                            Paint paintText = new Paint();
                            paintText.setAntiAlias(true);
                            paintText.setStrokeWidth(0.0f);
                            paintText.setColor(Color.BLACK);
                            paintText.setStyle(Paint.Style.FILL);
                            paintText.setStrokeJoin(Paint.Join.ROUND);
                            paintText.setTextSize(screenHeight / 80);

                            Paint paintTextTitle = new Paint(paintText);
                            paintTextTitle.setFakeBoldText(true);

                            StringBuilder timeString = new StringBuilder();
                            int seconds = (logPoints.get(i).getTime() / 1000) % 60;
                            int minutes = (logPoints.get(i).getTime() / 1000) / 60;
                            timeString.append(String.format("%02d", minutes));
                            timeString.append(":");
                            timeString.append(String.format("%02d", seconds));

                            drawView.addLabel(
                                    xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                    xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 13, screenHeight),
                                    new Double(logPoints.get(i).getTemperature()).intValue() + "\u00b0",
                                    paintText
                            );
                            drawView.addLabel(
                                    xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                    xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 9, screenHeight),
                                    timeString.toString(),
                                    paintText
                            );
                            drawView.addLabel(
                                    xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                    xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 5, screenHeight),
                                    getResources().getString(R.string.lbl_cooling_point),
                                    paintTextTitle
                            );
                        }

                        // mark events with point and labels
                        if (eventsMap.containsKey(logPoints.get(i).getTime())) {
                            View imageViewToDelete = (View) interestPointIndexesToDrawings.get(i);
                            if (imageViewToDelete != null) {
                                ViewGroup parent = (ViewGroup) imageViewToDelete.getParent();
                                if (parent != null) {
                                    parent.removeView(imageViewToDelete);
                                }
                            }

                            RoundedImageView imageViewRound = new RoundedImageView(imgHolder.getContext());
                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(pointSizePx, pointSizePx);
                            params.topMargin = (int)xy.convertY((int) Math.round(logPoints.get(i).getTemperature()), screenHeight) - halfPointSizePx;
                            params.leftMargin = (int)xy.convertX(logPoints.get(i).getTime(), drawViewParams.width) - halfPointSizePx;
                            imageViewRound.setLayoutParams(params);
                            imgHolder.addView(imageViewRound);
                            interestPointIndexesToDrawings.put(i, imageViewRound);

                            Paint paintText = new Paint();
                            paintText.setAntiAlias(true);
                            paintText.setStrokeWidth(0.0f);
                            paintText.setColor(Color.BLACK);
                            paintText.setStyle(Paint.Style.FILL);
                            paintText.setStrokeJoin(Paint.Join.ROUND);
                            paintText.setTextSize(screenHeight / 80);

                            Paint paintTextTitle = new Paint(paintText);
                            paintTextTitle.setFakeBoldText(true);

                            StringBuilder timeString = new StringBuilder();
                            int seconds = (logPoints.get(i).getTime() / 1000) % 60;
                            int minutes = (logPoints.get(i).getTime() / 1000) / 60;
                            timeString.append(String.format("%02d", minutes));
                            timeString.append(":");
                            timeString.append(String.format("%02d", seconds));

                            drawView.addLabel(
                                    xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                    xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 13, screenHeight),
                                    new Double(logPoints.get(i).getTemperature()).intValue() + "\u00b0",
                                    paintText
                            );
                            drawView.addLabel(
                                    xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                    xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 9, screenHeight),
                                    timeString.toString(),
                                    paintText
                            );
                            drawView.addLabel(
                                    xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                                    xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 5, screenHeight),
                                    eventsMap.get(logPoints.get(i).getTime()),
                                    paintTextTitle
                            );
                        }

                    }
                }
                if (!logDataStore.hasKnownRunningProfile()) drawView.addPathy(unknownProfileDrawPath, paintProfile);
                drawView.addPathy(logDrawPath, paintReading);
                if (showPowerSwitch.isChecked()) {
                    drawView.addPathy(powerDrawPath, paintPower);
                }

                if (logPoints.size() > 0) {
                    i = logPoints.size() - 1;
                    Paint paintText = new Paint();
                    paintText.setAntiAlias(true);
                    paintText.setStrokeWidth(0.0f);
                    paintText.setColor(Color.BLACK);
                    paintText.setStyle(Paint.Style.FILL);
                    paintText.setStrokeJoin(Paint.Join.ROUND);

                    paintText.setTextSize(screenHeight / 40);

                    StringBuilder timeString = new StringBuilder();
                    int seconds = (logPoints.get(i).getTime() / 1000) % 60;
                    int minutes = (logPoints.get(i).getTime() / 1000) / 60;
                    timeString.append(String.format("%02d", minutes));
                    timeString.append(":");
                    timeString.append(String.format("%02d", seconds));

                    StringBuilder localErrorString = new StringBuilder();
                    Double localErrorMeasure = logPoints.get(i).getTargetTemperature() - logPoints.get(i).getTemperature();
                    if (localErrorMeasure > 0.1 || localErrorMeasure < -0.1) {
                        if (localErrorMeasure > 0.0) localErrorString.append("-");
                        else localErrorString.append("+");
                        localErrorString.append(new DecimalFormat("#0.0").format(Math.abs(localErrorMeasure)));
                    }

                    drawView.addLabel(
                            xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                            xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 17, screenHeight),
                            new Double(logPoints.get(i).getTemperature()).intValue() + "\u00b0" + (!localErrorString.toString().equals("") ? " (" + localErrorString.toString() + ")" : ""),
                            paintText
                    );
                    drawView.addLabel(
                            xy.convertX(logPoints.get(i).getTime(), drawViewParams.width),
                            xy.convertY(new Double(logPoints.get(i).getTemperature()).intValue() - 9, screenHeight),
                            timeString.toString(),
                            paintText
                    );
                }

                Switch scrollScreenSwitch = findViewById(R.id.switch_scroll_screen);

                if (scrollScreenSwitch.isChecked() && logPoints.size() > 0 && xy.convertX(logPoints.get(logPoints.size() - 1).getTime(), drawViewParams.width) - imgHolder.getScrollX() >= (screenWidth * 2 / 3)) {

                    imgHolder.scrollBy((int)xy.convertX(logPoints.get(logPoints.size() - 1).getTime(), drawViewParams.width) - imgHolder.getScrollX() - (screenWidth * 2 / 3), 0);

                }

                drawView.invalidateCanvas();
            }



        });

        drawView.invalidateCanvas();
    }

    public void updateScreenSize() {

        drawLogOnCurrentState();
    }

    public static void runJustBeforeBeingDrawn(final View view, final Runnable runnable) {
        final ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                runnable.run();
                return true;
            }
        };
        view.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (dataUpdateReceiver == null) {
            dataUpdateReceiver = new DataUpdateReceiver();
        }

        IntentFilter intentFilter = new IntentFilter(MainActivity.INTENT_MESSAGE_PREFIX + "LOG_DATA_CHANGED");
        registerReceiver(dataUpdateReceiver, intentFilter);

        intentFilter = new IntentFilter(MainActivity.INTENT_MESSAGE_PREFIX + "LOG_DATA_STARTED");
        registerReceiver(dataUpdateReceiver, intentFilter);

        intentFilter = new IntentFilter(MainActivity.INTENT_MESSAGE_PREFIX + "LOG_DATA_COOLING");
        registerReceiver(dataUpdateReceiver, intentFilter);

        intentFilter = new IntentFilter(MainActivity.INTENT_MESSAGE_PREFIX + "LOG_DATA_ENDED");
        registerReceiver(dataUpdateReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (dataUpdateReceiver != null) unregisterReceiver(dataUpdateReceiver);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateScreenSize();
    }

}
