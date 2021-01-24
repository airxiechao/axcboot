package com.airxiechao.axcboot.storage.fs;

import java.io.FileNotFoundException;
import java.io.InputStream;

public interface IFs {
    boolean exist(String path);
    InputStream getFileAsStream(String path) throws FileNotFoundException;
}
