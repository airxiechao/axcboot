package com.airxiechao.axcboot.storage.fs;

import com.airxiechao.axcboot.storage.fs.common.FsFile;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface IFs {
    boolean mkdirs(String path);
    FsFile get(String path) throws FileNotFoundException;
    List<FsFile> list(String path);
    boolean exist(String path);
    boolean remove(String path);
    boolean move(String srcPath, String destPath);
    boolean copy(String srcPath, String destPath);
    long length(String path);
    boolean isDirectory(String path);
    InputStream getInputStream(String path) throws FileNotFoundException;
    OutputStream getOutputStream(String path) throws FileNotFoundException;
}
