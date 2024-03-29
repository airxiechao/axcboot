package com.airxiechao.axcboot.storage.db.sql.util;

import com.airxiechao.axcboot.storage.db.sql.model.SqlParams;
import com.airxiechao.axcboot.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class SqlParamsBuilder {

    private static final String AND = "and";
    private static final String OR = "or";

    public static class SqlWhereTriple {
        public String field;
        public String operator;
        public Object value;
        public boolean isMatchAgainst;

        public SqlWhereTriple(String field, String operator, Object value){
            this(field, operator, value, false);
        }

        public SqlWhereTriple(String field, String operator, Object value, boolean isMatchAgainst){
            this.field = field;
            this.operator = operator;
            this.value = value;
            this.isMatchAgainst = isMatchAgainst;
        }
    }

    public static class SqlWhereTripleGroup {

        public List<SqlWhereTriple> triples = new ArrayList<>();
        public List<String> logics = new ArrayList<>();

        public SqlWhereTripleGroup(){}

        public SqlWhereTripleGroup and(String field, String operator, Object value){
            and(field, operator, value, false);
            return this;
        }

        public SqlWhereTripleGroup or(String field, String operator, Object value, boolean isMatchAgainst){
            SqlWhereTriple triple = new SqlWhereTriple(field, operator, value);

            triples.add(triple);
            logics.add(OR);

            return this;
        }

        public SqlWhereTripleGroup and(String field, String operator, Object value, boolean isMatchAgainst){
            SqlWhereTriple triple = new SqlWhereTriple(field, operator, value, isMatchAgainst);

            triples.add(triple);
            logics.add(AND);

            return this;
        }

        public SqlWhereTripleGroup or(String field, String operator, Object value){
            or(field, operator, value, false);

            return this;
        }

        public SqlParams build(SqlParamsBuilder spBuilder){
            StringBuilder sb = new StringBuilder();
            Map params = new HashMap();

            sb.append(" ( 1=1 ");
            for(int i = 0; i < triples.size(); ++i){
                SqlWhereTriple triple = triples.get(i);
                String field = triple.field;
                String operator = triple.operator;
                boolean isMatchAgainst = triple.isMatchAgainst;
                Object value = triple.value;
                String logic = logics.get(i);

                String paramName = spBuilder.getParamName();
                if(isMatchAgainst){
                    sb.append(" "+logic+" MATCH("+field+") AGAINST(#{"+paramName+"}) ");
                }else{
                    sb.append(" "+logic+" "+field+" "+operator+" #{"+paramName+"} ");
                }
                params.put(paramName, value);
            }
            sb.append(") ");

            return new SqlParams(sb.toString(), params);
        }
    }

    public static class SqlOrder {
        public String orderField;
        public String orderType;

        public SqlOrder(String orderField, String orderType){
            this.orderField = orderField;
            this.orderType = orderType;
        }
    }

    private List<String> selectFields = new ArrayList<>();
    private String from;
    private List<String> joins = new ArrayList<>();
    private List<SqlWhereTriple> wheres = new ArrayList<>();
    private List<SqlWhereTripleGroup> whereGroups = new ArrayList<>();
    private String groupBy;
    private Integer pageNo;
    private Integer pageSize;
    private List<SqlOrder> orders = new ArrayList<>();
    private boolean isCount;
    private boolean isDelete;
    private int pnCount;

    public SqlParamsBuilder select(String field){
        this.selectFields.add(field);

        return this;
    }

    public SqlParamsBuilder from(String from){
        this.from = from;

        return this;
    }

    public SqlParamsBuilder join(String join){
        this.joins.add(join);

        return this;
    }

    public SqlParamsBuilder where(String field, String operator, Object value){
        SqlWhereTriple triple = new SqlWhereTriple(field, operator, value);
        wheres.add(triple);

        return this;
    }

    public SqlParamsBuilder where(String field, String operator, Object value, boolean isMatchAgainst){
        SqlWhereTriple triple = new SqlWhereTriple(field, operator, value, isMatchAgainst);
        wheres.add(triple);

        return this;
    }

    public SqlParamsBuilder whereGroup(SqlWhereTripleGroup tripleGroup){
        whereGroups.add(tripleGroup);

        return this;
    }

    public SqlParamsBuilder order(String orderField, String orderType){
        SqlOrder order = new SqlOrder(orderField, orderType);
        this.orders.add(order);

        return this;
    }

    public SqlParamsBuilder groupBy(String groupBy){
        this.groupBy = groupBy;

        return this;
    }

    public SqlParamsBuilder page(Integer pageNo, Integer pageSize){
        this.pageNo = pageNo;
        this.pageSize = pageSize;

        return this;
    }

    public SqlParamsBuilder count(){
        this.isCount = true;

        return this;
    }

    public SqlParamsBuilder delete(){
        this.isDelete = true;

        return this;
    }

    public SqlParams build(){

        this.pnCount = 0;

        // build select clause
        String select = "";
        if(this.isCount){
            select += "select count(*) as count ";
        }else if(this.isDelete){
            select += "delete ";
        }else{
            select += "select " + String.join(", ", this.selectFields) + " ";
        }

        // build from clause
        String from = "from " + this.from + " " + String.join(" ", this.joins) + " ";

        // build where clause
        StringBuilder sb = new StringBuilder();
        sb.append("where 1=1 ");
        Map params = new HashMap();
        for(int i = 0; i < wheres.size(); ++i){
            SqlWhereTriple triple = wheres.get(i);
            String field = triple.field;
            String operator = triple.operator;
            Object value = triple.value;
            boolean isMatchAgainst = triple.isMatchAgainst;

            String paramName = getParamName();
            if(isMatchAgainst){
                sb.append("and MATCH("+field+") AGAINST(#{"+paramName+"}) ");
            }else{
                sb.append("and "+field+" "+operator+" #{"+paramName+"} ");
            }
            params.put(paramName, value);
        }
        for(int i = 0; i < whereGroups.size(); ++i){
            SqlWhereTripleGroup tripleGroup = whereGroups.get(i);
            SqlParams sqlParams = tripleGroup.build(this);
            sb.append("and " +sqlParams.getSql());
            params.putAll(sqlParams.getParams());
        }
        String where = sb.toString();

        // build order clause
        sb = new StringBuilder();
        List<String> orderByList = this.orders
                .stream()
                .filter(order -> !StringUtil.isBlank(order.orderField) && !StringUtil.isBlank(order.orderType))
                .map(order -> {
                    String orderField = order.orderField;
                    String orderType = order.orderType;

                    switch (orderField){
                        default:
                            orderField = " `"+orderField+"` ";
                            break;
                    }

                    return orderField + " " + orderType + " ";

                }).collect(Collectors.toList());
        if (orderByList.size() > 0) {
            sb.append("order by " + String.join(",", orderByList) + " ");
        }
        String order = sb.toString();

        // build groupby clause
        sb = new StringBuilder();
        if(null != this.groupBy){
            sb.append("group by " + this.groupBy + " ");
        }
        String groupBy = sb.toString();

        // build limit clause
        sb = new StringBuilder();
        if(!this.isCount && null != this.pageNo && null != this.pageSize){
            sb.append("limit " + ((this.pageNo-1)*this.pageSize) + "," + this.pageSize);
        }
        String limit = sb.toString();

        String sql =
                select +
                from +
                where +
                order +
                groupBy +
                limit;

        return new SqlParams(sql, params);
    }

    private String getParamName(){
        this.pnCount++;
        return "PN"+pnCount;
    }
}
