package com.airxiechao.axcboot.util.os;

import java.util.List;

public class HostMetric {
    private CpuMetric cpu;
    private MemoryMetric memory;
    private List<FileSystemMetric> filesystem;
    private Long timestamp;

    public HostMetric(CpuMetric cpu, MemoryMetric memory, List<FileSystemMetric> filesystem, Long timestamp) {
        this.cpu = cpu;
        this.memory = memory;
        this.filesystem = filesystem;
        this.timestamp = timestamp;
    }

    public CpuMetric getCpu() {
        return cpu;
    }

    public void setCpu(CpuMetric cpu) {
        this.cpu = cpu;
    }

    public MemoryMetric getMemory() {
        return memory;
    }

    public void setMemory(MemoryMetric memory) {
        this.memory = memory;
    }

    public List<FileSystemMetric> getFilesystem() {
        return filesystem;
    }

    public void setFilesystem(List<FileSystemMetric> filesystem) {
        this.filesystem = filesystem;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
