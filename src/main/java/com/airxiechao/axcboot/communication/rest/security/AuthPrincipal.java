package com.airxiechao.axcboot.communication.rest.security;

import java.util.Date;

public class AuthPrincipal {

    private String userName;
    private Date expireTime;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }
}
