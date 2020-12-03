package org.chaffchief.app.database.model;

public class Profile {

    private int id;
    private int is_deleted;
    private int version;
    private String uuid;
    private String title;
    private String profile;
    private String timestamp;

    public Profile() {
    }

    public Profile(int id, String uuid, String title, String profile, String timestamp) {
        this.setId(id);
        this.uuid = uuid;
        this.title = title;
        this.profile = profile;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIsDeleted() {
        return is_deleted;
    }

    public void setIsDeleted(int is_deleted) {
        this.is_deleted = is_deleted;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
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
}
