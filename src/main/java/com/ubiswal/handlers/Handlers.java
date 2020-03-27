package com.ubiswal.handlers;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Handlers {
    private List<String> symbols;
    private Table table;

    public Handlers(List<String> symbols, Table table) {
        this.symbols = symbols;
        this.table = table;
    }

    public void homeHandler(HttpExchange exchange) throws IOException {
        String response = "<HTML>\n";
        response += "<header>\n";
        response += "</header>\n";
        response += "<body>\n";
        response += "<h1>Watch list</h1>\n";
        response += "<p>\n";
        for (String symbol : symbols) {
            System.out.println("Fetching max price for " + symbol);
            GetItemSpec spec = new GetItemSpec().withPrimaryKey(new KeyAttribute("symb", symbol), new KeyAttribute("type", "1_maxprice"));
            Item item = table.getItem(spec);
            if (item == null) {
                System.out.println("Could not fetch data for " + symbol);
                continue;
            }
            response += "</p>\n";
            response += "</body>\n";
            response = response + symbol + " : " + item.asMap().get("value") + "\n";
        }
        response = response + "</HTML>";
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public void testHandler(HttpExchange exchange) throws IOException {
        String response = "Test!";
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

}
