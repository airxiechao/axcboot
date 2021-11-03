package com.airxiechao.axcboot.storage.fs;

import com.airxiechao.axcboot.storage.fs.common.FsFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class JavaResourceFs implements IFs {

    private static final Logger logger = LoggerFactory.getLogger(JavaResourceFs.class);

    @Override
    public boolean mkdirs(String path) {
        return false;
    }

    @Override
    public List<FsFile> list(String path) {
        File dir = getFile(".");
        File file = getFile(path);
        List<FsFile> list = new ArrayList<>();
        for (File f : file.listFiles()) {
            Path relativePath = dir.toPath().relativize(f.toPath());
            list.add(new FsFile(relativePath.toString(), f.getName(), f.isDirectory(), f.length()));
        }
        return list;
    }

    @Override
    public boolean exist(String path) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(path);
        return null != url;
    }

    @Override
    public boolean remove(String path) {
        return false;
    }

    @Override
    public boolean move(String srcPath, String destPath) {
        return false;
    }

    @Override
    public boolean copy(String srcPath, String destPath) {
        return false;
    }

    @Override
    public long length(String path) {
        File file = getFile(path);
        return file.length();
    }

    @Override
    public boolean isDirectory(String path) {
        File file = getFile(path);
        return file.isDirectory();
    }

    @Override
    public InputStream getInputStream(String path) throws FileNotFoundException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(path);

        if (inputStream == null) {
            throw new FileNotFoundException("file not found: " + path);
        } else {
            return inputStream;
        }
    }

    @Override
    public OutputStream getOutputStream(String path) throws FileNotFoundException {
        throw new FileNotFoundException("file not found: " + path);
    }

    private File getFile(String path){
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(path);
        File file = new File(url.getPath());
        return file;
    }
}
