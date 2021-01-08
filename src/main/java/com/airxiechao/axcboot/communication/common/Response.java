package com.airxiechao.axcboot.communication.common;

import com.alibaba.fastjson.annotation.JSONField;

public class Response {

    public static final String CODE_OK = "0";
    public static final String CODE_ERROR = "-1";
    public static final String CODE_AUTH_ERROR = "-2";

    private String code;
    private String message;
    private Object data;

    public Response(){
        this.code = CODE_OK;
    }

    public Response success(){
        code = CODE_OK;
        return this;
    }

    public Response success(String message){
        this.code = CODE_OK;
        this.message = message;
        return this;
    }

    public Response error(){
        code = CODE_ERROR;
        return this;
    }

    public Response error(String message){
        this.code = CODE_ERROR;
        this.message = message;
        return this;
    }

    public Response authError(){
        code = CODE_AUTH_ERROR;
        return this;
    }

    public Response authError(String message){
        this.code = CODE_AUTH_ERROR;
        this.message = message;
        return this;
    }

    public Response data(Object data){
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

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @JSONField(serialize = false)
    public boolean isSuccess(){
        return CODE_OK.equals(this.code);
    }
}
