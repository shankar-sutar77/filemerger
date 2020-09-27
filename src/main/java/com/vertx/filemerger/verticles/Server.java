package com.vertx.filemerger.verticles;

import io.vertx.core.*;
import io.vertx.core.http.*;
import io.vertx.core.json.*;
import java.util.logging.*;

import io.vertx.core.shareddata.LocalMap;

public class Server extends AbstractVerticle {

  ServerWebSocket myWebsocket;
  private String textHandlerID;
  private static Logger logger = Logger.getLogger(Server.class.getName());
  private  LocalMap<String, JsonArray>  users_status;
  private Database database;

  @Override
  public void start() {
    database = new Database(vertx, config());
    users_status = vertx.sharedData().getLocalMap("users");
    HttpServer server = vertx.createHttpServer();
    server.websocketHandler(webSocket -> {
      logger.info(() -> "GOT NEW CONNECTION WITH :: " + webSocket.textHandlerID());
      this.createConsumerHandler(webSocket, webSocket.textHandlerID());
      webSocket.textMessageHandler( text -> {
        JsonObject request = new JsonObject(text);
        logger.info(()-> "REQUEST ACTION ====> "+ request.getString("action"));
        String action = request.getString("action");

        switch(action) {
          case "login"      : this.loginValidate(webSocket, request);break;
          case "logout"     : this.logout(webSocket, request);break;
          case "getUsers"   : this.getUsers(webSocket, request);break;
          case "getFiles"   : this.getFiles(webSocket, request, webSocket.textHandlerID());break;
          case "deleteMedia": this.deleteMedia(webSocket, request, webSocket.textHandlerID());break;
          case "merge"      : request.put("textHandlerID", webSocket.textHandlerID());
                            vertx.eventBus().send("media.process.fragments",request.encode());
                            break;
          default : logger.info(() -> "NO REQUEST MATCH...!");
        }
      });
    });

    server.listen(3001, res -> {
      if (res.succeeded()) {
        logger.info(() -> "Server is now listening on port 3001...!");
      } else {
        logger.info(() ->"Failed to bind!");
      }
    });
  }


  void deleteMedia(ServerWebSocket webSocket, JsonObject request,String textHandlerID) {
    vertx.eventBus().consumer(textHandlerID, message -> {
      webSocket.writeTextMessage(message.body().toString());
      webSocket.close();
    });
    String username = request.getString("username");
    int mediaID = request.getInteger("mediaID");
    database.updateQuery("delete from media where receiver ='" + username + "' and id=" +mediaID, delete -> {
        vertx.eventBus().send(textHandlerID, delete.toString());
    });
  }

  void getFiles(ServerWebSocket webSocket, JsonObject request,String textHandlerID) {
    vertx.eventBus().consumer(textHandlerID, message -> {
      webSocket.writeTextMessage(message.body().toString());
      webSocket.close();
    });
    String username = request.getString("username");
    database.selectQuery("select *from media where receiver ='" + username + "' and status='done'", list -> {
        vertx.eventBus().send(textHandlerID, list.getRows().toString());
    });
  }

  void createConsumerHandler(ServerWebSocket webSocket, String textHandlerID) {
    try {
      vertx.eventBus().consumer(textHandlerID, message -> {
        logger.info(() -> "Inside createConsumerHandler...............!");
        JsonObject ack = new JsonObject(message.body().toString());
        webSocket.writeTextMessage(ack.toString());
        webSocket.close();
      });
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  void loginValidate(ServerWebSocket websocket, JsonObject request) {
    try {
      String username = request.getString("username");
      JsonObject loginResponse = new JsonObject();
      if(users_status.get("users") != null) {
        JsonArray avalableUsers = users_status.get("users");
        System.out.println("------------------------------\n"+avalableUsers);
        if(avalableUsers.contains(username) == false) {
          avalableUsers.add(username);
          users_status.put("users",avalableUsers);
          loginResponse.put("isValid",true);
        } else {
          loginResponse.put("isValid",true);
        }
      } else {
        JsonArray newUser = new JsonArray().add(username);
        users_status.put("users",newUser);
        loginResponse.put("isValid",true);
      }
      websocket.writeTextMessage(loginResponse.toString());
      websocket.close();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  void logout(ServerWebSocket webSocket, JsonObject request) {
    try {
      System.out.println(request);
      JsonObject logoutResponse = new JsonObject();
      if(users_status.get("users") != null) {
      JsonArray avalableUsers = users_status.get("users");
      String username = request.getString("username");
      if(avalableUsers.contains(username)) {
        avalableUsers.remove(username);
        users_status.put("users",avalableUsers);
        logoutResponse.put("isLogout",true);
      } else {
        logoutResponse.put("isLogout",false);
      }
      webSocket.writeTextMessage(logoutResponse.toString());
      webSocket.close();
    } else {
      logoutResponse.put("isLogout",false);
      webSocket.writeTextMessage(logoutResponse.toString());
      webSocket.close();
    }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  void getUsers(ServerWebSocket webSocket, JsonObject request) {
    if(users_status.get("users") != null) {
      JsonArray avalableUsers = users_status.get("users");
      if(avalableUsers.contains(request.getString("username"))) {
        avalableUsers.remove(request.getString("username"));
      }
      webSocket.writeTextMessage(avalableUsers.toString());
      webSocket.close();
    }
  }
}
