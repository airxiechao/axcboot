package com.airxiechao.axcboot.storage.fs;

import com.airxiechao.axcboot.storage.fs.common.FsFile;
import com.airxiechao.axcboot.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class LocalFs implements IFs {

    private static final Logger logger = LoggerFactory.getLogger(LocalFs.class);

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
        File file = getFile(path);
        return file.exists();
    }

    @Override
    public boolean remove(String path) {
        File file = getFile(path);
        return FileUtil.rmDir(file);
    }

    @Override
    public boolean move(String srcPath, String destPath) {
        File srcFile = getFile(srcPath);
        File destFile = getFile(destPath);

        return srcFile.renameTo(destFile);
    }

    @Override
    public boolean copy(String srcPath, String destPath) {
        File srcFile = getFile(srcPath);
        File destFile = getFile(destPath);
        return FileUtil.copy(srcFile, destFile);
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
