package com.airxiechao.axcboot.storage.db.model;

public class DbProcess {

    private long Id;
    private String User;
    private String Host;
    private String db;
    private String Command;
    private int Time;
    private String State;
    private String Info;

    public long getId() {
        return Id;
    }

    public void setId(long id) {
        Id = id;
    }

    public String getUser() {
        return User;
    }

    public void setUser(String user) {
        User = user;
    }

    public String getHost() {
        return Host;
    }

    public void setHost(String host) {
        Host = host;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public String getCommand() {
        return Command;
    }

    public void setCommand(String command) {
        Command = command;
    }

    public int getTime() {
        return Time;
    }

    public void setTime(int time) {
        Time = time;
    }

    public String getState() {
        return State;
    }

    public void setState(String state) {
        State = state;
    }

    public String getInfo() {
        return Info;
    }

    public void setInfo(String info) {
        Info = info;
    }
}
