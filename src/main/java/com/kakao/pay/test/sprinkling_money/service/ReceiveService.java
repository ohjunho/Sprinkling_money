package com.kakao.pay.test.sprinkling_money.service;

import com.kakao.pay.test.sprinkling_money.common.Constants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ReceiveService extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(RetrieveService.class);

    /**
     * 잔액 체크 없음
     * Token 생성
     * 요청자가 뿌린 시점 기준으로 대상자 한정
     * 뿌릴 금액을 뿌릴 인원으로 랜덤하게 분배
     */
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        try {
            // receive data
            vertx.eventBus().consumer(Constants.EB_RECEIVE, this::receiveHandler);

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

    private void receiveHandler(Message<JsonObject> msg) {
        JsonObject data = msg.body();

        try {
            log.debug("receiveHandler -> data: {}", data);

            List<Future> getData = new ArrayList<>();
            getData.add(getUser(data));
            getData.add(getMoney(data));

            // 결과를 모아서..
            CompositeFuture.all(getData).onComplete(ar -> {
                if (ar.succeeded()) {

                    AtomicBoolean availableUser = new AtomicBoolean(false);
                    AtomicReference<JsonArray> money = new AtomicReference<>(new JsonArray());
                    AtomicInteger choose = new AtomicInteger();
                    AtomicInteger sprinkling_id = new AtomicInteger();

                    ar.result().<JsonArray>list().stream().forEach(r -> {
                        if (r.contains("user")) {
                            r.remove("user");
                            log.info("user -> " + r);
                            log.info("user -> " + r.size());

                            // 받지 않았다면
                            if(r.size()==2 && r.getInteger(1)==0) {
                                availableUser.set(true);
                                sprinkling_id.set(r.getInteger(0));
                            }
                        }
                        if (r.contains("money")) {
                            r.remove("money");
                            log.info("money -> " + r);
                            if(r.size()>0){
                                money.set(r);
                                choose.set(new Random().nextInt(r.size()));;
                            }
                        }
                    });

                    log.info("availableUser.get() -> " + availableUser.get());
                    log.info("money.get() -> " + money.get());

                    if(availableUser.get()==true && money.get()!=null && !money.get().isEmpty()) {
                        int receiveMoney = money.get().getInteger(choose.get());
                        data.put("receiveMoney", receiveMoney);
                        data.put("sprinkling_id", sprinkling_id.get());

                        // 저장
                        save(data).onComplete(rs -> {
                            if (rs.succeeded()) {
                                msg.reply(new JsonObject().put("receiveMoney", receiveMoney));
                            } else {
                                msg.fail(200000, rs.cause().getMessage());
                            }
                        });
                    }else{
                        msg.reply(null);
                    }
                } else {
                    msg.fail(200000, ar.cause().getMessage());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            msg.fail(200000, e.getMessage());
        }
    }

    public Future<JsonArray> getUser(JsonObject data) {
        Promise<JsonArray> promise = Promise.promise();
        vertx.eventBus().<JsonArray>request(Constants.EB_RECEIVE_USER_REPO, data, rs -> {
            if (rs.succeeded()) {

                JsonArray r = new JsonArray();
                if (rs.result().body() != null) {
                    r = rs.result().body();
                }
                r.add("user");
                promise.complete(r);
            } else {
                promise.fail(rs.cause());
            }
        });
        return promise.future();
    }

    public Future<JsonArray> getMoney(JsonObject data) {
        Promise<JsonArray> promise = Promise.promise();
        vertx.eventBus().<JsonArray>request(Constants.EB_RECEIVE_MONEY_REPO, data, rs -> {
            if (rs.succeeded()) {
                JsonArray r = new JsonArray();
                if (rs.result().body() != null) {
                    r = rs.result().body();
                }
                r.add("money");
                promise.complete(r);
            } else {
                promise.fail(rs.cause());
            }
        });
        return promise.future();
    }

    public Future<JsonArray> save(JsonObject data) {
        Promise<JsonArray> promise = Promise.promise();
        vertx.eventBus().<JsonArray>request(Constants.EB_RECEIVE_SAVE_REPO, data, rs -> {
            if (rs.succeeded()) {

                JsonArray r = new JsonArray();
                if (rs.result().body() != null) {
                    r = rs.result().body();
                }
                promise.complete(r);
            } else {
                promise.fail(rs.cause());
            }
        });
        return promise.future();
    }
}
