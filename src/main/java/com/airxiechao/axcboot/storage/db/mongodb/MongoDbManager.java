package com.airxiechao.axcboot.storage.db.mongodb;

import com.airxiechao.axcboot.storage.db.mongodb.annotation.MongoDbCollection;
import com.airxiechao.axcboot.util.AnnotationUtil;
import com.airxiechao.axcboot.util.ModelUtil;
import com.airxiechao.axcboot.util.StringUtil;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoDbManager {

    private static final CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));

    private MongoClient mongoClient;
    private String database;

    public MongoDbManager(String host, Integer port, String username, String password, String database) {
        if(null == port){
            port = 27017;
        }

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(
                        String.format("mongodb://%s:%s@%s:%d/%s", username, password, host, port, database)))
                .codecRegistry(pojoCodecRegistry)
                .build();

        this.mongoClient = MongoClients.create(settings);
        this.database = database;
    }

    public MongoDatabase getDatabase(){
        return mongoClient.getDatabase(this.database);
    }

    public <T> MongoCollection<T> getCollection(Class<T> cls){
        MongoDbCollection annotation = AnnotationUtil.getClassAnnotation(cls, MongoDbCollection.class);
        if(null == annotation){
            throw new RuntimeException("no collection name");
        }

        String name = annotation.value();
        return getDatabase().getCollection(name, cls);
    }
}
