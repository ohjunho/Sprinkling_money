package com.kakao.pay.test.sprinkling_money.main;

import com.kakao.pay.test.sprinkling_money.common.Constants;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.ArrayList;
import java.util.List;

public class Main extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args){
        Vertx.vertx().deployVerticle(Main.class.getName(), completionHandler -> {
            if (completionHandler.failed()) {
                System.exit(0);
            }
        });
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        // logger 설정
        setLogger();

        // 디플로이 리스트 읽기
        JsonArray deployList = vertx.fileSystem().readFileBlocking(Constants.COMM_DEPLOY_LIST_FILE).toJsonArray();
        // cluster manager 설정
        ClusterManager mgr = new HazelcastClusterManager();

        // 옵션 설정
        VertxOptions options = new VertxOptions()
                .setClusterManager(mgr)
                .setHAEnabled(true);
        // cluster mode로 시작
        Vertx.clusteredVertx(options, res -> {
            if (res.succeeded()) {
                Vertx vertx = res.result();

                // deploy 시작
                Future<String> result = deployProcess(deployList);

                result.onComplete(r->{
                    if (r.failed()) {
                        log.error("Failed to deploy normally. " + r.cause().getMessage());
                        startPromise.fail(r.cause());
                        System.exit(0);
                    }else{
                        startPromise.complete();
                    }
                });

            } else {
                // failed!
                log.error(res.cause().getMessage());
                startPromise.fail(res.cause());
                System.exit(0);
           }
        });

    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        stop();
        stopPromise.complete();
    }

    private Future<String>  deployProcess(JsonArray deployList) {

        Promise<String> promise = Promise.promise();

        List<Future> deployVerticles = new ArrayList<>();
        // 디플로이 대상을 리스트에 저장
        deployList.forEach(item ->{
            JsonObject verticle = (JsonObject) item;
            deployVerticles.add(deployItem(verticle.getString("class"), verticle.getJsonObject("options")));
        });

        // 디플로이가 모두 성공한다면...
        CompositeFuture.all(deployVerticles).setHandler(result -> {
            if (result.succeeded()) {
                result.result().list().stream().forEach(r -> {
                    log.info("deployed -> " + r);
                });
                promise.complete("ok");
            } else {
                log.error("deploy fail -> " + result.cause().getMessage());
                promise.fail(result.cause());
            }
        });

        return promise.future();
    }

    private Future<String> deployItem(String clazz, JsonObject config) {
        Promise<String> promise = Promise.promise();

        DeploymentOptions opt = new DeploymentOptions(config);
        if (opt.getInstances()<0) {
            // instance 생성 옵션이 0보다 작으면 기본값 사용
            opt.setInstances(Runtime.getRuntime().availableProcessors());
        }
        // 개별 디플로이
        vertx.deployVerticle(clazz, opt, id -> {
            if (id.succeeded()) {
                log.info(clazz + " started: " + id.result());
                promise.complete(id.result());
            } else {
                log.error("Server failed to start: " + id.cause().getMessage());
                promise.fail(id.cause());
            }
        });

        return promise.future();
    }

    private void setLogger() {
        // Redirect JUL logging to slf4j
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
}
