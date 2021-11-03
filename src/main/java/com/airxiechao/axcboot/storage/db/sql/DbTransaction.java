package com.airxiechao.axcboot.storage.db.sql;


public interface DbTransaction {
    void execute(DbMapper mapper) throws Exception;
}
