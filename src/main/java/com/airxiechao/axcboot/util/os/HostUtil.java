package com.airxiechao.axcboot.util.os;

import com.sun.management.OperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;

public class HostUtil {
    private static final Logger logger = LoggerFactory.getLogger(HostUtil.class);
    private static OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();

    public static HostMetric getHostMetric() throws Exception{
        CpuMetric cpu = null;
        try{
            cpu = getCpuMetric();
        }catch (Throwable e){
            logger.error("读取CPU信息发生错误", e);
        }

        MemoryMetric memory = null;
        try{
            memory = getMemoryMetric();
        }catch (Throwable e){
            logger.error("读取内存信息发生错误", e);
        }

        List<FileSystemMetric> fileSystem = null;
        try{
            fileSystem = getFileSystemMetric();
        }catch (Throwable e){
            logger.error("读取文件系统信息发生错误", e);
        }

        HostMetric metric = new HostMetric(cpu, memory, fileSystem, System.currentTimeMillis());
        return metric;
    }

    private static CpuMetric getCpuMetric(){
        double load = operatingSystemMXBean.getSystemCpuLoad();
        return new CpuMetric(load);
    }

    private static MemoryMetric getMemoryMetric(){
        long total = operatingSystemMXBean.getTotalPhysicalMemorySize();
        long available = operatingSystemMXBean.getFreePhysicalMemorySize();
        return new MemoryMetric(available, total);
    }

    private static List<FileSystemMetric> getFileSystemMetric() {
        List<FileSystemMetric> list = new ArrayList<>();

        FileSystem fileSystem = FileSystems.getDefault();
        for(FileStore fs : fileSystem.getFileStores()){
            try{
                String mount = fs.toString();
                long usable = fs.getUsableSpace();
                long total = fs.getTotalSpace();

                FileSystemMetric metric = new FileSystemMetric(mount, usable, total);
                list.add(metric);
            }catch (Exception e){
                logger.error("读取文件系统信息发生错误", e);
            }
        }

        // 排序
        list.sort((a, b) -> (a.getTotalBytes() == b.getTotalBytes()) ? 0 : (a.getTotalBytes() < b.getTotalBytes() ? 1 : -1));

        return list;
    }
}
