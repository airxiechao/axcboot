package com.airxiechao.axcboot.storage.db.util;

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
            .put(String.class, "varchar")
            .put(Date.class, "datetime")
            .build();

    private static Map<String, Integer> columnDefaultLengthMap = new MapBuilder<String, Integer>()
            .put("int", 11)
            .put("bigint", 20)
            .put("varchar", 50)
            .put("datetime", 0)
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
            if(columnLength > 0){
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

        if(dropIfExists){
            pwTable.println(String.format("DROP TABLE IF EXISTS `%s`;", tableName));
        }
        pwTable.println(String.format("CREATE TABLE `%s`  (", tableName));

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

            if(columnName.equals(tablePrimaryKeyColumn)){
                pwColumn.print(padding);
                pwColumn.println(String.format("`%s` %s(%s) NOT NULL %s,",
                        columnName, columnType, columnLength, tablePrimaryKeyAutoIncrement ? "AUTO_INCREMENT" : ""));
            } else {
                pwColumn.print(padding);
                pwColumn.println(String.format("`%s` %s(%d) %sNULL DEFAULT %s,",
                        columnName, columnType, columnLength, columnNotNull ? "NOT " : "", columnDefaultValue));
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
            String indexType = index.unique() ? "UNIQUE " : "";
            List<String> columns = Arrays.stream(index.fields()).map(f -> DbUtil.column(tClass, f)).collect(Collectors.toList());
            String indexColumns = String.join(",", columns.stream().map(c -> "`" + c + "`").collect(Collectors.toList()));
            String indexMethod = index.method();
            String indexNamePrefix = index.unique() ? "unique-" : "index-";
            String indexName = indexNamePrefix + String.join("-", columns);

            pwColumn.print(padding);
            pwColumn.println(String.format("%sINDEX `%s`(%s) USING %s,", indexType, indexName, indexColumns, indexMethod));
        }

        String ddlColumn = swColumn.toString();

        pwTable.println(ddlColumn.substring(0, ddlColumn.lastIndexOf(",")));
        pwTable.println(String.format(") ENGINE = %s AUTO_INCREMENT = 1 CHARACTER SET = %s COLLATE = %s ROW_FORMAT = %s;",
                tableEngine, tableCharset, tableCollate, tableRowFormat));

        return swTable.toString();
    }
}
