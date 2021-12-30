package com.airxiechao.axcboot.communication.common;

import com.alibaba.fastjson.annotation.JSONField;

public class Response<T> {

    public static final String CODE_OK = "0";
    public static final String CODE_ERROR = "-1";
    public static final String CODE_AUTH_ERROR = "-2";

    private String code;
    private String message;
    private T data;

    public Response(){
        this.code = CODE_OK;
    }

    public Response<T> success(){
        code = CODE_OK;
        return this;
    }

    public Response<T> success(String message){
        this.code = CODE_OK;
        this.message = message;
        return this;
    }

    public Response<T> error(){
        code = CODE_ERROR;
        return this;
    }

    public Response<T> error(String message){
        this.code = CODE_ERROR;
        this.message = message;
        return this;
    }

    public Response<T> authError(){
        code = CODE_AUTH_ERROR;
        return this;
    }

    public Response<T> authError(String message){
        this.code = CODE_AUTH_ERROR;
        this.message = message;
        return this;
    }

    public Response<T> data(T data){
        this.data = data;
        return this;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @JSONField(serialize = false)
    public boolean isSuccess(){
        return CODE_OK.equals(this.code);
    }
}
