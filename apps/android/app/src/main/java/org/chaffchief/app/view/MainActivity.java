package org.chaffchief.app.view;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.UUID;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.BluetoothSPP.BluetoothConnectionListener;
import app.akexorcist.bluetotohspp.library.BluetoothSPP.OnDataReceivedListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.chaffchief.app.R;
import org.chaffchief.app.database.DatabaseHelper;
import org.chaffchief.app.database.model.CurrentLog;
import org.chaffchief.app.database.model.LogEvent;
import org.chaffchief.app.database.model.Profile;
import org.chaffchief.app.utils.MyDividerItemDecoration;
import org.chaffchief.app.utils.RecyclerTouchListener;
import org.chaffchief.app.utils.RecyclerTouchHelper;

import android.support.v7.widget.helper.ItemTouchHelper;

import static android.support.design.widget.Snackbar.LENGTH_LONG;
import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity implements RecyclerTouchHelper.RecyclerItemTouchHelperListener {
    private ProfilesAdapter mAdapter;
    private final List<Profile> profilesList = new ArrayList<>();
    private CoordinatorLayout coordinatorLayout;
    private RecyclerView recyclerView;
    private TextView noProfilesView;

    public static final int REQUEST_ENABLE_BT = 1;

    String profileOnRoasterUuid;

    Profile profileOnRoaster;

    private DatabaseHelper db;

    private Menu menu;

    public static final String INTENT_MESSAGE_PREFIX = "org.chaffchief.app.";

    public boolean askedToTurnBluetoothOn = false;

    public Activity thisActivity = null;

    public static final int ACTIVITY_CHOOSE_FILE = 3;

    public static void setForceShowIcon(PopupMenu popupMenu) {
        try {
            Field[] fields = popupMenu.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper
                            .getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod(
                            "setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Throwable e) {

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case BluetoothState.REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    //
                }
                break;
            case 1:
                if (data == null) return;

                // Collect data from the intent and use it
                long profileId = data.getLongExtra(INTENT_MESSAGE_PREFIX + "return_profile_id", -1);
                int profilePosition = data.getIntExtra(INTENT_MESSAGE_PREFIX + "return_profile_position", -1);

                if (profilePosition >= 0 && profileId >= 0) {
                    Profile profile = db.getProfile(profileId);
                    profilesList.set(profilePosition, profile);
                    mAdapter.notifyItemChanged(profilePosition);
                }

                else if (profilePosition == -1 && profileId >= 0) {
                    Profile profile = db.getProfile(profileId);
                    profilesList.add(0, profile);
                    mAdapter.notifyItemInserted(0);
                }

                break;
            case ACTIVITY_CHOOSE_FILE:

                Uri selectedFileUri = data.getData();

                String selectedFilePath = selectedFileUri.getPath();

                String selectedFileName = getFileNameFromUri(selectedFileUri);

                InputStream inputStream = null;
                try {
                    inputStream = getContentResolver().openInputStream(selectedFileUri);
                } catch (FileNotFoundException e) {

                }

                if (inputStream != null && selectedFileName.endsWith(".db")) {

                    File currentDatabase = new File(db.getWritableDatabase().getPath());

                    db.close();
                    db = null;

                    try {
                        copyInputStream(inputStream, currentDatabase);
                    } catch (IOException e) {

                    }

                    db = new DatabaseHelper(this);

                    profilesList.clear();
                    profilesList.addAll(db.getAllProfiles());

                    mAdapter.notifyDataSetChanged();

                    restoreSharedPreferencesFromDb();

                    Toast.makeText(getApplicationContext(), R.string.msg_restore_database_successful, Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(getApplicationContext(), R.string.msg_restore_database_error, Toast.LENGTH_LONG).show();
                }

                break;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        coordinatorLayout = findViewById(R.id.coordinator_layout);
        recyclerView = findViewById(R.id.recycler_view);
        noProfilesView = findViewById(R.id.empty_profiles_view);

        thisActivity = this;

        db = new DatabaseHelper(this);

        profilesList.addAll(db.getAllProfiles());

        LinearLayout roasterToolbar = findViewById(R.id.roaster_toolbar);
        roasterToolbar.setVisibility(View.GONE);

        // read preferences from db and set them overwriting android`s own values
        restoreSharedPreferencesFromDb();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            showEditProfileDialog(false,null, -1, false);
            }
        });

        FloatingActionButton viewLogButton = findViewById(R.id.button_view_log);
        viewLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent2 = new Intent(getApplicationContext(), LogViewerActivity.class);
                String message2 = "aaa";
                intent2.putExtra(INTENT_MESSAGE_PREFIX + "log_viewer_data1", message2);
                startActivity(intent2);
            }
        });

        mAdapter = new ProfilesAdapter(this, profilesList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new MyDividerItemDecoration(this, LinearLayoutManager.VERTICAL, 16));
        recyclerView.setAdapter(mAdapter);

        toggleEmptyProfiles();

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                recyclerView, new RecyclerTouchListener.ClickListener() {

            @Override
            public void onClick(View view, final int position) {
                showEditProfileDialog(true, profilesList.get(position), position, true);
            }

            @Override
            public void onLongClick(final View view, final int position) {
                PopupMenu popup = new PopupMenu(recyclerView.getContext(), view);
                popup.getMenuInflater().inflate(R.menu.menu_notes, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.menu_profile_popup_rename:

                                showEditProfileDialog(true, profilesList.get(position), position, false);
                                return true;
                            case R.id.menu_profile_popup_duplicate:

                                showEditProfileDialog(false, profilesList.get(position), position, false);
                                return true;
                            case R.id.menu_profile_popup_delete:

                                deleteProfileWithUndo(position);
                                return true;
                            case R.id.menu_profile_popup_send:

                                sendProfile(profilesList.get(position));
                                return true;

                            case R.id.menu_profile_popup_share_as_text:

                                Profile profileToShare;
                                profileToShare = profilesList.get(position);

                                Intent sendIntent = new Intent();
                                sendIntent.setAction(Intent.ACTION_SEND);
                                sendIntent.putExtra(Intent.EXTRA_TEXT, profileToShare.getProfile());
                                sendIntent.setType("text/plain");
                                startActivity(sendIntent);

                                return true;

                            default:
                                return false;
                        }
                    }
                });

                setForceShowIcon(popup);
                boolean isRoasterConnected = (BluetoothServiceContainer.getInstance().bt.getServiceState() == BluetoothState.STATE_CONNECTED);
                popup.getMenu().findItem(R.id.menu_profile_popup_send).setEnabled(isRoasterConnected);
                popup.show();
            }
        }));

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerTouchHelper(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);

        BluetoothServiceContainer.getInstance().bt = new BluetoothSPP(this);

        ImageButton btnStartStop = findViewById(R.id.button_start_stop);
        btnStartStop.setOnClickListener(new OnClickListener(){
            public void onClick(View v){

                if (BluetoothServiceContainer.getInstance().bt != null) {
                    if(BluetoothServiceContainer.getInstance().bt.getServiceState() == BluetoothState.STATE_CONNECTED) {
                        BluetoothServiceContainer.getInstance().bt.send("command_start_stop", true);
                    }
                }

            }
        });

        if (ReceivedDataStore.getInstance().size() == 0) {
            List<String> currentLogLines = db.getCurrentLogLines();
            for (int i = 0; i < currentLogLines.size(); i++) {
                ReceivedDataStore.getInstance().addReceivedLogLine(currentLogLines.get(i));
            }
            List<LogEvent> currentLogEvents = db.getCurrentLogEvents();
            for (int i = 0; i < currentLogEvents.size(); i++) {
                ReceivedDataStore.getInstance().addEvent(currentLogEvents.get(i).getTime(), currentLogEvents.get(i).getDescription());
            }
        }

    }

    private void restoreSharedPreferencesFromDb() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (db.getPreferenceValue("preference_roaster_number_selected").isEmpty()) {
            db.setPreference("preference_roaster_number_selected", "1");
        }
        prefs.edit()
                .putString("preference_roaster1_address", db.getPreferenceValue("preference_roaster1_address"))
                .putString("preference_roaster2_address", db.getPreferenceValue("preference_roaster2_address"))
                .putString("preference_roaster3_address", db.getPreferenceValue("preference_roaster3_address"))
                .putString("preference_roaster4_address", db.getPreferenceValue("preference_roaster4_address"))
                .putString("preference_roaster5_address", db.getPreferenceValue("preference_roaster5_address"))
                .putString("preference_roaster6_address", db.getPreferenceValue("preference_roaster6_address"))
                .putString("preference_roaster_number_selected", db.getPreferenceValue("preference_roaster_number_selected"))
                .apply();
    }

    public String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public static void copyInputStream(InputStream in, File dst) throws IOException {
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        String message;

        switch (item.getItemId()) {
            case R.id.menu_main:

                return true;
            case R.id.menu_location:

                return true;
            case R.id.action_settings:

                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_logs_list:

                intent = new Intent(getApplicationContext(), LogListActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_log_viewer:

                intent = new Intent(getApplicationContext(), LogViewerActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_backup:

                String logDateTime = "";

                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date();
                SimpleDateFormat fmtOut = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
                logDateTime = fmtOut.format(date);

                String appName = getResources().getString(R.string.app_name);

                String backupFileTitle = logDateTime + "-" + appName + ".db";

                File backupFolderFile = new File(getFilesDir(),"backups");
                if (!backupFolderFile.exists()){
                    backupFolderFile.mkdir();
                }

                File backupDestinatonFile = new File(backupFolderFile, backupFileTitle);

                File currentDatabase = new File(db.getWritableDatabase().getPath());

                try {
                    copy(currentDatabase, backupDestinatonFile);
                } catch (IOException e) {

                }

                File newFile = new File(backupFolderFile, backupFileTitle);
                Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), "org.chaffchief.app.provider", newFile);

                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.setType("application/octet-stream");
                startActivity(Intent.createChooser(shareIntent, "Send backup to..."));

                return true;
            case R.id.action_restore:

                Intent chooseFile;
                Intent chooseIntent;
                chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
                chooseFile.setType("*/*");
                chooseIntent = Intent.createChooser(chooseFile, "Choose a file");
                startActivityForResult(chooseIntent, ACTIVITY_CHOOSE_FILE);

                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void onDestroy() {
      super.onDestroy();
    }

    public void onStart() {
        super.onStart();
    }

    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom rnd = new SecureRandom();

    @Override
    protected void onResume() {
        super.onResume();

        boolean deviceHasBluetooth = (BluetoothAdapter.getDefaultAdapter() != null);
        boolean bluetoothIsEnabled = deviceHasBluetooth && BluetoothAdapter.getDefaultAdapter().isEnabled();

        if (deviceHasBluetooth && bluetoothIsEnabled) {
            BluetoothServiceContainer.getInstance().bt.setBluetoothConnectionListener(new BluetoothConnectionListener() {
                public void onDeviceConnected(String name, String address) {
                    menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_connection_ok));
                    String subtitle = "Connected: " + name;
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle(subtitle);
                    }

                    LinearLayout roasterToolbar = findViewById(R.id.roaster_toolbar);
                    roasterToolbar.setVisibility(View.VISIBLE);
                }

                public void onDeviceDisconnected() {
                    if (menu != null) {
                        menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_connection_error));
                        String subtitle = "Disconnected";
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setSubtitle(subtitle);
                        }
                    }

                    LinearLayout roasterToolbar = findViewById(R.id.roaster_toolbar);
                    roasterToolbar.setVisibility(View.GONE);
                }

                public void onDeviceConnectionFailed() {
                    if (menu != null) {
                        menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_connection_error));
                        String subtitle = "Connection failed";
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setSubtitle(subtitle);
                        }

                    }

                    LinearLayout roasterToolbar = findViewById(R.id.roaster_toolbar);
                    roasterToolbar.setVisibility(View.GONE);
                }
            });


            if (BluetoothServiceContainer.getInstance().bt.isServiceAvailable() && !BluetoothServiceContainer.getInstance().bt.isBluetoothEnabled()) {
                // only once in the lifecycle of the activity
                if (!askedToTurnBluetoothOn) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
                    askedToTurnBluetoothOn = true;
                }
            }
            else {
                if(!BluetoothServiceContainer.getInstance().bt.isServiceAvailable() || BluetoothServiceContainer.getInstance().bt.getServiceState() != BluetoothState.STATE_CONNECTED) {

                    BluetoothServiceContainer.getInstance().bt.setupService();
                    BluetoothServiceContainer.getInstance().bt.startService(BluetoothState.DEVICE_OTHER);

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                    String roaster_address = "";
                    String roaster_name = "";

                    String string_roaster_number = prefs.getString("preference_roaster_number_selected", "1");
                    int roaster_number = Integer.parseInt(string_roaster_number);
                    switch (roaster_number) {
                        case 1:
                            roaster_address = prefs.getString("preference_roaster1_address", "");
                            break;
                        case 2:
                            roaster_address = prefs.getString("preference_roaster2_address", "");
                            break;
                        case 3:
                            roaster_address = prefs.getString("preference_roaster3_address", "");
                            break;
                        case 4:
                            roaster_address = prefs.getString("preference_roaster4_address", "");
                            break;
                        case 5:
                            roaster_address = prefs.getString("preference_roaster5_address", "");
                            break;
                        case 6:
                            roaster_address = prefs.getString("preference_roaster6_address", "");
                            break;
                        default:
                            roaster_address = "";
                            break;
                    }

                    // user can specify a name for each roaster using a comma to separate the address from the name
                    String[] parts = roaster_address.split(",");
                    if (parts.length > 1) {
                        roaster_address = parts[0];
                        roaster_name = parts[1];
                    }

                    if (roaster_address.length() > 0 && roaster_address.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) {
                        BluetoothServiceContainer.getInstance().bt.connect(roaster_address);
                        BluetoothServiceContainer.getInstance().setRoasterName(roaster_name);
                        BluetoothServiceContainer.getInstance().setRoasterAddress(roaster_address);
                    }

                }
            }

            BluetoothServiceContainer.getInstance().bt.setOnDataReceivedListener(new OnDataReceivedListener() {
                public void onDataReceived(byte[] data, final String message) {
                    if (message.matches("(\\s*([\\d|.]+)\\s*,?)*")) {
                        if (ReceivedDataStore.getInstance().isLogging()) {
                            ReceivedDataStore.getInstance().addReceivedLogLine(message);
                            db.insertCurrentLogLine(message);
                            sendBroadcast(new Intent(INTENT_MESSAGE_PREFIX + "LOG_DATA_CHANGED"));
                        }

                        final Button buttonViewTime = findViewById(R.id.button_view_time);
                        final Button buttonViewTemp = findViewById(R.id.button_view_temp);
                        final Button buttonViewProfileTitle = findViewById(R.id.button_view_profile_title);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                FloatingActionButton viewLogButton = findViewById(R.id.button_view_log);
                                viewLogButton.setVisibility(View.VISIBLE);

                                ArrayList<String> reads = new ArrayList<String>();
                                Pattern pattern = Pattern.compile("([\\d|.]+)+");
                                Matcher matcher = pattern.matcher(message);
                                while (matcher.find()) {
                                    reads.add(matcher.group());
                                }
                                if (reads.size() >= 6) {
                                    if (Integer.parseInt(reads.get(0)) > 0) {
                                        ReceivedDataStore.getInstance().setLogging(true);
                                    }

                                    int bruteSeconds = Integer.parseInt(reads.get(0)) / 1000;
                                    int minutes = bruteSeconds / 60;
                                    int seconds = bruteSeconds % 60;
                                    String timeString = String.format("%02d:%02d", minutes, seconds);

                                    if (profileOnRoaster != null) {
                                        buttonViewProfileTitle.setText(profileOnRoaster.getTitle());
                                    }
                                    else {
                                        buttonViewProfileTitle.setText("");
                                    }

                                    buttonViewTime.setText(timeString);
                                    double temperatureValue = Double.parseDouble(reads.get(1));
                                    DecimalFormat df = new DecimalFormat("0.0");
                                    buttonViewTemp.setText(df.format(temperatureValue) + "\u00b0");
                                }
                            }
                        });


                    }
                    else if (message.equals("log_start")) {
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

                        Toast.makeText(thisActivity, getResources().getString(R.string.msg_roast_started), Toast.LENGTH_SHORT).show();

                        sendBroadcast(new Intent(INTENT_MESSAGE_PREFIX + "LOG_DATA_STARTED"));

                        ReceivedDataStore.getInstance().setLogging(true);

                        db.changeCurrentLogTimestamp();
                        db.changeCurrentLogRoaster(BluetoothServiceContainer.getInstance().getRoasterName(), BluetoothServiceContainer.getInstance().getRoasterAddress());

                        if (profileOnRoaster != null) {
                            db.changeCurrentLogProfileUuid(profileOnRoasterUuid);
                            db.changeCurrentLogName(profileOnRoaster.getTitle());
                            ReceivedDataStore.getInstance().setRunningProfile(profileOnRoaster);
                        }
                        else {
                            db.changeCurrentLogProfileUuid("");
                            db.changeCurrentLogName("(%s)".replace("%s", getResources().getString(R.string.unknown_profile_title)));
                            ReceivedDataStore.getInstance().setRunningProfile(null);
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                FloatingActionButton viewLogButton = findViewById(R.id.button_view_log);
                                viewLogButton.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                    else if (message.startsWith("uuid")) {

                        profileOnRoasterUuid = message.substring(4);

                        Profile lastProfileOnRoaster = profileOnRoaster;

                        if (profileOnRoasterUuid != null && !profileOnRoasterUuid.isEmpty()) {
                            profileOnRoaster = db.getProfileByUuid(profileOnRoasterUuid);
                        }
                        else {
                            profileOnRoaster = null;
                        }

                        if (lastProfileOnRoaster == null && profileOnRoaster != null) {
                            db.changeCurrentLogProfileUuid(profileOnRoasterUuid);
                            db.changeCurrentLogName(profileOnRoaster.getTitle());
                            ReceivedDataStore.getInstance().setRunningProfile(profileOnRoaster);
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Button buttonViewProfileTitle = findViewById(R.id.button_view_profile_title);

                                if (profileOnRoaster != null) {
                                    buttonViewProfileTitle.setText(profileOnRoaster.getTitle());
                                }
                                else {
                                    buttonViewProfileTitle.setText("");
                                }
                            }
                        });

                    }
                    else if (message.equals("log_cooling")) {
                        Toast.makeText(thisActivity, getResources().getString(R.string.msg_roast_cooling), Toast.LENGTH_SHORT).show();

                        int timeStartedCooling = ReceivedDataStore.getInstance().getLogPoints().get(ReceivedDataStore.getInstance().getLogPoints().size() - 1).getTime();
                        ReceivedDataStore.getInstance().setCoolingTime(timeStartedCooling);

                        sendBroadcast(new Intent(INTENT_MESSAGE_PREFIX + "LOG_DATA_COOLING"));
                    }
                    else if (message.equals("log_end")) {
                        Toast.makeText(thisActivity, getResources().getString(R.string.msg_roast_ended), Toast.LENGTH_SHORT).show();

                        ReceivedDataStore.getInstance().setLogging(false);

                        sendBroadcast(new Intent(INTENT_MESSAGE_PREFIX + "LOG_DATA_ENDED"));
                    }
                    else if (message.equals("ack")) {
                        BluetoothServiceContainer.getInstance().setAckReceived(true);
                    }

                }
            });

        }

        FloatingActionButton viewLogButton = findViewById(R.id.button_view_log);

        if (ReceivedDataStore.getInstance().size() > 0) {
            viewLogButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {

    }

    private void createProfile(String title, String profile) {
        long id = db.insertProfile(title, profile, UUID.randomUUID().toString());
        Profile n = db.getProfile(id);
        if (n != null) {
            profilesList.add(0, n);
            mAdapter.notifyDataSetChanged();

            toggleEmptyProfiles();
        }
    }

    private void recreateProfile(int id) {
        db.undeleteProfile(id);
        Profile undeletedProfile = db.getProfile(id);
        profilesList.add(profilesList.size(), undeletedProfile);
        mAdapter.notifyDataSetChanged();

        toggleEmptyProfiles();
    }

    private void updateProfile(String title, String profile, int position) {
        Profile n = profilesList.get(position);

        n.setTitle(title);
        n.setProfile(profile);

        n.setUuid(UUID.randomUUID().toString());

        db.updateProfile(n);

        profilesList.set(position, n);
        mAdapter.notifyItemChanged(position);

        toggleEmptyProfiles();
    }

    private void deleteProfile(int position) {
        db.deleteProfile(profilesList.get(position));
        profilesList.remove(position);
        mAdapter.notifyItemRemoved(position);

        toggleEmptyProfiles();
    }

    private void deleteProfileWithUndo(int position) {
        final Profile profileToDelete = profilesList.get(position);

        deleteProfile(position);

        Snackbar snackbar = Snackbar.make(coordinatorLayout, getResources().getString(R.string.msg_action_note_deleted), LENGTH_LONG);
        snackbar.setAction(getResources().getString(R.string.msg_action_undo), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            recreateProfile(profileToDelete.getId());
            }
        });
        snackbar.setDuration(30000);
        snackbar.setActionTextColor(Color.YELLOW);
        snackbar.show();
    }

    public class MyRunnable implements Runnable {
        private final String data;
        public MyRunnable(String _data) {
            this.data = _data;
        }

        @Override
        public void run() {
            if (BluetoothServiceContainer.getInstance().bt != null) BluetoothServiceContainer.getInstance().bt.send(this.data, true);
        }
    }

    private void sendProfile(final Profile profile) {
        if (profile != null) {
            String profileContent = profile.getProfile();
            final String profileToSend;

            profileToSend = new StringBuilder().append("start_receive_profile\nuuid").append(profile.getUuid()).append("\n").append(profileContent).append("\nend_receive_profile\n\n").toString();

            Thread sendProfileThread = new Thread() {
                @Override
                public void run() {

                    Snackbar snackbar = Snackbar.make(coordinatorLayout, getResources().getString(R.string.msg_action_note_sending), Snackbar.LENGTH_LONG);
                    snackbar.setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //
                        }
                    });
                    snackbar.setDuration(3000);
                    snackbar.setActionTextColor(Color.YELLOW);
                    snackbar.show();

                    BufferedReader reader = new BufferedReader(new StringReader(profileToSend));
                    String lineToSend;
                    int i = 0;
                    try {
                        lineToSend = reader.readLine();
                        while (lineToSend != null) {

                            if (lineToSend.isEmpty()) {
                                i++;
                                lineToSend = reader.readLine();
                                continue;
                            }

                            long startTime = System.currentTimeMillis();

                            if (BluetoothServiceContainer.getInstance().bt != null) {

                                BluetoothServiceContainer.getInstance().setAckReceived(false);

                                BluetoothServiceContainer.getInstance().bt.send(lineToSend, true);

                                while(!BluetoothServiceContainer.getInstance().isAckReceived()) {
                                    // this thread just waits for the ack, but it has a time limit on each command it sends

                                    sleep(100);

                                    if (System.currentTimeMillis() - startTime > 5000) {
                                        Snackbar snackbar2 = Snackbar.make(coordinatorLayout, getResources().getString(R.string.msg_action_note_error), Snackbar.LENGTH_INDEFINITE);
                                        snackbar2.setAction("OK", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                            //
                                            }
                                        });
                                        View snackbar2View = snackbar2.getView();
                                        TextView snackbar2TextView = snackbar2View.findViewById(R.id.snackbar_text);
                                        snackbar2TextView.setTextColor(Color.RED);
                                        snackbar2.setActionTextColor(Color.YELLOW);
                                        snackbar2.show();

                                        this.interrupt();
                                    }
                                }
                            }

                            i++;
                            lineToSend = reader.readLine();
                        }

                        Snackbar snackbar3 = Snackbar.make(coordinatorLayout, getResources().getString(R.string.msg_action_note_received), LENGTH_LONG);
                        snackbar3.setAction("OK", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                //
                            }
                        });
                        snackbar3.setDuration(3000);
                        snackbar3.setActionTextColor(Color.YELLOW);
                        snackbar3.show();
                    }
                    catch(Exception e) { }
                }
            };

            sendProfileThread.start();
        }
    }

    private void showEditProfileDialog(final boolean shouldUpdate, final Profile profile, final int position, final boolean showEditContent) {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(getApplicationContext());
        View view = layoutInflaterAndroid.inflate(R.layout.note_dialog, null);

        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilderUserInput.setView(view);

        final EditText inputTitle = view.findViewById(R.id.title);
        final EditText inputProfile = view.findViewById(R.id.profile);
        final TextView showTimestamp = view.findViewById(R.id.timestamp);
        TextView dialogTitle = view.findViewById(R.id.dialog_title);
        dialogTitle.setText(!shouldUpdate ? getString(R.string.lbl_new_note_title) : getString(R.string.lbl_edit_note_title));

        if (!showEditContent) {
            inputProfile.setVisibility(GONE);
        }

        if (shouldUpdate && profile != null) {
            inputTitle.setText(profile.getTitle());
            inputProfile.setText(profile.getProfile());
            String timestampToShow;
            try {
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = fmt.parse(profile.getTimestamp());

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeZone(TimeZone.getDefault());
                calendar.setTime(fmt.parse(profile.getTimestamp()));

                SimpleDateFormat fmtOut = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

                timestampToShow = fmtOut.format(calendar.getTime());
            }
            catch (ParseException e) {
                timestampToShow = "";
            }
            showTimestamp.setText(timestampToShow);
        }
        else if (!shouldUpdate && profile != null) {
            inputTitle.setText(profile.getTitle());
        }

        alertDialogBuilderUserInput
                .setCancelable(true)
                .setPositiveButton(shouldUpdate ? getResources().getString(R.string.lbl_update) : getResources().getString(R.string.lbl_save), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) {

                    }
                })
                .setNegativeButton(getResources().getString(R.string.lbl_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) {
                                dialogBox.cancel();
                            }
                        });

        final AlertDialog alertDialog = alertDialogBuilderUserInput.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (showEditContent && TextUtils.isEmpty(inputProfile.getText().toString())) {
                    String stringHintEnterProfile = getResources().getString(R.string.hint_enter_note);
                    Toast.makeText(MainActivity.this, stringHintEnterProfile, Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    alertDialog.dismiss();
                }

                if (shouldUpdate && profile != null) {
                    // update profile by its id

                    String profileText;
                    if (!showEditContent) {
                        profileText = profile.getProfile();
                    }
                    else {
                        profileText = inputProfile.getText().toString();
                    }

                    updateProfile(inputTitle.getText().toString(), profileText, position);
                } else {
                    // create new profile

                    String profileText = "";
                    if (!showEditContent) {
                        if (profile == null) {
                            profileText = "0,30,80";
                        }
                        else {
                            profileText = profile.getProfile();
                        }
                    }
                    else {
                        profileText = inputProfile.getText().toString();
                    }

                    createProfile(inputTitle.getText().toString(), profileText);
                }
            }
        });
    }

    private void toggleEmptyProfiles() {
        if (db.getProfilesCount() > 0) {
            noProfilesView.setVisibility(GONE);
        } else {
            noProfilesView.setVisibility(View.VISIBLE);
        }
    }

}
