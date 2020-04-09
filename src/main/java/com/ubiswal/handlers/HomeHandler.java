package com.ubiswal.handlers;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.sun.net.httpserver.HttpExchange;
import j2html.tags.ContainerTag;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static j2html.TagCreator.*;

public class HomeHandler {
    private List<String> symbols;
    private Table table;
    private ContainerTag pageHead;
    private ContainerTag heading;

    public HomeHandler(List<String> symbols, Table table) {
        this.symbols = symbols;
        this.table = table;
        pageHead = head(
                title("Stock Info"),
                meta().withCharset("utf-8"),
                link().withHref("https://fonts.googleapis.com/css2?family=Noto+Serif&family=Roboto+Condensed&display=swap").withRel("stylesheet"),
                style()
                        .withText("#heading {height: 5vh; text-align:left; background: black; padding-top: 1vh; padding-left:1vw; box-shadow: 1px 1px 1px 1px gray; color:#2EA231; font-family:Roboto Condensed, sans-serif}")
                        .withText("#datacard {padding-left: 1vw; padding-top: 3vh; font-family:Roboto Condensed, sans-serif; color:#2EA231; font-size: 2vh;}")
                        .withText("#symbol {font-family:Noto Serif, serif; font-size: 3vh; color: #E12931; padding-left: 2vw;}")
        );
        heading = h1("Mamun's stock digest").withId("heading");
    }

    private String getPropertyForSymbol(String symbol, String type, String column) {
        GetItemSpec spec = new GetItemSpec().withPrimaryKey(new KeyAttribute("symb", symbol), new KeyAttribute("type", type));
        Item item = table.getItem(spec);
        if (item == null) {
            System.out.println(String.format("Could not fetch %s for %s", type, symbol));
            return null;
        } else {
            return item.asMap().get(column).toString();
        }
    }

    private ContainerTag getContainerForSymbol(String symbol, String maxProfit, String diff, String highest) {
        String[] parts = maxProfit.split(";");
        String maxPft = parts[0];
        //String maxBuyIdx = parts[1];
        //String maxSellIdx = parts[2];
        ContainerTag imageDiv = div(
                (img().withSrc(String.format("https://ubiswal-website-contents.s3.amazonaws.com/%s_light.jpg", symbol)).withStyle("height: 300px; width: 300px;"))
        ).withStyle("float: left; padding-left: 2vw; width:25vw;");

        return imageDiv;
    }

    private ContainerTag djiBanner() {
        float diff = Math.round(Float.valueOf(getPropertyForSymbol("DJI", "3_diff", "value")));
        String chartUrl = "https://ubiswal-website-contents.s3.amazonaws.com/DJI_light.jpg";
        ContainerTag info = div(
                p().withText(String.format("%s$%s",diff > 0? "+":"-", Float.toString(diff)))
                    .withStyle(String.format("font-family:Roboto Condensed, sans-serif; color:%s; font-size: 50px;", diff > 0?"#009036": "#99220E")),
                br(),
                img().withSrc(String.format("https://ubiswal-website-contents.s3.amazonaws.com/%s.png", diff > 0? "up": "down")).withStyle("height: 70px; width: 70px; padding-left: 2vw;")
        ).withStyle("padding-left: 1vw; height: 300px; width: 200px; float: right");
        ContainerTag container = div(
                br(),
                img().withSrc(chartUrl).withSrc(chartUrl).withStyle("height: 300px; width: 800px;padding-left: 3vw; margin-left: 3vw; box-shadow: 5px 5px 5px 5px gray; border-radius: 3px; float: left"),
                info
        ).withStyle("height: 360px; width: 70%; margin-left: 15%; padding-left: 30px; padding-top: 30px; box-shadow: 5px 5px 5px 5px gray; border-radius: 3px; margin-bottom: 5vh;");
        return container;
    }

    private ContainerTag remStocksFloater() {

        List<ContainerTag> bodyElements = new ArrayList<>();
        for (String symbol : symbols) {
            if (symbol.equals("DJI")) {
                continue;
            }
            System.out.println("Fetching max price for " + symbol);

            String  maxPrice = getPropertyForSymbol(symbol, "1_maxprice", "value");
            String maxProfit = getPropertyForSymbol(symbol, "2_bestProfit", "value");
            String diff =  getPropertyForSymbol(symbol, "3_diff", "value");

            if (maxPrice == null || maxProfit == null || diff == null) {
                continue;
            } else {
                bodyElements.add(
                        div(
                                p(symbol).withStyle("font-family:Noto Serif, serif; font-size: 3vh; color: #E12931; display:inline-block; vertical-align:top;"),
                                img().withSrc(String.format("https://ubiswal-website-contents.s3.amazonaws.com/%s.png", Float.valueOf(diff) > 0? "up": "down")).withStyle("height: 20px; width: 20px; padding-left: 2vw;  display:inline-block; vertical-align:top; padding-top: 3vh;"),
                                getContainerForSymbol(symbol, maxProfit, diff, maxPrice),
                                br(),
                                hr()
                        ).withStyle("padding-top: 10px; padding-right: 10px;")
                );
            }
        }
        return div(bodyElements.toArray(new ContainerTag[0]))
                .withStyle("width: 400px; float: right; box-shadow: 5px 5px 5px 5px gray; border-radius: 3px; padding-left: 50px; margin-top: 3vh; margin-right: 3vw;");
    }

    public void homeHandler(HttpExchange exchange) throws IOException {
        ContainerTag banner = djiBanner();
        ContainerTag rightFloater = remStocksFloater();

        ContainerTag b = body(
                heading,
                p("DOW Jones").withId("symbol"),
                banner,
                hr(),
                rightFloater
        ).withStyle("margin:0; padding:0; background-color: #FFFFFF");
        String response = html(pageHead, b).render();
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
