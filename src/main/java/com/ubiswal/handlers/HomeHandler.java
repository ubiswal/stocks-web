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
        heading = h1().withText("$tock Digest")
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

    private ContainerTag getSymbolInfo(final String symbol, float diff, int heightInPx) {
        String textStyle = String.format("font-family:Roboto Condensed, sans-serif; color:%s; font-size: %spx;", diff > 0?"#009036": "#99220E", heightInPx);
        EmptyTag arrowIcon = img().withSrc(String.format("https://ubiswal-website-contents.s3.amazonaws.com/%s.png", diff > 0? "up": "down"))
                .withStyle(String.format("height: %spx; width: %spx;", heightInPx, heightInPx)); // This is a square
        ContainerTag diffText = p().withText(String.format("$%s", diff))
                .withStyle(textStyle);
        ContainerTag nameText = p().withText(symbol)
                .withStyle(textStyle);
        return tr(td(
                button(nameText).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;"),
                button(arrowIcon).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;"),
                button(diffText).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;")
        )).withStyle("background-color: black;");
    }

    private List<ContainerTag> getNewsArticlesForSymbol(String sym, int max, int heightInPx, int fontSizeInPx, int maxChars, boolean dark) {
        List<News> allStories = getNewsArticlesForSymbol(sym);
        List<News> someStories = allStories.subList(0, Math.min(max, allStories.size()));
        List<ContainerTag> storyTable = new ArrayList<>();
        for (News story : someStories) {
            storyTable.add(
                    tr(
                            td(img().withSrc(story.getUrlToImage()).withStyle(String.format("height: %spx; width: %spx; object-fit: cover;", heightInPx, heightInPx))),
                            td(button().withText(story.getTitle().substring(0, Math.min(maxChars, story.getTitle().length())) +(story.getTitle().length() > maxChars? "...": ""))
                                    .attr("onclick", String.format(" window.open('%s', '_blank'); return false;", story.getUrl()))
                                    .attr("target", "_blank")
                                    .withStyle(String.format("font-family:Roboto Condensed, sans-serif; color:black; font-size: %spx; background-color: Transparent;background-repeat:no-repeat;cursor:pointer;overflow: hidden; border: none; height: %spx; text-align: left; color: %s", fontSizeInPx, heightInPx, dark? "white": "black"))
                            )).withStyle(String.format("height: %spx; outline: thin solid; margin-bottom: 10px;", heightInPx))
            );
        }
        return storyTable;
    }

    /**
     * This section will contain the Dow jones chart and some related news articles.
     * @return
     */
    private ContainerTag getDjiBanner() {
        final float diff = Math.round(Float.valueOf(getPropertyForSymbol("DJI", "3_diff", "value")));
        EmptyTag chart = img().withSrc("https://ubiswal-website-contents.s3.amazonaws.com/DJI_dark.jpg").withStyle("width: 800px; height: 300px;padding: 10px 10px 10px 10px;");
        ContainerTag info = td(
                table(
                        getSymbolInfo("DJI", diff, 20),
                        tr(td(chart)).withStyle("height: 360px; width: 850px; outline: thin solid; padding-left: 10px;")
                ).withStyle("height: 400px; width: 850px;")
        ).withStyle("height: 550px; padding-left: 100px;");

        EmptyTag newsIcon = img().withSrc("https://ubiswal-website-contents.s3.amazonaws.com/news-icon.png").withStyle("width: 100px; height: 100px; object-fit: cover;");
        List<ContainerTag> newsTable = new ArrayList<>();
        newsTable.add(tr(
                td(newsIcon).withStyle("width: 100px"),
                td(h2().withText("News for DJI").withStyle("font-family:Roboto Condensed, sans-serif; color:black; font-size: 30px; color: white"))
        ));
        newsTable.addAll(getNewsArticlesForSymbol("DJI", 3, 100, 18, 150, true));

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
            if (symbol.equals("DJI")) {
                continue;
            }
            final float diff = Math.round(Float.valueOf(getPropertyForSymbol(symbol, "3_diff", "value")));
            charts.add(getSymbolInfo(symbol, diff, 20));
            charts.add(tr(td(
                    img()
                            .withSrc(String.format("https://ubiswal-website-contents.s3.amazonaws.com/%s_light.jpg", symbol))
                            .withStyle("width: 300px; height: 300px;")
            )));
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
            stories.addAll(getNewsArticlesForSymbol(symbol, 3, 100, 18, 800, false));
        }
        return td(
                table(
                        stories.toArray(new ContainerTag[0])
                )
        );
    }

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
                        getDjiBanner(),
                        tr(
                                getNewsLists().withStyle("width: 70%; padding-left: 50px;"),
                                getTicker().withStyle("width: 29%;padding-left: 100px; margin-top: 50px;")
                        )
                ).withStyle("position:relative; width: 100%; border-spacing: 0;").attr("border", "0")
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
