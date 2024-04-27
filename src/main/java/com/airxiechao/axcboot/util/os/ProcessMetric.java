package com.airxiechao.axcboot.util.os;

public class ProcessMetric {
    private long pid;
    private Double cpuUsage;
    private Long memoryBytes;
    private Long timestamp;

    public ProcessMetric(long pid, Double cpuUsage, Long memoryBytes, Long timestamp) {
        this.pid = pid;
        this.cpuUsage = cpuUsage;
        this.memoryBytes = memoryBytes;
        this.timestamp = timestamp;
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Long getMemoryBytes() {
        return memoryBytes;
    }

    public void setMemoryBytes(Long memoryBytes) {
        this.memoryBytes = memoryBytes;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
