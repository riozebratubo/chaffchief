package org.chaffchief.app.database.model;

public class Preference {

    private String key;
    private String value;

    public Preference() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
