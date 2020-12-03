package org.chaffchief.app.database.model;

public class LogPoint {

    private int time;
    private double temperature;
    private int power;
    private int fan;
    private double targetTemperature;
    private double targetFan;

    public LogPoint() {
    }

    public LogPoint(int time, double temperature, int power, int fan, double targetTemperature, double targetFan) {
        this.time = time;
        this.temperature = temperature;
        this.power = power;
        this.fan = fan;
        this.targetTemperature = targetTemperature;
        this.targetFan = targetFan;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) { this.time = time; }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getFan() {
        return fan;
    }

    public void setFan(int fan) {
        this.fan = fan;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public double getTargetTemperature() {
        return targetTemperature;
    }

    public void setTargetTemperature(double targetTemperature) {
        this.targetTemperature = targetTemperature;
    }

    public double getTargetFan() {
        return targetFan;
    }

    public void setTargetFan(double targetFan) {
        this.targetFan = targetFan;
    }
}
