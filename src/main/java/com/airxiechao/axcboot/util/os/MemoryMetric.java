package com.airxiechao.axcboot.util.os;

public class MemoryMetric {
    private long availableBytes;
    private long totalBytes;

    public MemoryMetric(long availableBytes, long totalBytes) {
        this.availableBytes = availableBytes;
        this.totalBytes = totalBytes;
    }

    public long getAvailableBytes() {
        return availableBytes;
    }

    public void setAvailableBytes(long availableBytes) {
        this.availableBytes = availableBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }
}
