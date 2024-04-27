package com.airxiechao.axcboot.util.os;

public class FileSystemMetric {
    private String mount;
    private long usableBytes;
    private long totalBytes;

    public FileSystemMetric(String mount, long usableBytes, long totalBytes) {
        this.mount = mount;
        this.usableBytes = usableBytes;
        this.totalBytes = totalBytes;
    }

    public String getMount() {
        return mount;
    }

    public void setMount(String mount) {
        this.mount = mount;
    }

    public long getUsableBytes() {
        return usableBytes;
    }

    public void setUsableBytes(long usableBytes) {
        this.usableBytes = usableBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }
}
