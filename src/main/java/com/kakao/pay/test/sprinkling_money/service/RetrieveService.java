package com.kakao.pay.test.sprinkling_money.service;

import com.kakao.pay.test.sprinkling_money.common.Constants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetrieveService extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(RetrieveService.class);

    /**
     * 잔액 체크 없음
     * Token 생성
     * 요청자가 뿌린 시점 기준으로 대상자 한정
     * 뿌릴 금액을 뿌릴 인원으로 랜덤하게 분배
     * */
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        try{
            // receive data
            vertx.eventBus().consumer(Constants.EB_RETRIEVE, this::retrieveHandler);

            startPromise.complete();
        }catch(Exception e){
            startPromise.fail(e);
        }
    }
    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        stop();
        stopPromise.complete();
    }


    private void retrieveHandler(Message<JsonObject> msg) {
        JsonObject data = msg.body();

        log.debug("retrieveHandler -> data: {}", data);

        vertx.eventBus().request(Constants.EB_RETRIEVE_REPO,data,rs ->{
            log.info("rs : {}",rs.result().body());

            JsonObject result = new JsonObject();
            if(rs.result().body()!=null) {
                result = makeData((JsonArray) rs.result().body());
            }
            msg.reply(result);
        });

    }

    private JsonObject makeData(JsonArray src){
        JsonObject result = new JsonObject();

        if(src!=null && src.size()>0) {

            result.put("total_amount", src.getJsonArray(0).getInteger(0));
            result.put("target_count", src.getJsonArray(0).getInteger(1));
            result.put("created_time", src.getJsonArray(0).getInstant(2));
            JsonArray users = new JsonArray();
            int amount = 0;

            for (Object item : src) {
                JsonArray jsonArray = (JsonArray) item;
                users.add( new JsonObject().put(jsonArray.getString(3), jsonArray.getInteger(4)));
                result.put("users", users);
                amount += jsonArray.getInteger(4);
            }
            result.put("sprinkling_amount", amount);
        }
        return result;
    }
}
