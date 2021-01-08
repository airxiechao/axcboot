package com.airxiechao.axcboot.storage.db.model;

import java.util.Map;

public class SqlParams {

    private String sql;
    private Map params;

    public SqlParams(String sql, Map params){
        this.sql = sql;
        this.params = params;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public Map getParams() {
        return params;
    }

    public void setParams(Map params) {
        this.params = params;
    }
}
