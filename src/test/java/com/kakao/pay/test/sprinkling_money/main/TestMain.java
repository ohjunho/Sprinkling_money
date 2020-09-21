package com.kakao.pay.test.sprinkling_money.main;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

// deploy test
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestMain {

  private static String token = "";
  private static String[] receiver = {"o1234","j1234","h1234"};
  private static Integer receiveMoney = 0;


  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new Main(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  @Order(1)
  void verticle_deployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }

  @Test
  @Order(2)
  void restSprinkling(Vertx vertx, VertxTestContext testContext) throws Throwable {
    String json= "{\n" +
            "    \"total_amount\": 10000,\n" +
            "    \"target_count\": 5,\n" +
            "    \"user_list\": [\n" +
            "        \"o1234\",\n" +
            "        \"j1234\",\n" +
            "        \"h1234\",\n" +
            "        \"a1234\",\n" +
            "        \"b1234\",\n" +
            "        \"c1234\",\n" +
            "        \"d1234\"\n" +
            "    ]\n" +
            "}";

    HttpClient httpclient = HttpClients.createDefault();
    HttpPost request = new HttpPost("http://127.0.0.1:8080/v1/sprinkling");
    request.setHeader("X-ROOM-ID", "123");
    request.setHeader("X-USER-ID", "ohjunho");
    StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
    request.setEntity(entity);

    HttpResponse response = httpclient.execute(request);

    int returnStatusCode = response.getStatusLine().getStatusCode();

    assertEquals(returnStatusCode,201);

    HttpEntity httpEntity = response.getEntity();

    String apiOutput = null;

    apiOutput = EntityUtils.toString(httpEntity);

    assertNotNull(apiOutput);

    JsonObject result = new JsonObject(apiOutput);

    token = result.getString("token");

    testContext.completeNow();
  }

  @Test
  @Order(3)
  void restRetrieve(Vertx vertx, VertxTestContext testContext) throws Throwable {

    assertTrue(!token.equals(""));

    HttpClient httpclient = HttpClients.createDefault();
    HttpGet request = new HttpGet("http://127.0.0.1:8080/v1/retrieve");
    request.setHeader("X-ROOM-ID", "123");
    request.setHeader("X-USER-ID", "ohjunho");
    request.setHeader("X-TOKEN", token);

    HttpResponse response = httpclient.execute(request);

    int returnStatusCode = response.getStatusLine().getStatusCode();

    assertEquals(returnStatusCode,200);

    if (returnStatusCode != 204) {
      HttpEntity httpEntity = response.getEntity();
      String apiOutput = EntityUtils.toString(httpEntity);
    }
    testContext.completeNow();
  }

  @Test
  @Order(4)
  void restReceive(Vertx vertx, VertxTestContext testContext) throws Throwable {

    assertTrue(!token.equals(""));

    HttpClient httpclient = HttpClients.createDefault();
    HttpGet request = new HttpGet("http://127.0.0.1:8080/v1/receive");
    request.setHeader("X-ROOM-ID", "123");
    request.setHeader("X-USER-ID", receiver[0]);
    request.setHeader("X-TOKEN", token);

    HttpResponse response = httpclient.execute(request);

    int returnStatusCode = response.getStatusLine().getStatusCode();

    assertTrue(returnStatusCode==200 || returnStatusCode==204);

    if (returnStatusCode == 200) {
      HttpEntity httpEntity = response.getEntity();
      String apiOutput = EntityUtils.toString(httpEntity);

      JsonObject result = new JsonObject(apiOutput);

      receiveMoney = result.getInteger("receiveMoney");

    }
    testContext.completeNow();
  }

  @Test
  @Order(5)
  void restRetrieve2(Vertx vertx, VertxTestContext testContext) throws Throwable {

    assertTrue(!token.equals(""));

    HttpClient httpclient = HttpClients.createDefault();
    HttpGet request = new HttpGet("http://127.0.0.1:8080/v1/retrieve");
    request.setHeader("X-ROOM-ID", "123");
    request.setHeader("X-USER-ID", "ohjunho");
    request.setHeader("X-TOKEN", token);

    HttpResponse response = httpclient.execute(request);

    int returnStatusCode = response.getStatusLine().getStatusCode();

    assertEquals(returnStatusCode,200);

    HttpEntity httpEntity = response.getEntity();
    String apiOutput = EntityUtils.toString(httpEntity);

    JsonObject result = new JsonObject(apiOutput);

    boolean assertValue = false;
    for ( Object u: result.getJsonArray("users")){
      JsonObject user = (JsonObject)u;

      System.out.println("receiveMoney --> "+receiveMoney);
      System.out.println("user.getInteger(receiver[0]) --> "+user.getInteger(receiver[0]));
      if(receiveMoney.equals(user.getInteger(receiver[0]))){
        assertValue=true;
        break;
      }
    }

    assertTrue(assertValue);

    testContext.completeNow();
  }

  @Test
  @Order(6)
  void restReceive2(Vertx vertx, VertxTestContext testContext) throws Throwable {

    assertTrue(!token.equals(""));

    HttpClient httpclient = HttpClients.createDefault();
    HttpGet request = new HttpGet("http://127.0.0.1:8080/v1/receive");
    request.setHeader("X-ROOM-ID", "123");
    request.setHeader("X-USER-ID", receiver[0]);
    request.setHeader("X-TOKEN", token);

    HttpResponse response = httpclient.execute(request);

    int returnStatusCode = response.getStatusLine().getStatusCode();

    assertTrue(returnStatusCode==200 || returnStatusCode==204);

    if (returnStatusCode == 200) {
      HttpEntity httpEntity = response.getEntity();
      String apiOutput = EntityUtils.toString(httpEntity);

      JsonObject result = new JsonObject(apiOutput);

      receiveMoney = result.getInteger("receiveMoney");

    }
    testContext.completeNow();
  }

  @Test
  @Order(7)
  void restRetrieve3(Vertx vertx, VertxTestContext testContext) throws Throwable {

    assertTrue(!token.equals(""));

    HttpClient httpclient = HttpClients.createDefault();
    HttpGet request = new HttpGet("http://127.0.0.1:8080/v1/retrieve");
    request.setHeader("X-ROOM-ID", "123");
    request.setHeader("X-USER-ID", "ohjunho");
    request.setHeader("X-TOKEN", token);

    HttpResponse response = httpclient.execute(request);

    int returnStatusCode = response.getStatusLine().getStatusCode();

    assertEquals(returnStatusCode,200);

    HttpEntity httpEntity = response.getEntity();
    String apiOutput = EntityUtils.toString(httpEntity);

    JsonObject result = new JsonObject(apiOutput);

    boolean assertValue = false;
    for ( Object u: result.getJsonArray("users")){
      JsonObject user = (JsonObject)u;

      System.out.println("receiveMoney --> "+receiveMoney);
      System.out.println("user.getInteger(receiver[0]) --> "+user.getInteger(receiver[0]));
      if(receiveMoney.equals(user.getInteger(receiver[0]))){
        assertValue=true;
        break;
      }
    }
    assertTrue(assertValue);

    testContext.completeNow();
  }
}
