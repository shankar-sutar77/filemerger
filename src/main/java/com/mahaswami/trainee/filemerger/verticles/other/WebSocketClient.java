package com.mahaswami.trainee.filemerger.verticles;

import io.vertx.core.*;
import io.vertx.core.http.*;
import io.vertx.core.json.*;
import java.util.*;

public class WebSocketClient extends AbstractVerticle {

    public void start() {
      System.out.println("WebSocketClient deployed......>!");
      HttpClient client = vertx.createHttpClient();

      client.websocket("/", websocket -> {
        System.out.println("Connected!");
      });
    }

}
