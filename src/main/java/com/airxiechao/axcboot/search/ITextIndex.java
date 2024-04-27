package com.airxiechao.axcboot.search;

import java.util.List;
import java.util.Map;

public interface ITextIndex {
    void add(String id, Object object, boolean toCommit) throws Exception;
    List<String> query(Map<String, String> terms, Map<String, String> matches, int pageNo, int pageSize) throws Exception;
    void update(String id, Object object, boolean toCommit) throws Exception;
    void delete(String id, boolean toCommit) throws Exception;
    void commit() throws Exception;
    void close() throws Exception;
    void destroy() throws Exception;
}
