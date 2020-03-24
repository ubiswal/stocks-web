package com.ubiswal.handlers;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class Handlers {
    private List<String> symbols;
    public Handlers(List<String> symbols){
        this.symbols = symbols;
    }

    public void homeHandler(HttpExchange exchange) throws IOException {
        String response = symbols.toString();
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public void testHandler(HttpExchange exchange) throws IOException{
        String response = "Test!";
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

}
