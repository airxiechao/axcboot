package com.airxiechao.axcboot.communication.common;

import io.undertow.server.handlers.form.FormData;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileData {
    private String name;
    private FormData.FileItem fileItem;

    public FileData() {
    }

    public FileData(File file){
        this.name = file.getName();
        FormData.FileItem fileItem = new FormData.FileItem(file.toPath());
        this.fileItem =fileItem;
    }

    public FileData(String path){
        Path filePath = Paths.get(path);
        this.name = filePath.getFileName().toString();
        FormData.FileItem fileItem = new FormData.FileItem(filePath);
        this.fileItem =fileItem;
    }

    public FileData(Path path){
        this.name = path.getFileName().toString();
        FormData.FileItem fileItem = new FormData.FileItem(path);
        this.fileItem =fileItem;
    }

    public FileData(String name, FormData.FileItem fileItem) {
        this.name = name;
        this.fileItem = fileItem;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FormData.FileItem getFileItem() {
        return fileItem;
    }

    public void setFileItem(FormData.FileItem fileItem) {
        this.fileItem = fileItem;
    }
}
