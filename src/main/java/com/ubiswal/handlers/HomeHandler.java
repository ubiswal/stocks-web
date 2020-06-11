package com.ubiswal.handlers;

import com.amazonaws.services.dynamodbv2.document.*;
import com.sun.net.httpserver.HttpExchange;
import com.ubiswal.utils.DynamoUtils;
import com.ubiswal.utils.PageUtils;
import j2html.tags.ContainerTag;
import j2html.tags.EmptyTag;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static j2html.TagCreator.*;

public final class HomeHandler {
    private final List<String> symbols;
    private final Table table;

    private AtomicReference<String> page;

    public HomeHandler(List<String> symbols, Table table) {
        this.symbols = symbols;
        this.table = table;
        page = new AtomicReference<>();
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(new PageRefresh(5) {
            @Override
            void updateHtml() {
                refreshHtml();
            }
        });
    }

    /**
     * This section will contain the Dow jones chart and some related news articles.
     * @return
     */
    private ContainerTag getDjiBanner() {
        final float diff = Math.round(Float.valueOf(DynamoUtils.getPropertyForSymbol("DJI", "3_diff", "value", table)));
        String freshness = DynamoUtils.getPropertyForSymbol("DJI", "3_diff", "status", table);
        final boolean status = (freshness == null) || freshness.equals("fresh");
        EmptyTag chart = img().withSrc("https://ubiswal-website-contents.s3.amazonaws.com/DJI_dark_large.jpg").withStyle("width: 800px; height: 300px;padding: 10px 10px 10px 10px;");
        ContainerTag info = td(
                table(
                        PageUtils.getSymbolInfo("DJI", diff, 20, status),
                        tr(td(chart)).withStyle("height: 360px; width: 850px; outline: thin solid; padding-left: 10px;")
                ).withStyle("height: 400px; width: 850px;")
        ).withStyle("height: 550px; padding-left: 100px;");

        EmptyTag newsIcon = img().withSrc("https://ubiswal-website-contents.s3.amazonaws.com/news-icon.png").withStyle("width: 100px; height: 100px; object-fit: cover;");
        List<ContainerTag> newsTable = new ArrayList<>();
        newsTable.add(tr(
                td(newsIcon).withStyle("width: 100px"),
                td(h2().withText("News for DJI").withStyle("font-family:Roboto Condensed, sans-serif; color:black; font-size: 30px; color: white"))
        ));
        newsTable.addAll(PageUtils.getNewsArticlesForSymbol("DJI", 3, 100, 18, 150, true, table));

        ContainerTag news = td(
                table(
                        newsTable.toArray(new ContainerTag[0])
                )
        ).withStyle("height: 550px; padding-right: 50px; padding-top: 10px;");
        return tr(info, news).withStyle("width: 100%; background-color: #2B2D32");
    }

    private ContainerTag getTicker() {
        List<ContainerTag> charts = new ArrayList<>();
        for (String symbol : symbols) {
            try {
                if (symbol.equals("DJI")) {
                    continue;
                }
                final float diff = Math.round(Float.valueOf(DynamoUtils.getPropertyForSymbol(symbol, "3_diff", "value", table)));
                String freshness = DynamoUtils.getPropertyForSymbol(symbol, "3_diff", "status", table);
                final boolean status = (freshness == null) || freshness.equals("fresh");
                charts.add(PageUtils.getSymbolInfo(symbol, diff, 20, String.format("/sym/%s", symbol), status));
                charts.add(tr(td(
                        img()
                                .withSrc(String.format("https://ubiswal-website-contents.s3.amazonaws.com/%s_light.jpg", symbol))
                                .withStyle("width: 300px; height: 300px;")
                )));
            } catch (Exception e) {
                System.out.println(String.format("Failed to generate ticet for %s ", symbol));
                e.printStackTrace();
            }
        }
        return td(
                table(
                        charts.toArray(new ContainerTag[0])
                )
        ).withStyle("padding-top: 50px;");
    }

    private ContainerTag getNewsLists() {
        List<ContainerTag> stories = new ArrayList<>();
        EmptyTag newsIcon = img().withSrc("https://ubiswal-website-contents.s3.amazonaws.com/news-icon.png").withStyle("width: 100px; height: 100px; object-fit: cover;");
        stories.add(
                tr(
                        td(newsIcon).withStyle("width: 100px"),
                        td(h2().withText("News digest").withStyle("font-family:Roboto Condensed, sans-serif; color:black; font-size: 30px;"))
                )
        );
        for (String symbol : symbols) {
            if (symbol.equals("DJI")) {
                continue;
            }
            stories.addAll(PageUtils.getNewsArticlesForSymbol(symbol, 3, 100, 18, 800, false, table));
        }
        return td(
                table(
                        stories.toArray(new ContainerTag[0])
                )
        );
    }

    private void refreshHtml() {
        try {
            System.out.println("Refreshing home page");
            ContainerTag pageHead = head(
                    title("Stock Info"),
                    meta().withCharset("utf-8"),
                    link().withHref("https://fonts.googleapis.com/css2?family=Noto+Serif&family=Roboto+Condensed&display=swap").withRel("stylesheet")
            );
            ContainerTag heading = h1().withText("$tock Digest")
                    .withStyle("width: 100%; height: 50px; padding-left: 10px; padding-top: 10px; margin: 0; font-family:Roboto Condensed, sans-serif; font-size:40px; color: #2DC925; background-color: black;");
            ContainerTag accredation = p().withText("Powered by newsapi.org and alphavantage.com")
                    .withStyle("width: 100%; height: 40px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px; margin: 0; font-family:Roboto Condensed, sans-serif; font-size:18px; color: #2DC925; background-color: black;");

            ContainerTag b = body(
                    heading,
                    accredation,
                    table(
                            getDjiBanner(),
                            tr(
                                    getNewsLists().withStyle("width: 70%; padding-left: 50px;"),
                                    getTicker().withStyle("width: 29%;padding-left: 100px; margin-top: 50px;")
                            )
                    ).withStyle("position:relative; width: 100%; border-spacing: 0;").attr("border", "0")
            ).withStyle("background-image: url('https://ubiswal-website-contents.s3.amazonaws.com/stock-header.svg'); object-fit: fill;  padding: 0; margin: 0;");
            String content = html(pageHead, b).render();
            page.set(content);
            System.out.println("Successfully refreshed data for homepage.");
        } catch (Exception e) {
            System.out.println("Failed to fetch data for homepage");
            e.printStackTrace();
            if(page.get() == null) {
                System.out.println("Setting blank for homepage");
                page.set(PageUtils.brokenHtml());
            }
        }
    }

    public void homeHandler(HttpExchange exchange) throws IOException {
        String response = page.get();
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
