package com.airxiechao.axcboot.storage.db;

import com.airxiechao.axcboot.storage.annotation.Table;
import com.airxiechao.axcboot.storage.db.model.SqlParams;
import com.airxiechao.axcboot.storage.fs.IFs;
import com.airxiechao.axcboot.storage.fs.JavaResourceFs;
import com.airxiechao.axcboot.util.StringUtil;
import com.alibaba.fastjson.JSON;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbManager {

    private static final Logger logger = LoggerFactory.getLogger(DbManager.class);

    private static final String DEFAULT_DATASOURCE = "default";

    private Map<String, SqlSessionFactory> sqlSessionFactoryMap = new HashMap<>();

    public DbManager(){
        this(new JavaResourceFs(), "mybatis.xml");
    }

    public DbManager(IFs configFs, String configFilePath) {
        List<String> envIds;
        try (InputStream inputStream = configFs.getInputStream(configFilePath)){
            envIds = parseEnvironmentIds(inputStream);
        }catch (Exception e){
            logger.error("db manager parse environment ids error.", e);
            return;
        }

        // default datasource
        try (InputStream inputStream = configFs.getInputStream(configFilePath)){
            SqlSessionFactory defaultSqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            defaultSqlSessionFactory.getConfiguration().addMapper(DbMapper.class);
            sqlSessionFactoryMap.put(DEFAULT_DATASOURCE, defaultSqlSessionFactory);
        }catch (Exception e){
            logger.error("db manager init default datasource error.", e);
            return;
        }

        // named datasource
        for(String envId : envIds){
            try (InputStream inputStream = configFs.getInputStream(configFilePath)){
                SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, envId);
                sqlSessionFactory.getConfiguration().addMapper(DbMapper.class);
                sqlSessionFactoryMap.put(envId, sqlSessionFactory);
            }catch (Exception e){
                logger.error("db manager init datasource [{}] error.", envId, e);
            }
        }
    }

    private List<String> parseEnvironmentIds(InputStream inputStream) throws Exception {
        List<String> envIds = new ArrayList<>();

        DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder xmlBuilder = xmlFactory.newDocumentBuilder();
        Document xml = xmlBuilder.parse(inputStream);
        NodeList envNodes = xml.getDocumentElement().getElementsByTagName("environment");
        for(int i = 0; i < envNodes.getLength(); ++i){
            Node envNode = envNodes.item(i);
            if(envNode.getNodeType() == Node.ELEMENT_NODE){
                Element envElement = (Element)envNode;
                String envId = envElement.getAttribute("id");
                envIds.add(envId);
            }
        }

        return envIds;
    }

    // ---------------------------------------- seesion ------------------------------------------

    public SqlSession openSession(String datasource){
        SqlSessionFactory sqlSessionFactory = getSqlSessionFactory(datasource);
        return sqlSessionFactory.openSession();
    }

    public SqlSession openSession(String datasource, boolean autoCommit){
        SqlSessionFactory sqlSessionFactory = getSqlSessionFactory(datasource);
        return sqlSessionFactory.openSession(autoCommit);
    }

    private SqlSessionFactory getSqlSessionFactory(String datasource){
        SqlSessionFactory sqlSessionFactory = null;
        if(!StringUtil.isBlank(datasource)){
            sqlSessionFactory = sqlSessionFactoryMap.get(datasource);
        }
        if(null == sqlSessionFactory){
            sqlSessionFactory = sqlSessionFactoryMap.get(DEFAULT_DATASOURCE);
        }

        return sqlSessionFactory;
    }

    private String getDatasource(Class<?> tClass){
        String datasource = null;

        if(null == tClass){
            return datasource;
        }

        Table table = tClass.getAnnotation(Table.class);
        if(null != table){
            datasource = table.datasource();
            if(!StringUtil.isBlank(datasource)){
                return datasource;
            }

            String datasourceMethod = table.datasourceMethod();
            if(!StringUtil.isBlank(datasourceMethod)){
                try {
                    Method method = tClass.getMethod(datasourceMethod);
                    datasource = (String)method.invoke(null);
                    return datasource;
                } catch (Exception e) {
                    logger.error("call datasourceMethod {}.{} error", tClass.getName(), datasourceMethod);
                }
            }
        }

        return datasource;
    }

    // ---------------------------------------- transaction ------------------------------------------

    /**
     * 执行事务
     * @param transaction
     * @return
     */
    public <T> boolean executeTransaction(DbTransaction transaction){
        String datasource = null;
        return executeTransaction(transaction, datasource);
    }

    public <T> boolean executeTransaction(DbTransaction transaction, Class<T> tClass){
        String datasource = getDatasource(tClass);
        return executeTransaction(transaction, datasource);
    }

    public boolean executeTransaction(DbTransaction transaction, String datasource){
        if(StringUtil.isBlank(datasource)){
            datasource = DEFAULT_DATASOURCE;
        }

        SqlSession session = openSession(datasource);
        DbMapper mapper = session.getMapper(DbMapper.class);

        boolean success = false;
        try{
            transaction.execute(mapper);

            session.commit();
            success = true;
        }catch (Exception e){
            session.rollback();
            logger.error("transaction rollback", e);
        }finally {
            session.close();
        }

        return success;
    }


    // ---------------------------------------- query -----------------------------------------------


    /**
     * 查询对象
     * @param id
     * @param tClass
     * @param <T>
     * @return
     */
    public <T> T getById(long id, Class<T> tClass){
        String datasource = getDatasource(tClass);
        try(SqlSession session = openSession(datasource)){
            DbMapper mapper = session.getMapper(DbMapper.class);
            Map map = mapper.selectById(id, tClass);
            T ret = JSON.parseObject(JSON.toJSONString(map), tClass);
            return ret;
        }
    }

    /**
     * SQL查询
     * @param sql
     * @param tClass
     * @param <T>
     * @return
     */
    public <T> List<T> selectBySql(String sql, Class<T> tClass){
        return selectBySql(sql, null, tClass);
    }

    public <T> List<T> selectBySql(String sql, Class<T> tClass, String datasource){
        return selectBySql(sql, null, tClass, datasource);
    }

    public <T> List<T> selectBySql(String sql, Map params, Class<T> tClass){
        String datasource = getDatasource(tClass);
        return selectBySql(sql, params, tClass, datasource);
    }

    public <T> List<T> selectBySql(SqlParams sqlParams, Class<T> tClass){
        return selectBySql(sqlParams.getSql(), sqlParams.getParams(), tClass);
    }

    public <T> List<T> selectBySql(String sql, Map params, Class<T> tClass, String datasource){
        try(SqlSession session = openSession(datasource)){
            DbMapper mapper = session.getMapper(DbMapper.class);
            List<Map> list = mapper.selectBySql(sql, params);

            List<T> ret = new ArrayList<>();
            for(Map map : list){
                T t = JSON.parseObject(JSON.toJSONString(map), tClass);
                ret.add(t);
            }

            return ret;
        }
    }

    public <T> List<T> selectBySql(SqlParams sqlParams, Class<T> tClass, String datasource){
        return selectBySql(sqlParams.getSql(), sqlParams.getParams(), tClass, datasource);
    }

    /**
     * SQL查询第一个
     * @param sql
     * @param tClass
     * @param <T>
     * @return
     */
    public <T> T selectFirstBySql(String sql, Class<T> tClass){
        return selectFirstBySql(sql, null, tClass);
    }

    public <T> T selectFirstBySql(String sql, Map params, Class<T> tClass){
        sql = "select * from ( " + sql + " ) _ limit 1";
        List<T> ret = selectBySql(sql, params, tClass);
        if(ret.size() > 0){
            return ret.get(0);
        }else{
            return null;
        }
    }

    public <T> T selectFirstBySql(SqlParams sqlParams, Class<T> tClass) {
        return selectFirstBySql(sqlParams.getSql(), sqlParams.getParams(), tClass);
    }

    /**
     * SQL查询一个整数
     * @param sql
     * @return
     */
    public Long longBySql(String sql){
        return longBySql(sql, null, null);
    }

    public Long longBySql(String sql, Map params){
        return longBySql(sql, params, null);
    }

    public Long longBySql(String sql, Class<?> tClass){
        return longBySql(sql, null, tClass);
    }

    public Long longBySql(String sql, Map params, Class<?> tClass){
        String datasource = getDatasource(tClass);
        try(SqlSession session = openSession(datasource)){
            DbMapper mapper = session.getMapper(DbMapper.class);
            return mapper.longBySql(sql, params);
        }
    }

    public Long longBySql(SqlParams sqlParams, Class<?> tClass){
        return longBySql(sqlParams.getSql(), sqlParams.getParams(), tClass);
    }

    /**
     * SQL查询一个DOUBLE
     * @param sql
     * @return
     */
    public Double doubleBySql(String sql){
        return doubleBySql(sql, null, null);
    }

    public Double doubleBySql(String sql, Map params){
        return doubleBySql(sql, params, null);
    }

    public Double doubleBySql(String sql, Class<?> tClass){
        return doubleBySql(sql, null, tClass);
    }

    public Double doubleBySql(String sql, Map params, Class<?> tClass){
        String datasource = getDatasource(tClass);
        try(SqlSession session = openSession(datasource)){
            DbMapper mapper = session.getMapper(DbMapper.class);
            return mapper.doubleBySql(sql, params);
        }
    }

    public Double doubleBySql(SqlParams sqlParams, Class<?> tClass){
        return doubleBySql(sqlParams.getSql(), sqlParams.getParams(), tClass);
    }


    /**
     * 插入对象
     * @param object
     * @return
     */
    public int insert(Object object){
        String datasource = getDatasource(object.getClass());
        try(SqlSession session = openSession(datasource, true)){
            DbMapper mapper = session.getMapper(DbMapper.class);
            int ret = mapper.insert(object);
            return ret;
        }
    }

    public int insertWithId(Object object){
        String datasource = getDatasource(object.getClass());
        try(SqlSession session = openSession(datasource, true)){
            DbMapper mapper = session.getMapper(DbMapper.class);
            int ret = mapper.insertWithId(object);
            return ret;
        }
    }

    /**
     * 批量插入
     * @param list
     * @return
     */
    public int insertBatch(List<?> list){
        Class tClass = Object.class;
        if(list.size() > 0){
            tClass = list.get(0).getClass();
        }
        String datasource = getDatasource(tClass);

        try(SqlSession session = openSession(datasource, true)){
            DbMapper mapper = session.getMapper(DbMapper.class);
            int ret = mapper.insertBatch(list);
            return ret;
        }
    }

    /**
     * 插入或更新
     * @param object
     * @return
     */
    public int insertOrUpdate(Object object){
        String datasource = getDatasource(object.getClass());
        try(SqlSession session = openSession(datasource, true)){
            DbMapper mapper = session.getMapper(DbMapper.class);
            int ret = mapper.insertOrUpdate(object);
            return ret;
        }
    }

    public int insertOrUpdateWithId(Object object){
        String datasource = getDatasource(object.getClass());
        try(SqlSession session = openSession(datasource, true)){
            DbMapper mapper = session.getMapper(DbMapper.class);
            int ret = mapper.insertOrUpdateWithId(object);
            return ret;
        }
    }

    public int insertOrUpdateBatch(List<?> list){
        Class tClass = Object.class;
        if(list.size() > 0){
            tClass = list.get(0).getClass();
        }
        String datasource = getDatasource(tClass);

        try(SqlSession session = openSession(datasource, true)){
            DbMapper mapper = session.getMapper(DbMapper.class);
            int ret = mapper.insertOrUpdateBatch(list);
            return ret;
        }
    }

    public int insertOrUpdateBatch(List<?> list, int batchSize) {

        Class tClass = Object.class;
        if(list.size() > 0){
            tClass = list.get(0).getClass();
        }
        String datasource = getDatasource(tClass);

        try (SqlSession session = openSession(datasource, true)) {
            DbMapper mapper = session.getMapper(DbMapper.class);

            int ret = 0;
            List<Object> sub = new ArrayList<>();
            for(Object object : list){
                sub.add(object);

                if(sub.size() == batchSize){
                    ret += mapper.insertOrUpdateBatch(sub);
                    sub.clear();
                }
            }

            if(sub.size() > 0){
                ret += mapper.insertOrUpdateBatch(sub);
                sub.clear();
            }

            return ret;
        }
    }

    public int insertOrUpdateWithIdBatch(List<?> list, int batchSize) {

        Class tClass = Object.class;
        if(list.size() > 0){
            tClass = list.get(0).getClass();
        }
        String datasource = getDatasource(tClass);

        try (SqlSession session = openSession(datasource, true)) {
            DbMapper mapper = session.getMapper(DbMapper.class);

            int ret = 0;
            List<Object> sub = new ArrayList<>();
            for(Object object : list){
                sub.add(object);

                if(sub.size() == batchSize){
                    ret += mapper.insertOrUpdateWithIdBatch(sub);
                    sub.clear();
                }
            }

            if(sub.size() > 0){
                ret += mapper.insertOrUpdateWithIdBatch(sub);
                sub.clear();
            }

            return ret;
        }
    }

    /**
     * 更新对象
     * @param object
     * @return
     */
    public int update(Object object){
        String datasource = getDatasource(object.getClass());
        try(SqlSession session = openSession(datasource, true)){
            DbMapper mapper = session.getMapper(DbMapper.class);
            int ret = mapper.update(object);
            return ret;
        }
    }

    /**
     * SQL更新
     * @param sql
     * @return
     */
    public int updateBySql(String sql){
        return updateBySql(sql, null, null);
    }

    public int updateBySql(String sql, Map params){
        return updateBySql(sql, params, null);
    }

    public int updateBySql(String sql, Class<?> tClass){
        return updateBySql(sql, null, tClass);
    }

    public int updateBySql(String sql, Map params, Class<?> tClass){
        String datasource = getDatasource(tClass);
        try(SqlSession session = openSession(datasource,true)){
            DbMapper mapper = session.getMapper(DbMapper.class);
            int ret = mapper.updateBySql(sql, params);
            return ret;
        }
    }

    public int updateBySql(SqlParams sqlParams, Class<?> tClass) {
        return updateBySql(sqlParams.getSql(), sqlParams.getParams(), tClass);
    }

    /**
     * 删除对象
     * @param id
     * @param tClass
     * @return
     */
    public int deleteById(long id, Class<?> tClass){
        String datasource = getDatasource(tClass);
        try(SqlSession session = openSession(datasource, true)){
            DbMapper mapper = session.getMapper(DbMapper.class);
            int ret = mapper.deleteById(id, tClass);
            return ret;
        }
    }

    /**
     * SQL删除
     * @param sql
     * @return
     */
    public int deleteBySql(String sql){
        return deleteBySql(sql, null, null);
    }

    public int deleteBySql(String sql, Map params){
        return deleteBySql(sql, params, null);
    }

    public int deleteBySql(SqlParams sqlParams){
        return deleteBySql(sqlParams.getSql(), sqlParams.getParams());
    }

    public int deleteBySql(String sql, Class<?> tClass){
        return deleteBySql(sql, null, tClass);
    }

    public int deleteBySql(String sql, Map params, Class<?> tClass){
        String datasource = getDatasource(tClass);
        try(SqlSession session = openSession(datasource, true)){
            DbMapper mapper = session.getMapper(DbMapper.class);
            int ret = mapper.deleteBySql(sql, params);
            return ret;
        }
    }

    public int deleteBySql(SqlParams sqlParams, Class<?> tClass){
        return deleteBySql(sqlParams.getSql(), sqlParams.getParams(), tClass);
    }


    /**
     * 执行SQL
     * @param sql
     * @return
     */
    public int executeBySql(String sql){
        return executeBySql(sql, null, Object.class);
    }

    public int executeBySql(String sql, Map params){
        return executeBySql(sql, params, Object.class);
    }

    public int executeBySql(SqlParams sqlParams){
        return executeBySql(sqlParams, Object.class);
    }

    public int executeBySql(String sql, String datasource){
        return executeBySql(sql, null, datasource);
    }

    public int executeBySql(String sql, Map params, Class<?> tClass){
        String datasource = getDatasource(tClass);
        return executeBySql(sql, params, datasource);
    }

    public int executeBySql(SqlParams sqlParams, Class<?> tClass){
        String datasource = getDatasource(tClass);
        return executeBySql(sqlParams, datasource);
    }

    public int executeBySql(String sql, Map params, String datasource){
        try(SqlSession session = openSession(datasource,true)){
            DbMapper mapper = session.getMapper(DbMapper.class);
            int ret = mapper.executeBySql(sql, params);
            return ret;
        }
    }

    public int executeBySql(SqlParams sqlParams, String datasource){
        return executeBySql(sqlParams.getSql(), sqlParams.getParams(), datasource);
    }
}

