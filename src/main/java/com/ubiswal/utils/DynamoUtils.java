package com.ubiswal.utils;

import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.ubiswal.handlers.News;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DynamoUtils {
    public static List<News> getNewsArticlesForSymbol(String symbol, Table table) {
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

    public static String getPropertyForSymbol(String symbol, String type, String column, Table table) {
        GetItemSpec spec = new GetItemSpec().withPrimaryKey(new KeyAttribute("symb", symbol), new KeyAttribute("type", type));
        Item item = table.getItem(spec);
        if (item == null) {
            System.out.println(String.format("Could not fetch %s for %s", type, symbol));
            return null;
        } else {
            return item.asMap().get(column).toString();
        }
    }

}
