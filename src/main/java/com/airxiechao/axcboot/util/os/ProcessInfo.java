package com.airxiechao.axcboot.util.os;

public class ProcessInfo {
    private long pid;
    private String command;

    public ProcessInfo(long pid, String command) {
        this.pid = pid;
        this.command = command;
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
