package com.ubiswal.utils;

import com.amazonaws.services.dynamodbv2.document.Table;
import com.ubiswal.handlers.News;
import j2html.tags.ContainerTag;
import j2html.tags.EmptyTag;

import java.util.ArrayList;
import java.util.List;

import static j2html.TagCreator.*;
import static j2html.TagCreator.button;

public class PageUtils {
    public static ContainerTag getSymbolInfo(final String symbol, float diff, int heightInPx, boolean fresh) {
        System.out.println(String.format("Data for symbo %s is %s", symbol, fresh? "fresh":"stale"));
        String textStyle = String.format("font-family:Roboto Condensed, sans-serif; color:%s; font-size: %spx;", diff > 0?"#009036": "#99220E", heightInPx);
        EmptyTag arrowIcon = img().withSrc(String.format("https://ubiswal-website-contents.s3.amazonaws.com/%s.png", diff > 0? "up": "down"))
                .withStyle(String.format("height: %spx; width: %spx;", heightInPx, heightInPx)); // This is a square
        ContainerTag diffText = p().withText(String.format("$%s", diff))
                .withStyle(textStyle);
        ContainerTag nameText = p().withText(symbol)
                .withStyle(textStyle);
        if (!fresh) {
            EmptyTag staleSymbol = img().withSrc("https://ubiswal-website-contents.s3.amazonaws.com/exclaimation.png")
                    .withStyle(String.format("height: %spx; width: %spx;", heightInPx, heightInPx))
                    .attr("title", "This data is more than a day old due to limitations from our providers.");
            return tr(td(
                    button(nameText).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;"),
                    button(arrowIcon).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;"),
                    button(diffText).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;"),
                    button(staleSymbol).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;")
            )).withStyle("background-color: black;");
        } else {
            return tr(td(
                    button(nameText).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;"),
                    button(arrowIcon).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;"),
                    button(diffText).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;")
            )).withStyle("background-color: black;");
        }
    }

    public static ContainerTag getSymbolInfo(final String symbol, float diff, int heightInPx, String href, boolean fresh) {
        String textStyle = String.format("font-family:Roboto Condensed, sans-serif; color:%s; font-size: %spx;", diff > 0?"#009036": "#99220E", heightInPx);
        EmptyTag arrowIcon = img().withSrc(String.format("https://ubiswal-website-contents.s3.amazonaws.com/%s.png", diff > 0? "up": "down"))
                .withStyle(String.format("height: %spx; width: %spx;", heightInPx, heightInPx)); // This is a square
        ContainerTag diffText = p().withText(String.format("$%s", diff))
                .withStyle(textStyle);
        ContainerTag nameText = p().withText(symbol)
                .withStyle(textStyle);
        ContainerTag clickText = p().withText("  read more...")
                .withStyle(textStyle);
        if (!fresh) {
            EmptyTag staleSymbol = img().withSrc("https://ubiswal-website-contents.s3.amazonaws.com/exclaimation.png")
                    .withStyle(String.format("height: %spx; width: %spx;", heightInPx, heightInPx))
                    .attr("title", "This data is more than a day old due to limitations from our providers.");
            return tr(td(
                    button(nameText).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;"),
                    button(arrowIcon).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;"),
                    button(diffText).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;"),
                    button(staleSymbol).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;"),
                    button(clickText).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;")
                            .attr("onclick", String.format(" window.open('%s', '_blank'); return false;", href))
            )).withStyle("background-color: black;");
        } else {
            return tr(td(
                    button(nameText).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;"),
                    button(arrowIcon).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;"),
                    button(diffText).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;"),
                    button(clickText).withStyle("background-color: Transparent;background-repeat:no-repeat;border: none;cursor:pointer;overflow: hidden;")
                            .attr("onclick", String.format(" window.open('%s', '_blank'); return false;", href))
            )).withStyle("background-color: black;");
        }
    }

    public static List<ContainerTag> getNewsArticlesForSymbol(String sym, int max, int heightInPx, int fontSizeInPx, int maxChars, boolean dark, Table table) {
        List<News> allStories = DynamoUtils.getNewsArticlesForSymbol(sym, table);
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

    public static String brokenHtml() {
        return html(
                body(
                   h1("Oops! Something is broken. Please retry after some time.").withStyle("text-align: center; color: white")
                ).withStyle("background-color: blue;")
        ).render();
    }

}
