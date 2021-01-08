package com.airxiechao.axcboot.util;

import java.io.File;

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
}
