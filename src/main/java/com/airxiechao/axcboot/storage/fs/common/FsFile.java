package com.airxiechao.axcboot.storage.fs.common;

public class FsFile {
    private String path;
    private String name;
    private boolean isDir;
    private long size;
    private Long lastModified;

    public FsFile() {
    }

    public FsFile(String path, String name, boolean isDir, long size, Long lastModified) {
        this.path = path;
        this.name = name;
        this.isDir = isDir;
        this.size = size;
        this.lastModified = lastModified;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDir() {
        return isDir;
    }

    public void setDir(boolean dir) {
        isDir = dir;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }
}
