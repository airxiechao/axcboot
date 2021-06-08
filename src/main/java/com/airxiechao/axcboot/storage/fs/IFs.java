package com.airxiechao.axcboot.storage.fs;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface IFs {
    boolean mkdirs(String path);
    String[] list(String path);
    boolean exist(String path);
    boolean remove(String path);
    long length(String path);
    boolean isDirectory(String path);
    InputStream getInputStream(String path) throws FileNotFoundException;
    OutputStream getOutputStream(String path) throws FileNotFoundException;
}
