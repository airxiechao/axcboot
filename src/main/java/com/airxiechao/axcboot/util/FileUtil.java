package com.airxiechao.axcboot.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class FileUtil {

    public static void mkParentDirs(String fileName){
        File file = new File(fileName).getParentFile();
        if(null != file && !file.exists()){
            file.mkdirs();
        }
    }

    public static void mkDirs(String dirName){
        File file = new File(dirName);
        if(!file.exists()){
            file.mkdirs();
        }
    }

    public static boolean rmDir(File dirFile){
        if(!dirFile.isDirectory()){
            return dirFile.delete();
        }else{
            try {
                FileUtils.deleteDirectory(dirFile);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    public static boolean rmDir(String dirName){
        File dirFile =  new File(dirName);
        return rmDir(dirFile);
    }

    public static boolean copy(String srcPath, String destPath){
        File srcFile = new File(srcPath);
        File destFile = new File(destPath);
        return copy(srcFile, destFile);
    }

    public static boolean copy(File srcFile, File destFile) {
        if(!srcFile.isDirectory()){
            try {
                FileUtils.copyFile(srcFile, destFile);
                return true;
            } catch (IOException e) {
                return false;
            }
        }else{
            try {
                FileUtils.copyDirectory(srcFile, destFile);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    public static long sizeOfDirectory(File dir){
        if(dir.isDirectory()) {
            return FileUtils.sizeOfDirectory(dir);
        }else{
            return 0;
        }
    }
}
