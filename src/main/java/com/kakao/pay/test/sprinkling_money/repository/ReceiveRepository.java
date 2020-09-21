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

public class ReceiveRepository extends Repository {

    private Logger log = LoggerFactory.getLogger(ReceiveRepository.class);
    private JDBCClient client;

    private final static String user_sql = "SELECT \n" +
            "\ts.ID, u.AMOUNT \n" +
            "FROM \n" +
            "\tSPRINKLING s\n" +
            "JOIN\n" +
            "\tSPRINKLING_USER u \n" +
            "ON \n" +
            "\ts.id=u.SPRINKLING_ID \n" +
            "\tAND s.ROOM_ID =?\n" +
            "\tAND s.TOKEN = ?\n" +
            "\tAND u.USER_ID = ?\n" +
            "\tAND created_time > DATE_ADD(now, INTERVAL -10 MINUTE)";

    private final static String money_sql = "SELECT \n" +
            "\tAMOUNT\n" +
            "FROM \n" +
            "\tSPRINKLING s\n" +
            "JOIN\n" +
            "\tSPRINKLING_DIVIDED_MONEY m \n" +
            "ON \n" +
            "\ts.id=m.SPRINKLING_ID \n" +
            "\tAND s.ROOM_ID =?\n" +
            "\tAND s.TOKEN = ?\n" +
            "\tAND created_time > DATE_ADD(now, INTERVAL -10 MINUTE)";

    @Override
    public void start() throws Exception {

        // get Config
        client = JDBCClient.createShared(vertx, config().getJsonObject("database").getJsonObject("jdbc"));
        // receive data
        vertx.eventBus().consumer(Constants.EB_RECEIVE_USER_REPO, this::receiveUserHandler);
        vertx.eventBus().consumer(Constants.EB_RECEIVE_MONEY_REPO, this::receiveMoneyHandler);
        vertx.eventBus().consumer(Constants.EB_RECEIVE_SAVE_REPO, this::saveHandler);

    }

    private void receiveUserHandler(Message<JsonObject> msg) {
        try {
            JsonObject data = msg.body();

            JsonArray params = new JsonArray();
            params.add(data.getString("roomId"));
            params.add(data.getString("token"));
            params.add(data.getString("receiver"));

            client.getConnection(conn -> {
                if (conn.failed()) {
                    msg.fail(3000, conn.cause().getMessage());
                    return;
                }

                queryWithParams(conn.result(),
                        user_sql, params, rs -> {
                            List<JsonArray> rsSprinkling = rs.getResults();
                            if(rsSprinkling.size()>0) {
                                msg.reply(rsSprinkling.get(0));
                            }else{
                                msg.reply(null);
                            }
                            // and close the connection
                            conn.result().close(done -> {
                                if (done.failed()) {
                                    throw new RuntimeException(done.cause());
                                }
                            });
                        });
            });
        } catch (Exception e) {
            msg.fail(3001, e.getMessage());
        }
    }

    private void receiveMoneyHandler(Message<JsonObject> msg) {
        try {
            JsonObject data = msg.body();

            JsonArray params = new JsonArray();
            params.add(data.getString("roomId"));
            params.add(data.getString("token"));

            client.getConnection(conn -> {
                if (conn.failed()) {
                    msg.fail(3000, conn.cause().getMessage());
                    return;
                }

                queryWithParams(conn.result(),
                        money_sql, params, rs -> {
                            List<JsonArray> rsSprinkling = rs.getResults();
                            JsonArray retrieveData = new JsonArray();
                            rsSprinkling.forEach(r->{
                                retrieveData.add(r.getInteger(0));
                            });

                            msg.reply(retrieveData);
                            // and close the connection
                            conn.result().close(done -> {
                                if (done.failed()) {
                                    throw new RuntimeException(done.cause());
                                }
                            });
                        });
            });
        } catch (Exception e) {
            msg.fail(3001, e.getMessage());
        }
    }

    private void saveHandler(Message<JsonObject> msg) {
        try {
            JsonObject data = msg.body();

            JsonArray sql = makeQuery(data);

            client.getConnection(conn -> {
                if (conn.failed()) {
                    msg.fail(2003,conn.cause().getMessage());
                    return;
                }

                // transaction start
                startTx(conn.result(), beginTrans -> {
                    batch(conn.result(), sql.getList(), insert -> {
                        // commit data
                        endTx(conn.result(), commitTrans -> {

                            msg.reply(null);
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

        } catch (Exception e) {
            msg.fail(3001, e.getMessage());
        }
    }

    private JsonArray makeQuery(JsonObject data){

        String user_update="UPDATE SPRINKLING_USER SET AMOUNT=? WHERE SPRINKLING_ID=? AND USER_ID=?";
        String money_delete="DELETE FROM SPRINKLING_DIVIDED_MONEY WHERE SPRINKLING_ID=? AND AMOUNT=?";

        //query 생성
        JsonArray queryArray = new JsonArray();
        queryArray.add(new JsonArray().add(user_update).add(data.getInteger("receiveMoney")).add(data.getInteger("sprinkling_id")).add(data.getString("receiver")));
        queryArray.add(new JsonArray().add(money_delete).add(data.getInteger("sprinkling_id")).add(data.getInteger("receiveMoney")));

        JsonArray querys = new JsonArray();
        JsonArray srcQuerys = queryArray;

        for (Object rs : srcQuerys) {
            JsonArray src = (JsonArray) rs;
            querys.add(format(src));
        }

        log.info("querys -> {}", querys.encodePrettily());
        return querys;
    }
}
