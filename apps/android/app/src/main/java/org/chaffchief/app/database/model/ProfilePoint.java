package org.chaffchief.app.database.model;

public class ProfilePoint {

    private int id;

    private int time;
    private int temperature;
    private int fan;

    public ProfilePoint() {
    }

    public ProfilePoint(int id, int time, int temperature, int fan) {
        this.id = id;
        this.time = time;
        this.temperature = temperature;
        this.fan = fan;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) { this.time = time; }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getFan() {
        return fan;
    }

    public void setFan(int fan) {
        this.fan = fan;
    }
}
