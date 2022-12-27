package com.airxiechao.axcboot.db;

import com.airxiechao.axcboot.storage.annotation.Table;
import com.airxiechao.axcboot.storage.db.sql.DbManager;

import java.util.Arrays;

public class DbTest {

    @Table(value = "x", datasource = "datasource1")
    public static class X {

        private int id;
        private String value;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @Table(value = "y", datasourceMethod = "getDatasource")
    public static class Y {

        public static String getDatasource(){
            return "datasource2";
        }

        private int id;
        private String value;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static void main(String[] args){
        X x = new X();
        x.setValue("111");

        Y y = new Y();
        y.setValue("222");

        DbManager dbManager = new DbManager();

        dbManager.insert(x);
        dbManager.insert(y);

        x.setValue("333");
        dbManager.updateFields(x, Arrays.asList("value"));
    }

}
