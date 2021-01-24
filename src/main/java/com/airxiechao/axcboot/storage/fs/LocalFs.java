package com.airxiechao.axcboot.storage.fs;

import com.airxiechao.axcboot.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class LocalFs implements IFs {

    private String dir;

    public LocalFs(String dir){
        FileUtil.mkDirs(dir);

        this.dir = dir;
    }

    @Override
    public boolean exist(String path) {
        File file = new File(this.dir, path);
        return file.exists();
    }

    @Override
    public InputStream getFileAsStream(String path) throws FileNotFoundException {
        File file = new File(this.dir, path);
        return new FileInputStream(file);
    }
}
