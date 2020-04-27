package com.ubiswal.handlers;

import com.amazonaws.services.dynamodbv2.document.Table;
import com.sun.net.httpserver.HttpExchange;
import com.ubiswal.utils.DynamoUtils;
import com.ubiswal.utils.PageUtils;
import j2html.tags.ContainerTag;
import j2html.tags.EmptyTag;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static j2html.TagCreator.*;
import static j2html.TagCreator.tr;

public class SymbolHandler {
    private final List<String> symbols;
    private Map<String, AtomicReference<String>> pages;
    private final Table table;

    public SymbolHandler(List<String> symbols, Table table) {
        this.symbols = symbols;
        this.table = table;
        pages = new HashMap<>();
        for (String symbol : symbols) {
            pages.put(symbol, new AtomicReference<>());
        }
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(new PageRefresh(5) {
            @Override
            void updateHtml() {
                refreshAllHtmls();
            }
        });
    }

    private ContainerTag getBanner(String symbol) {
        final float diff = Math.round(Float.valueOf(DynamoUtils.getPropertyForSymbol(symbol, "3_diff", "value", table)));
        String freshness = DynamoUtils.getPropertyForSymbol(symbol, "3_diff", "status", table);
        final boolean status = (freshness == null) || freshness.equals("fresh");
        String chartUrl = String.format("https://ubiswal-website-contents.s3.amazonaws.com/%s_dark_large.jpg", symbol);
        EmptyTag chart = img().withSrc(chartUrl).withStyle("width: 800px; height: 300px;padding: 10px 10px 10px 10px;");
        ContainerTag chartInfo = td(
                table(
                        PageUtils.getSymbolInfo(symbol, diff, 20, status),
                        tr(td(chart)).withStyle("height: 360px; width: 850px; outline: thin solid; padding-left: 10px;")
                ).withStyle("height: 400px; width: 850px;")
        ).withStyle("height: 550px; padding-left: 100px;");
        ContainerTag analyticsInfo = getAnalyticsSummary(symbol);
        return tr(chartInfo, analyticsInfo).withStyle("width: 100%; background-color: #2B2D32");
    }

    private ContainerTag getAnalyticsSummary(String symbol) {
        final float maxPrice = Math.round(Float.valueOf(DynamoUtils.getPropertyForSymbol(symbol, "1_maxprice", "value", table)));
        final float maxPft = Math.round(Float.valueOf(DynamoUtils.getPropertyForSymbol(symbol, "2_bestProfit", "value", table).split(";")[0]));

        ContainerTag maxPriceText = p().withText(String.format("Last seen best stock price was %.2f", maxPrice))
                .withStyle("font-family:Roboto Condensed, sans-serif; color: #009036; font-size: 30px;");
        ContainerTag maxPftText = p().withText(String.format("Maximum profit possible in last 24 hours was %.2f", maxPft))
                .withStyle("font-family:Roboto Condensed, sans-serif; color: #009036; font-size: 30px;");
        ContainerTag info = td(
                maxPriceText,
                br(),
                maxPftText
        ).withStyle("height: 550px; padding-left: 100px;");
        return info;
    }

    private void refreshHtml(String symbol) {
        System.out.println("Refreshing home page");
        try {
            ContainerTag pageHead = head(
                    title(String.format("%s stock Info", symbol)),
                    meta().withCharset("utf-8"),
                    link().withHref("https://fonts.googleapis.com/css2?family=Noto+Serif&family=Roboto+Condensed&display=swap").withRel("stylesheet")
            );
            ContainerTag heading = h1().withText(symbol)
                    .withStyle("width: 100%; height: 50px; padding-left: 10px; padding-top: 10px; margin: 0; font-family:Roboto Condensed, sans-serif; font-size:40px; color: #2DC925; background-color: black;");
            ContainerTag b = body(
                    heading,
                    table(
                            getBanner(symbol)
                    ).withStyle("position:relative; width: 100%; border-spacing: 0;").attr("border", "0")
            ).withStyle("background-image: url('https://ubiswal-website-contents.s3.amazonaws.com/stock-header.svg'); object-fit: fill;  padding: 0; margin: 0;");
            String content = html(pageHead, b).render();
            pages.get(symbol).set(content);
        } catch (Exception e) {
            System.out.println(String.format("Failed to fetch data for %s", symbol));
            e.printStackTrace();
            if(pages.get(symbol).get() == null) {
                pages.get(symbol).set(PageUtils.brokenHtml());
            }
        }
    }

    private void refreshAllHtmls() {
        for (String symbol : symbols) {
            refreshHtml(symbol);
        }
    }

    public void symbolHanler(HttpExchange exchange) throws IOException {
        String url = exchange.getRequestURI().toString();
        String parts[] = url.split("/");
        String symbol = parts[parts.length -1];
        if (!pages.containsKey(symbol)) {
            String response = html(
                    body(
                            h1("Page not found!!")
                    )
            ).render();
            exchange.sendResponseHeaders(404, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            String response = pages.get(symbol).get();
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
