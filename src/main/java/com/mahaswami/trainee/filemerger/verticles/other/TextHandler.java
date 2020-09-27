package com.mahaswami.trainee.filemerger.verticles;

import io.vertx.core.*;
import io.vertx.core.http.*;
import io.vertx.core.buffer.*;

public class TextHandler extends AbstractVerticle {

public void start(Future<Void> fut) {
    System.out.println("TextHandler deployed.....>>>!");
    vertx.createHttpServer().websocketHandler(new Handler<ServerWebSocket>() {
      public void handle(final ServerWebSocket ws) {
        final String id = ws.textHandlerID();
        System.out.println("new connection from"+ ws.toString() + "id "+id);

        vertx.eventBus().consumer(id, message -> {
          ws.writeFinalTextFrame((String) message.body());
        });

        ws.handler(new Handler<Buffer>() {
          public void handle(Buffer data) {
            // When our websocket receive data we publish it to our consumer
            System.out.println("RECEVIED ::" + data);
            vertx.eventBus().send(id, "FROM SERVER===>" + data.toString());
          }
        });

        ws.closeHandler(handler -> {
          System.out.println("Close WS ");
        });


      }}
        ).requestHandler(new Handler<HttpServerRequest>() {
          public void handle(HttpServerRequest req) {
            req.response().end("Chat");
            //Not usefull but it display chat on our browser
          }
        }).listen(3002);
  }
}
