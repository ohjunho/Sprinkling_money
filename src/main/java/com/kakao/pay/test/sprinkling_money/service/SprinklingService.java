package com.kakao.pay.test.sprinkling_money.service;

import com.kakao.pay.test.sprinkling_money.common.Constants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Random;

public class SprinklingService extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(SprinklingService.class);

    private static final long MINUTES_10 = 10 * 60 * 1000L;

    /**
     * 잔액 체크 없음
     * Token 생성
     * 요청자가 뿌린 시점 기준으로 대상자 한정
     * 뿌릴 금액을 뿌릴 인원으로 랜덤하게 분배
     */
    @Override
    public void start(Promise<Void> startPromise) {
        try {
            // receive data
            vertx.eventBus().consumer(Constants.EB_SPRINKLING, this::sprinklingHandler);
            startPromise.complete();
        } catch (Exception e) {
            startPromise.fail(e);
        }
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        stop();
        stopPromise.complete();
    }

    // 주요 비즈 로직 처리
    private void sprinklingHandler(Message<JsonObject> msg) {

        try {
            JsonObject data = msg.body();

            // 사용된(제외할) 토큰 가져오기
            vertx.eventBus().request(Constants.EB_SPRINKLING_GET_TOKEN_REPO,data,rs ->{
                if(rs.failed()){
                    msg.fail(1003, rs.cause().getMessage());
                    return;
                }
                log.info("rs : {}",rs.result().body());

                JsonArray result = new JsonArray();
                if(rs.result().body()!=null) {
                    result = (JsonArray) rs.result().body();
                }
                String token = makeToken(result);

                JsonArray divided_money = makeSprinklingMoney(data);

                data.put("token", token);
                data.put("divided_money", divided_money);
                data.put("create_time", LocalDateTime.now().toString());

                log.debug("sprinklingHandler -> data: {}", data);

                vertx.eventBus().request(Constants.EB_SPRINKLING_REPO,data,request ->{
                    if (request.succeeded()) {
                        msg.reply(new JsonObject().put("token",token));
                    } else {
                        msg.fail(1003, request.cause().getMessage());
                    }
                });
            });

        } catch (Exception e) {
            e.printStackTrace();
            msg.fail(1004, e.getMessage());
        }
    }

    // 토큰 만들기
    private String makeToken(JsonArray exceptionTokenArray) {

        int tokenNumber = new Random().nextInt(999) + 1;
        DecimalFormat decimalFormat = new DecimalFormat("#000");
        String token = decimalFormat.format(tokenNumber);

        if(exceptionTokenArray.contains(token)){
            token = makeToken(exceptionTokenArray);
        }

        return decimalFormat.format(tokenNumber);
    }

    // 할당 된 인원만큼 미리 랜덤 값을 분배함
    private JsonArray makeSprinklingMoney(JsonObject data) {

        int total_amount = data.getInteger("total_amount");
        int target_count = data.getInteger("target_count");

        int left_money = total_amount;

        JsonArray result = new JsonArray();
        // 남은 돈에서 (전체 인원수-1) 만큼의 배분 값을 먼저 할당한다.
        for (int i = 0; i < target_count - 1; i++) {
            int amount = randomRange(1, left_money);
            left_money = left_money - amount;
            result.add(amount);
        }
        // 남은 돈을 마지막 인원에게 배분한다.
        result.add(left_money);

        return result;
    }

    // 남은 돈 범위 내 금액 지정
    private int randomRange(int n1, int n2) {
        return (int) Math.abs((Math.random() * (n2 - n1 + 1)) + n1);
    }
}
