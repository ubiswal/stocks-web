package com.ubiswal.handlers;

import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.sun.net.httpserver.HttpExchange;
import j2html.tags.ContainerTag;
import j2html.tags.EmptyTag;
import lombok.Getter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import static j2html.TagCreator.*;

class News {
    @Getter
    private String title;

    @Getter
    private String urlToImage;

    @Getter
    private String url;

    News(String title, String url, String urlToImage) {
        this.title = title;
        this.urlToImage = urlToImage;
        this.url = url;
    }
}

public class HomeHandler {
    private final List<String> symbols;
    private final Table table;
    private final ContainerTag pageHead;
    private final ContainerTag heading;

    public HomeHandler(List<String> symbols, Table table) {
        this.symbols = symbols;
        this.table = table;
        pageHead = head(
                title("Stock Info"),
                meta().withCharset("utf-8"),
                link().withHref("https://fonts.googleapis.com/css2?family=Noto+Serif&family=Roboto+Condensed&display=swap").withRel("stylesheet")
        );
        heading = h1().withText("Mamun's stock digest")
                .withStyle("width: 100%; height: 100px; padding-left: 10px; padding-top: 10px; margin: 0; font-family:Roboto Condensed, sans-serif; font-size:40px; color: #2DC925; background-color: black;");
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

    private List<News> getNewsArticlesForSymbol(String symbol) {
        try {
            System.out.println("Getting news articles");
            QuerySpec spec = new QuerySpec()
                    .withKeyConditionExpression("symb = :sym and begins_with(#type, :news)")
                    .withValueMap(
                            new ValueMap()
                                    .withString(":sym", symbol)
                                    .withString(":news", "100_")
                    )
                    .withNameMap(
                            new NameMap().with("#type", "type")
                    );
            ItemCollection<QueryOutcome> items = table.query(spec);

            List<News> newsArticles = new ArrayList<>();
            Iterator<Item> it = items.iterator();
            while (it.hasNext()) {
                Item item = it.next();
                Map<String, Object> newsProperties = item.asMap();
                newsArticles.add(
                        new News(newsProperties.get("desc").toString(), newsProperties.get("url").toString(), newsProperties.get("url2image").toString())
                );
            }
            return newsArticles;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private ContainerTag getDjiBanner() {
        final float diff = Math.round(Float.valueOf(getPropertyForSymbol("DJI", "3_diff", "value")));
        //EmptyTag stockIcon = img().withSrc("https://ubiswal-website-contents.s3.amazonaws.com/stock-icon.png").withStyle("width: 100px; height: 100px; object-fit: contain");
        EmptyTag chart = img().withSrc("https://ubiswal-website-contents.s3.amazonaws.com/DJI_light.jpg").withStyle("width: 800px; height: 300px;padding: 10px 10px 10px 10px;");
        EmptyTag arrowIcon = img().withSrc(String.format("https://ubiswal-website-contents.s3.amazonaws.com/%s.png", diff > 0? "up": "down"))
                .withStyle("height: 20px; width: 20px;");
        ContainerTag diffText = p().withText(String.format("%s$%s",diff > 0? "+":"-", Float.toString(diff)))
                .withStyle(String.format("font-family:Roboto Condensed, sans-serif; color:%s; font-size: 20px;", diff > 0?"#009036": "#99220E"));
        ContainerTag nameText = p().withText("DJI")
                .withStyle(String.format("font-family:Roboto Condensed, sans-serif; color:%s; font-size: 20px;", diff > 0?"#009036": "#99220E"));

        ContainerTag info = td(
                table(
                        tr(
                                td(
                                        button(nameText).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;"),
                                        button(arrowIcon).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;"),
                                        button(diffText).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;")
                                )
                        ),
                        tr(td(chart)).withStyle("height: 360px; width: 850px; outline: thin solid; padding-left: 10px;")
                ).withStyle("height: 400px; width: 850px;")
        ).withStyle("height: 550px; width: 40%;");

        EmptyTag newsIcon = img().withSrc("https://ubiswal-website-contents.s3.amazonaws.com/news-icon.png").withStyle("width: 100px; height: 100px; object-fit: cover;");
        List<News> stories = getNewsArticlesForSymbol("DJI").subList(0, 3);
        ContainerTag news = td(
                table(
                        tr(
                                td(newsIcon).withStyle("width: 100px"),
                                td(h2().withText("Related news").withStyle("font-family:Roboto Condensed, sans-serif; color:black; font-size: 30px;"))
                        ),
                        tr(
                                td(img().withSrc(stories.size() > 0? stories.get(0).getUrlToImage() : "").withStyle("height: 100px; width: 100px; object-fit: cover;")),
                                td(button().withText(stories.size() > 0? stories.get(0).getTitle()/*.substring(0, Math.min(stories.get(0).getTitle().length(), 50)) + "..."*/: "")
                                .withStyle("font-family:Roboto Condensed, sans-serif; color:black; font-size: 18px; background-color: Transparent;background-repeat:no-repeat;cursor:pointer;overflow: hidden; border: none; height: 70px; margin-bottom: 10px; text-align: left")
                                .attr("onclick", String.format("window.location.href = '%s';", stories.size() > 0 ? stories.get(0).getUrl(): ""))
                        )).withStyle("height: 80px; outline: thin solid"),

                        tr(
                                td(img().withSrc(stories.size() > 1? stories.get(1).getUrlToImage() : "").withStyle("height: 100px; width: 100px; object-fit: cover;")),
                                td(button().withText(stories.size() > 1? stories.get(1).getTitle()/*.substring(0, Math.min(stories.get(0).getTitle().length(), 50)) + "..."*/: "")
                                .withStyle("font-family:Roboto Condensed, sans-serif; color:black; font-size: 18px; background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden; height: 70px; margin-bottom: 10px; text-align: left")
                                .attr("onclick", String.format("window.location.href = '%s';", stories.size() > 1 ? stories.get(1).getUrl(): ""))
                        )).withStyle("height: 80px; outline: thin solid"),
                        tr(
                                td(img().withSrc(stories.size() > 2? stories.get(2).getUrlToImage() : "").withStyle("height: 100px; width: 100px; object-fit: cover;")),
                                td(button().withText(stories.size() > 2? stories.get(2).getTitle()/*.substring(0, Math.min(stories.get(0).getTitle().length(), 50)) + "..."*/: "")
                                .withStyle("font-family:Roboto Condensed, sans-serif; color:black; font-size: 18px; background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden; height: 70px; margin-bottom: 10px; text-align: left")
                                .attr("onclick", String.format("window.location.href = '%s';", stories.size() > 2 ? stories.get(2).getUrl(): ""))
                        )).withStyle("height: 80px; outline: thin solid")
                )
        ).withStyle("height: 550px; width: 40%; padding-left: 20px; padding-top: 10px;");
        return tr(info, news).withStyle("width: 100%");
    }

    /*private ContainerTag getContainerForSymbol(String symbol, String maxProfit, String diff, String highest) {
        String[] parts = maxProfit.split(";");
        String maxPft = parts[0];
        //String maxBuyIdx = parts[1];
        //String maxSellIdx = parts[2];
        ContainerTag imageDiv = div(
                (img().withSrc(String.format("https://ubiswal-website-contents.s3.amazonaws.com/%s_light.jpg", symbol)).withStyle("height: 200px; width: 200px;"))
        ).withStyle("padding-left: 10px;");

        return imageDiv;
    }

    private List<News> getNewsForSymbol(String symbol) {
        QuerySpec spec = new QuerySpec().withKeyConditionExpression("type <= 100");
        ItemCollection<QueryOutcome> items = table.query(spec);
        for (Item item : items) {
            Map<String, Object> newsProperties = item.asMap();
        }
        table().wi
    }

    private ContainerTag djiBanner() {
        float diff = Math.round(Float.valueOf(getPropertyForSymbol("DJI", "3_diff", "value")));
        String chartUrl = "https://ubiswal-website-contents.s3.amazonaws.com/DJI_light.jpg";
        ContainerTag info = div(
                p().withText(String.format("%s$%s",diff > 0? "+":"-", Float.toString(diff)))
                    .withStyle(String.format("font-family:Roboto Condensed, sans-serif; color:%s; font-size: 20px;", diff > 0?"#009036": "#99220E")),
                br(),
                img().withSrc(String.format("https://ubiswal-website-contents.s3.amazonaws.com/%s.png", diff > 0? "up": "down"))
                        .withStyle("height: 70px; width: 70px;")
        ).withStyle("padding-left: 20px; height: 300px; width: 200px; float: right; padding-right: 20px;");
        ContainerTag container = div(
                br(),
                img().withSrc(chartUrl).withSrc(chartUrl)
                        .withStyle("height: 300px; width: 800px;padding-left: 30px; box-shadow: 5px 5px 5px 5px gray; border-radius: 3px; float: left"),
                info
        ).withStyle("height: 360px; width: 1300px; margin-top: 20px; padding-left: 30px; padding-top: 30px; box-shadow: 5px 5px 5px 5px gray; border-radius: 3px; margin-bottom: 5vh; float: left");
        return container;
    }

    private ContainerTag djiNews() {
        ContainerTag container = div()
                .withStyle("float: right; height: 360px; width: 500px; box-shadow: 5px 5px 5px 5px gray; border-radius: 3px; margin-right: 20px; margin-top: 20px;");
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
                                img().withSrc(String.format("https://ubiswal-website-contents.s3.amazonaws.com/%s.png", Float.valueOf(diff) > 0? "up": "down"))
                                        .withStyle("height: 20px; width: 20px; padding-left: 2vw;  display:inline-block; vertical-align:top; padding-top: 3vh;"),
                                getContainerForSymbol(symbol, maxProfit, diff, maxPrice),
                                br(),
                                hr()
                        ).withStyle("padding-top: 10px; padding-right: 10px; margin-top: 700px;")
                );
            }
        }
        return div(bodyElements.toArray(new ContainerTag[0]))
                .withStyle("width: 400px; float: right; box-shadow: 5px 5px 5px 5px gray; border-radius: 3px; padding-left: 50px; margin-top: 3vh; margin-right: 3vw;");
    }*/

    public void homeHandler(HttpExchange exchange) throws IOException {
        //ContainerTag banner = djiBanner();
        //ContainerTag rightFloater = remStocksFloater();
        /*ContainerTag b = body(
                heading,
                h2("DJI")
                    .withStyle("height: 5vh; text-align:left; background: #1A3067; padding-top: 0px; padding-left:1vw; box-shadow: 1px 1px 1px 1px gray; color:#2EA231; font-family:Roboto Condensed, sans-serif; margin: 0"),
                banner,
                djiNews(),
                rightFloater
        ).withStyle("margin:0; padding:0; background-color: #FFFFFF;");*/
        ContainerTag b = body(
                heading,
                table(
                        getDjiBanner()
                ).withStyle("width: 95%;")
        ).withStyle("background-image: url('https://ubiswal-website-contents.s3.amazonaws.com/stock-header.svg'); object-fit: fill;  padding: 0; margin: 0;");
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

