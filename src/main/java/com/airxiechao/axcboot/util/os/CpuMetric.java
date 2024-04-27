package com.airxiechao.axcboot.util.os;

public class CpuMetric {
    private double load;

    public CpuMetric(double load) {
        this.load = load;
    }

    public double getLoad() {
        return load;
    }

    public void setLoad(double load) {
        this.load = load;
    }
}
