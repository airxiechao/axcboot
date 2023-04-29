package com.airxiechao.axcboot.communication.common;

public class PageParam{

    private Integer pageNo;
    private Integer pageSize;
    private String orderField;
    private String orderType;

    public PageParam(){

    }

    public PageParam(Integer pageNo, Integer pageSize, String orderField, String orderType){
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.orderField = orderField;
        this.orderType = orderType;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getOrderField() {
        return orderField;
    }

    public void setOrderField(String orderField) {
        this.orderField = orderField;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }
}