package com.kakao.pay.test.sprinkling_money.repository;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;

import java.util.List;

public abstract class Repository extends AbstractVerticle {

    protected void batch(SQLConnection conn, List<String> sqls, Handler<Void> done) {
        conn.batch(sqls, res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }

            done.handle(null);
        });
    }

    protected void query(SQLConnection conn, String sql, Handler<ResultSet> done) {
        conn.query(sql, res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }

            done.handle(res.result());
        });
    }
    protected void queryWithParams(SQLConnection conn, String sql, JsonArray params, Handler<ResultSet> done) {
        conn.queryWithParams(sql, params, res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }

            done.handle(res.result());
        });
    }

    protected void startTx(SQLConnection conn, Handler<ResultSet> done) {
        conn.setAutoCommit(false, res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }

            done.handle(null);
        });
    }

    protected void endTx(SQLConnection conn, Handler<ResultSet> done) {
        conn.commit(res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }

            done.handle(null);
        });
    }

    protected String format(JsonArray query) {
        String sql = query.getString(0);
        for (int i = 1; i < query.size(); i++) {
            int index = sql.indexOf("?");
            if (query.getValue(i) instanceof Number) {
                sql = sql.substring(0, index) + query.getValue(i) + sql.substring(index + 1);
            } else {
                sql = sql.substring(0, index) + "'" + query.getValue(i) + "'" + sql.substring(index + 1);
            }
        }
        return sql;
    }

}
