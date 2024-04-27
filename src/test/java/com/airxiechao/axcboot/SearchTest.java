package com.airxiechao.axcboot;

import com.airxiechao.axcboot.search.ITextIndex;
import com.airxiechao.axcboot.search.annotation.IndexField;
import com.airxiechao.axcboot.search.lucene.LuceneTextIndex;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchTest {
    public static void main(String[] args) throws Exception {
        ITextIndex textIndex = new LuceneTextIndex(Path.of("tmp", "test-index"));
        textIndex.destroy();

        textIndex.add("1", new Doc("你好，今天天气很好, how are you"), false);
        textIndex.add("2", new Doc("你好，昨天天气很好"), false);
        textIndex.add("3", new Doc("你好，明天天气很好"), false);
        textIndex.commit();

        Map<String, String> terms = new HashMap<>();
        Map<String, String> matches = new HashMap<>();
        matches.put("text", "明天");
        List<String> list = textIndex.query(terms, matches, 1, 10);
        for (int i = 0; i < list.size(); i++) {
            String id = list.get(i);
            System.out.println((i+1) + ". [" + id + "]");
        }

        textIndex.update("2", new Doc("你好，明天天气不好, how good you are"), true);

        list = textIndex.query(terms, matches, 1, 10);
        for (int i = 0; i < list.size(); i++) {
            String id = list.get(i);
            System.out.println((i+1) + ". [" + id + "]");
        }

        textIndex.close();
    }
}

class Doc {
    @IndexField(isText = true)
    private String text;

    public Doc(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
