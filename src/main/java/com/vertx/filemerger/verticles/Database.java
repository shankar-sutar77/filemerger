package com.vertx.filemerger.verticles;
import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.core.json.JsonObject;
import java.util.logging.*;


class Database {
  private SQLClient client = null;
  private static Logger logger = Logger.getLogger(Database.class.getName());

  public Database(Vertx vertx, JsonObject config) {
    client = JDBCClient.createShared(vertx, config.getJsonObject("jdbcconfig"));
  }

  public void selectQuery(String statement, GenericCallback<ResultSet> callback) {
    client.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        try {
          connection.query(statement, select -> {
            if (select.succeeded()) {
              callback.perform(select.result());
            } else {
              logger.severe(() -> "ERROR IN SELECT QUERY " + select.cause());
            }
          });
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          connection.close();
        }
      } else {
        logger.severe(() -> "ERROR IN GETTING CONNECTION TO DATABASE " + res.cause());
      }
    });
  }

  public void updateQuery(String statement, GenericCallback<Boolean> callback) {
    client.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        try {
          connection.update(statement, updtae -> {
            if (updtae.succeeded()) {
              callback.perform(true);
            } else {
              callback.perform(false);
              logger.severe(() -> "ERROR IN UPDATE QUERY " + updtae.cause());
            }
          });
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          connection.close();
        }
      } else {
        logger.severe(() -> "ERROR IN GETTING CONNECTION TO DATABASE " + res.cause());
      }
    });
  }


  public void insertQuery(String statement, GenericCallback<Boolean> callback) {
    client.getConnection(res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        try {
          connection.execute(statement, insert -> {
            if (insert.succeeded()) {
              callback.perform(true);
            } else {
              logger.severe(() -> "ERROR IN INSERT QUERY " + insert.cause());
            }
          });
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          connection.close();
        }
      } else {
        logger.severe(() -> "ERROR IN GETTING CONNECTION TO DATABASE " + res.cause());
      }
    });
  }
}
