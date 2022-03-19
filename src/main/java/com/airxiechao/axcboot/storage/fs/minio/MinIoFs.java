package com.airxiechao.axcboot.storage.fs.minio;

import com.airxiechao.axcboot.process.threadpool.ThreadPool;
import com.airxiechao.axcboot.process.threadpool.ThreadPoolManager;
import com.airxiechao.axcboot.storage.fs.IFs;
import com.airxiechao.axcboot.storage.fs.common.FsFile;
import com.airxiechao.axcboot.util.UuidUtil;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

public class MinIoFs implements IFs {

    private static final Logger logger = LoggerFactory.getLogger(MinIoFs.class);

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private MinioClient minioClient;
    private ThreadPool threadPool;

    public MinIoFs(String endpoint, String accessKey, String secretKey, String bucket,
                   int threadPoolCoreSize, int threadPoolMaxSize, int threadPoolQueueSize){
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucket = bucket;
        this.threadPool = ThreadPoolManager.getInstance().getThreadPool(
                "minio-thread-pool-" + UuidUtil.random(),
                threadPoolCoreSize,
                threadPoolMaxSize,
                threadPoolQueueSize);

        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if(!bucketExists){
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            logger.error("connect minio error", e);
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean mkdirs(String path) {
        path = getNormalDirPath(path);

        if(exist(path)){
            return false;
        }

        try{
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(path + ".empty")
                    .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                    .build());
            return true;
        }catch (Exception e){
            logger.error("minio mkdirs error", e);
            return false;
        }
    }

    @Override
    public FsFile get(String path) throws FileNotFoundException {
        path = getNormalFilePath(path);

        try{
            Iterable<Result<Item>> res = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(path)
                    .build());
            for(Result<Item> item : res){
                Item it = item.get();
                String name = it.objectName();
                if(!name.startsWith("/")){
                    name = "/" + name;
                }

                if(name.equals(path) || name.equals(path + "/")){
                    boolean isDir = it.isDir();
                    long size = it.size();
                    Long lastModified = null;
                    try{
                        lastModified = Date.from(it.lastModified().toInstant()).getTime();

                    }catch (Exception e){

                    }

                    String shortName = getShortName(name);
                    String normalName = getNormalFilePath(name);
                    return new FsFile(normalName, shortName, isDir, size, lastModified);
                }
            }

            throw new FileNotFoundException("file not found: " + path);
        }catch (Exception e){
            logger.error("minio get file error", e);
            throw new FileNotFoundException("get file error: " + path);
        }
    }

    @Override
    public List<FsFile> list(String path) {
        return listObjects(path).stream().map( item -> {
            String name = item.objectName();
            Long lastModified = null;
            try{
                lastModified = Date.from(item.lastModified().toInstant()).getTime();
            }catch (Exception e){

            }

            String shortName = getShortName(name);
            String normalName = getNormalFilePath(name);
            return new FsFile(normalName, shortName, item.isDir(), item.size(), lastModified);
        }).collect(Collectors.toList());
    }

    @Override
    public boolean exist(String path) {
        path = getNormalFilePath(path);

        if(path.equals("/")){
            return true;
        }

        try{
            Iterable<Result<Item>> res = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(path)
                    .build());
            for(Result<Item> item : res){
                String objectName = item.get().objectName();
                if(!objectName.startsWith("/")){
                    objectName = "/" + objectName;
                }

                if(objectName.equals(path) || objectName.equals(path + "/")){
                    return true;
                }
            }

            return false;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public boolean remove(String path) {
        path = getNormalFilePath(path);

        if(!isDirectory(path)){
            try{
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(path)
                        .build());
                return true;
            }catch (Exception e){
                logger.error("minio remove [{}] error", path, e);
                return false;
            }
        }else{
            List<DeleteObject> deleteObjects = listObjectsRecursively(path).stream().map(item -> new DeleteObject(item.objectName())).collect(Collectors.toList());
            try{
                Iterable<Result<DeleteError>> res = minioClient.removeObjects(RemoveObjectsArgs.builder()
                        .bucket(bucket)
                        .objects(deleteObjects)
                        .build());
                for(Result<DeleteError> r : res){
                    DeleteError error = r.get();
                }
                return true;
            }catch (Exception e){
                logger.error("minio remove [{}] error", path, e);
                return false;
            }
        }

    }

    @Override
    public boolean move(String srcPath, String destPath) {
        boolean copied = copy(srcPath, destPath);
        if(!copied){
            return false;
        }

        boolean removed = remove(srcPath);
        if(!removed){
            return false;
        }

        return true;
    }

    @Override
    public boolean copy(String srcPath, String destPath) {
        srcPath = getNormalFilePath(srcPath);
        destPath = getNormalFilePath(destPath);

        if(!isDirectory(srcPath)){
            try{
                minioClient.copyObject(CopyObjectArgs.builder()
                        .bucket(bucket)
                        .object(destPath)
                        .source(CopySource.builder()
                                .bucket(bucket)
                                .object(srcPath)
                                .build())
                        .build());
                return true;
            }catch (Exception e){
                logger.error("minio copy from [{}] to [{}] error", srcPath, destPath, e);
                return false;
            }
        }else{
            for(Item item : listObjectsRecursively(srcPath)){
                String srcName = item.objectName();
                if(!srcName.startsWith("/")){
                    srcName = "/" + srcName;
                }

                String destName = destPath + srcName.substring(srcPath.length());
                try{
                    minioClient.copyObject(CopyObjectArgs.builder()
                        .bucket(bucket)
                        .object(destName)
                        .source(CopySource.builder()
                                .bucket(bucket)
                                .object(srcName)
                                .build())
                        .build());
                }catch (Exception e){
                    logger.error("minio copy from [{}] to [{}] error", srcName, destName, e);
                    return false;
                }
            }

            return true;
        }
    }

    @Override
    public long length(String path) {
        path = getNormalFilePath(path);

        if(isDirectory(path)){
            return 0;
        }

        try{
            Iterable<Result<Item>> res = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(path)
                    .build());
            for(Result<Item> item : res){
                String objectName = item.get().objectName();
                if(!objectName.startsWith("/")){
                    objectName = "/" + objectName;
                }

                long size = item.get().size();
                if(objectName.equals(path) || objectName.equals(path + "/")){
                    return size;
                }
            }

            return 0;
        }catch (Exception e){
            return 0;
        }
    }

    @Override
    public boolean isDirectory(String path) {
        path = getNormalFilePath(path);

        try{
            Iterable<Result<Item>> res = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(path)
                    .build());
            for(Result<Item> item : res){
                String itemName = item.get().objectName();
                if(!itemName.startsWith("/")){
                    itemName = "/" + itemName;
                }

                boolean isDir = item.get().isDir();
                if(itemName.equals(path) || itemName.equals(path + "/")){
                    return isDir;
                }
            }
            return false;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public InputStream getInputStream(String path) throws FileNotFoundException {
        path = getNormalFilePath(path);

        if(isDirectory(path)){
            throw new FileNotFoundException(String.format("minio object [%s] not found", path));
        }

        try{
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .build());
        }catch (Exception e){
            logger.error("minio get input stream [{}] error", path, e);
            throw new FileNotFoundException(String.format("minio object [%s] not found", path));
        }
    }

    @Override
    public OutputStream getOutputStream(String path) throws FileNotFoundException {
        path = getNormalFilePath(path);

        if(isDirectory(path)){
            throw new FileNotFoundException(String.format("minio object [%s] not found", path));
        }

        try{
            PipedOutputStream outputStream = new PipedOutputStream();
            PipedInputStream inputStream = new PipedInputStream(outputStream);

            String path2 = path;
            ThreadPoolExecutor executor = threadPool.getExecutor();
            logger.info("minio put object [pool:{}, active:{}, core:{}, max:{}, queue:{}]",
                    executor.getPoolSize(), executor.getActiveCount(), executor.getCorePoolSize(), executor.getMaximumPoolSize(), executor.getQueue().size());

            CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
                try{
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(path2)
                            .stream(inputStream, -1, ObjectWriteArgs.MIN_MULTIPART_SIZE)
                            .build());
                }catch (Exception e){
                    logger.error("minio put object [{}] error", path2, e);
                    try {
                        inputStream.close();
                    } catch (IOException ioException) {

                    }
                }
            }, executor);

            return new MinIoFutureOutputStream(outputStream, future);
        }catch (Exception e){
            logger.error("minio get output stream [{}] error", path, e);
            throw new FileNotFoundException(String.format("minio object [%s] not found", path));
        }
    }

    private String getNormalFilePath(String path){
        if(!path.startsWith("/")){
            path = "/" + path;
        }
        if(path.endsWith("/") && path.length() > 1){
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private String getNormalDirPath(String path){
        if(!path.startsWith("/")){
            path = "/" + path;
        }
        if(!path.endsWith("/")){
            path = path + "/";
        }
        return path;
    }

    private String getShortName(String name){
        name = getNormalFilePath(name);
        if(name.equals("/")){
            return name;
        }

        String shortName = name.substring(name.lastIndexOf("/") + 1);

        return shortName;
    }

    private List<Item> listObjectsRecursively(String path){
        List<Item> items = new ArrayList<>();
        listObjects(path).forEach(item -> {
            if(!item.isDir()){
                items.add(item);
            }else{
                List<Item> subItems = listObjectsRecursively(item.objectName());
                items.addAll(subItems);
            }
        });
        return items;
    }

    private List<Item> listObjects(String path){
        if(isDirectory(path)){
            path = getNormalDirPath(path);
        }else {
            path = getNormalFilePath(path);
        }

        List<Item> items = new ArrayList<>();
        minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucket)
                .prefix(path)
                .build())
                .forEach( res -> {
                    try{
                        Item item = res.get();
                        items.add(item);
                    }catch (Exception e){
                        logger.error("get minio object error", e);
                    }
                });
        return items;
    }
}
