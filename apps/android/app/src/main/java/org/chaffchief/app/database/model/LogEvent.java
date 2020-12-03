package org.chaffchief.app.database.model;

public class LogEvent {

    private int time;
    private String description;

    public LogEvent() {
    }

    public LogEvent(int time, String description) {
        this.time = time;
        this.description = description;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) { this.time = time; }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
