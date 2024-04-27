package com.airxiechao.axcboot.search.lucene;

import com.airxiechao.axcboot.search.ITextIndex;
import com.airxiechao.axcboot.search.annotation.IndexField;
import com.airxiechao.axcboot.util.ClsUtil;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LuceneTextIndex implements ITextIndex {

    private Path indexPath;
    private SmartChineseAnalyzer smartChineseAnalyzer = new SmartChineseAnalyzer();
    private IndexWriter indexWriter;
    private DirectoryReader indexReader;

    public LuceneTextIndex(Path indexPath) {
        this.indexPath = indexPath;
    }

    @Override
    public void add(String id, Object object, boolean toCommit) throws Exception {
        Document document = buildDocument(id, object);

        IndexWriter writer = getIndexWriter();
        writer.addDocument(document);

        if(toCommit) {
            writer.commit();
        }
    }

    @Override
    public List<String> query(Map<String, String> terms, Map<String, String> matches, int pageNo, int pageSize) throws Exception {
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        if(null != terms){
            for (Map.Entry<String, String> entry : terms.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                Query q = new TermQuery(new Term(key, value));
                queryBuilder.add(q, BooleanClause.Occur.MUST);
            }
        }
        if(null != matches){
            for (Map.Entry<String, String> entry : matches.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                Query q = new QueryParser(key, smartChineseAnalyzer).parse(value);
                queryBuilder.add(q, BooleanClause.Occur.MUST);
            }
        }

        Query query = queryBuilder.build();

        IndexReader reader = getIndexReader();
        IndexSearcher indexSearcher = new IndexSearcher(reader);
        StoredFields storedFields = reader.storedFields();

        TopDocs topDocs = indexSearcher.search(query, pageNo*pageSize);
        List<String> list = new ArrayList<>();
        ScoreDoc[] hits = topDocs.scoreDocs;
        for (int i = 0; i < hits.length; i++) {
            if (i >= (pageNo-1) * pageSize && i < pageNo * pageSize) {
                Document document = storedFields.document(hits[i].doc);
                String id = document.get("id");

                list.add(id);
            }
        }

        return list;
    }

    @Override
    public void update(String id, Object object, boolean toCommit) throws Exception {
        Document document = buildDocument(id, object);

        IndexWriter writer = getIndexWriter();
        writer.updateDocument(new Term("id", id), document);

        if(toCommit) {
            writer.commit();
        }
    }

    @Override
    public void delete(String id, boolean toCommit) throws Exception {
        IndexWriter writer = getIndexWriter();
        writer.deleteDocuments(new Term("id", id));

        if(toCommit) {
            writer.commit();
        }
    }

    @Override
    public void commit() throws Exception {
        IndexWriter writer = getIndexWriter();
        writer.commit();
    }

    @Override
    public void close() throws Exception {
        if(null != indexReader){
            indexReader.close();
            indexReader = null;
        }

        if(null != indexWriter){
            indexWriter.close();
            indexWriter = null;
        }
    }

    @Override
    public void destroy() throws Exception {
        // 删除索引文件夹
        FileUtils.deleteDirectory(indexPath.toFile());
    }

    private Document buildDocument(String id, Object object) throws Exception{
        Document document = new Document();
        document.add(new StringField("id", id, Field.Store.YES));

        for (java.lang.reflect.Field f : ClsUtil.getFields(object.getClass(), IndexField.class)) {
            f.setAccessible(true);

            IndexField indexField = f.getAnnotation(IndexField.class);
            String type = f.getType().getSimpleName();
            switch (type){
                case "String":
                    if(!indexField.isText()) {
                        document.add(new StringField(f.getName(), (String) f.get(object), Field.Store.YES));
                    }else {
                        document.add(new TextField(f.getName(), (String) f.get(object), Field.Store.YES));
                    }
                    break;
                case "Integer":
                    document.add(new IntField(f.getName(), (Integer) f.get(object), Field.Store.YES));
                    break;
                case "Long":
                    document.add(new LongField(f.getName(), (Long)f.get(object), Field.Store.YES));
                    break;
                case "Float":
                    document.add(new FloatField(f.getName(), (Float) f.get(object), Field.Store.YES));
                    break;
                case "Double":
                    document.add(new DoubleField(f.getName(), (Double) f.get(object), Field.Store.YES));
                    break;
            }
        }

        return document;
    }

    private DirectoryReader getIndexReader() throws Exception {
        Directory directory = FSDirectory.open(indexPath);
        if(null == indexReader) {
            indexReader = DirectoryReader.open(directory);
        }else{
            DirectoryReader newIndexReader = DirectoryReader.openIfChanged(indexReader);
            if(null != newIndexReader){
                indexReader = newIndexReader;
            }
        }

        return indexReader;
    }

    private IndexWriter getIndexWriter() throws Exception {
        if(null == indexWriter){
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(smartChineseAnalyzer);
            Directory directory = FSDirectory.open(indexPath);
            indexWriter = new IndexWriter(directory, indexWriterConfig);
        }

        return indexWriter;
    }

}
