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

public class RetrieveRepository extends Repository {

    private Logger log = LoggerFactory.getLogger(RetrieveRepository.class);
    private JDBCClient client;

    private final static String sql = "SELECT \n" +
            "\tTOTAL_AMOUNT, TARGET_COUNT, CREATED_TIME, u.user_id,u.AMOUNT \n" +
            "FROM \n" +
            "\tSPRINKLING s\n" +
            "JOIN\n" +
            "\tsprinkling_user u \n" +
            "ON \n" +
            "\ts.id=u.SPRINKLING_ID \n" +
            "\tAND s.ROOM_ID =?\n" +
            "\tAND s.REQUESTER =?\n" +
            "\tAND s.TOKEN = ?\n" +
            "\tAND created_time > DATE_ADD(current_date, INTERVAL -7 day)";

    @Override
    public void start() throws Exception {

        // get Config
        client = JDBCClient.createShared(vertx, config().getJsonObject("database").getJsonObject("jdbc"));
        // receive data
        vertx.eventBus().consumer(Constants.EB_RETRIEVE_REPO, this::retrieveHandler);

    }

    private void retrieveHandler(Message<JsonObject> msg) {
        try {
            JsonObject data = msg.body();

            JsonArray params = new JsonArray();
            params.add(data.getString("roomId"));
            params.add(data.getString("requester"));
            params.add(data.getString("token"));

            client.getConnection(conn -> {
                if (conn.failed()) {
                    msg.fail(3000, conn.cause().getMessage());
                    return;
                }

                queryWithParams(conn.result(),
                        sql, params, rs -> {
                            List<JsonArray> rsSprinkling = rs.getResults();
                            JsonArray retrieveData = Utility.list2JsonArray(rsSprinkling);

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

}
