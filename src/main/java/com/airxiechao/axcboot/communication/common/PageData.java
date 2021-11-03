package com.airxiechao.axcboot.communication.common;

public class PageData<T> {

    private Integer pageNo;
    private Integer pageSize;
    private Long total;
    private T page;

    public PageData(Integer pageNo, Integer pageSize, Long total, T page){
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.total = total;
        this.page = page;
    }

    public PageData(PageParam pageParam, Long total, T page){
        this.pageNo = pageParam.getPageNo();
        this.pageSize = pageParam.getPageSize();
        this.total = total;
        this.page = page;
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

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public T getPage() {
        return page;
    }

    public void setPage(T page) {
        this.page = page;
    }
}
