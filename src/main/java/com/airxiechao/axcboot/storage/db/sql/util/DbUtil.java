package com.airxiechao.axcboot.storage.db.sql.util;

import com.airxiechao.axcboot.storage.annotation.Column;
import com.airxiechao.axcboot.storage.annotation.Index;
import com.airxiechao.axcboot.storage.annotation.Indexes;
import com.airxiechao.axcboot.storage.annotation.Table;
import com.airxiechao.axcboot.util.MapBuilder;
import com.airxiechao.axcboot.util.StringUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class DbUtil {

    private static Map<Class, String> columnTypeMap = new MapBuilder<Class, String>()
            .put(Integer.class, "int")
            .put(int.class, "int")
            .put(Long.class, "bigint")
            .put(long.class, "bigint")
            .put(float.class, "float")
            .put(Float.class, "float")
            .put(double.class, "double")
            .put(Double.class, "double")
            .put(String.class, "varchar")
            .put(Date.class, "datetime")
            .put(Boolean.class, "tinyint")
            .put(boolean.class, "tinyint")
            .build();

    private static Map<String, Integer> columnDefaultLengthMap = new MapBuilder<String, Integer>()
            .put("int", 11)
            .put("bigint", 20)
            .put("float", -1)
            .put("double", -1)
            .put("varchar", 50)
            .put("datetime", 0)
            .put("text", -1)
            .put("mediumtext", -1)
            .put("longtext", -1)
            .build();

    public static String table(Class<?> tClass){
        Table table = tClass.getAnnotation(Table.class);
        if(null != table){
            return table.value();
        }

        return null;
    }

    public static String column(Class<?> tClass, String fieldName) {
        try {
            return column(tClass.getDeclaredField(fieldName));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static String columns(Class<?> tClass, String[] fieldNames) {
        List<String> names = new ArrayList<>();
        for(String fieldName : fieldNames){
            names.add(column(tClass, fieldName));
        }

        return String.join(",", names);
    }

    public static String column(Field field){
        Column column = field.getAnnotation(Column.class);
        if(null != column){
            String columnName = column.name();
            if(!StringUtil.isBlank(columnName)){
                return columnName;
            }
        }

        return StringUtil.camelCaseToUnderscore(field.getName());
    }

    public static String columns(Field[] fields) {
        List<String> names = new ArrayList<>();
        for(Field field : fields){
            names.add(column(field));
        }

        return String.join(",", names);
    }

    public static String columnType(Field field){
        Column column = field.getAnnotation(Column.class);
        if(null != column){
            String columnType = column.type();
            if(!StringUtil.isBlank(columnType)){
                return columnType;
            }
        }

        Class fieldClass = field.getType();

        String columnType = columnTypeMap.get(fieldClass);
        if(null == columnType){
            throw new RuntimeException("no column type for " + fieldClass);
        }

        return columnType;
    }

    public static int columnLength(Field field){
        Column column = field.getAnnotation(Column.class);
        if(null != column){
            int columnLength = column.length();
            if(columnLength != 0){
                return columnLength;
            }
        }

        String columnType = columnType(field);
        int defaultLength = columnDefaultLengthMap.getOrDefault(columnType, 0);

        return defaultLength;
    }

    public static boolean columnNotNull(Field field){
        Column column = field.getAnnotation(Column.class);
        if(null != column){
            return column.notNull();
        }

        return false;
    }

    public static String columnDefaultValue(Field field){
        Column column = field.getAnnotation(Column.class);
        if(null != column){
            return column.defaultValue();
        }

        return "NULL";
    }

    public static String ddl(Class<?> tClass, boolean dropIfExists){
        StringWriter swTable = new StringWriter();
        PrintWriter pwTable = new PrintWriter(swTable);

        Table table = tClass.getAnnotation(Table.class);
        if(null == table){
            return pwTable.toString();
        }

        // ddl table

        String tableName = table.value();
        String tableEngine = table.engine();
        String tableCharset = table.charset();
        String tableCollate = table.collate();
        String tableRowFormat = table.rowFormat();
        String tablePrimaryKeyColumn = "id";
        String tablePrimaryKeyMethod = table.primaryKeyMethod();
        boolean tablePrimaryKeyAutoIncrement = table.primaryKeyAutoIncrement();
        int tablePrimaryKeyAutoIncrementBegin = table.primaryKeyAutoIncrementBegin();

        if(dropIfExists){
            pwTable.println(String.format("DROP TABLE IF EXISTS `%s`;", tableName));
        }
        pwTable.println(String.format("CREATE TABLE IF NOT EXISTS `%s` (", tableName));

        // ddl column

        StringWriter swColumn = new StringWriter();
        PrintWriter pwColumn = new PrintWriter(swColumn);
        String padding = "  ";

        // columns
        for(Field field : tClass.getDeclaredFields()){
            if(Modifier.isStatic(field.getModifiers())){
                continue;
            }

            String columnName = column(field);
            String columnType = columnType(field);
            int columnLength = columnLength(field);
            boolean columnNotNull = columnNotNull(field);
            String columnDefaultValue = columnDefaultValue(field);

            String columnTypeAndLength;
            if(columnLength >= 0){
                columnTypeAndLength = String.format("%s(%d)", columnType, columnLength);
            }else{
                columnTypeAndLength = columnType;
            }

            if(columnName.equals(tablePrimaryKeyColumn)){
                pwColumn.print(padding);
                pwColumn.println(String.format("`%s` %s NOT NULL %s,",
                        columnName, columnTypeAndLength, tablePrimaryKeyAutoIncrement ? "AUTO_INCREMENT" : ""));
            } else {
                pwColumn.print(padding);
                pwColumn.println(String.format("`%s` %s %sNULL DEFAULT %s,",
                        columnName, columnTypeAndLength, columnNotNull ? "NOT " : "", columnDefaultValue));
            }

        }

        // primary key
        pwColumn.print(padding);
        pwColumn.println(String.format("PRIMARY KEY (`%s`) USING %s,", tablePrimaryKeyColumn, tablePrimaryKeyMethod));

        // indexes
        List<Index> indexList = new ArrayList<>();
        Index singleIndex = tClass.getAnnotation(Index.class);
        if(null != singleIndex){
            indexList.add(singleIndex);
        }
        Indexes multipleIndexes = tClass.getAnnotation(Indexes.class);
        if(null != multipleIndexes){
            indexList.addAll(Arrays.asList(multipleIndexes.value()));
        }
        for(Index index : indexList){
            String indexType;
            String indexNamePrefix;
            if(index.unique()){
                indexType = "UNIQUE ";
                indexNamePrefix = "unique-";
            }else if(index.fulltext()){
                indexType = "FULLTEXT ";
                indexNamePrefix = "ft-";
            }else{
                indexType = "";
                indexNamePrefix = "index-";
            }
            List<String> columns = Arrays.stream(index.fields()).map(f -> DbUtil.column(tClass, f)).collect(Collectors.toList());
            String indexColumns = String.join(",", columns.stream().map(c -> "`" + c + "`").collect(Collectors.toList()));
            String indexName = indexNamePrefix + String.join("-", columns);

            pwColumn.print(padding);
            pwColumn.print(String.format("%sINDEX `%s`(%s) ", indexType, indexName, indexColumns));
            if(index.fulltext()){
                String indexParser = index.parser();
                pwColumn.println(String.format("WITH PARSER %s,", indexParser));
            }else{
                String indexMethod = index.method();
                pwColumn.println(String.format("USING %s,", indexMethod));
            }
        }

        String ddlColumn = swColumn.toString();

        pwTable.println(ddlColumn.substring(0, ddlColumn.lastIndexOf(",")));
        pwTable.println(String.format(") ENGINE = %s AUTO_INCREMENT = %d CHARACTER SET = %s COLLATE = %s ROW_FORMAT = %s;",
                tableEngine, tablePrimaryKeyAutoIncrementBegin, tableCharset, tableCollate, tableRowFormat));

        return swTable.toString();
    }
}
