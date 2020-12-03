package org.chaffchief.app.database.model;

public class CurrentLog {

    private String name;
    private String timestamp;
    private String uuid;
    private String comments;
    private int yellowingTime;
    private int crackTime;
    private int coolingTime;
    private String roasterName;
    private String roasterAddress;

    public CurrentLog() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getComments() {
        return comments;
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
