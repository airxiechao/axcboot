package com.airxiechao.axcboot.storage.fs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class JavaResourceFs implements IFs {

    @Override
    public boolean mkdirs(String path) {
        return false;
    }

    @Override
    public String[] list(String path) {
        File file = getFile(path);
        return file.list();
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
