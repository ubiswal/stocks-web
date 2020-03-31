package com.ubiswal.handlers;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.sun.net.httpserver.HttpExchange;
import j2html.tags.ContainerTag;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static j2html.TagCreator.*;

public class Handlers {
    private List<String> symbols;
    private Table table;

    public Handlers(List<String> symbols, Table table) {
        this.symbols = symbols;
        this.table = table;
    }

    public void homeHandler(HttpExchange exchange) throws IOException {
        ContainerTag h = head(
                title("Stock Info"),
                link().withRel("stylesheet").withHref("/css/main.css")
        );

        List<ContainerTag> bodyElements = new ArrayList<>();
        bodyElements.add(h1("Max Stock Price"));

        for (String symbol : symbols) {
            System.out.println("Fetching max price for " + symbol);
            GetItemSpec spec = new GetItemSpec().withPrimaryKey(new KeyAttribute("symb", symbol), new KeyAttribute("type", "1_maxprice"));
            Item item = table.getItem(spec);
            if (item == null) {
                System.out.println("Could not fetch data for " + symbol);
                continue;
            }
            bodyElements.add(
                    p(String.format("%s : %s", symbol,  item.asMap().get("value").toString()))
            );
        }
        ContainerTag b = body(
                bodyElements.toArray(new ContainerTag[0])
        );
        String response = html(h, b).render();
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
