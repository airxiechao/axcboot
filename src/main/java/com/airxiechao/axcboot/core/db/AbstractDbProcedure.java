package com.airxiechao.axcboot.core.db;

import com.airxiechao.axcboot.storage.db.sql.DbManager;

public abstract class AbstractDbProcedure {

    protected DbManager dbManager;

    public AbstractDbProcedure(DbManager dbManager){
        this.dbManager = dbManager;
    }

}
