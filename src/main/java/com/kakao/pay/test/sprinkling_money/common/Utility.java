package com.kakao.pay.test.sprinkling_money.common;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.UUID;

public class Utility {

    /***
     * add tx_id
     ***/
    public static void generateTxId(RoutingContext routingContext) {
        routingContext.put("tx_id", UUID.randomUUID().toString());
        routingContext.next();
    }

    public static JsonArray list2JsonArray(List<JsonArray> data) {

        JsonArray items = new JsonArray();
        try {
            for (JsonArray s : data) {
                items.add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }
}
