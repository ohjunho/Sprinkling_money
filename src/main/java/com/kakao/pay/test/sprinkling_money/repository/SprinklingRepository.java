package com.kakao.pay.test.sprinkling_money.repository;

import com.kakao.pay.test.sprinkling_money.common.Constants;
import com.kakao.pay.test.sprinkling_money.common.Utility;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SprinklingRepository extends Repository {

    private Logger log = LoggerFactory.getLogger(SprinklingRepository.class);
    private JDBCClient client;

    @Override
    public void start() throws Exception {

        // get Config
        client = JDBCClient.createShared(vertx, config().getJsonObject("database").getJsonObject("jdbc"));

        // getToken
        vertx.eventBus().consumer(Constants.EB_SPRINKLING_GET_TOKEN_REPO, this::getTokenHandler);

        // receive data
        vertx.eventBus().consumer(Constants.EB_SPRINKLING_REPO, this::sprinklingHandler);

    }

    private void getTokenHandler(Message<JsonObject> msg) {
        try {
            JsonObject data = msg.body();

            JsonArray params = new JsonArray();
            params.add(data.getString("roomId"));

            String sql = "SELECT \n" +
                    "\tTOKEN\n" +
                    "FROM \n" +
                    "\tSPRINKLING s\n" +
                    "where\n" +
                    "\tROOM_ID = ?\n" +
                    "\tAND created_time > DATE_ADD(current_date, INTERVAL -7 day)";

            client.getConnection(conn -> {
                if (conn.failed()) {
                    msg.fail(2001,conn.cause().getMessage());
                    return;
                }
                // create a test table
                queryWithParams(conn.result(), sql, params, rs -> {
                    List<JsonArray> rsToken = rs.getResults();
                    JsonArray retrieveData = Utility.list2JsonArray(rsToken);

                    msg.reply(retrieveData);

                    // and close the connection
                    conn.result().close(done -> {
                        if (done.failed()) {
                            throw new RuntimeException(done.cause());
                        }
                    });
                });
            });
        }catch(Exception e){
            msg.fail(2002,e.getMessage());
        }
    }
    private void sprinklingHandler(Message<JsonObject> msg) {
        try {
            JsonObject data = msg.body();

            client.getConnection(conn -> {
                if (conn.failed()) {
                    msg.fail(2003,conn.cause().getMessage());
                    return;
                }

                // create a test table
                query(conn.result(), "SELECT ID FROM SPRINKLING order by ID desc limit 1", getId -> {
                    Long tid = 0l;
                    if(getId.getResults()==null || getId.getResults().size()<1) {
                        tid = 1l;
                    }else{
                        tid = getId.getResults().get(0).getLong(0) + 1;
                    }
                    final Long id = tid;
                    // transaction start
                    startTx(conn.result(), beginTrans -> {
                        // make querys
                        JsonArray queryArray = makeQuery(data, id);

                        batch(conn.result(), queryArray.getList(), insert -> {
                            // commit data
                            endTx(conn.result(), commitTrans -> {

                                msg.reply(id);
                                // and close the connection
                                conn.result().close(done -> {
                                    if (done.failed()) {
                                        throw new RuntimeException(done.cause());
                                    }
                                });

                            });
                        });
                    });
                });
            });
        }catch(Exception e){
            msg.fail(2004,e.getMessage());
        }
    }

    private JsonArray makeQuery(JsonObject data, Long id){
        //query 생성
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(new JsonArray().add("INSERT INTO PUBLIC.PUBLIC.SPRINKLING (ID, ROOM_ID, REQUESTER, TOKEN, TOTAL_AMOUNT, TARGET_COUNT) VALUES(?, ?, ?, ?, ?, ?)")
                .add(id)
                .add(data.getString("roomId"))
                .add(data.getString("requester"))
                .add(data.getString("token"))
                .add(data.getInteger("total_amount"))
                .add(data.getInteger("target_count")));
        data.getJsonArray("user_list").forEach(user -> jsonArray.add(new JsonArray().add("INSERT INTO PUBLIC.PUBLIC.SPRINKLING_USER (SPRINKLING_ID, USER_ID, AMOUNT) VALUES(?,?,?)")
                .add(id).add(user).add(0)));
        data.getJsonArray("divided_money").forEach(money -> jsonArray.add(new JsonArray().add("INSERT INTO PUBLIC.PUBLIC.SPRINKLING_DIVIDED_MONEY (SPRINKLING_ID, AMOUNT) VALUES(?, ?)")
                .add(id).add(money)));

        JsonArray querys = new JsonArray();
        JsonArray srcQuerys = jsonArray;

        for (Object rs : srcQuerys) {
            JsonArray src = (JsonArray) rs;
            querys.add(format(src));
        }

        log.info("querys -> {}", querys.encodePrettily());
        return querys;
    }
}
