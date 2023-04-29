package com.airxiechao.axcboot.communication.rest.security;

import java.util.Date;

public class GuardPrincipal {

    private String path;
    private Date expireTime;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }
}
