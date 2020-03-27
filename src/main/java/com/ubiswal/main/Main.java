package com.ubiswal.main;

import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import com.ubiswal.config.Config;
import com.ubiswal.handlers.Handlers;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    private static final String BUCKETNAME = "stocks-testing";

    public static void main(String args []) throws IOException {
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
        Config cfg = getConfig(s3);
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable("Analytics-testing");
        Handlers handlers = new Handlers(cfg.getStockSymbols(), table);
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
        HttpContext homeContext = httpServer.createContext("/");
        homeContext.setHandler(handlers::homeHandler);
        HttpContext testContext = httpServer.createContext("/test");
        testContext.setHandler(handlers::testHandler);
        httpServer.start();
    }

    private static Config getConfig(final AmazonS3 s3Client) throws IOException {
        try {
            S3Object s3obj = s3Client.getObject(BUCKETNAME , "config.json");
            S3ObjectInputStream inputStream = s3obj.getObjectContent();
            FileUtils.copyInputStreamToFile(inputStream, new File("config.json"));
        } catch (SdkClientException e) {
            System.out.println("Failed to download config file from s3 because " + e.getMessage());
            throw e;
        } catch (IOException e) {
            System.out.println("Failed to save downloaded config file from s3 because " + e.getMessage());
            throw e;
        }

        //use jackson for json to class conversion for the Config
        ObjectMapper mapper = new ObjectMapper();
        try {
            // JSON file to Java object
            Config config = mapper.readValue(new File("config.json"), Config.class);
            return config;
        } catch (IOException e) {
            System.out.println("Failed to read the downloaded config file from s3 because " + e.getMessage());
            throw e;
        }

    }

}

