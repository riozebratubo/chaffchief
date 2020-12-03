package org.chaffchief.app.database.model;

import android.content.Context;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.chaffchief.app.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Log {

    private int id;
    private String uuid;
    private String title;
    private String timestamp;
    private ArrayList<String> lines;
    private String comments;
    private int yellowingTime;
    private int crackTime;
    private int coolingTime;
    private final ArrayList<LogEvent> events = new ArrayList<>();
    private String roasterName;
    private String roasterAddress;

    public Log() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public ArrayList<String> getLines() {
        return lines;
    }

    public ArrayList<String> getLinesWithEvents(Context context) {

        Map<Integer, String> eventsMap = new HashMap<Integer, String>();
        for (LogEvent event: this.getEvents()) {
            if (eventsMap.containsKey(event.getTime())) {
                eventsMap.put(event.getTime(), eventsMap.get(event.getTime()) + event.getDescription());
            }
            else {
                eventsMap.put(event.getTime(), event.getDescription());
            }
        }

        ArrayList<String> localLines = lines;

        for (int i = 0; i < localLines.size(); i++) {

            ArrayList<String> reads = new ArrayList<String>();
            Pattern pattern = Pattern.compile("([\\d|.]+)+");
            Matcher matcher = pattern.matcher(localLines.get(i));
            while (matcher.find()) {
                reads.add(matcher.group());
            }
            if (reads.size() >= 6) {

                int time = Integer.parseInt(reads.get(0));

                ArrayList<String> events = new ArrayList<>();

                if (i == 0) {
                    events.add(context.getResources().getString(R.string.label_roast_event_started));
                }

                if (time == coolingTime) {
                    events.add(context.getResources().getString(R.string.label_roast_event_cooling));
                }

                if (i == localLines.size() - 1) {
                    events.add(context.getResources().getString(R.string.label_roast_event_stopped));
                }

                if (eventsMap.containsKey(time)) {
                    events.add(eventsMap.get(time));
                }

                if (events.size() > 0) {
                    localLines.set(i, localLines.get(i) + ",\"" + TextUtils.join(", ", events) + "\"");
                }
            }
        }

        return localLines;
    }

    public void setLines(ArrayList<String> lines) {
        this.lines = lines;
    }

    public void setLines(String s) {
        this.lines = new ArrayList<String>(Arrays.asList(s.split("\n")));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProfileVersionUuid() {
        return uuid;
    }

    public void setProfileVersionUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getComments() {
        return comments == null ? "" : comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public int getYellowingTime() {
        return yellowingTime;
    }

    public void setYellowingTime(int yellowingTime) {
        this.yellowingTime = yellowingTime;
    }

    public int getCrackTime() {
        return crackTime;
    }

    public void setCrackTime(int crackTime) {
        this.crackTime = crackTime;
    }

    public int getCoolingTime() {
        return coolingTime;
    }

    public void setCoolingTime(int coolingTime) {
        this.coolingTime = coolingTime;
    }

    public ArrayList<LogEvent> getEvents() {
        return events;
    }

    public void addEvent(int time, String description) {
        this.events.add(new LogEvent(time, description));
    }

    public void setEventsFromJson(String strEvents) {
        try {
            JSONObject document = new JSONObject(strEvents);
            JSONArray events = document.getJSONArray("events");
            this.events.clear();
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                int time = event.getInt("time");
                String description = event.getString("description");
                this.events.add(new LogEvent(time, description));
            }
        }
        catch (Exception e) {

        }
    }

    public String getEventsJson() {
        JSONObject document = new JSONObject();
        try {
            JSONArray eventList = new JSONArray();
            for (LogEvent logEvent : this.events) {
                JSONObject event = new JSONObject();
                event.put("time", logEvent.getTime());
                event.put("description", logEvent.getDescription());
                eventList.put(event);
            }
            document.put("events", eventList);
        }
        catch (Exception e) {

        }
        return document.toString();
    }

    public String getRoasterName() {
        return roasterName;
    }

    public void setRoasterName(String roasterName) {
        this.roasterName = roasterName;
    }

    public String getRoasterAddress() {
        return roasterAddress;
    }

    public void setRoasterAddress(String roasterAddress) {
        this.roasterAddress = roasterAddress;
    }

}
