package com.airxiechao.axcboot.util.os;

public class ServiceInfo {
    private boolean running;
    private String name;

    public ServiceInfo(boolean running, String name) {
        this.running = running;
        this.name = name;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
