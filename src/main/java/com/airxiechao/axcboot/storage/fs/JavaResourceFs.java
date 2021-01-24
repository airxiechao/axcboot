package com.airxiechao.axcboot.storage.fs;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class JavaResourceFs implements IFs {

    @Override
    public boolean exist(String path) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(path);
        return null != url;
    }

    @Override
    public InputStream getFileAsStream(String path) throws FileNotFoundException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(path);

        if (inputStream == null) {
            throw new FileNotFoundException("file not found: " + path);
        } else {
            return inputStream;
        }
    }
}
