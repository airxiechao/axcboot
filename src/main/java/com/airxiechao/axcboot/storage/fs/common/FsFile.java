package com.airxiechao.axcboot.storage.fs.common;

public class FsFile {
    private String path;
    private String name;
    private boolean isDir;
    private long size;

    public FsFile() {
    }

    public FsFile(String path, String name, boolean isDir, long size) {
        this.path = path;
        this.name = name;
        this.isDir = isDir;
        this.size = size;
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
}
