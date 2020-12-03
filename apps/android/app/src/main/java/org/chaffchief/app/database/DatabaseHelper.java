package org.chaffchief.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.chaffchief.app.database.model.CurrentLog;
import org.chaffchief.app.database.model.Log;
import org.chaffchief.app.database.model.LogEvent;
import org.chaffchief.app.database.model.Profile;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "chaffchief_db";

    public static final String PROFILE_CREATE_TABLE1 =
            "CREATE TABLE profile("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "is_deleted INTEGER DEFAULT 0"
            + ")";

    public static final String PROFILE_CREATE_TABLE2 =
            "CREATE TABLE profile_version("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "uuid TEXT,"
            + "profile_id INTEGER,"
            + "title TEXT,"
            + "profile TEXT,"
            + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP"
            + ")";

    public static final String PROFILE_CREATE_TABLE3 =
            "CREATE TABLE log_current("
                    + "title TEXT PRIMARY KEY,"
                    + "profile_version_uuid TEXT,"
                    + "comments TEXT,"
                    + "info_yellowing_time INTEGER,"
                    + "info_crack_time INTEGER,"
                    + "info_cooling_time INTEGER,"
                    + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "roaster_name TEXT,"
                    + "roaster_address TEXT"
                    + ")";

    public static final String PROFILE_CREATE_TABLE4 =
            "CREATE TABLE log_current_lines("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "line TEXT"
                    + ")";

    public static final String PROFILE_CREATE_TABLE4_2 =
            "CREATE TABLE log_current_events("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "time INTEGER,"
                    + "description TEXT"
                    + ")";

    public static final String PROFILE_CREATE_TABLE5 =
            "CREATE TABLE log("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "profile_version_uuid TEXT,"
                    + "title TEXT,"
                    + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "comments TEXT,"
                    + "lines TEXT,"
                    + "info_yellowing_time INTEGER,"
                    + "info_crack_time INTEGER,"
                    + "info_cooling_time INTEGER,"
                    + "info_events TEXT,"
                    + "roaster_name TEXT,"
                    + "roaster_address TEXT"
                    + ")";

    public static final String PROFILE_CREATE_TABLE6 =
            "CREATE TABLE preferences("
                    + "key TEXT PRIMARY KEY,"
                    + "value TEXT"
                    + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        SQLiteDatabase db = this.getWritableDatabase();

        db.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PROFILE_CREATE_TABLE1);
        db.execSQL(PROFILE_CREATE_TABLE2);
        db.execSQL(PROFILE_CREATE_TABLE3);
        db.execSQL(PROFILE_CREATE_TABLE4);
        db.execSQL(PROFILE_CREATE_TABLE4_2);
        db.execSQL(PROFILE_CREATE_TABLE5);
        db.execSQL(PROFILE_CREATE_TABLE6);
        populateCurrentLogName(db);
        populateDefaultPreferences(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        switch (oldVersion) {
            case 1:
                // we should put code here to upgrade the version 1 to the next
                if (newVersion <= 2) break;
            default:
        }

    }

    public long insertProfile(String title, String profile, String uuid) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("is_deleted", 0);

        long id = db.insert("profile", null, values);

        ContentValues values2 = new ContentValues();

        values2.put("profile_id", (int) id);
        values2.put("uuid", uuid);
        values2.put("title", title);
        values2.put("profile", profile);

        long id_version = db.insert("profile_version", null, values2);

        db.close();

        return id;
    }

    public Profile getProfile(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT *, datetime(timestamp, 'localtime') AS timestamp FROM profile_version WHERE profile_id = ? ORDER BY timestamp DESC LIMIT 1", new String[]{String.valueOf(id)});

        if (cursor != null) {

            cursor.moveToFirst();

            Profile profile = new Profile(
                    cursor.getInt(cursor.getColumnIndex("profile_id")),
                    cursor.getString(cursor.getColumnIndex("uuid")),
                    cursor.getString(cursor.getColumnIndex("title")),
                    cursor.getString(cursor.getColumnIndex("profile")),
                    cursor.getString(cursor.getColumnIndex("timestamp"))
            );

            cursor.close();

            return profile;
        }

        return null;
    }

    public Profile getProfileByUuid(String uuid) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT *, datetime(timestamp, 'localtime') AS timestamp FROM profile_version WHERE uuid = ? ORDER BY timestamp DESC LIMIT 1", new String[]{String.valueOf(uuid)});

        if (cursor != null && cursor.getCount() > 0) {

            cursor.moveToFirst();

            Profile profile = new Profile(
                    cursor.getInt(cursor.getColumnIndex("profile_id")),
                    cursor.getString(cursor.getColumnIndex("uuid")),
                    cursor.getString(cursor.getColumnIndex("title")),
                    cursor.getString(cursor.getColumnIndex("profile")),
                    cursor.getString(cursor.getColumnIndex("timestamp"))
            );

            cursor.close();

            return profile;
        }

        return null;
    }

    public List<Profile> getAllProfiles() {
        List<Profile> profiles = new ArrayList<>();

        String selectQuery = "SELECT\n" +
                "  profile.id,\n" +
                "  profile.is_deleted,\n" +
                "  profile_version3.id AS id_version,\n" +
                "  profile_version3.uuid,\n" +
                "  profile_version3.title,\n" +
                "  profile_version3.profile,\n" +
                "  datetime(profile_version3.timestamp, 'localtime') AS timestamp\n" +
                "FROM \n" +
                "  profile\n" +
                "  JOIN (SELECT profile_id, MAX(timestamp) AS max_timestamp FROM profile_version GROUP BY profile_id) AS profile_version2 ON profile.id = profile_version2.profile_id\n" +
                "  JOIN profile_version AS profile_version3 ON profile_version2.max_timestamp = profile_version3.timestamp AND profile_version2.profile_id = profile_version3.profile_id\n" +
                "WHERE\n" +
                "  profile.is_deleted = 0\n" +
                "ORDER BY\n" +
                "  timestamp DESC";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {

                Profile profile = new Profile();

                profile.setId(cursor.getInt(cursor.getColumnIndex("id")));
                profile.setIsDeleted(cursor.getInt(cursor.getColumnIndex("is_deleted")));
                profile.setVersion(cursor.getInt(cursor.getColumnIndex("id_version")));
                profile.setUuid(cursor.getString(cursor.getColumnIndex("uuid")));
                profile.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                profile.setProfile(cursor.getString(cursor.getColumnIndex("profile")));
                profile.setTimestamp(cursor.getString(cursor.getColumnIndex("timestamp")));

                profiles.add(profile);

            } while (cursor.moveToNext());
        }

        db.close();

        return profiles;
    }

    public int getProfilesCount() {
        String countQuery = "SELECT * FROM profile";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);

        int count = cursor.getCount();
        cursor.close();

        return count;
    }

    public List<Profile> getProfileAllVersions(long id) {
        List<Profile> profiles = new ArrayList<>();

        String selectQuery = "SELECT\n" +
                "  profile.id,\n" +
                "  profile.is_deleted,\n" +
                "  profile_version.id AS id_version,\n" +
                "  profile_version.uuid,\n" +
                "  profile_version.title,\n" +
                "  profile_version.profile,\n" +
                "  datetime(profile_version.timestamp, 'localtime') AS timestamp\n" +
                "FROM \n" +
                "  profile\n" +
                "  JOIN profile_version ON profile_version.profile_id = profile.id\n" +
                "WHERE\n" +
                "  profile.id = ?\n" +
                "  AND profile.is_deleted = 0\n" +
                "ORDER BY\n" +
                "  timestamp DESC";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(id)});

        if (cursor.moveToFirst()) {
            do {

                Profile profile = new Profile();
                profile.setId(cursor.getInt(cursor.getColumnIndex("id")));
                profile.setIsDeleted(cursor.getInt(cursor.getColumnIndex("is_deleted")));
                profile.setVersion(cursor.getInt(cursor.getColumnIndex("id_version")));
                profile.setUuid(cursor.getString(cursor.getColumnIndex("uuid")));
                profile.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                profile.setProfile(cursor.getString(cursor.getColumnIndex("profile")));
                profile.setTimestamp(cursor.getString(cursor.getColumnIndex("timestamp")));

                profiles.add(profile);

            } while (cursor.moveToNext());
        }

        db.close();

        return profiles;
    }

    public int updateProfile(Profile profile) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("profile_id", profile.getId());
        values.put("uuid", profile.getUuid());
        values.put("title", profile.getTitle());
        values.put("profile", profile.getProfile());

        long id = db.insert("profile_version", null, values);

        db.close();

        return (int) id;
    }

    public void deleteProfile(Profile profile) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("is_deleted", 1);

        db.update("profile", values, "id = ?",
                  new String[]{String.valueOf(profile.getId())});

        db.close();
    }

    public void undeleteProfile(long id) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("is_deleted", 0);

        db.update("profile", values, "id = ?",
                new String[]{String.valueOf(id)});

        db.close();
    }

    public void setPreference(String key, String value) {
        String selectQuery = "INSERT OR REPLACE INTO preferences VALUES (?, ?)";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(selectQuery, new String[]{key, value});
        db.close();
    }

    public String getPreferenceValue(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM preferences WHERE key = ? LIMIT 1", new String[]{key});
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            String value = cursor.getString(cursor.getColumnIndex("value"));
            cursor.close();
            return value;
        }
        return "";
    }

    public void insertCurrentLogEvent(int time, String description) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("time", time);
        values.put("description", description);

        long id = db.insert("log_current_events", null, values);

        db.close();
    }

    public void setCurrentLogEventAt(int time, String description) {
        List<LogEvent> currentEvents = getCurrentLogEventsAt(time);

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("time", time);
        values.put("description", description);

        if (currentEvents.size() > 1) {
            db.delete("log_current_events", "1", new String[]{});
            long id = db.insert("log_current_events", null, values);
        }
        else  if (currentEvents.size() > 0) {
            long id = db.update("log_current_events", values, "time = ?", new String[]{String.valueOf(time)});
        }
        else {
            long id = db.insert("log_current_events", null, values);
        }

        db.close();
    }

    public void clearCurrentLogEvents() {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete("log_current_events", "1", new String[]{});

        db.close();
    }

    public List<LogEvent> getCurrentLogEvents() {
        List<LogEvent> events = new ArrayList<LogEvent>();

        String selectQuery = "SELECT\n" +
                "  *\n" +
                "FROM \n" +
                "  log_current_events\n" +
                "ORDER BY\n" +
                "  id";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                events.add(new LogEvent(cursor.getInt(cursor.getColumnIndex("time")), cursor.getString(cursor.getColumnIndex("description"))));
            } while (cursor.moveToNext());
        }

        db.close();

        return events;
    }

    public List<LogEvent> getCurrentLogEventsAt(int time) {
        List<LogEvent> events = new ArrayList<LogEvent>();

        String selectQuery = "SELECT\n" +
                "  *\n" +
                "FROM \n" +
                "  log_current_events\n" +
                "WHERE \n" +
                "  time = ?\n" +
                "ORDER BY\n" +
                "  id";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(time)});

        if (cursor.moveToFirst()) {
            do {
                events.add(new LogEvent(cursor.getInt(cursor.getColumnIndex("time")), cursor.getString(cursor.getColumnIndex("description"))));
            } while (cursor.moveToNext());
        }

        db.close();

        return events;
    }

    public void insertCurrentLogLine(String line) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("line", line);

        long id = db.insert("log_current_lines", null, values);

        db.close();
    }

    public List<String> getCurrentLogLines() {
        List<String> lines = new ArrayList<String>();

        String selectQuery = "SELECT\n" +
                "  line\n" +
                "FROM \n" +
                "  log_current_lines\n" +
                "ORDER BY\n" +
                "  id";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                lines.add(cursor.getString(cursor.getColumnIndex("line")));
            } while (cursor.moveToNext());
        }

        db.close();

        return lines;
    }

    public void clearCurrentLogLines() {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete("log_current_lines", "1", new String[]{});

        db.close();
    }

    public CurrentLog getCurrentLog() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT title, comments, timestamp, profile_version_uuid, info_yellowing_time, info_crack_time, info_cooling_time, roaster_name, roaster_address FROM log_current LIMIT 1", new String[]{});

        if (cursor != null) {

            cursor.moveToFirst();

            CurrentLog currentLog = new CurrentLog();

            currentLog.setName(cursor.getString(cursor.getColumnIndex("title")));
            currentLog.setComments(cursor.getString(cursor.getColumnIndex("comments")));
            currentLog.setTimestamp(cursor.getString(cursor.getColumnIndex("timestamp")));
            currentLog.setUuid(cursor.getString(cursor.getColumnIndex("profile_version_uuid")));
            currentLog.setYellowingTime(cursor.getInt(cursor.getColumnIndex("info_yellowing_time")));
            currentLog.setCrackTime(cursor.getInt(cursor.getColumnIndex("info_crack_time")));
            currentLog.setCoolingTime(cursor.getInt(cursor.getColumnIndex("info_cooling_time")));
            currentLog.setRoasterName(cursor.getString(cursor.getColumnIndex("roaster_name")));
            currentLog.setRoasterAddress(cursor.getString(cursor.getColumnIndex("roaster_address")));

            cursor.close();

            return currentLog;
        }

        cursor.close();

        return null;
    }

    private void populateDefaultPreferences(SQLiteDatabase db) {

        String countQuery = "SELECT * FROM preferences WHERE key = ? LIMIT 1";
        Cursor cursor = db.rawQuery(countQuery, new String[]{"preference_roaster_number_selected"});
        int count = cursor.getCount();
        cursor.close();
        if (count == 0) {
            String selectQuery = "INSERT INTO preferences VALUES('preference_roaster_number_selected', '1')";
            db.execSQL(selectQuery, new String[]{});
        }

    }

    private void populateCurrentLogName(SQLiteDatabase db) {
        String countQuery = "SELECT * FROM log_current LIMIT 1";
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();

        if (count == 0) {
            String selectQuery = "INSERT INTO log_current VALUES('...', '', '', 0, 0, 0, strftime('%s','now'), '', '')";
            db.execSQL(selectQuery, new String[]{});
        }

    }

    public void changeCurrentLogName(String name) {
        String selectQuery = "UPDATE log_current SET title = ?";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(selectQuery, new String[]{name});

        db.close();
    }

    public void changeCurrentLogComments(String comments) {
        String selectQuery = "UPDATE log_current SET comments = ?";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(selectQuery, new String[]{comments});
        db.close();
    }

    public void changeCurrentLogTimestamp() {
        String selectQuery = "UPDATE log_current SET timestamp = CURRENT_TIMESTAMP";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(selectQuery, new String[]{});
        db.close();
    }

    public void changeCurrentLogProfileUuid(String uuid) {
        String selectQuery = "UPDATE log_current SET profile_version_uuid = ?";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(selectQuery, new String[]{uuid});
        db.close();
    }

    public void changeCurrentLogYellowingTime(int time) {
        String selectQuery = "UPDATE log_current SET info_yellowing_time = ?";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(selectQuery, new Integer[]{time});
        db.close();
    }

    public void changeCurrentLogCrackTime(int time) {
        String selectQuery = "UPDATE log_current SET info_crack_time = ?";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(selectQuery, new Integer[]{time});
        db.close();
    }

    public void changeCurrentLogCoolingTime(int coolingTime) {
        String selectQuery = "UPDATE log_current SET info_cooling_time = ?";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(selectQuery, new Integer[]{coolingTime});
        db.close();
    }

    public void changeCurrentLogRoaster(String name, String address) {
        String selectQuery = "UPDATE log_current SET roaster_name = ?, roaster_address = ?";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(selectQuery, new String[]{name, address});
        db.close();
    }

    public void clearCurrentLogAll() {
        changeCurrentLogComments("");
        changeCurrentLogRoaster("", "");
        clearCurrentLogEvents();
        clearCurrentLogLines();

        changeCurrentLogYellowingTime(0);
        changeCurrentLogCrackTime(0);
        changeCurrentLogCoolingTime(0);
    }

    public long insertLog(String uuid, String title, String timestamp, String lines, String comments, int yellowingTime, int crackTime, int coolingTime, String events, String roasterName, String roasterAddress) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("profile_version_uuid", uuid);
        values.put("title", title);
        values.put("lines", lines);
        values.put("comments", comments);
        values.put("timestamp", timestamp);
        values.put("info_yellowing_time", yellowingTime);
        values.put("info_crack_time", crackTime);
        values.put("info_cooling_time", coolingTime);
        values.put("info_events", events.isEmpty() ? "{\"events\":[]}": events);
        values.put("roaster_name", roasterName);
        values.put("roaster_address", roasterAddress);

        long id = db.insert("log", null, values);

        db.close();

        return id;
    }

    public void deleteLog(Log log) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete("log", "id = ?", new String[]{String.valueOf(log.getId())});

        db.close();
    }

    public List<Log> getLogs() {
        List<Log> logs = new ArrayList<>();

        String selectQuery = "SELECT\n" +
                "  id,\n" +
                "  profile_version_uuid,\n" +
                "  title,\n" +
                "  comments,\n" +
                "  datetime(timestamp, 'localtime') AS timestamp,\n" +
                "  roaster_name,\n" +
                "  roaster_address\n" +
                "FROM \n" +
                "  log\n" +
                "ORDER BY\n" +
                "  timestamp DESC";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {

                Log log = new Log();

                log.setId(cursor.getInt(cursor.getColumnIndex("id")));
                log.setProfileVersionUuid(cursor.getString(cursor.getColumnIndex("profile_version_uuid")));
                log.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                log.setComments(cursor.getString(cursor.getColumnIndex("comments")));
                log.setTimestamp(cursor.getString(cursor.getColumnIndex("timestamp")));
                log.setRoasterName(cursor.getString(cursor.getColumnIndex("roaster_name")));
                log.setRoasterAddress(cursor.getString(cursor.getColumnIndex("roaster_address")));

                logs.add(log);

            } while (cursor.moveToNext());
        }

        db.close();

        return logs;
    }

    public int getLogsCount() {
        String countQuery = "SELECT  * FROM log";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);

        int count = cursor.getCount();
        cursor.close();

        return count;
    }

    public void clearAllLogs() {
        String selectQuery = "DELETE FROM log";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(selectQuery, new String[]{});

        db.close();
    }

    public Log getLog(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT *, datetime(timestamp, 'localtime') AS timestamp FROM log WHERE id = ? ORDER BY timestamp DESC LIMIT 1", new String[]{String.valueOf(id)});

        if (cursor != null) {

            cursor.moveToFirst();

            Log log = new Log();
            log.setId(cursor.getInt(cursor.getColumnIndex("id")));
            log.setProfileVersionUuid(cursor.getString(cursor.getColumnIndex("profile_version_uuid")));
            log.setTitle(cursor.getString(cursor.getColumnIndex("title")));
            log.setTimestamp(cursor.getString(cursor.getColumnIndex("timestamp")));
            log.setComments(cursor.getString(cursor.getColumnIndex("comments")));
            log.setLines(cursor.getString(cursor.getColumnIndex("lines")));
            log.setYellowingTime(cursor.getInt(cursor.getColumnIndex("info_yellowing_time")));
            log.setCrackTime(cursor.getInt(cursor.getColumnIndex("info_crack_time")));
            log.setCoolingTime(cursor.getInt(cursor.getColumnIndex("info_cooling_time")));
            log.setEventsFromJson(cursor.getString(cursor.getColumnIndex("info_events")));
            log.setRoasterName(cursor.getString(cursor.getColumnIndex("roaster_name")));
            log.setRoasterAddress(cursor.getString(cursor.getColumnIndex("roaster_address")));

            cursor.close();

            return log;
        }

        cursor.close();

        return null;
    }

    public void changeLogComments(long id, String comments) {
        String selectQuery = "UPDATE log SET comments = ? WHERE id = ?";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(selectQuery, new String[]{comments, String.valueOf(id)});
        db.close();
    }

}
