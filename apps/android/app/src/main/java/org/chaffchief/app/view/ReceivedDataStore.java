package org.chaffchief.app.view;

import org.chaffchief.app.database.model.LogEvent;
import org.chaffchief.app.database.model.LogPoint;
import org.chaffchief.app.database.model.Profile;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceivedDataStore {

    private static ReceivedDataStore instance = null;

    final private ArrayList<String> receivedLogLines = new ArrayList<String>();

    final private ArrayList<LogPoint> logPoints = new ArrayList<LogPoint>();

    final private ArrayList<LogEvent> logEvents = new ArrayList<LogEvent>();

    private boolean isLogging = false;

    private Profile runningProfile = null;

    private int yellowingTime = 0;
    private int crackTime = 0;
    private int coolingTime = 0;

    private ReceivedDataStore() {
        //
    }

    public static ReceivedDataStore getInstance() {
        if (instance == null) {
            instance = new ReceivedDataStore();
        }

        return instance;
    }


    public ArrayList<String> getReceivedLogLines() {
        return receivedLogLines;
    }

    public void addReceivedLogLine(String line) {
        receivedLogLines.add(line);

        ArrayList<String> reads = new ArrayList<String>();
        Pattern pattern = Pattern.compile("([\\d|.]+)+");
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            reads.add(matcher.group());
        }
        if (reads.size() >= 6) {
            logPoints.add(
                    new LogPoint(Integer.parseInt(reads.get(0)), Double.parseDouble(reads.get(1)), Integer.parseInt(reads.get(2)), Integer.parseInt(reads.get(3)), Double.parseDouble(reads.get(4)), Double.parseDouble(reads.get(5)))
            );
        }
    }

    public void addEvent(int time, String description) {
        logEvents.add(new LogEvent(time, description));
    }

    public void setEventAt(int time, String description) {
        ArrayList<LogEvent> newEventList = new ArrayList<>();
        for (LogEvent event : logEvents) {
            if (event.getTime() != time) {
                newEventList.add(event);
            }
        }
        boolean inserted = false;
        for (int i = 0; i < newEventList.size(); i++) {
            if (newEventList.get(i).getTime() > time) {
                newEventList.add(i, new LogEvent(time, description));
                inserted = true;
                break;
            }
        }
        if (inserted == false) {
            newEventList.add(new LogEvent(time, description));
        }
        logEvents.clear();
        logEvents.addAll(newEventList);
    }

    public ArrayList<LogEvent> getEvents() {
        return logEvents;
    }

    public String getEventsAt(int time) {
        StringBuilder strEvents = new StringBuilder();
        for (LogEvent event : logEvents) {
            if (event.getTime() == time) {
                if (strEvents.length() > 0) strEvents.append("\n");
                strEvents.append(event.getDescription());
            }
        }
        return strEvents.toString();
    }

    public void clearEvents() {
        logEvents.clear();
    }

    public void clearReceivedLog() {
        receivedLogLines.clear();
        logPoints.clear();
    }

    public void clearAll() {
        clearReceivedLog();
        clearEvents();

        setYellowingTime(0);
        setCrackTime(0);
        setCoolingTime(0);

        setRunningProfile(null);
    }

    public int size() {
        return receivedLogLines.size();
    }

    public ArrayList<LogPoint> getLogPoints() {
        return logPoints;
    }

    public boolean isLogging() {
        return isLogging;
    }

    public void setLogging(boolean isLogging) {
        this.isLogging = isLogging;
    }

    public Profile getRunningProfile() {
        return runningProfile;
    }

    public void setRunningProfile(Profile runningProfile) {
        this.runningProfile = runningProfile;
    }

    public boolean hasKnownRunningProfile() {
        return (runningProfile != null);
    }

    public int getCoolingTime() {
        return coolingTime;
    }

    public void setCoolingTime(int coolingTime) {
        this.coolingTime = coolingTime;
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
}