package com.vertx.filemerger.verticles.other;

import io.vertx.core.*;
import io.vertx.core.http.*;
import io.vertx.core.json.*;

public class Client extends AbstractVerticle {

  @Override
  public void start() {
    System.out.println("SERVER STARTED...!");
    HttpServer server = vertx.createHttpServer();
    server.websocketHandler(websocket -> {
      System.out.println("Connected!");
      websocket.textMessageHandler( text -> {
        System.out.println("FROM CLIENT===>" + text);
        JsonObject fragmentInfo = new JsonObject(text);
        System.out.println("FILENAME+=>"+fragmentInfo.getString("fileName"));
      });

      websocket.writeTextMessage("FROM SERVER");
    });

    server.listen(1234, "192.168.0.135", res -> {
      if (res.succeeded()) {
        System.out.println("Server is now listening!");
      } else {
        System.out.println("Failed to bind!");
      }
    });

  }
}
