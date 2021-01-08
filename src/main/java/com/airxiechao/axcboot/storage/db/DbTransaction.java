package com.airxiechao.axcboot.storage.db;


public interface DbTransaction {
    void execute(DbMapper mapper) throws Exception;
}
