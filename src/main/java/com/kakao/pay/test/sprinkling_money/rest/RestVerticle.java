package com.kakao.pay.test.sprinkling_money.rest;

import com.kakao.pay.test.sprinkling_money.common.Constants;
import com.kakao.pay.test.sprinkling_money.common.Utility;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class RestVerticle extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(RestVerticle.class);

    private static final String VERSION = "/v1";

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        // get Config
        Integer port = config().getInteger("port", 8080);
        Integer timeout = config().getInteger("timeout", 30);
        Integer BodyLimitSize = config().getInteger("bodyLimitSize", 5120); //default 5 k-byte

        // get Validation Scheme
        ValidationData validationData = new ValidationData(vertx);
        HTTPRequestValidationHandler sprinklingValidationHandler = validationData.getValidation("config/validation/sprinkling_scheme");

        // REST router
        final Router router = Router.router(vertx);

        // body size 제한
        // 모든 유입 데이터에 대한 고유키 추가
        router.route()
                .handler(BodyHandler.create().setBodyLimit(BodyLimitSize))
                .handler(Utility::generateTxId);

        // 뿌리기
        router.post(VERSION+"/sprinkling")
                .handler(sprinklingValidationHandler)
                .handler(this::handleSprinkling)
                .handler(TimeoutHandler.create(timeout));

        // 받기
        router.get(VERSION+"/receive")
                .handler(this::handleReceive)
                .handler(TimeoutHandler.create(timeout));

        // 조회
        router.get(VERSION+"/retrieve")
                //.handler(retrieveValidationHandler)
                .handler(this::handleRetrieve)
                .handler(TimeoutHandler.create(timeout));

        // Manage the validation failure for all routes in the router
        router.errorHandler(400, routingContext -> {
            if (routingContext.failure() instanceof ValidationException) {
                // Something went wrong during validation!
                String validationErrorMessage = routingContext.failure().getMessage();
                routingContext.response().setStatusCode(400).setStatusMessage(validationErrorMessage).end();
            } else {
                // Unknown 400 failure happened
                routingContext.response().setStatusCode(400).setStatusMessage("Unknown...").end();
            }
        });

        // 서버 기동
        vertx.createHttpServer().requestHandler(router).listen(port, res -> {
            if (res.failed()) {
                res.cause().printStackTrace();
                startPromise.fail(res.cause());
            } else {
                System.out.println("Server listening port : " + port);
                startPromise.complete();
            }
        });
    }
    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        stop();
        stopPromise.complete();
    }

    // Sprinkling
    private void handleSprinkling(RoutingContext routingContext) {
        String txId = routingContext.get("tx_id");
        String roomId = routingContext.request().getHeader("X-ROOM-ID");
        String requester = routingContext.request().getHeader("X-USER-ID");

        JsonObject body = routingContext.getBody().toJsonObject();

        Integer total_amount = body.getInteger("total_amount");
        Integer target_count = body.getInteger("target_count");
        JsonArray user_list = body.getJsonArray("user_list");

        if(target_count > user_list.size()){
            routingContext.response().setStatusCode(400).setStatusMessage("target_count cannot be greater than the number of users").end();
            return;
        }
        JsonObject data = new JsonObject();
        data.put("txId",txId);
        data.put("roomId",roomId);
        data.put("requester",requester);
        data.put("total_amount",total_amount);
        data.put("target_count",target_count);
        data.put("user_list",user_list);

        vertx.eventBus().request(Constants.EB_SPRINKLING, data, msg -> {
            if (msg.succeeded()) {
                JsonObject result = (JsonObject)msg.result().body();

                log.info("msg -> {}",msg.result().body());

                sendResponse(routingContext, 201, result);

            } else {
                sendResponse(routingContext, 500, msg.cause().getMessage());
            }
        });

    }

    // Receive
    private void handleReceive(RoutingContext routingContext) {

        String txId = routingContext.get("tx_id");
        String roomId = routingContext.request().getHeader("X-ROOM-ID");
        String receiver = routingContext.request().getHeader("X-USER-ID");
        String token = routingContext.request().getHeader("X-TOKEN");

        log.debug("txId: {}, roomId: {}, receiver: {}, token: {}"
                , txId, roomId, receiver, token);

        JsonObject data = new JsonObject();
        data.put("txId",txId);
        data.put("roomId",roomId);
        data.put("receiver",receiver);
        data.put("token",token);

        vertx.eventBus().<JsonObject>request(Constants.EB_RECEIVE, data, msg -> {
            if (msg.succeeded()) {
                JsonObject result = msg.result().body();
                log.info("msg -> {}",result);

                int statusCode = (result==null || result.isEmpty())?204:200;
                sendResponse(routingContext, statusCode, result);
            } else {
                sendResponse(routingContext, 500, msg.cause().getMessage());
            }
        });
    }
    // Retrieve
    private void handleRetrieve(RoutingContext routingContext) {

        String txId = routingContext.get("tx_id");
        String roomId = routingContext.request().getHeader("X-ROOM-ID");
        String requester = routingContext.request().getHeader("X-USER-ID");
        String token = routingContext.request().getHeader("X-TOKEN");

        log.debug("txId: {}, roomId: {}, requester: {}, token: {}"
                , txId, roomId, requester, token);

        JsonObject data = new JsonObject();
        data.put("txId",txId);
        data.put("roomId",roomId);
        data.put("requester",requester);
        data.put("token",token);

        vertx.eventBus().<JsonObject>request(Constants.EB_RETRIEVE, data, msg -> {
            if (msg.succeeded()) {
                JsonObject result = msg.result().body();
                log.info("msg -> {}",result);

                int statusCode = (result==null || result.isEmpty())?204:200;
                sendResponse(routingContext, statusCode, result);
            } else {
                sendResponse(routingContext, 500, msg.cause().getMessage());
            }
        });
    }

    private void sendResponse(RoutingContext rc, int statusCode, Object msg){

        rc.response().setStatusCode(statusCode);
        rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");

        JsonObject tempBody = new JsonObject();
        if (msg instanceof String) {
            log.info("msg ->{}",msg);

            tempBody = new JsonObject().put("message", msg);
        } else if (msg instanceof JsonObject) {
            tempBody = (JsonObject) msg;
        } else if (msg instanceof JsonArray) {
            tempBody.put("message", ((JsonArray) msg).getList());
        }
        String body = tempBody.encode();
        if(body!=null && !body.isBlank()) {
            rc.response().putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.getBytes(StandardCharsets.UTF_8).length));
            rc.response().write(body);
        }

        rc.response().end();
        rc.clearUser();
    }

}
