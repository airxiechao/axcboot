package com.airxiechao.axcboot.core.db.mongo;

import com.airxiechao.axcboot.storage.db.mongodb.MongoDbManager;

public class AbstractMongoDbProcedure {

    protected MongoDbManager dbManager;

    public AbstractMongoDbProcedure(MongoDbManager dbManager){
        this.dbManager = dbManager;
    }

}
