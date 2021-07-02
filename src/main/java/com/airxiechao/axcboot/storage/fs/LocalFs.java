package com.airxiechao.axcboot.storage.fs;

import com.airxiechao.axcboot.util.FileUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class LocalFs implements IFs {

    private String dir;

    public LocalFs(String dir){
        this.dir = dir;
    }

    public LocalFs(String dir, boolean mkdir){
        if(mkdir){
            FileUtil.mkDirs(dir);
        }

        this.dir = dir;
    }

    @Override
    public boolean mkdirs(String path) {
        File file = getFile(path);
        return file.mkdirs();
    }

    @Override
    public String[] list(String path) {
        File file = getFile(path);
        return file.list();
    }

    @Override
    public boolean exist(String path) {
        File file = getFile(path);
        return file.exists();
    }

    @Override
    public boolean remove(String path) {
        File file = getFile(path);
        if(!file.isDirectory()){
            return file.delete();
        }else{
            try {
                Files.walk(file.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
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
        File file = getFile(path);
        return new FileInputStream(file);
    }

    @Override
    public OutputStream getOutputStream(String path) throws FileNotFoundException {
        File file = getFile(path);
        return new FileOutputStream(file);
    }

    private File getFile(String path){
        File file = new File(this.dir, path);
        return file;
    }
}
