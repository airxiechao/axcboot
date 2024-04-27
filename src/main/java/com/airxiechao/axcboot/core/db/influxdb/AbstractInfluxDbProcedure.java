package com.airxiechao.axcboot.core.db.influxdb;

import com.airxiechao.axcboot.storage.db.influxdb.InfluxDbManager;

public class AbstractInfluxDbProcedure {
    protected InfluxDbManager dbManager;

    public AbstractInfluxDbProcedure(InfluxDbManager dbManager){
        this.dbManager = dbManager;
    }

}
