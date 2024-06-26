//package com.example.product.config;
//
//
//import com.mongodb.reactivestreams.client.MongoClient;
//import com.mongodb.reactivestreams.client.MongoClients;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
//import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
//
//@Configuration
//public class MongoReactiveApplication extends AbstractReactiveMongoConfiguration {
//
//    @Value("${spring.data.mongodb.uri}")
//    private String uri;
//
//    @Value("${spring.data.mongodb.database}")
//    private String db;
//
//    @Bean
//    public MongoClient mongoClient() {
//        MongoClient client = MongoClients.create(uri);
//        return client;
//    }
//
//    protected String getDatabaseName() {
//        return db;
//    }
//
//    @Bean
//    public ReactiveMongoTemplate reactiveMongoTemplate(){
//        return new ReactiveMongoTemplate(mongoClient(), getDatabaseName());
//    }
//}
